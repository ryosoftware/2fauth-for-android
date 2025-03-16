package com.twofauth.android.main_activity;

import android.util.Log;

import androidx.annotation.Nullable;

import com.twofauth.android.BaseActivity;
import com.twofauth.android.Constants;
import com.twofauth.android.StringUtils;
import com.twofauth.android.main_service.ServerDataLoader;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DataLoader extends Thread {
    public interface OnDataLoadListener {
        public abstract void onDataLoadSuccess(List<JSONObject> items);
        public abstract void onDataLoadError();
    }

    private class DataLoaderDisplayer implements Runnable, BaseActivity.SynchronizedCallback {
        private boolean mSuccess;
        private final List<JSONObject> mAccounts;

        private final List<String> mGroups;

        DataLoaderDisplayer(final boolean success, @Nullable final List<JSONObject> accounts, @Nullable final List<String> groups) {
            mSuccess = success;
            mAccounts = accounts;
            mGroups = groups;
        }

        @Override
        public Object synchronizedCode(Object object)
        {
            mAccountsListAdapter.setItems(mAccounts);
            mGroupsListAdapter.setItems(mGroups);
            return null;
        }

        @Override
        public void run() {
            try {
                if (mSuccess) {
                    mActivity.executeSynchronized(this, null);
                }
            }
            catch (Exception e) {
                mSuccess = false;
                Log.e(Constants.LOG_TAG_NAME, "Exception while trying to display data", e);
            }
            finally {
                if (mSuccess) {
                    mListener.onDataLoadSuccess(mAccounts);
                }
                else {
                    mListener.onDataLoadError();
                }
            }
        }
    }

    private final BaseActivity mActivity;

    private final AccountsListAdapter mAccountsListAdapter;
    private final GroupsListAdapter mGroupsListAdapter;

    private final OnDataLoadListener mListener;

    public DataLoader(@NotNull final BaseActivity activity, @NotNull final AccountsListAdapter accounts_list_adapter, @NotNull final GroupsListAdapter groups_list_adapter, @NotNull OnDataLoadListener listener) {
        mActivity = activity;
        mAccountsListAdapter = accounts_list_adapter;
        mGroupsListAdapter = groups_list_adapter;
        mListener = listener;
    }

    private List<String> getGroups(@Nullable final List<JSONObject> items) {
        final List<String> groups = new ArrayList<String>();
        if (items != null) {
            for (JSONObject object : items) {
                final String group = object.optString(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_GROUP_KEY);
                if (Thread.interrupted()) {
                    return null;
                }
                else if ((! group.isEmpty()) && (! groups.contains(group))) {
                    groups.add(group);
                }
            }
            groups.sort(new Comparator<String>() {
                @Override
                public int compare(String string_1, String string_2) {
                    return StringUtils.compare(string_1, string_2, true);
                }
            });
        }
        return groups;
    }

    public void run() {
        boolean success = false;
        List<JSONObject> accounts = null;
        List<String>groups = null;
        try {
            accounts = ServerDataLoader.getTwoFactorAuthCodes(mActivity);
            groups = getGroups(accounts);
            success = true;
        }
        catch (Exception e) {
            Log.e(Constants.LOG_TAG_NAME, "Exception while trying to display data", e);
        }
        finally {
            if ((! Thread.interrupted()) && (! mActivity.isFinishedOrFinishing())) {
                mActivity.runOnUiThread(new DataLoaderDisplayer(success, accounts, groups));
            }
        }
    }
}
