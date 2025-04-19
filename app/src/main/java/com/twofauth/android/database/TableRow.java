package com.twofauth.android.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class TableRow {
    public static final String ROW_ID = "_id";

    public static final int ROW_ID_ORDER = 0;

    private final String mTableName;

    protected long _id;

    private ContentValues mContentValues = null;
    private boolean mDirty = false;

    public TableRow(@NotNull final String table_name, @NotNull final Cursor cursor) {
        _id = cursor.getLong(ROW_ID_ORDER);
        mTableName = table_name;
        mDirty = false;
    }

    public TableRow(@NotNull final String table_name) {
        _id = -1;
        mTableName = table_name;
    }

    public boolean inDatabase() { return _id >= 0; }

    public long getRowId() {
        return _id;
    }

    public void setRowId(final long row_id) {
        if ((_id != row_id)) {
            _id = row_id;
            setDirty();
        }
    }

    private @NotNull synchronized ContentValues getContentValues() {
        if (mContentValues == null) { mContentValues = new ContentValues(); }
        return mContentValues;
    }

    public boolean isDirty() {
        return mDirty;
    }

    protected synchronized void setDirty(final boolean dirty) {
        if (mDirty != dirty) {
            mDirty = dirty;
            if ((! mDirty) && (mContentValues != null)) { mContentValues.clear(); }
        }
    }

    protected void setDirty() {
        setDirty(true);
    }

    protected void setDirty(@NotNull final String key, @Nullable final String value) {
        getContentValues().put(key, value);
        setDirty();
    }

    protected void setDirty(@NotNull final String key, final long value) {
        getContentValues().put(key, value);
        setDirty();
    }

    protected void setDirty(@NotNull final String key, final int value) {
        getContentValues().put(key, value);
        setDirty();
    }

    protected void setDirty(@NotNull final String key, final boolean value) {
        getContentValues().put(key, value ? 1 : 0);
        setDirty();
    }

    protected boolean onSavingData(@NotNull final SQLiteDatabase database, @NotNull final Context context, @NotNull final ContentValues values) throws Exception { return true; }

    protected abstract void setDatabaseValues(@NotNull final ContentValues values);

    protected void onDataSaved(@NotNull final Context context) throws Exception {}

    private void save(@NotNull final SQLiteDatabase database, @NotNull final ContentValues values) throws SQLException {
        if (_id < 0) {
            if ((_id = database.insert(mTableName, null, values)) < 0) { throw new SQLException(String.format("Error adding data to '%s' table", mTableName)); }
        }
        else if (database.update(mTableName, values, String.format("%s=?", ROW_ID), new String[] { String.valueOf(_id) }) != 1) {
            throw new SQLException(String.format("Error updating data at '%s' table", mTableName));
        }
    }

    public synchronized void save(@NotNull final SQLiteDatabase database, @NotNull final Context context) throws Exception {
        if ((_id < 0) || (isDirty())) {
            final ContentValues values = getContentValues();
            if (onSavingData(database, context, values)) {
                setDatabaseValues(values);
                save(database, values);
                setDirty(false);
                onDataSaved(context);
            }
        }
    }

    protected boolean onDeletingData(@NotNull final Context context) throws Exception { return true; }

    protected void onDataDeleted(@NotNull final SQLiteDatabase database, @NotNull final Context context) throws Exception {}

    public synchronized void delete(@NotNull final SQLiteDatabase database, @NotNull final Context context) throws Exception {
        if (onDeletingData(context)) {
            if ((_id >= 0) && (database.delete(mTableName, String.format("%s=?", ROW_ID), new String[] { String.valueOf(_id) }) != 1)) { throw new Exception(String.format("Error deleting data from '%s' table", mTableName)); }
            onDataDeleted(database, context);
            _id = -1;
        }
    }
}
