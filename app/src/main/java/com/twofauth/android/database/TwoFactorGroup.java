package com.twofauth.android.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.util.Log;

import com.twofauth.android.API;
import com.twofauth.android.Constants;
import com.twofauth.android.Main;
import com.twofauth.android.utils.Strings;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

public class TwoFactorGroup extends TableRow {
    public static final String SERVER_IDENTITY = "server_identity";
    public static final String REMOTE_ID = "remote_id";
    public static final String NAME = "name";
    public static final String STATUS = "status";

    public static final int STATUS_DEFAULT = 0;
    public static final int STATUS_NOT_SYNCED = 1;
    public static final int STATUS_DELETED = 2;

    protected static final String[] PROJECTION = new String[] {
        ROW_ID,
        SERVER_IDENTITY,
        REMOTE_ID,
        NAME,
        STATUS,
    };

    private static final int SERVER_IDENTITY_ORDER = ROW_ID_ORDER + 1;
    private static final int REMOTE_ID_ORDER = SERVER_IDENTITY_ORDER + 1;
    private static final int NAME_ORDER = REMOTE_ID_ORDER + 1;
    private static final int STATUS_ORDER = NAME_ORDER + 1;

    private TwoFactorServerIdentity mServerIdentity;
    private int mRemoteId;
    private String mName;
    private int mStatus;

    public TwoFactorGroup(@NotNull final SQLiteDatabase database, @NotNull final Cursor cursor) throws Exception {
        super(TwoFactorGroupsHelper.TABLE_NAME, cursor);
        mServerIdentity = Main.getInstance().getDatabaseHelper().getTwoFactorServerIdentitiesHelper().instance(database, cursor.getLong(SERVER_IDENTITY_ORDER));
        mRemoteId = cursor.getInt(REMOTE_ID_ORDER);
        mName = cursor.getString(NAME_ORDER);
        mStatus = cursor.getInt(STATUS_ORDER);
    }

    public TwoFactorGroup() {
        super(TwoFactorGroupsHelper.TABLE_NAME);
        mRemoteId = 0;
        mName = null;
        mStatus = STATUS_NOT_SYNCED;
    }

    public @NotNull JSONObject toJSONObject() {
        try {
            final JSONObject object = new JSONObject();
            object.put(SERVER_IDENTITY, hasServerIdentity() ? getServerIdentity().getRowId() : -1);
            if (getRemoteId() > 0) { object.put(Constants.GROUP_DATA_ID_KEY, getRemoteId()); }
            object.put(Constants.GROUP_DATA_NAME_KEY, getName());
            return object;
        }
        catch (JSONException e) {
            Log.e(Main.LOG_TAG_NAME, "Exception while trying to convert a group to JSON", e);
            throw new RuntimeException(e);
        }
    }

    public boolean hasServerIdentity() {
        return ((mServerIdentity != null) && (mServerIdentity.getRemoteId() != 0));
    }

    public @Nullable TwoFactorServerIdentity getServerIdentity() { return mServerIdentity; }

    public void setServerIdentity(@Nullable final TwoFactorServerIdentity server_identity) {
        mServerIdentity = server_identity;
        setDirty(SERVER_IDENTITY, true);
    }

    public boolean isReferenced(@NotNull final SQLiteDatabase database) {
        try (final Cursor cursor = database.query(true, TwoFactorAccountsHelper.TABLE_NAME, new String[] { "COUNT(*)" }, String.format("%s=?", TwoFactorAccount.GROUP), new String[] { String.valueOf(_id) }, null, null, null, null, null)) {
            if ((cursor != null) && (cursor.getCount() == 1) && cursor.moveToFirst()) {
                return cursor.getInt(0) > 0;
            }
        }
        return false;
    }

    public boolean isReferenced() {
        final SQLiteDatabase database = Main.getInstance().getDatabaseHelper().open(false);
        if (database != null) {
            try {
                return isReferenced(database);
            }
            finally {
                Main.getInstance().getDatabaseHelper().close(database);
            }
        }
        return false;
    }

    public boolean isRemote() {
        return mRemoteId != 0;
    }

    public int getRemoteId() {
        return mRemoteId;
    }

    public void setRemoteId(final int server_id) {
        if (mRemoteId != server_id) {
            setDirty(REMOTE_ID, mRemoteId = server_id);
            setDirty(STATUS, mStatus = STATUS_NOT_SYNCED);
        }
    }

    public boolean hasName() { return ! Strings.isEmptyOrNull(getName()); }

    public @Nullable String getName() {
        return mName;
    }

    public void setName(@Nullable final String name) {
        if (! Strings.equals(mName, name)) {
            setDirty(NAME, mName = name);
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
        if (values.size() == 0) {
            if (mServerIdentity == null) { throw new SQLException("Server Identity cannot be NULL"); }
            values.put(SERVER_IDENTITY, mServerIdentity.getRowId());
            values.put(REMOTE_ID, mRemoteId);
            values.put(NAME, mName);
            values.put(STATUS, mStatus);
        }
        else {
            if (values.containsKey(SERVER_IDENTITY)) {
                if (mServerIdentity == null) { throw new SQLException("Server Identity cannot be NULL"); }
                values.put(SERVER_IDENTITY, mServerIdentity.getRowId());
            }
        }
    }
}
