package com.twofauth.android.database;

import android.content.Context;
import android.database.Cursor;

import com.twofauth.android.DatabaseHelper;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
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
