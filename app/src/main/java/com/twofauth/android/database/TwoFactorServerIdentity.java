package com.twofauth.android.database;

import android.content.ContentValues;
import android.content.res.Resources;
import android.database.Cursor;

import com.twofauth.android.Main;
import com.twofauth.android.R;
import com.twofauth.android.utils.Strings;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TwoFactorServerIdentity extends TableRow {
    public static final String LABEL = "label";
    public static final String SERVER = "server";
    public static final String TOKEN = "token";
    public static final String REMOTE_ID = "remote_id";
    public static final String NAME = "name";
    public static final String EMAIL = "email";
    public static final String IS_ADMIN = "is_admin";
    public static final String SYNC_ON_STARTUP = "sync_on_startup";
    public static final String SYNC_IMMEDIATELY = "sync_immediately";

    protected static final String[] PROJECTION = new String[] {
        ROW_ID,
        LABEL,
        SERVER,
        TOKEN,
        REMOTE_ID,
        NAME,
        EMAIL,
        IS_ADMIN,
        SYNC_ON_STARTUP,
        SYNC_IMMEDIATELY,
    };

    private static final int LABEL_ORDER = ROW_ID_ORDER  + 1;
    private static final int SERVER_ORDER = LABEL_ORDER  + 1;
    private static final int TOKEN_ORDER = SERVER_ORDER + 1;
    private static final int REMOTE_ID_ORDER = TOKEN_ORDER + 1;
    private static final int NAME_ORDER = REMOTE_ID_ORDER + 1;
    private static final int EMAIL_ORDER = NAME_ORDER + 1;
    private static final int IS_ADMIN_ORDER = EMAIL_ORDER + 1;
    private static final int SYNC_ON_STARTUP_ORDER = IS_ADMIN_ORDER + 1;
    private static final int SYNC_IMMEDIATELY_ORDER = SYNC_ON_STARTUP_ORDER + 1;

    private String mLabel;
    private String mServer;
    private String mToken;
    private int mRemoteId;
    private String mName;
    private String mEmail;
    private boolean mIsAdmin;
    private boolean mSyncOnStartup;
    private boolean mSyncImmediately;

    public TwoFactorServerIdentity(@NotNull final Cursor cursor) {
        super(TwoFactorServerIdentitiesHelper.TABLE_NAME, cursor);
        mLabel = cursor.getString(LABEL_ORDER);
        mServer = cursor.getString(SERVER_ORDER);
        mToken = cursor.getString(TOKEN_ORDER);
        mRemoteId = cursor.getInt(REMOTE_ID_ORDER);
        mName = cursor.getString(NAME_ORDER);
        mEmail = cursor.getString(EMAIL_ORDER);
        mIsAdmin = cursor.getInt(IS_ADMIN_ORDER) != 0;
        mSyncOnStartup = cursor.getInt(SYNC_ON_STARTUP_ORDER) != 0;
        mSyncImmediately = cursor.getInt(SYNC_IMMEDIATELY_ORDER) != 0;
    }

    public TwoFactorServerIdentity() {
        super(TwoFactorServerIdentitiesHelper.TABLE_NAME);
        mLabel = null;
        mServer = null;
        mToken = null;
        mRemoteId = 0;
        mName = null;
        mEmail = null;
        mIsAdmin = false;
        final Resources resources = Main.getInstance().getResources();
        mSyncOnStartup = resources.getBoolean(R.bool.sync_on_startup);
        mSyncImmediately = resources.getBoolean(R.bool.sync_immediately);
    }

    public TwoFactorServerIdentity(@NotNull final TwoFactorServerIdentity server_identity) {
        this();
        setFrom(server_identity);
    }

    public void setFrom(@NotNull final TwoFactorServerIdentity server_identity) {
        setRowId(server_identity.getRowId());
        setLabel(server_identity.getLabel());
        setServer(server_identity.getServer());
        setToken(server_identity.getToken());
        setRemoteId(server_identity.getRemoteId());
        setName(server_identity.getName());
        setEmail(server_identity.getEmail());
        setIsAdmin(server_identity.isAdmin());
        setSyncOnStartup(server_identity.isSyncingOnStartup());
        setSyncImmediately(server_identity.isSyncingImmediately());
        setDirty(server_identity.isDirty());
    }

    public @Nullable String getTitle() { return hasLabel() ? getLabel() : Strings.isEmptyOrNull(mName) ? getServer() : getName(); }

    public boolean hasLabel() { return ! Strings.isEmptyOrNull(getLabel()); }

    public @Nullable String getLabel() { return mLabel; }

    public void setLabel(@Nullable final String label) {
        if (! Strings.equals(mLabel, label)) { setDirty(SERVER, mLabel = label); }
    }

    public @Nullable String getServer() {
        return mServer;
    }

    public boolean hasServer() {
        return ! Strings.isEmptyOrNull(getServer());
    }

    public void setServer(@Nullable final String server) {
        if (! Strings.equals(mServer, server)) { setDirty(SERVER, mServer = server); }
    }

    public @Nullable String getToken() {
        return mToken;
    }

    public boolean hasToken() {
        return ! Strings.isEmptyOrNull(getToken());
    }

    public void setToken(@Nullable final String token) {
        if (! Strings.equals(mToken, token)) { setDirty(TOKEN, mToken = token); }
    }

    public int getRemoteId() {
        return mRemoteId;
    }

    public void setRemoteId(final int server_id) {
        if (mRemoteId != server_id) { setDirty(REMOTE_ID, mRemoteId = server_id); }
    }

    public @Nullable String getName() {
        return mName;
    }

    public boolean hasName() { return ! Strings.isEmptyOrNull(getName()); }

    public void setName(@Nullable final String name) {
        if (! Strings.equals(mName, name)) { setDirty(NAME, mName = name); }
    }

    public @Nullable String getEmail() {
        return mEmail;
    }

    public void setEmail(@Nullable final String email) {
        if (! Strings.equals(mEmail, email)) { setDirty(EMAIL, mEmail = email); }
    }

    public boolean isAdmin() {
        return mIsAdmin;
    }

    public void setIsAdmin(final boolean is_admin) {
        if (mIsAdmin != is_admin) { setDirty(IS_ADMIN, mIsAdmin = is_admin); }
    }

    public boolean isSyncingOnStartup() {
        return mSyncOnStartup;
    }

    public void setSyncOnStartup(final boolean sync_on_startup) {
        if (mSyncOnStartup != sync_on_startup) { setDirty(SYNC_ON_STARTUP, mSyncOnStartup = sync_on_startup); }
    }

    public boolean isSyncingImmediately() {
        return mSyncImmediately;
    }

    public void setSyncImmediately(final boolean sync_immediately) {
        if (mSyncImmediately != sync_immediately) { setDirty(SYNC_IMMEDIATELY, mSyncImmediately = sync_immediately); }
    }

    protected void setDatabaseValues(@NotNull final ContentValues values) {
        if (values.size() == 0) {
            values.put(LABEL, mLabel);
            values.put(SERVER, mServer);
            values.put(TOKEN, mToken);
            values.put(REMOTE_ID, mRemoteId);
            values.put(NAME, mName);
            values.put(EMAIL, mEmail);
            values.put(IS_ADMIN, mIsAdmin ? 1 : 0);
            values.put(SYNC_ON_STARTUP, mSyncImmediately ? 1 : 0);
            values.put(SYNC_IMMEDIATELY, mSyncImmediately ? 1 : 0);
        }
    }
}
