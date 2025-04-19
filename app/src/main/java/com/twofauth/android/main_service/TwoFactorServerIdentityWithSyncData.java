package com.twofauth.android.main_service;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;

import com.twofauth.android.utils.Preferences;
import com.twofauth.android.database.TwoFactorAccount;
import com.twofauth.android.database.TwoFactorGroup;
import com.twofauth.android.database.TwoFactorServerIdentity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TwoFactorServerIdentityWithSyncData {
    private static final String LAST_SYNC_TIME_KEY_PREFIX = "last-sync-time-";
    private static final String LAST_SYNCED_ACCOUNTS_KEY_PREFIX = "last-synced-accounts-";

    private static final String LAST_SYNC_ERROR_TIME_KEY_PREFIX = "last-sync-error-time-";
    private static final String LAST_SYNC_ERROR_KEY_PREFIX = "last-sync-error-";

    public final TwoFactorServerIdentity storedData;

    public TwoFactorServerIdentityWithSyncData(@NotNull final TwoFactorServerIdentity server_identity) {
        storedData = server_identity;
    }

    public TwoFactorServerIdentityWithSyncData(@NotNull final Cursor cursor) {
        storedData = new TwoFactorServerIdentity(cursor);
    }

    public TwoFactorServerIdentityWithSyncData() {
        storedData = new TwoFactorServerIdentity();
    }

    private String getPreferencesKey(@NotNull final String key) {
        return key + String.valueOf(storedData.getRowId());
    }

    public void onSyncSuccess(@NotNull final Context context, @Nullable final List<TwoFactorAccount> accounts, @Nullable final List<TwoFactorGroup> groups) {
        if (storedData.inDatabase()) {
            final SharedPreferences.Editor editor = Preferences.getDefaultSharedPreferences(context).edit();
            editor.putLong(getPreferencesKey(LAST_SYNC_TIME_KEY_PREFIX), System.currentTimeMillis());
            editor.putInt(getPreferencesKey(LAST_SYNCED_ACCOUNTS_KEY_PREFIX), accounts == null ? 0 : accounts.size());
            editor.remove(getPreferencesKey(LAST_SYNC_ERROR_TIME_KEY_PREFIX));
            editor.remove(getPreferencesKey(LAST_SYNC_ERROR_KEY_PREFIX));
            editor.apply();
        }
    }

    public void onSyncError(@NotNull final Context context, @NotNull final String error_message) {
        if (storedData.inDatabase()) {
            final SharedPreferences.Editor editor = Preferences.getDefaultSharedPreferences(context).edit();
            editor.putLong(getPreferencesKey(LAST_SYNC_ERROR_TIME_KEY_PREFIX), System.currentTimeMillis());
            editor.putString(getPreferencesKey(LAST_SYNC_ERROR_KEY_PREFIX), error_message);
            editor.apply();
        }
    }

    public void onDataDeleted(@NotNull final Context context) {
        if (storedData.inDatabase()) {
            final SharedPreferences.Editor editor = Preferences.getDefaultSharedPreferences(context).edit();
            editor.remove(getPreferencesKey(LAST_SYNC_TIME_KEY_PREFIX));
            editor.remove(getPreferencesKey(LAST_SYNCED_ACCOUNTS_KEY_PREFIX));
            editor.remove(getPreferencesKey(LAST_SYNC_ERROR_TIME_KEY_PREFIX));
            editor.remove(getPreferencesKey(LAST_SYNC_ERROR_KEY_PREFIX));
            editor.apply();
        }
    }

    public boolean hasBeenSynced(@NotNull final Context context) {
        return getLastSyncTime(context) != 0;
    }

    public long getLastSyncTime(@NotNull final Context context) {
        return storedData.inDatabase() ? Preferences.getDefaultSharedPreferences(context).getLong(getPreferencesKey(LAST_SYNC_TIME_KEY_PREFIX), 0) : 0;
    }

    public int getLastSyncedAccounts(@NotNull final Context context) {
        return storedData.inDatabase() ? Preferences.getDefaultSharedPreferences(context).getInt(getPreferencesKey(LAST_SYNCED_ACCOUNTS_KEY_PREFIX), 0) : 0;
    }

    public boolean hasSyncErrors(@NotNull final Context context) {
        return getLastSyncErrorTime(context) != 0;
    }

    public long getLastSyncErrorTime(@NotNull final Context context) {
        return storedData.inDatabase() ? Preferences.getDefaultSharedPreferences(context).getLong(getPreferencesKey(LAST_SYNC_ERROR_TIME_KEY_PREFIX), 0) : 0;
    }

    public @Nullable String getLastSyncError(@NotNull final Context context) {
        return storedData.inDatabase() ? Preferences.getDefaultSharedPreferences(context).getString(getPreferencesKey(LAST_SYNC_ERROR_KEY_PREFIX), null) : null;
    }
}
