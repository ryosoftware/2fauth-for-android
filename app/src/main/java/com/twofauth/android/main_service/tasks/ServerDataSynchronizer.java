package com.twofauth.android.main_service.tasks;

import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.util.Log;

import com.twofauth.android.Constants;
import com.twofauth.android.Main;
import com.twofauth.android.API;
import com.twofauth.android.Main.OnBackgroundTaskExecutionListener;
import com.twofauth.android.MainService;
import com.twofauth.android.MainService.SyncResultType;
import com.twofauth.android.R;
import com.twofauth.android.main_service.TwoFactorServerIdentityWithSyncData;
import com.twofauth.android.utils.Lists;
import com.twofauth.android.utils.Strings;
import com.twofauth.android.database.TwoFactorAccount;
import com.twofauth.android.database.TwoFactorGroup;
import com.twofauth.android.database.TwoFactorIcon;
import com.twofauth.android.database.TwoFactorServerIdentity;
import com.twofauth.android.utils.UI;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerDataSynchronizer
{
    public static final int SYNCHRONIZE_ALL_IDENTITIES = 0;

    private static class ServerDataSynchronizerImplementation implements OnBackgroundTaskExecutionListener {
        private static class TwoFactorGroupUtils {
            public static @Nullable Map<Integer, TwoFactorGroup> toMap(@Nullable final List<TwoFactorGroup> groups_list) {
                if ((groups_list != null) && (! groups_list.isEmpty())) {
                    Map<Integer, TwoFactorGroup> groups_map = new HashMap<Integer, TwoFactorGroup>();
                    for (final TwoFactorGroup group : groups_list) {
                        groups_map.put(group.getRemoteId(), group);
                    }
                    return groups_map;
                }
                return null;
            }
        }

        private static class TwoFactorAccountUtils {
            public static @Nullable Map<Integer, TwoFactorAccount> toMap(@Nullable final List<TwoFactorAccount> accounts_list) {
                if ((accounts_list != null) && (! accounts_list.isEmpty())) {
                    Map<Integer, TwoFactorAccount> accounts_map = null;
                    for (final TwoFactorAccount account : accounts_list) {
                        if (account.isRemote()) {
                            if (accounts_map == null) {
                                accounts_map = new HashMap<Integer, TwoFactorAccount>();
                            }
                            accounts_map.put(account.getRemoteId(), account);
                        }
                    }
                    return accounts_map;
                }
                return null;
            }
        }

        private final MainService mService;
        private final boolean mIsAppStartup;
        private final long mIdentityToSynchronize;

        ServerDataSynchronizerImplementation(@NotNull final MainService service, final boolean is_app_startup) {
            mService = service;
            mIsAppStartup = is_app_startup;
            mIdentityToSynchronize = -1;
        }

        ServerDataSynchronizerImplementation(@NotNull final MainService service, final long identity_to_synchronize) {
            mService = service;
            mIsAppStartup = false;
            mIdentityToSynchronize = identity_to_synchronize;
        }

        private void synchronizeIconsData(@NotNull final SQLiteDatabase database, @NotNull final TwoFactorServerIdentity server_identity, @Nullable final List<TwoFactorIcon> local_loaded_icons, final boolean raise_exception_on_network_error) throws Exception {
            if (local_loaded_icons != null) {
                for (final TwoFactorIcon icon : local_loaded_icons) {
                    API.synchronizeIcon(server_identity, database, mService, icon, raise_exception_on_network_error);
                }
            }
        }

        private void synchronizeGroupsData(@NotNull final SQLiteDatabase database, @Nullable final List<TwoFactorGroup> local_loaded_groups, final boolean raise_exception_on_network_error) throws Exception {
            if (local_loaded_groups != null) {
                for (final TwoFactorGroup group : local_loaded_groups) {
                    API.synchronizeGroup(database, mService, group, raise_exception_on_network_error);
                }
            }
        }

        private void synchronizeAccountsData(@NotNull final SQLiteDatabase database, @Nullable final List<TwoFactorAccount> local_loaded_accounts, @Nullable final List<TwoFactorGroup> local_loaded_groups, @Nullable final List<TwoFactorIcon> local_loaded_icons, final boolean raise_exception_on_network_error) throws Exception {
            if (local_loaded_accounts != null) {
                final Map<Long, TwoFactorGroup> local_loaded_groups_map = (local_loaded_groups == null) ? null : new HashMap<Long, TwoFactorGroup>();
                final Map<Long, TwoFactorIcon> local_loaded_icons_map = (local_loaded_icons == null) ? null : new HashMap<Long, TwoFactorIcon>();
                if (local_loaded_groups != null) {
                    for (final TwoFactorGroup local_loaded_group : local_loaded_groups) {
                        local_loaded_groups_map.put(local_loaded_group.getRowId(), local_loaded_group);
                    }
                }
                if (local_loaded_icons != null) {
                    for (final TwoFactorIcon local_loaded_icon : local_loaded_icons) {
                        local_loaded_icons_map.put(local_loaded_icon.getRowId(), local_loaded_icon);
                    }
                }
                for (final TwoFactorAccount account : local_loaded_accounts) {
                    boolean will_be_synchronized = account.isDeleted() || (! account.isSynced());
                    if (account.hasGroup() && (local_loaded_groups_map != null) && local_loaded_groups_map.containsKey(account.getGroup().getRowId())) {
                        account.setGroup(local_loaded_groups_map.get(account.getGroup().getRowId()));
                        will_be_synchronized = true;
                    }
                    if (account.hasIcon() && (local_loaded_icons_map != null) && local_loaded_icons_map.containsKey(account.getIcon().getRowId())) {
                        account.setIcon(local_loaded_icons_map.get(account.getIcon().getRowId()));
                        will_be_synchronized = true;
                    }
                    if (will_be_synchronized) {
                        if (account.isSynced()) { account.setStatus(TwoFactorAccount.STATUS_NOT_SYNCED); }
                        API.synchronizeAccount(database, mService, account, raise_exception_on_network_error);
                    }
                }
            }
        }

        public static @Nullable List<TwoFactorAccount> parseAccounts(@NotNull final TwoFactorServerIdentity server_identity, @Nullable Collection<JSONObject> accounts_objects, final boolean raise_exception_on_network_error) throws Exception {
            if (accounts_objects != null) {
                final List<TwoFactorAccount> accounts = new ArrayList<TwoFactorAccount>();
                for (final JSONObject account_object : accounts_objects) {
                    final TwoFactorAccount account = new TwoFactorAccount();
                    account.setRemoteId(account_object.getInt(Constants.ACCOUNT_DATA_ID_KEY));
                    account.setService(account_object.getString(Constants.ACCOUNT_DATA_SERVICE_KEY));
                    account.setAccount(account_object.optString(Constants.ACCOUNT_DATA_USER_KEY, ""));
                    account.setServerIdentity(server_identity);
                    if (account_object.has(Constants.ACCOUNT_DATA_GROUP_KEY) && (! account_object.isNull(Constants.ACCOUNT_DATA_GROUP_KEY))) { account.setGroup((TwoFactorGroup) account_object.get(Constants.ACCOUNT_DATA_GROUP_KEY)); }
                    if (account_object.has(Constants.ACCOUNT_DATA_ICON_KEY) && (! account_object.isNull(Constants.ACCOUNT_DATA_ICON_KEY))) { account.setIcon((TwoFactorIcon) account_object.get(Constants.ACCOUNT_DATA_ICON_KEY)); }
                    account.setOtpType(account_object.getString(Constants.ACCOUNT_DATA_OTP_TYPE_KEY));
                    account.setSecret(account_object.getString(Constants.ACCOUNT_DATA_SECRET_KEY));
                    account.setOtpLength(account_object.getInt(Constants.ACCOUNT_DATA_OTP_LENGTH_KEY));
                    account.setAlgorithm(account_object.getString(Constants.ACCOUNT_DATA_ALGORITHM_KEY));
                    if (account_object.has(Constants.ACCOUNT_DATA_PERIOD_KEY) && (! account_object.isNull(Constants.ACCOUNT_DATA_PERIOD_KEY))) { account.setPeriod(account_object.getInt(Constants.ACCOUNT_DATA_PERIOD_KEY)); }
                    if (account_object.has(Constants.ACCOUNT_DATA_COUNTER_KEY) && (! account_object.isNull(Constants.ACCOUNT_DATA_COUNTER_KEY))) { account.setPeriod(account_object.getInt(Constants.ACCOUNT_DATA_COUNTER_KEY)); }
                    account.setStatus(TwoFactorAccount.STATUS_DEFAULT);
                    accounts.add(account);
                }
                return accounts;
            }
            return null;
        }

        private void setAccountGroup(@Nullable final List<JSONObject> account_objects, @Nullable final List<TwoFactorGroup> groups) throws Exception {
            if ((account_objects != null) && (groups != null)) {
                final Map<Integer, TwoFactorGroup> groups_map = new HashMap<Integer, TwoFactorGroup>();
                for (final TwoFactorGroup group : groups) {
                    groups_map.put(group.getRemoteId(), group);
                }
                for (final JSONObject account_object : account_objects) {
                    if (account_object.has(Constants.ACCOUNT_DATA_GROUP_KEY) && (! account_object.isNull(Constants.ACCOUNT_DATA_GROUP_KEY))) {
                        account_object.put(Constants.ACCOUNT_DATA_GROUP_KEY, groups_map.get(account_object.getInt(Constants.ACCOUNT_DATA_GROUP_KEY)));
                    }
                }
            }
        }

        private void combineData(@NotNull final TwoFactorAccount local_loaded_account, @NotNull final TwoFactorAccount server_loaded_account) {
            local_loaded_account.setService(server_loaded_account.getService());
            local_loaded_account.setAccount(server_loaded_account.getAccount());
            local_loaded_account.setGroup(server_loaded_account.getGroup());
            local_loaded_account.setIcon(server_loaded_account.getIcon());
            local_loaded_account.setOtpType(server_loaded_account.getOtpType());
            local_loaded_account.setSecret(server_loaded_account.getSecret());
            local_loaded_account.setOtpLength(server_loaded_account.getOtpLength());
            local_loaded_account.setAlgorithm(server_loaded_account.getAlgorithm());
            local_loaded_account.setPeriod(server_loaded_account.getPeriod());
            local_loaded_account.setCounter(server_loaded_account.getCounter());
            local_loaded_account.setLastUse(server_loaded_account.getLastUse());
        }

        private void combineData(@NotNull final SQLiteDatabase database, @Nullable final List<TwoFactorAccount> server_loaded_accounts, @Nullable final List<TwoFactorAccount> local_loaded_accounts, @Nullable final List<TwoFactorGroup> server_loaded_groups) throws Exception {
            Map<Integer, TwoFactorAccount> removed_accounts = Lists.isEmptyOrNull(local_loaded_accounts) ? null : TwoFactorAccountUtils.toMap(local_loaded_accounts);
            if (! Lists.isEmptyOrNull(server_loaded_accounts)) {
                for (int i = server_loaded_accounts.size() - 1; i >= 0; i --) {
                    final TwoFactorAccount server_loaded_account = server_loaded_accounts.get(i), local_loaded_account = (removed_accounts == null) ? null : removed_accounts.remove(server_loaded_account.getRemoteId());
                    if (local_loaded_account != null) {
                        if (local_loaded_account.getStatus() == TwoFactorAccount.STATUS_DEFAULT) {
                            combineData(local_loaded_account, server_loaded_account);
                            local_loaded_account.save(database, mService);
                        }
                        server_loaded_accounts.remove(i);
                    }
                    else {
                        server_loaded_account.save(database, mService);
                    }
                }
            }
            if (removed_accounts != null) {
                for (final TwoFactorAccount removed_account : removed_accounts.values()) {
                    removed_account.delete(database, mService);
                }
            }
        }

        private void saveServerLoadedGroups(@NotNull final SQLiteDatabase database, @Nullable final List<TwoFactorGroup> server_loaded_groups) throws Exception {
            if (server_loaded_groups != null) {
                for (final TwoFactorGroup group : server_loaded_groups) {
                    if (! group.inDatabase()) { group.save(database, mService); }
                }
            }
        }

        private void removeNotReferencedData(@NotNull final SQLiteDatabase database) throws Exception {
            final List<TwoFactorGroup> groups = Main.getInstance().getDatabaseHelper().getTwoFactorGroupsHelper().get(database);
            if (groups != null) {
                for (final TwoFactorGroup group : groups) {
                    try { group.delete(database, mService); }
                    catch (SQLiteConstraintException ignored) {}
                }
            }
            final List<TwoFactorIcon> icons = Main.getInstance().getDatabaseHelper().getTwoFactorIconsHelper().get(database);
            if (icons != null) {
                for (final TwoFactorIcon icon : icons) {
                    try { icon.delete(database, mService); }
                    catch (SQLiteConstraintException ignored) {}
                }
            }
        }

        private boolean willBeSynchronized(@NotNull final TwoFactorServerIdentity server_identity) {
            if (mIsAppStartup) { return server_identity.isSyncingOnStartup(); }
            return ((server_identity.getRowId() == mIdentityToSynchronize) || (mIdentityToSynchronize == SYNCHRONIZE_ALL_IDENTITIES));
        }

        @Override
        public @NotNull Object onBackgroundTaskStarted(@Nullable final Object data) {
            SyncResultType result_type = SyncResultType.NO_IDENTITIES;
            try {
                int accounts_will_be_synced = 0, accounts_has_been_synced = 0, accounts_has_errors = 0;
                final SQLiteDatabase database = Main.getInstance().getDatabaseHelper().open(true);
                if (database != null) {
                    try {
                        // Get server identities from database then execute synchronization process foreach one...
                        for (final TwoFactorServerIdentity server_identity : Main.getInstance().getDatabaseHelper().getTwoFactorServerIdentitiesHelper().get(database)) {
                            try {
                                if (willBeSynchronized(server_identity)) {
                                    accounts_will_be_synced ++;
                                    // Refresh server identity data
                                    API.refreshIdentityData(server_identity, true);
                                    // Synchronize out of sync accounts and groups (deleted, updated or added accounts) with server before start download
                                    final List<TwoFactorIcon> not_synced_icons = Main.getInstance().getDatabaseHelper().getTwoFactorIconsHelper().get(database, server_identity, true);
                                    final List<TwoFactorGroup> not_synced_groups = Main.getInstance().getDatabaseHelper().getTwoFactorGroupsHelper().get(database, server_identity, true);
                                    final List<TwoFactorAccount> not_synced_accounts = Main.getInstance().getDatabaseHelper().getTwoFactorAccountsHelper().get(database, server_identity, false, null);
                                    synchronizeIconsData(database, server_identity, not_synced_icons, true);
                                    synchronizeGroupsData(database, not_synced_groups, true);
                                    synchronizeAccountsData(database, not_synced_accounts, not_synced_groups, not_synced_icons, true);
                                    // Gets server data (we raise an exception if we can't access data)
                                    final List<JSONObject> server_loaded_accounts_raw = API.getAccounts(server_identity, true);
                                    final List<TwoFactorGroup> server_loaded_groups = API.getGroups(server_identity, true);
                                    setAccountGroup(server_loaded_accounts_raw, server_loaded_groups);
                                    // Icons are less relevant than other data (we do not raise an exception on network error)
                                    API.getIcons(server_identity, mService, server_loaded_accounts_raw, false);
                                    // Now we parse accounts data then start database transition
                                    final List<TwoFactorAccount> server_loaded_accounts = parseAccounts(server_identity, server_loaded_accounts_raw, true);
                                    final int server_loaded_accounts_count = Lists.size(server_loaded_accounts), server_loaded_groups_count = Lists.size(server_loaded_groups);
                                    if (Main.getInstance().getDatabaseHelper().beginTransaction(database)) {
                                        boolean commit_transaction = false;
                                        try {
                                            // Save server identity refreshed data
                                            server_identity.save(database, mService);
                                            // Gets local accounts again, but now we include all, not only not synced accounts
                                            final List<TwoFactorAccount> local_loaded_accounts = Main.getInstance().getDatabaseHelper().getTwoFactorAccountsHelper().get(database, server_identity, false, null);
                                            // We combine downloaded data with the one previously downloaded
                                            // This procedure do this:
                                            // * for accounts previously downloaded:
                                            //   - if there are not present at the downloaded accounts list we remove from database
                                            //   - else if account is synced we replace new data with old data
                                            // * else we add to database
                                            combineData(database, server_loaded_accounts, local_loaded_accounts, server_loaded_groups);
                                            // Remove not referenced groups and icons
                                            removeNotReferencedData(database);
                                            // Save groups that are not referenced by any group
                                            saveServerLoadedGroups(database, server_loaded_groups);
                                            // Sync is finished, we annotate then commit the transaction
                                            (new TwoFactorServerIdentityWithSyncData(server_identity)).onSyncSuccess(mService, server_loaded_accounts_count, server_loaded_groups_count);
                                            commit_transaction = true;
                                            accounts_has_been_synced ++;
                                            result_type = SyncResultType.UPDATED;
                                        }
                                        finally {
                                            Main.getInstance().getDatabaseHelper().endTransaction(database, commit_transaction);
                                        }
                                    }
                                    else {
                                        throw new SQLException("Database lock cannot be acquired");
                                    }
                                }
                            }
                            catch (Exception e) {
                                final String exception_message = e.getMessage();
                                (new TwoFactorServerIdentityWithSyncData(server_identity)).onSyncError(mService, Strings.isEmptyOrNull(exception_message) ? "Unknown error" : exception_message);
                                Log.e(Main.LOG_TAG_NAME, "Exception while trying to synchronize accounts", e);
                                accounts_has_errors ++;
                            }
                        }
                        if (accounts_will_be_synced > 0) {
                            result_type = (accounts_has_been_synced > 0) ? SyncResultType.UPDATED : (accounts_has_errors > 0) ? SyncResultType.ERROR : SyncResultType.NO_CHANGES;
                        }
                    }
                    finally {
                        Main.getInstance().getDatabaseHelper().close(database);
                    }
                }
                else {
                    throw new SQLException("Cannot open database for Read and Write");
                }
            }
            catch (Exception e) {
                Log.e(Main.LOG_TAG_NAME, "Exception while syncing 2FA accounts", e);
            }
            return result_type;
        }

        @Override
        public void onBackgroundTaskFinished(@NotNull final Object data) {
            final SyncResultType result = (SyncResultType) data;
            if (result != SyncResultType.NO_IDENTITIES) { UI.showToast(mService, result == SyncResultType.UPDATED ? R.string.sync_completed : result == SyncResultType.NO_CHANGES ? R.string.sync_no_changes : R.string.sync_error); }
            mService.stopSelf(result);
        }
    }

    public static @NotNull Thread getBackgroundTask(@NotNull final MainService service, final boolean is_app_startup) {
        return Main.getInstance().getBackgroundTask(new ServerDataSynchronizerImplementation(service, is_app_startup));
    }

    public static @NotNull Thread getBackgroundTask(@NotNull final MainService service, final long identity_to_synchronize) {
        return Main.getInstance().getBackgroundTask(new ServerDataSynchronizerImplementation(service, identity_to_synchronize));
    }
}
