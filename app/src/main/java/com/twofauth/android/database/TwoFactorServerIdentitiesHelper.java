package com.twofauth.android.database;

import android.database.Cursor;

import com.twofauth.android.DatabaseHelper;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TwoFactorServerIdentitiesHelper extends TableHelper<TwoFactorServerIdentity> {
    public static final String TABLE_NAME = "two_factor_server_identities";

    public TwoFactorServerIdentitiesHelper(@NotNull final DatabaseHelper database_helper) {
        super(TABLE_NAME, TwoFactorServerIdentity.ROW_ID, TwoFactorServerIdentity.PROJECTION, database_helper);
    }

    @Override
    protected @NotNull TwoFactorServerIdentity instance(@NotNull final SQLiteDatabase database, @NotNull final Cursor cursor) throws Exception {
        return new TwoFactorServerIdentity(cursor);
    }

    @Override
    protected @NotNull TwoFactorServerIdentity instance() {
        return new TwoFactorServerIdentity();
    }

    protected @NotNull Cursor query(@NotNull final SQLiteDatabase database, @Nullable final Object data) {
        return database.query(TABLE_NAME, TwoFactorServerIdentity.PROJECTION, null, null, null, null, String.format("%s COLLATE NOCASE ASC, %s COLLATE NOCASE ASC, %s COLLATE NOCASE ASC", TwoFactorServerIdentity.LABEL, TwoFactorServerIdentity.NAME, TwoFactorServerIdentity.SERVER), null);
    }

    public @Nullable List<TwoFactorServerIdentity> get(@NotNull final SQLiteDatabase database) throws Exception {
        return super.get(database);
    }

    public @Nullable List<TwoFactorServerIdentity> get() throws Exception {
        return super.get();
    }

    public @Nullable TwoFactorServerIdentity get(@NotNull final SQLiteDatabase database, final long id) throws Exception {
        return super.get(database, id);
    }

    public @Nullable TwoFactorServerIdentity get(final long id) throws Exception {
        return super.get(id);
    }
}
