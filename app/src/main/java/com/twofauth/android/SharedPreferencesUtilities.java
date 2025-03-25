package com.twofauth.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import org.jetbrains.annotations.NotNull;

public class SharedPreferencesUtilities {
    private static String getEncryptedString(@NotNull final Context context, @NotNull final String encryption_key_alias, @NotNull final SharedPreferences preferences, @NotNull final String preference_key, @Nullable final String default_value) {
        try {
            return preferences.contains(preference_key) ? KeyStoreUtils.decrypt(encryption_key_alias, preferences.getString(preference_key, null)) : default_value;
        }
        catch (Exception e) {
            UiUtils.showToast(context, R.string.keystore_access_error);
            Log.e(Constants.LOG_TAG_NAME, "Exception while trying to decrypt a string");
        }
        return null;
    }

    public static String getEncryptedString(@NotNull final Context context, @NotNull final String encryption_key_alias, @NotNull final String preference_key, @Nullable final String default_value) {
        return getEncryptedString(context, encryption_key_alias, getDefaultSharedPreferences(context), preference_key, default_value);
    }

    public static String getEncryptedString(@NotNull final Context context, @NotNull final SharedPreferences preferences, @NotNull final String preference_key, @Nullable final String default_value) {
        return getEncryptedString(context, Constants.APP_DATA_ENCRYPTION_KEYSTORE_ENTRY_NAME, preferences, preference_key, default_value);
    }

    public static String getEncryptedString(@NotNull final Context context, @NotNull final String preference_key, @Nullable final String default_value) {
        return getEncryptedString(context, getDefaultSharedPreferences(context), preference_key, default_value);
    }

    public static boolean putEncryptedString(@NotNull final Context context, @NotNull final String encryption_key_alias, @NotNull final SharedPreferences.Editor editor, @NotNull final String preference_key, @NotNull final String value) {
        try {
            KeyStoreUtils.addKeyAlias(encryption_key_alias);
            editor.putString(preference_key, KeyStoreUtils.encrypt(encryption_key_alias, value));
            return true;
        }
        catch (Exception e) {
            UiUtils.showToast(context, R.string.keystore_access_error);
            Log.e(Constants.LOG_TAG_NAME, "Exception while trying to encrypt a string");
        }
        return false;
    }

    public static boolean putEncryptedString(@NotNull final Context context, @NotNull final SharedPreferences.Editor editor, @NotNull final String preference_key, @NotNull final String value) {
        return putEncryptedString(context, Constants.APP_DATA_ENCRYPTION_KEYSTORE_ENTRY_NAME, editor, preference_key, value);
    }

    public static SharedPreferences getDefaultSharedPreferences(@NotNull final Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
