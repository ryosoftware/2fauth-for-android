package com.twofauth.android.database;

import android.content.ContentValues;
import android.database.Cursor;

import org.jetbrains.annotations.NotNull;

public abstract class SynceableTableRow extends TableRow {
    public static final String REMOTE_ID = "remote_id";
    public static final String STATUS = "status";

    public static final int STATUS_DEFAULT = 0;
    public static final int STATUS_NOT_SYNCED = 1;
    public static final int STATUS_DELETED = 2;

    protected int mRemoteId;
    protected int mStatus;

    public SynceableTableRow(@NotNull final String table_name, @NotNull final Cursor cursor, final int remote_id_order, final int status_order) {
        super(table_name, cursor);
        mRemoteId = cursor.getInt(remote_id_order);
        mStatus = cursor.getInt(status_order);
    }

    public SynceableTableRow(@NotNull final String table_name) {
        super(table_name);
        mRemoteId = 0;
        mStatus = STATUS_NOT_SYNCED;
    }

    public boolean isRemote() {
        return mRemoteId > 0;
    }

    public int getRemoteId() {
        return mRemoteId;
    }

    public void setRemoteId(final int id) {
        if ((! isDeleted()) && (mRemoteId != id)) {
            setDirty(REMOTE_ID, mRemoteId = id);
            setDirty(STATUS, mStatus = STATUS_NOT_SYNCED);
        }
    }

    public boolean isDeleted() {
        return mStatus == STATUS_DELETED;
    }

    public boolean isSynced() {
        return (mStatus == STATUS_DEFAULT);
    }

    public int getStatus() {
        return mStatus;
    }

    public void setStatus(final int status) {
        if (mStatus != status) { setDirty(STATUS, mStatus = status); }
    }

    protected void setDatabaseValues(@NotNull final ContentValues values) {
        values.put(REMOTE_ID, mRemoteId);
        values.put(STATUS, mStatus);
    }
}
