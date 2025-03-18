package com.twofauth.android.main_activity;

import android.util.Log;

import androidx.annotation.Nullable;

import com.twofauth.android.BaseActivity;
import com.twofauth.android.Constants;
import com.twofauth.android.StringUtils;
import com.twofauth.android.main_service.ServerDataLoader;
import com.twofauth.android.main_service.ServerDataLoader.TwoAuthLoadedData;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DataLoader extends Thread {
    public interface OnDataLoadListener {
        public abstract void onDataLoadSuccess(TwoAuthLoadedData data);
        public abstract void onDataLoadError();
    }

    private class DataLoaderDisplayer implements Runnable, BaseActivity.SynchronizedCallback {
        private boolean mSuccess;
        private final TwoAuthLoadedData mTwoAuthLoadedData;

        private final List<String> mGroups;

        DataLoaderDisplayer(final boolean success, @Nullable final TwoAuthLoadedData two_auth_loaded_data, @Nullable final List<String> groups) {
            mSuccess = success;
            mTwoAuthLoadedData = two_auth_loaded_data;
            mGroups = groups;
        }

        @Override
        public Object synchronizedCode(Object object)
        {
            mAccountsListAdapter.setItems(mTwoAuthLoadedData == null ? null : mTwoAuthLoadedData.accounts);
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
                    mListener.onDataLoadSuccess(mTwoAuthLoadedData);
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
        List<String> groups = null;
        if (items != null) {
            groups = new ArrayList<String>();
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
        TwoAuthLoadedData two_auth_loaded_data = null;
        List<String> groups = null;
        try {
            two_auth_loaded_data = ServerDataLoader.getTwoFactorAuthCodes(mActivity);
            groups = getGroups(two_auth_loaded_data.accounts);
            success = true;
        }
        catch (Exception e) {
            Log.e(Constants.LOG_TAG_NAME, "Exception while trying to display data", e);
        }
        finally {
            if ((! Thread.interrupted()) && (! mActivity.isFinishedOrFinishing())) {
                mActivity.runOnUiThread(new DataLoaderDisplayer(success, two_auth_loaded_data, groups));
            }
        }
    }
}
