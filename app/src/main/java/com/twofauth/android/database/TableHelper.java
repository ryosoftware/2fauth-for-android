package com.twofauth.android.database;

import android.database.Cursor;

import com.twofauth.android.DatabaseHelper;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class TableHelper<TableRow> {
    private final String mTableName;
    private final String mRowId;
    private final String[] mProjection;

    protected final DatabaseHelper mDatabaseHelper;

    public TableHelper(@NotNull final String table_name, @NotNull final String row_id, @NotNull final String[] projection, @NotNull final DatabaseHelper database_helper) {
        mTableName = table_name;
        mRowId = row_id;
        mProjection = projection;
        mDatabaseHelper = database_helper;
    }

    protected abstract @NotNull TableRow instance(@NotNull final SQLiteDatabase database, @NotNull final Cursor cursor) throws Exception;

    protected @Nullable TableRow instance(@NotNull final SQLiteDatabase database, final long id) throws Exception {
        try (final Cursor cursor = database.query(true, mTableName, mProjection, String.format("%s=?", mRowId), new String[] { String.valueOf(id) }, null, null, null, null, null)) {
            if ((cursor != null) && (cursor.getCount() == 1) && cursor.moveToFirst()) {
                return instance(database, cursor);
            }
        }
        return null;
    }

    protected abstract @NotNull TableRow instance() throws Exception;

    protected Cursor query(@NotNull final SQLiteDatabase database, @Nullable final Object data) {
        return database.query(mTableName, mProjection, null, null, null, null, null, null);
    }

    protected @Nullable List<TableRow> get(@NotNull final SQLiteDatabase database, @Nullable final Object data) throws Exception {
        try (final Cursor cursor = query(database, data)) {
            if ((cursor != null) && (cursor.getCount() > 0) && cursor.moveToFirst()) {
                final List<TableRow> elements = new ArrayList<TableRow>();
                while (! cursor.isAfterLast()) {
                    elements.add(instance(database, cursor));
                    cursor.moveToNext();
                }
                return elements;
            }
        }
        return null;
    }

    protected @Nullable List<TableRow> get(@NotNull final SQLiteDatabase database) throws Exception {
        return get(database, null);
    }

    protected @Nullable List<TableRow> get(@Nullable final Object data) throws Exception {
        final SQLiteDatabase database = mDatabaseHelper.open(false);
        if (database != null) {
            try {
                return get(database, data);
            }
            finally {
                mDatabaseHelper.close(database);
            }
        }
        return null;
    }

    protected @Nullable List<TableRow> get() throws Exception {
        return get((Object) null);
    }

    protected @Nullable TableRow get(@NotNull final SQLiteDatabase database, final long id) throws Exception {
        return instance(database, id);
    }

    protected @Nullable TableRow get(final long id) throws Exception {
        final SQLiteDatabase database = mDatabaseHelper.open(false);
        if (database != null) {
            try {
                return get(database, id);
            }
            finally {
                mDatabaseHelper.close(database);
            }
        }
        return null;
    }

    protected void delete(@NotNull final SQLiteDatabase database) throws Exception {
        database.delete(mTableName, null, null);
    }

    protected int count(@NotNull final SQLiteDatabase database, @Nullable final Object data) throws Exception {
        try (final Cursor cursor = query(database, data)) {
            return cursor.getCount();
        }
    }

    protected int count(@NotNull final SQLiteDatabase database) throws Exception {
        return count(database, null);
    }

    protected int count(@Nullable final Object data) throws Exception {
        final SQLiteDatabase database = mDatabaseHelper.open(false);
        if (database != null) {
            try {
                return count(database, data);
            }
            finally {
                mDatabaseHelper.close(database);
            }
        }
        return 0;
    }

    protected int count() throws Exception {
        return count((Object) null);
    }

    protected boolean exists(@NotNull final SQLiteDatabase database, long id) throws Exception {
        try (final Cursor cursor = query(database, id)) {
            return ((cursor != null) && (cursor.getCount() > 0));
        }
    }

    protected boolean exists(@NotNull final SQLiteDatabase database, @Nullable final Object data) throws Exception {
        try (final Cursor cursor = query(database, data)) {
            return cursor.getCount() > 0;
        }
    }

    protected boolean exists(@NotNull final SQLiteDatabase database) throws Exception {
        return exists(database, null);
    }

    protected boolean exists(@Nullable final Object data) throws Exception {
        final SQLiteDatabase database = mDatabaseHelper.open(false);
        if (database != null) {
            try {
                return exists(database, data);
            }
            finally {
                mDatabaseHelper.close(database);
            }
        }
        return false;
    }

    protected boolean exists() throws Exception {
        return exists((Object) null);
    }
}
