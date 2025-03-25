package com.twofauth.android.main_activity.tasks;

import android.util.Log;


import com.twofauth.android.BaseActivity;
import com.twofauth.android.Constants;
import com.twofauth.android.Main;
import com.twofauth.android.StringUtils;
import com.twofauth.android.Database.TwoFactorAccount;
import com.twofauth.android.main_activity.AccountsListAdapter;
import com.twofauth.android.main_activity.GroupsListAdapter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DataFilterer
{
    public interface OnDataFilteredListener {
        public abstract void onDataFilterSuccess(boolean any_filter_applied);
        public abstract void onDataFilterError();
    }

    private static class DataFiltererImplementation implements BaseActivity.SynchronizedCallback, Main.OnBackgroundTaskExecutionListener {
        private final BaseActivity mActivity;

        private final AccountsListAdapter mAccountsListAdapter;
        private final GroupsListAdapter mGroupsListAdapter;

        private final List<TwoFactorAccount> mAccounts;

        private final String mActiveGroup;
        private final String mText;

        private final OnDataFilteredListener mListener;

        private List<TwoFactorAccount> mFilteredAccounts = null;
        private boolean mSuccess = false;

        public DataFiltererImplementation(@NotNull final BaseActivity activity, @NotNull final AccountsListAdapter accounts_list_adapter, @NotNull final GroupsListAdapter groups_list_adapter, @NotNull final List<TwoFactorAccount> accounts, @Nullable final String active_group, @Nullable final String text, @NotNull final OnDataFilteredListener listener) {
            mActivity = activity;
            mAccountsListAdapter = accounts_list_adapter;
            mGroupsListAdapter = groups_list_adapter;
            mAccounts = accounts;
            mActiveGroup = active_group;
            mText = text;
            mListener = listener;
        }

        private boolean isVisible(@NotNull final TwoFactorAccount account) {
            if ((mActiveGroup == null) || (mActiveGroup.equals(account.getGroup() == null ? null : account.getGroup().name))) {
                if ((mText != null) && (! mText.isEmpty())) {
                    boolean in = false;
                    if (StringUtils.in(account.getService(), mText, true)) {
                        in = true;
                    }
                    else if (StringUtils.in(account.getUser(), mText, true)) {
                        in = true;
                    }
                    return in;
                }
                return true;
            }
            return false;
        }

        private List<TwoFactorAccount> filterAccounts() {
            List<TwoFactorAccount> accounts = new ArrayList<TwoFactorAccount>();
            for (TwoFactorAccount account : mAccounts) {
                if (isVisible(account)) {
                    accounts.add(account);
                }
            }
            return accounts;
        }

        public Object onBackgroundTaskStarted(@Nullable final Object data) {
            try {
                mFilteredAccounts = filterAccounts();
                mSuccess = true;
            }
            catch (Exception e) {
                Log.e(Constants.LOG_TAG_NAME, "Exception while trying to filter data", e);
            }
            return null;
        }

        @Override
        public Object synchronizedCode(@Nullable final Object object)
        {
            mAccountsListAdapter.setItems(mFilteredAccounts);
            mGroupsListAdapter.setActiveGroup(mActiveGroup);
            return null;
        }

        @Override
        public void onBackgroundTaskFinished(@Nullable final Object data) {
            if (! mActivity.isFinishedOrFinishing()) {
                try {
                    if (mSuccess) {
                        mActivity.executeSynchronized(this);
                    }
                }
                catch (Exception e) {
                    Log.e(Constants.LOG_TAG_NAME, "Exception while trying to display filtered data", e);
                    mSuccess = false;
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
    }

    public static Thread getBackgroundTask(@NotNull final BaseActivity activity, @NotNull final AccountsListAdapter accounts_list_adapter, @NotNull final GroupsListAdapter groups_list_adapter, @NotNull final List<TwoFactorAccount> accounts, @Nullable final String active_group, @Nullable final String text, @NotNull final OnDataFilteredListener listener) {
        return Main.getInstance().getBackgroundTask(new DataFiltererImplementation(activity, accounts_list_adapter, groups_list_adapter, accounts, active_group, text, listener));
    }
}
