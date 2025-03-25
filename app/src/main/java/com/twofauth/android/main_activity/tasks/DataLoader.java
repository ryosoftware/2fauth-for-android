package com.twofauth.android.main_activity.tasks;

import android.util.Log;

import com.twofauth.android.BaseActivity;
import com.twofauth.android.Constants;
import com.twofauth.android.Database;
import com.twofauth.android.Database.TwoFactorAccount;
import com.twofauth.android.Main;
import com.twofauth.android.R;
import com.twofauth.android.SharedPreferencesUtilities;
import com.twofauth.android.StringUtils;
import com.twofauth.android.main_activity.AccountsListAdapter;
import com.twofauth.android.main_activity.GroupsListAdapter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DataLoader {
    public static class LoadedAccountsData {
        public final List<TwoFactorAccount> accounts;
        public final boolean alphaSorted;

        public LoadedAccountsData(@androidx.annotation.Nullable final List<TwoFactorAccount> _accounts, final boolean alpha_sorted) {
            accounts = _accounts;
            alphaSorted = alpha_sorted;
        }
    }

    public interface OnDataLoadListener {
        public abstract void onDataLoadSuccess(LoadedAccountsData data);
        public abstract void onDataLoadError();
    }

    private static class DataLoaderImplementation implements BaseActivity.SynchronizedCallback, Main.OnBackgroundTaskExecutionListener {
        private final BaseActivity mActivity;

        private final AccountsListAdapter mAccountsListAdapter;
        private final GroupsListAdapter mGroupsListAdapter;

        private final OnDataLoadListener mListener;

        private LoadedAccountsData mLoadedAccountsData = null;
        private List<String> mGroups = null;
        private boolean mSuccess = false;

        DataLoaderImplementation(@NotNull final BaseActivity activity, @NotNull final AccountsListAdapter accounts_list_adapter, @NotNull final GroupsListAdapter groups_list_adapter, @NotNull final OnDataLoadListener listener) {
            mActivity = activity;
            mAccountsListAdapter = accounts_list_adapter;
            mGroupsListAdapter = groups_list_adapter;
            mListener = listener;
        }

        private List<String> getGroups(@Nullable final List<TwoFactorAccount> accounts) {
            List<String> groups = null;
            if ((accounts != null) && (! accounts.isEmpty())) {
                groups = new ArrayList<String>();
                for (TwoFactorAccount account : accounts) {
                    final String group = (account.getGroup() == null) ? null : account.getGroup().name;
                    if ((group != null) && (! group.isEmpty()) && (! groups.contains(group))) {
                        groups.add(group);
                    }
                }
                groups.sort(new Comparator<String>() {
                    @Override
                    public int compare(final String string_1, final String string_2) {
                        return StringUtils.compare(string_1, string_2, true);
                    }
                });
            }
            return groups;
        }

        @Override
        public Object onBackgroundTaskStarted(@Nullable final Object data) {
            try {
                final boolean sort_using_last_use = SharedPreferencesUtilities.getDefaultSharedPreferences(mActivity).getBoolean(Constants.SORT_ACCOUNTS_BY_LAST_USE_KEY, mActivity.getResources().getBoolean(R.bool.sort_accounts_by_last_use_default));
                mLoadedAccountsData = new LoadedAccountsData(Database.TwoFactorAccountOperations.get(sort_using_last_use ? Database.TwoFactorAccountOperations.SortMode.SORT_BY_LAST_USE : Database.TwoFactorAccountOperations.SortMode.SORT_BY_SERVICE), ! sort_using_last_use);
                mGroups = getGroups(mLoadedAccountsData.accounts);
                mSuccess = true;
            }
            catch (Exception e) {
                Log.e(Constants.LOG_TAG_NAME, "Exception while trying to load data", e);
            }
            return null;
        }

        @Override
        public Object synchronizedCode(@Nullable final Object object)
        {
            mAccountsListAdapter.setItems(mLoadedAccountsData == null ? null : mLoadedAccountsData.accounts);
            mGroupsListAdapter.setItems(mGroups);
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
                    Log.e(Constants.LOG_TAG_NAME, "Exception while trying to display loaded data", e);
                    mSuccess = false;
                }
                finally {
                    if (mSuccess) {
                        mListener.onDataLoadSuccess(mLoadedAccountsData);
                    }
                    else {
                        mListener.onDataLoadError();
                    }
                }
            }
        }
    }

    public static Thread getBackgroundTask(@NotNull final BaseActivity activity, @NotNull final AccountsListAdapter accounts_list_adapter, @NotNull final GroupsListAdapter groups_list_adapter, @NotNull OnDataLoadListener listener) {
        return Main.getInstance().getBackgroundTask(new DataLoaderImplementation(activity, accounts_list_adapter, groups_list_adapter, listener));
    }
}
