package com.twofauth.android.database;

import android.database.Cursor;

import com.twofauth.android.DatabaseHelper;
import com.twofauth.android.Main;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TwoFactorGroupsHelper extends TableHelper<TwoFactorGroup> {
    public static final String TABLE_NAME = "two_factor_groups";

    private static class QueryOptions {
        public final TwoFactorServerIdentity serverIdentity;
        public final boolean onlyNotSyncedAccounts;

        QueryOptions(@Nullable final TwoFactorServerIdentity server_identity, final boolean only_not_synced_accounts) {
            serverIdentity = server_identity;
            onlyNotSyncedAccounts = only_not_synced_accounts;
        }
    }

    public TwoFactorGroupsHelper(@NotNull final DatabaseHelper database_helper) {
        super(TABLE_NAME, TwoFactorGroup.ROW_ID, TwoFactorGroup.PROJECTION, database_helper);
    }

    @Override
    protected @NotNull TwoFactorGroup instance(@NotNull final SQLiteDatabase database, @NotNull final Cursor cursor) throws Exception {
        return new TwoFactorGroup(database, cursor);
    }

    @Override
    protected @NotNull TwoFactorGroup instance() {
        return new TwoFactorGroup();
    }

    protected @NotNull Cursor query(@NotNull final SQLiteDatabase database, @Nullable final Object data) {
        final QueryOptions query_options = (data == null) ? new QueryOptions(null, false) : (QueryOptions) data;
        if (query_options.serverIdentity == null) {
            return database.query(TABLE_NAME, TwoFactorGroup.PROJECTION, query_options.onlyNotSyncedAccounts ? String.format("%s!=?", TwoFactorAccount.STATUS) : null, query_options.onlyNotSyncedAccounts ? new String[] { String.valueOf(TwoFactorAccount.STATUS_DEFAULT) } : null, null, null, String.format("%s COLLATE NOCASE ASC", TwoFactorGroup.NAME), null);
        }
        return database.query(TABLE_NAME, TwoFactorGroup.PROJECTION, String.format(query_options.onlyNotSyncedAccounts ? "%s=? and %s!=?" : "%s=?", TwoFactorAccount.SERVER_IDENTITY, TwoFactorAccount.STATUS), query_options.onlyNotSyncedAccounts  ? new String[] { String.valueOf(query_options.serverIdentity.getRowId()), String.valueOf(TwoFactorAccount.STATUS_DEFAULT) } : new String[] { String.valueOf(query_options.serverIdentity.getRowId()) }, null, null, String.format("%s COLLATE NOCASE ASC", TwoFactorGroup.NAME), null);
    }

    public @Nullable TwoFactorGroup get(@NotNull final SQLiteDatabase database, final long id) throws Exception {
        return super.get(database, id);
    }

    public @Nullable TwoFactorGroup get(final long id) throws Exception {
        return super.get(id);
    }

    public @Nullable List<TwoFactorGroup> get(@NotNull final SQLiteDatabase database, @Nullable final TwoFactorServerIdentity server_identity, final boolean only_not_synced_accounts) throws Exception {
        return super.get(database, new QueryOptions(server_identity, only_not_synced_accounts));
    }

    public @Nullable List<TwoFactorGroup> get(@NotNull final SQLiteDatabase database, @Nullable final TwoFactorServerIdentity server_identity) throws Exception {
        return super.get(database, new QueryOptions(server_identity, false));
    }

    public @Nullable List<TwoFactorGroup> get(@Nullable final TwoFactorServerIdentity server_identity, final boolean only_not_synced_accounts) throws Exception {
        final SQLiteDatabase database = Main.getInstance().getDatabaseHelper().open(false);
        if (database != null) {
            try {
                return get(database, server_identity, only_not_synced_accounts);
            }
            finally {
                Main.getInstance().getDatabaseHelper().close(database);
            }
        }
        return null;
    }

    public @Nullable List<TwoFactorGroup> get(@Nullable final TwoFactorServerIdentity server_identity) throws Exception {
        return get(server_identity, false);
    }

    public @Nullable List<TwoFactorGroup> get(final boolean only_not_synced_accounts) throws Exception {
        return get(null, only_not_synced_accounts);
    }

    public @Nullable List<TwoFactorGroup> get(@NotNull final SQLiteDatabase database) throws Exception {
        return super.get(database);
    }

    public List<TwoFactorGroup> get() throws Exception {
        return super.get();
    }

    public int count(@NotNull final SQLiteDatabase database, @Nullable final TwoFactorServerIdentity server_identity, final boolean only_not_synced_accounts) throws Exception {
        return count(database, new QueryOptions(server_identity, only_not_synced_accounts));
    }

    public int count(@Nullable final TwoFactorServerIdentity server_identity, final boolean only_not_synced_accounts) throws Exception {
        return count(new QueryOptions(server_identity, only_not_synced_accounts));
    }

    public boolean exists(@Nullable final TwoFactorServerIdentity server_identity, final boolean only_not_synced_accounts) throws Exception {
        return exists(new QueryOptions(server_identity, only_not_synced_accounts));
    }
}
