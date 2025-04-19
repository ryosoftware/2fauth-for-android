package com.twofauth.android.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;

import com.twofauth.android.Main;
import com.twofauth.android.utils.Strings;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TwoFactorGroup extends TableRow {
    public static final String SERVER_IDENTITY = "server_identity";
    public static final String REMOTE_ID = "remote_id";
    public static final String NAME = "name";

    protected static final String[] PROJECTION = new String[] {
        ROW_ID,
        SERVER_IDENTITY,
        REMOTE_ID,
        NAME,
    };

    private static final int SERVER_IDENTITY_ORDER = ROW_ID_ORDER + 1;
    private static final int REMOTE_ID_ORDER = SERVER_IDENTITY_ORDER + 1;
    private static final int NAME_ORDER = REMOTE_ID_ORDER + 1;

    private TwoFactorServerIdentity mServerIdentity;
    private int mRemoteId;
    private String mName;

    public TwoFactorGroup(@NotNull final SQLiteDatabase database, @NotNull final Cursor cursor) throws Exception {
        super(TwoFactorGroupsHelper.TABLE_NAME, cursor);
        mServerIdentity = Main.getInstance().getDatabaseHelper().getTwoFactorServerIdentitiesHelper().instance(database, cursor.getLong(SERVER_IDENTITY_ORDER));
        mRemoteId = cursor.getInt(REMOTE_ID_ORDER);
        mName = cursor.getString(NAME_ORDER);
    }

    public TwoFactorGroup() {
        super(TwoFactorGroupsHelper.TABLE_NAME);
        mRemoteId = 0;
        mName = null;
    }

    public boolean hasServerIdentity() {
        return ((mServerIdentity != null) && (mServerIdentity.getRemoteId() != 0));
    }

    public @Nullable TwoFactorServerIdentity getServerIdentity() { return mServerIdentity; }

    public void setServerIdentity(@Nullable final TwoFactorServerIdentity server_identity) {
        mServerIdentity = server_identity;
        setDirty(SERVER_IDENTITY, true);
    }

    public boolean isRemote() {
        return mRemoteId != 0;
    }

    public int getRemoteId() {
        return mRemoteId;
    }

    public void setRemoteId(final int server_id) {
        if (mRemoteId != server_id) { setDirty(REMOTE_ID, mRemoteId = server_id); }
    }

    public boolean hasName() { return ! Strings.isEmptyOrNull(getName()); }

    public @Nullable String getName() {
        return mName;
    }

    public void setName(@Nullable final String name) {
        if (! Strings.equals(mName, name)) { setDirty(NAME, mName = name); }
    }

    protected void setDatabaseValues(@NotNull final ContentValues values) {
        if (values.size() == 0) {
            if (mServerIdentity == null) { throw new SQLException("Server Identity cannot be NULL"); }
            values.put(SERVER_IDENTITY, mServerIdentity.getRowId());
            values.put(REMOTE_ID, mRemoteId);
            values.put(NAME, mName);
        }
        else {
            if (values.containsKey(SERVER_IDENTITY)) {
                if (mServerIdentity == null) { throw new SQLException("Server Identity cannot be NULL"); }
                values.put(SERVER_IDENTITY, mServerIdentity.getRowId());
            }
        }
    }
}
