package com.twofauth.android.database;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;

import com.twofauth.android.DatabaseHelper;
import com.twofauth.android.utils.Lists;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TwoFactorIconsHelper extends TableHelper<TwoFactorIcon> {
    public static final String TABLE_NAME = "two_factor_icons";

    private static final String ICONS_FOLDER = "icons";

    public TwoFactorIconsHelper(@NotNull final DatabaseHelper database_helper) {
        super(TABLE_NAME, TwoFactorIcon.ROW_ID, TwoFactorIcon.PROJECTION, database_helper);
    }

    @Override
    public @NotNull TwoFactorIcon instance(@NotNull final SQLiteDatabase database, @NotNull final Cursor cursor) throws Exception {
        return new TwoFactorIcon(cursor);
    }

    @Override
    public @NotNull TwoFactorIcon instance() {
        return new TwoFactorIcon();
    }

    public @Nullable List<TwoFactorIcon> get(@NotNull final SQLiteDatabase database) throws Exception {
        return super.get(database);
    }

    public @Nullable List<TwoFactorIcon> get() throws Exception {
        return super.get();
    }

    public @Nullable TwoFactorIcon get(@NotNull final SQLiteDatabase database, final long id) throws Exception {
        return super.get(database, id);
    }

    public @Nullable TwoFactorIcon get(final long id) throws Exception {
        return super.get(id);
    }

    @SuppressLint("DefaultLocale")
    public @Nullable List<TwoFactorIcon> get(@NotNull final SQLiteDatabase database, @NotNull final TwoFactorServerIdentity server_identity, final boolean only_not_synced_icons) throws Exception {
        final StringBuilder query = new StringBuilder();
        query.append("SELECT ");
        for (int i = 0; i < TwoFactorIcon.PROJECTION.length; i ++) {
            if (i > 0) { query.append(", "); }
            query.append(String.format("%s.%s", TABLE_NAME, TwoFactorIcon.PROJECTION[i]));
        }
        query.append(String.format(" FROM %s JOIN %s ON %s.%s=%s.%s WHERE %s.%s=?", TwoFactorAccountsHelper.TABLE_NAME, TABLE_NAME, TwoFactorAccountsHelper.TABLE_NAME, TwoFactorAccount.ICON, TABLE_NAME, TwoFactorIcon.ROW_ID, TwoFactorAccountsHelper.TABLE_NAME, TwoFactorAccount.SERVER_IDENTITY));
        if (only_not_synced_icons) { query.append(String.format(" AND %s.%s IS NULL", TABLE_NAME, TwoFactorIcon.SOURCE)); }
        try (final Cursor cursor = database.rawQuery(query.toString(), new String[] { String.valueOf(server_identity.getRowId()) })) {
            if ((cursor != null) && (cursor.getCount() > 0) && cursor.moveToFirst()) {
                final List<TwoFactorIcon> icons = new ArrayList<TwoFactorIcon>();
                while (! cursor.isAfterLast()) {
                    icons.add(new TwoFactorIcon(cursor));
                    cursor.moveToNext();
                }
                return icons;
            }
        }
        return null;
    }

    public @Nullable List<TwoFactorIcon> get(@NotNull final SQLiteDatabase database, @NotNull final TwoFactorServerIdentity server_identity) throws Exception {
        return get(database, server_identity, false);
    }

    public @Nullable TwoFactorIcon get(@NotNull final SQLiteDatabase database, @Nullable final TwoFactorServerIdentity server_identity, final String source, final String source_id) throws Exception {
        if (server_identity == null) {
            try (final Cursor cursor = database.query(TABLE_NAME, TwoFactorIcon.PROJECTION, String.format("%s=? AND %s=?", TwoFactorIcon.SOURCE, TwoFactorIcon.SOURCE_ID), new String[] { source, source_id }, null, null, null, null)) {
                if ((cursor != null) && (cursor.getCount() == 1) && cursor.moveToFirst()) { return instance(database, cursor); }
            }
        }
        else {
            final StringBuilder query = new StringBuilder();
            query.append("SELECT ");
            for (int i = 0; i < TwoFactorIcon.PROJECTION.length; i ++) {
                if (i > 0) { query.append(", "); }
                query.append(String.format("%s.%s", TABLE_NAME, TwoFactorIcon.PROJECTION[i]));
            }
            query.append(String.format(" FROM %s JOIN %s ON %s.%s=%s.%s WHERE %s.%s=? AND %s.%s=? AND %s.%s=?", TwoFactorAccountsHelper.TABLE_NAME, TABLE_NAME, TwoFactorAccountsHelper.TABLE_NAME, TwoFactorAccount.ICON, TABLE_NAME, TwoFactorIcon.ROW_ID, TwoFactorAccountsHelper.TABLE_NAME, TwoFactorAccount.SERVER_IDENTITY, TABLE_NAME, TwoFactorIcon.SOURCE, TABLE_NAME, TwoFactorIcon.SOURCE_ID));
            try (final Cursor cursor = database.rawQuery(query.toString(), new String[] { String.valueOf(server_identity.getRowId()), source, source_id })) {
                if ((cursor != null) && (cursor.getCount() == 1) && cursor.moveToFirst()) { return instance(database, cursor); }
            }
        }
        return null;
    }

    private void deleteFiles(@NotNull final Context context) {
        final File base_folder = getBaseFolder(context);
        if (base_folder.exists()) {
            File[] files = base_folder.listFiles();
            if (files != null) {
                for (final File file : files) {
                    file.delete();
                }
            }
        }
    }

    protected void delete(@NotNull final SQLiteDatabase database, @NotNull final Context context) throws Exception {
        super.delete(database);
        deleteFiles(context);
    }

    protected static @NotNull File getBaseFolder(@NotNull final Context context) {
        final File folder = new File(context.getFilesDir(), ICONS_FOLDER);
        folder.mkdirs();
        return folder;
    }
}
