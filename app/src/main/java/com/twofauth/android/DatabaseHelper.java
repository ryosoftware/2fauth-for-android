package com.twofauth.android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.Log;

import com.twofauth.android.database.TwoFactorServerIdentity;
import com.twofauth.android.database.TwoFactorServerIdentitiesHelper;
import com.twofauth.android.database.TwoFactorAccount;
import com.twofauth.android.database.TwoFactorAccountsHelper;
import com.twofauth.android.database.TwoFactorGroup;
import com.twofauth.android.database.TwoFactorGroupsHelper;
import com.twofauth.android.database.TwoFactorIcon;
import com.twofauth.android.database.TwoFactorIconsHelper;
import com.twofauth.android.utils.KeyStore;
import com.twofauth.android.utils.Preferences;
import com.twofauth.android.utils.Threads;

import net.zetetic.database.sqlcipher.SQLiteDatabase;
import net.zetetic.database.sqlcipher.SQLiteOpenHelper;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Base64;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "database.db";
    private static final int DATABASE_VERSION = 2;

    private static final String DATABASE_PASSWORD_KEY = "database-password";
    private static final int PASSWORD_LENGTH = 128;

    private final Context mContext;

    private final TwoFactorServerIdentitiesHelper mTwoFactorServerIdentitiesHelper;
    private final TwoFactorGroupsHelper mTwoFactorGroupsHelper;
    private final TwoFactorIconsHelper mTwoFactorIconsHelper;
    private final TwoFactorAccountsHelper mTwoFactorAccountsHelper;

    private SQLiteDatabase mReadableDatabase = null;

    private SQLiteDatabase mWritableDatabase = null;
    private Thread mWritableDatabaseOwner = null;

    DatabaseHelper(@NotNull final Context context) {
        super(context, DATABASE_NAME, getDatabasePassword(context), null, DATABASE_VERSION, 1, null, null, false);
        mContext = context;
        System.loadLibrary("sqlcipher");
        mTwoFactorServerIdentitiesHelper = new TwoFactorServerIdentitiesHelper(this);
        mTwoFactorGroupsHelper = new TwoFactorGroupsHelper(this);
        mTwoFactorIconsHelper = new TwoFactorIconsHelper(this);
        mTwoFactorAccountsHelper = new TwoFactorAccountsHelper(this);
        context.getDatabasePath(DatabaseHelper.DATABASE_NAME).getParentFile().mkdirs();
    }

    @Override
    public void onConfigure(@NotNull final SQLiteDatabase database) {
        super.onConfigure(database);
        database.setForeignKeyConstraintsEnabled(true);
    }

    @SuppressLint("DefaultLocale")
    private void onCreateV2(@NotNull final SQLiteDatabase database) {
        final Resources resources = mContext.getResources();
        database.execSQL(String.format("CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s TEXT, %s TEXT NOT NULL, %s TEXT NOT NULL, %s INTEGER, %s TEXT, %s TEXT, %s INTEGER DEFAULT 0, %s INTEGER DEFAULT %d, %s INTEGER DEFAULT %d)", TwoFactorServerIdentitiesHelper.TABLE_NAME, TwoFactorServerIdentity.ROW_ID, TwoFactorServerIdentity.LABEL, TwoFactorServerIdentity.SERVER, TwoFactorServerIdentity.TOKEN, TwoFactorServerIdentity.REMOTE_ID, TwoFactorServerIdentity.NAME, TwoFactorServerIdentity.EMAIL, TwoFactorServerIdentity.IS_ADMIN, TwoFactorServerIdentity.SYNC_ON_STARTUP, resources.getBoolean(R.bool.sync_on_startup) ? 1 : 0, TwoFactorServerIdentity.SYNC_IMMEDIATELY, resources.getBoolean(R.bool.sync_immediately) ? 1 : 0));
        database.execSQL(String.format("CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s INTEGER NOT NULL REFERENCES %s(%s) ON DELETE RESTRICT, %s INTEGER DEFAULT %d, %s TEXT DEFAULT '', %s INTEGER DEFAULT %d)", TwoFactorGroupsHelper.TABLE_NAME, TwoFactorGroup.ROW_ID, TwoFactorGroup.SERVER_IDENTITY, TwoFactorServerIdentitiesHelper.TABLE_NAME, TwoFactorServerIdentity.ROW_ID, TwoFactorGroup.REMOTE_ID, 0, TwoFactorGroup.NAME, TwoFactorGroup.STATUS, TwoFactorGroup.STATUS_DEFAULT));
        database.execSQL(String.format("CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s SOURCE, %s TEXT)", TwoFactorIconsHelper.TABLE_NAME, TwoFactorIcon.ROW_ID, TwoFactorIcon.SOURCE, TwoFactorIcon.SOURCE_ID));
        database.execSQL(String.format("CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s INTEGER NOT NULL REFERENCES %s(%s) ON DELETE RESTRICT, %s INTEGER DEFAULT %d, %s TEXT DEFAULT '', %s TEXT DEFAULT '', %s INTEGER REFERENCES %s(%s) ON DELETE RESTRICT, %s INTEGER REFERENCES %s(%s) ON DELETE RESTRICT, %s TEXT DEFAULT '%s', %s TEXT DEFAULT '', %s INTEGER DEFAULT %d, %s TEXT DEFAULT '%s', %s INTEGER DEFAULT %d, %s INTEGER DEFAULT %d, %s INTEGER DEFAULT 0, %s INTEGER DEFAULT %d)", TwoFactorAccountsHelper.TABLE_NAME, TwoFactorAccount.ROW_ID, TwoFactorAccount.SERVER_IDENTITY, TwoFactorServerIdentitiesHelper.TABLE_NAME, TwoFactorServerIdentity.ROW_ID, TwoFactorAccount.REMOTE_ID, 0, TwoFactorAccount.SERVICE, TwoFactorAccount.ACCOUNT, TwoFactorAccount.GROUP, TwoFactorGroupsHelper.TABLE_NAME, TwoFactorGroup.ROW_ID, TwoFactorAccount.ICON, TwoFactorIconsHelper.TABLE_NAME, TwoFactorIcon.ROW_ID, TwoFactorAccount.OTP_TYPE, Constants.DEFAULT_OTP_TYPE, TwoFactorAccount.SECRET, TwoFactorAccount.OTP_LENGTH, Constants.DEFAULT_OTP_LENGTH, TwoFactorAccount.ALGORITHM, Constants.DEFAULT_ALGORITHM, TwoFactorAccount.PERIOD, Constants.DEFAULT_PERIOD, TwoFactorAccount.COUNTER, Constants.DEFAULT_COUNTER, TwoFactorAccount.LAST_USE, TwoFactorAccount.STATUS, TwoFactorAccount.STATUS_DEFAULT));
    }

    @Override
    public void onCreate(@NotNull final SQLiteDatabase database) {
        onCreateV2(database);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onUpgrade(@NotNull final SQLiteDatabase database, final int old_version, final int new_version) {
        if (old_version < 2) {
            database.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s INTEGER DEFAULT %d", TwoFactorGroupsHelper.TABLE_NAME, TwoFactorGroup.STATUS, TwoFactorGroup.STATUS_DEFAULT));
        }
    }

    public @Nullable synchronized SQLiteDatabase open(final boolean can_write) {
        final Thread thread = Threads.getCurrent();
        while (can_write && (mWritableDatabaseOwner != null)) {
            try {
                wait();
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        if (can_write) {
            mWritableDatabaseOwner = thread;
            if (mWritableDatabase == null) {
                mWritableDatabase = getWritableDatabase();
            }
        }
        else if (mReadableDatabase == null) {
            mReadableDatabase = getReadableDatabase();
        }
        return can_write ? mWritableDatabase : mReadableDatabase;
    }

    public synchronized void close(@Nullable final SQLiteDatabase database) {
        if ((mWritableDatabaseOwner == Threads.getCurrent()) && (mWritableDatabase == database)) {
            mWritableDatabaseOwner = null;
            notify();
        }
    }

    public boolean beginTransaction(@NotNull final SQLiteDatabase database) {
        if (mWritableDatabase == database) {
            database.beginTransaction();
            return true;
        }
        return false;
    }

    public void endTransaction(@NotNull final SQLiteDatabase database, final boolean commit_transaction) {
        if (mWritableDatabase == database) {
            if (commit_transaction) {
                database.setTransactionSuccessful();
            }
            database.endTransaction();
        }
    }

    public void onTerminate() {
        try {
            if (mReadableDatabase != null) { mReadableDatabase.close(); }
            if (mWritableDatabase != null) { mWritableDatabase.close(); }
        }
        catch (Exception e) {
            Log.e(Main.LOG_TAG_NAME, "Exception while trying to effectively close a database", e);
        }
        finally {
            mReadableDatabase = mWritableDatabase = null;
            mWritableDatabaseOwner = null;
        }
    }

    private static @NotNull String getDatabasePassword(@NotNull final Context context) {
        final SharedPreferences preferences = Preferences.getDefaultSharedPreferences(context);
        if (! preferences.contains(DATABASE_PASSWORD_KEY)) {
            final SharedPreferences.Editor editor = preferences.edit();
            Preferences.putEncryptedString(context, editor, DATABASE_PASSWORD_KEY, Base64.getEncoder().withoutPadding().encodeToString(KeyStore.getRandom(PASSWORD_LENGTH)));
            editor.apply();
        }
        final String password = Preferences.getEncryptedString(context, preferences, DATABASE_PASSWORD_KEY, null);
        if (password == null) {
            throw new RuntimeException("Cannot decode database password!");
        }
        return password;
    }

    public @NotNull TwoFactorServerIdentitiesHelper getTwoFactorServerIdentitiesHelper() { return mTwoFactorServerIdentitiesHelper; }

    public @NotNull TwoFactorGroupsHelper getTwoFactorGroupsHelper() { return mTwoFactorGroupsHelper; }

    public @NotNull TwoFactorIconsHelper getTwoFactorIconsHelper() {
        return mTwoFactorIconsHelper;
    }

    public @NotNull TwoFactorAccountsHelper getTwoFactorAccountsHelper() { return mTwoFactorAccountsHelper; }
}

