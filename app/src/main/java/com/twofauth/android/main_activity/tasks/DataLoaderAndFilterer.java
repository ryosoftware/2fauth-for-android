package com.twofauth.android.main_activity.tasks;

import android.content.Context;
import android.util.Log;

import com.twofauth.android.Constants;
import com.twofauth.android.Main;
import com.twofauth.android.R;
import com.twofauth.android.database.TwoFactorAccount;
import com.twofauth.android.database.TwoFactorAccountsHelper;
import com.twofauth.android.database.TwoFactorGroup;
import com.twofauth.android.database.TwoFactorServerIdentity;
import com.twofauth.android.utils.Lists;
import com.twofauth.android.utils.Preferences;
import com.twofauth.android.utils.Strings;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataLoaderAndFilterer {
    public interface OnDataLoadedListener {
        public abstract void onDataLoadSuccess(List<TwoFactorServerIdentity> server_identities, Map<Long, List<TwoFactorGroup>> groups, Map<Long, List<TwoFactorAccount>> accounts, boolean alpha_sorted_accounts);
        public abstract void onDataLoadError();
    }

    public interface OnDataFilteredListener {
        public abstract void onDataFiltered(boolean any_filter_applied, List<TwoFactorAccount> accounts);
    }

    private static class DataLoaderAndFiltererImplementation implements Main.OnBackgroundTaskExecutionListener {
        private static class TwoFactorAccountUtils {
            private static Map<Long, List<TwoFactorAccount>> toMap(final List<TwoFactorAccount> accounts) {
                final Map<Long, List<TwoFactorAccount>> map = new HashMap<Long, List<TwoFactorAccount>>();
                for (final TwoFactorAccount account : accounts) {
                    final long id = account.getServerIdentity().getRowId();
                    if (! map.containsKey(id)) { map.put(id, new ArrayList<TwoFactorAccount>()); }
                    map.get(id).add(account);
                }
                map.put(-1L, accounts);
                return map;
            }
        }

        private static class TwoFactorGroupUtils {
            private static Map<Long, List<TwoFactorGroup>> toMap(final List<TwoFactorGroup> groups) {
                final Map<Long, List<TwoFactorGroup>> map = new HashMap<Long, List<TwoFactorGroup>>();
                for (final TwoFactorGroup group : groups) {
                    final long id = group.getServerIdentity().getRowId();
                    if (! map.containsKey(id)) { map.put(id, new ArrayList<TwoFactorGroup>()); }
                    map.get(id).add(group);
                }
                map.put(-1L, groups);
                return map;
            }
        }

        private final Context mContext;

        private List<TwoFactorServerIdentity> mServerIdentities = null;
        private Map<Long, List<TwoFactorGroup>> mGroups = null;
        private Map<Long, List<TwoFactorAccount>> mAccounts = null;
        private boolean mAlphaSortedAccounts;

        private List<TwoFactorAccount> mFilterableAccounts = null;

        private final TwoFactorServerIdentity mActiveServerIdentity;
        private final TwoFactorGroup mActiveGroup;
        private final String mText;

        private boolean mAnyFilteredApplied = false;
        private List<TwoFactorAccount> mFilteredAccounts;

        private boolean mLoadDataSuccess = false;

        private final OnDataLoadedListener mOnDataLoadedListener;
        private final OnDataFilteredListener mOnDataFilteredListener;

        DataLoaderAndFiltererImplementation(@NotNull final Context context, @Nullable final TwoFactorServerIdentity active_server_identity, @Nullable final TwoFactorGroup active_group, @Nullable final String text, @Nullable final OnDataLoadedListener on_data_loaded_listener, @NotNull final OnDataFilteredListener on_data_filtered_listener) {
            mContext = context;
            mActiveServerIdentity = active_server_identity;
            mActiveGroup = active_group;
            mText = text;
            mOnDataLoadedListener = on_data_loaded_listener;
            mOnDataFilteredListener = on_data_filtered_listener;
        }

        DataLoaderAndFiltererImplementation(@NotNull final Context context, @Nullable final List<TwoFactorAccount> filterable_accounts, @Nullable final TwoFactorServerIdentity active_server_identity, @Nullable final TwoFactorGroup active_group, @Nullable final String text, @NotNull final OnDataFilteredListener on_data_filtered_listener) {
            this(context, active_server_identity, active_group, text, null, on_data_filtered_listener);
            mFilterableAccounts = filterable_accounts;
            mLoadDataSuccess = (mAccounts != null);
        }

        private @Nullable List<TwoFactorServerIdentity> getServerIdentities(@Nullable final List<TwoFactorAccount> accounts) {
            List<TwoFactorServerIdentity> server_identities = null;
            if ((accounts != null) && (! accounts.isEmpty())) {
                final Map<Long, Boolean> server_identities_map = new HashMap<Long, Boolean>();
                server_identities = new ArrayList<TwoFactorServerIdentity>();
                for (final TwoFactorAccount account : accounts) {
                    final TwoFactorServerIdentity server_identity = account.getServerIdentity();
                    if (! server_identities_map.containsKey(server_identity.getRowId())) {
                        server_identities_map.put(server_identity.getRowId(), true);
                        server_identities.add(server_identity);
                    }
                }
                server_identities.sort(new Comparator<TwoFactorServerIdentity>() {
                    @Override
                    public int compare(@NotNull final TwoFactorServerIdentity server_identity_1, @NotNull final TwoFactorServerIdentity server_identity_2) {
                        return Strings.compare(server_identity_1.getTitle(), server_identity_2.getTitle(), true);
                    }
                });
            }
            return server_identities;
        }

        private @Nullable List<TwoFactorGroup> getGroups(@Nullable final List<TwoFactorAccount> accounts) {
            List<TwoFactorGroup> groups = null;
            if ((accounts != null) && (! accounts.isEmpty())) {
                final Map<Long, Boolean> groups_map = new HashMap<Long, Boolean>();
                groups = new ArrayList<TwoFactorGroup>();
                for (final TwoFactorAccount account : accounts) {
                    final TwoFactorGroup group = account.hasGroup() ? account.getGroup() : null;
                    if ((group != null) && (! groups_map.containsKey(group.getRowId()))) {
                        groups_map.put(group.getRowId(), true);
                        groups.add(group);
                    }
                }
                groups.sort(new Comparator<TwoFactorGroup>() {
                    @Override
                    public int compare(@NotNull final TwoFactorGroup group_1, @NotNull final TwoFactorGroup group_2) {
                        int result = Strings.compare(group_1.getServerIdentity().getTitle(), group_2.getServerIdentity().getTitle(), true);
                        if (result == 0) { result = Strings.compare(group_1.getName(), group_2.getName(), true); }
                        return result;
                    }
                });
            }
            return groups;
        }

        private List<TwoFactorAccount> loadData() {
            try {
                mAlphaSortedAccounts = ! Preferences.getDefaultSharedPreferences(mContext).getBoolean(Constants.SORT_ACCOUNTS_BY_LAST_USE_KEY, mContext.getResources().getBoolean(R.bool.sort_accounts_by_last_use));
                List<TwoFactorAccount> accounts = Main.getInstance().getDatabaseHelper().getTwoFactorAccountsHelper().get(false, mAlphaSortedAccounts ? TwoFactorAccountsHelper.SortMode.SORT_BY_SERVICE : TwoFactorAccountsHelper.SortMode.SORT_BY_LAST_USE);
                if ((! Lists.isEmptyOrNull(accounts)) && (mOnDataLoadedListener != null)) {
                    mServerIdentities = getServerIdentities(accounts);
                    final List<TwoFactorGroup> groups = getGroups(accounts);
                    if (groups != null) { mGroups = TwoFactorGroupUtils.toMap(groups); }
                    mAccounts = TwoFactorAccountUtils.toMap(accounts);
                }
                mLoadDataSuccess = true;
                return accounts;
            }
            catch (Exception e) {
                Log.e(Main.LOG_TAG_NAME, "Exception while trying to load accounts", e);
            }
            return null;
        }

        private boolean isVisible(@NotNull final TwoFactorAccount account) {
            if ((mActiveServerIdentity != null) && (account.getServerIdentity().getRowId() != mActiveServerIdentity.getRowId())) { return false; }
            if ((mActiveGroup != null) && ((! account.hasGroup()) || (mActiveGroup.getRowId() != account.getGroup().getRowId()))) { return false; }
            if (Strings.isEmptyOrNull(mText)) { return true; }
            if ((account.hasService()) && (Strings.in(account.getService(), mText, true))) { return true; }
            if ((account.hasAccount()) && (Strings.in(account.getAccount(), mText, true))) { return true; }
            return false;
        }

        private @NotNull List<TwoFactorAccount> filterAccounts(@NotNull final List<TwoFactorAccount> accounts) {
            final List<TwoFactorAccount> filtered_accounts = new ArrayList<TwoFactorAccount>();
            for (final TwoFactorAccount account : accounts) {
                if (isVisible(account)) { filtered_accounts.add(account); }
            }
            return filtered_accounts;
        }

        @Override
        public @Nullable Object onBackgroundTaskStarted(@Nullable final Object data) {
            if ((mFilterableAccounts == null) || (mOnDataLoadedListener != null)) { mFilterableAccounts = loadData(); }
            mAnyFilteredApplied = ((mActiveServerIdentity != null) || (mActiveGroup != null) || (! Strings.isEmptyOrNull(mText)));
            mFilteredAccounts = (mAnyFilteredApplied && (! Lists.isEmptyOrNull(mFilterableAccounts))) ? filterAccounts(mFilterableAccounts) : mFilterableAccounts;
            return null;
        }

        @Override
        public void onBackgroundTaskFinished(@Nullable final Object data) {
            if (mOnDataLoadedListener != null) {
                if (mLoadDataSuccess) { mOnDataLoadedListener.onDataLoadSuccess(mServerIdentities, mGroups, mAccounts, mAlphaSortedAccounts); }
                else { mOnDataLoadedListener.onDataLoadError(); }
            }
            mOnDataFilteredListener.onDataFiltered(mAnyFilteredApplied, mFilteredAccounts);
        }
    }

    public static @NotNull Thread getBackgroundTask(@NotNull final Context context, @Nullable final TwoFactorServerIdentity active_server_identity, @Nullable final TwoFactorGroup active_group, @Nullable final String text, @Nullable final OnDataLoadedListener on_data_loaded_listener, @NotNull final OnDataFilteredListener on_data_filtered_listener) {
        return Main.getInstance().getBackgroundTask(new DataLoaderAndFiltererImplementation(context, active_server_identity, active_group, text, on_data_loaded_listener, on_data_filtered_listener));
    }

    public static @NotNull Thread getBackgroundTask(@NotNull final Context context, @Nullable final List<TwoFactorAccount> filterable_accounts, @Nullable final TwoFactorServerIdentity active_server_identity, @Nullable final TwoFactorGroup active_group, @Nullable final String text, @NotNull final OnDataFilteredListener on_data_filtered_listener) {
        return Main.getInstance().getBackgroundTask(new DataLoaderAndFiltererImplementation(context, filterable_accounts, active_server_identity, active_group, text, on_data_filtered_listener));
    }
}
