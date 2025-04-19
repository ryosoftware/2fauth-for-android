package com.twofauth.android.database;

import android.database.Cursor;

import com.twofauth.android.DatabaseHelper;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TwoFactorGroupsHelper extends TableHelper<TwoFactorGroup> {
    public static final String TABLE_NAME = "two_factor_groups";

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
        return database.query(TABLE_NAME, TwoFactorGroup.PROJECTION, null, null, null, null, String.format("%s COLLATE NOCASE ASC", TwoFactorGroup.NAME), null);
    }

    public @Nullable List<TwoFactorGroup> get(@NotNull final SQLiteDatabase database) throws Exception {
        return super.get(database);
    }

    public @Nullable List<TwoFactorGroup> get() throws Exception {
        return super.get();
    }

    public @Nullable TwoFactorGroup get(@NotNull final SQLiteDatabase database, final long id) throws Exception {
        return super.get(database, id);
    }

    public @Nullable TwoFactorGroup get(final long id) throws Exception {
        return super.get(id);
    }
}
