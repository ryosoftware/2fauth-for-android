package com.twofauth.android.main_activity;

import android.util.Log;


import com.twofauth.android.BaseActivity;
import com.twofauth.android.Constants;
import com.twofauth.android.StringUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DataFilterer extends Thread
{
    public interface OnDataFilteredListener {
        public abstract void onDataFilterSuccess(boolean any_filter_applied);
        public abstract void onDataFilterError();
    }

    private class DataFiltererDisplayer implements Runnable, BaseActivity.SynchronizedCallback {
        private boolean mSuccess;
        private final List<JSONObject> mAccounts;

        DataFiltererDisplayer(final boolean success, @Nullable List<JSONObject> accounts) {
            mSuccess = success;
            mAccounts = accounts;
        }

        @Override
        public Object synchronizedCode(Object object)
        {
            mAccountsListAdapter.setItems(mAccounts);
            mGroupsListAdapter.setActiveGroup(mActiveGroup);
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
                Log.e(Constants.LOG_TAG_NAME, "Exception while trying to display filtered data", e);
            }
            finally {
                if (mSuccess) {
                    mListener.onDataFilterSuccess((! StringUtils.isEmptyOrNull(mActiveGroup)) || (! StringUtils.isEmptyOrNull(mText)));
                }
                else {
                    mListener.onDataFilterError();
                }
            }
        }
    }
    private final BaseActivity mActivity;
    private final AccountsListAdapter mAccountsListAdapter;

    private final GroupsListAdapter mGroupsListAdapter;

    private final List<JSONObject> mAccounts;
    private final String mActiveGroup;

    private final String mText;

    private final OnDataFilteredListener mListener;

    public DataFilterer(@NotNull final BaseActivity activity, @NotNull final AccountsListAdapter accounts_list_adapter, @NotNull final GroupsListAdapter groups_list_adapter, @NotNull final List<JSONObject> accounts, @Nullable final String active_group, @Nullable final String text, @NotNull final OnDataFilteredListener listener) {
        mActivity = activity;
        mAccountsListAdapter = accounts_list_adapter;
        mGroupsListAdapter = groups_list_adapter;
        mAccounts = accounts;
        mActiveGroup = active_group;
        mText = text;
        mListener = listener;
    }

    private boolean isVisible(JSONObject object) {
        if ((mActiveGroup == null) || (mActiveGroup.equals(object.optString(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_GROUP_KEY)))) {
            if ((mText != null) && (! mText.isEmpty())) {
                boolean in = false;
                if (StringUtils.in(object.optString(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_SERVICE_KEY, ""), mText, true)) {
                    in = true;
                }
                else if (StringUtils.in(object.optString(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_ACCOUNT_KEY, ""), mText, true)) {
                    in = true;
                }
                return in;
            }
            return true;
        }
        return false;
    }
    private List<JSONObject> getVisibleAccounts() {
        List<JSONObject> items = new ArrayList<JSONObject>();
        for (JSONObject object : mAccounts) {
            if (Thread.interrupted()) {
                return null;
            }
            else if (isVisible(object)) {
                items.add(object);
            }
        }
        return items;
    }

    public void run() {
        boolean success = false;
        List<JSONObject> accounts = null;
        try {
            accounts = getVisibleAccounts();
            success = true;
        }
        catch (Exception e) {
            Log.e(Constants.LOG_TAG_NAME, "Exception while trying to filter data", e);
        }
        finally {
            if ((! Thread.interrupted()) && (! mActivity.isFinishedOrFinishing())) {
                mActivity.runOnUiThread(new DataFiltererDisplayer(success, accounts));
            }
        }
    }
}
