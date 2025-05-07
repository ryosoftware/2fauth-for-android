package com.twofauth.android.database;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.twofauth.android.DatabaseHelper;
import com.twofauth.android.Main;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

public class TwoFactorAccountsHelper extends TableHelper<TwoFactorAccount> {
    public static final String TABLE_NAME = "two_factor_accounts";

    public enum SortMode { SORT_BY_LAST_USE, SORT_BY_SERVICE };

    private static class QueryOptions {
        public final TwoFactorServerIdentity serverIdentity;
        public final boolean onlyNotSyncedAccounts;
        public final SortMode sortMode;

        QueryOptions(@Nullable final TwoFactorServerIdentity server_identity, final boolean only_not_synced_accounts, @Nullable final SortMode sort_mode) {
            serverIdentity = server_identity;
            onlyNotSyncedAccounts = only_not_synced_accounts;
            sortMode = sort_mode;
        }
    }

    public TwoFactorAccountsHelper(@NotNull final DatabaseHelper database_helper) {
        super(TABLE_NAME, TwoFactorAccount.ROW_ID, TwoFactorAccount.PROJECTION, database_helper);
    }

    protected @NotNull TwoFactorAccount instance(@NotNull final SQLiteDatabase database, @NotNull final Cursor cursor) throws Exception {
        return new TwoFactorAccount(database, cursor);
    }

    @Override
    protected @NotNull TwoFactorAccount instance() throws Exception {
        return new TwoFactorAccount();
    }

    protected @NotNull Cursor query(@NotNull final SQLiteDatabase database, @Nullable final Object data) {
        final QueryOptions query_options = (data == null) ? new QueryOptions(null, false, null) : (QueryOptions) data;
        if (query_options.serverIdentity == null) {
            return database.query(TABLE_NAME, TwoFactorAccount.PROJECTION, query_options.onlyNotSyncedAccounts ? String.format("%s!=?", TwoFactorAccount.STATUS) : null, query_options.onlyNotSyncedAccounts ? new String[] { String.valueOf(TwoFactorAccount.STATUS_DEFAULT) } : null, null, null, query_options.sortMode == SortMode.SORT_BY_LAST_USE ? String.format("%s DESC, %S DESC", TwoFactorAccount.PIN_TIME, TwoFactorAccount.LAST_USE) : query_options.sortMode == SortMode.SORT_BY_SERVICE ? String.format("%s DESC, %s COLLATE NOCASE ASC, %s COLLATE NOCASE ASC", TwoFactorAccount.PIN_TIME, TwoFactorAccount.SERVICE, TwoFactorAccount.ACCOUNT) : null, null);
        }
        return database.query(TABLE_NAME, TwoFactorAccount.PROJECTION, String.format(query_options.onlyNotSyncedAccounts ? "%s=? and %s!=?" : "%s=?", TwoFactorAccount.SERVER_IDENTITY, TwoFactorAccount.STATUS), query_options.onlyNotSyncedAccounts ? new String[] { String.valueOf(query_options.serverIdentity.getRowId()), String.valueOf(TwoFactorAccount.STATUS_DEFAULT) } : new String[] { String.valueOf(query_options.serverIdentity.getRowId()) }, null, null, query_options.sortMode == SortMode.SORT_BY_LAST_USE ? String.format("%S DESC", TwoFactorAccount.LAST_USE) : query_options.sortMode == SortMode.SORT_BY_SERVICE ? String.format("%s COLLATE NOCASE ASC, %s COLLATE NOCASE ASC", TwoFactorAccount.SERVICE, TwoFactorAccount.ACCOUNT) : null, null);
    }

    public @Nullable TwoFactorAccount get(@NotNull final SQLiteDatabase database, final long id) throws Exception {
        return super.get(database, id);
    }

    public @Nullable TwoFactorAccount get(final long id) throws Exception {
        return super.get(id);
    }

    public @Nullable List<TwoFactorAccount> get(@NotNull final SQLiteDatabase database, @Nullable final TwoFactorServerIdentity server_identity, final boolean only_not_synced_accounts, @Nullable final SortMode sort_mode) throws Exception {
        return super.get(database, new QueryOptions(server_identity, only_not_synced_accounts, sort_mode));
    }

    public @Nullable List<TwoFactorAccount> get(@NotNull final SQLiteDatabase database, @Nullable final TwoFactorServerIdentity server_identity, final boolean only_not_synced_accounts) throws Exception {
        return super.get(database, new QueryOptions(server_identity, only_not_synced_accounts, null));
    }

    public @Nullable List<TwoFactorAccount> get(@NotNull final SQLiteDatabase database, @Nullable final TwoFactorServerIdentity server_identity) throws Exception {
        return super.get(database, new QueryOptions(server_identity, false, null));
    }

    public @Nullable List<TwoFactorAccount> get(@Nullable final TwoFactorServerIdentity server_identity, final boolean only_not_synced_accounts, final SortMode sort_mode) throws Exception {
        final SQLiteDatabase database = Main.getInstance().getDatabaseHelper().open(false);
        if (database != null) {
            try {
                return get(database, server_identity, only_not_synced_accounts, sort_mode);
            }
            finally {
                Main.getInstance().getDatabaseHelper().close(database);
            }
        }
        return null;
    }

    public @Nullable List<TwoFactorAccount> get(@Nullable final TwoFactorServerIdentity server_identity, final boolean only_not_synced_accounts) throws Exception {
        return get(server_identity, only_not_synced_accounts, null);
    }

    public @Nullable List<TwoFactorAccount> get(final boolean only_not_synced_accounts, @Nullable SortMode sort_mode) throws Exception {
        return get(null, only_not_synced_accounts, sort_mode);
    }

    public @Nullable List<TwoFactorAccount> get(final boolean only_not_synced_accounts) throws Exception {
        return get(null, only_not_synced_accounts, null);
    }

    public @Nullable List<TwoFactorAccount> get(@NotNull final SQLiteDatabase database) throws Exception {
        return super.get(database);
    }

    public List<TwoFactorAccount> get() throws Exception {
        return super.get();
    }

    public int count(@NotNull final SQLiteDatabase database, @Nullable final TwoFactorServerIdentity server_identity, final boolean only_not_synced_accounts) throws Exception {
        return count(database, new QueryOptions(server_identity, only_not_synced_accounts, null));
    }

    public int count(@Nullable final TwoFactorServerIdentity server_identity, final boolean only_not_synced_accounts) throws Exception {
        return count(new QueryOptions(server_identity, only_not_synced_accounts, null));
    }
}
