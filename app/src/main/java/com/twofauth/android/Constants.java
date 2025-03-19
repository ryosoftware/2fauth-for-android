package com.twofauth.android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.preference.PreferenceManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

public class Constants {
    public static final String LOG_TAG_NAME = "2FAuth";

    public static final Uri GITHUB_REPO = Uri.parse("https://github.com/kslcsdalsadg/2fauth-for-android");

    // App options

    public static final String SORT_ACCOUNTS_BY_LAST_USE_KEY = "sort-accounts-by-last-use";
    public static final String UNGROUP_OTP_CODE_KEY = "ungroup-otp-code";
    public static final String DISPLAY_ACCOUNT_GROUP_KEY = "display-account-group";

    public static final String DISABLE_SCREENSHOTS_KEY = "disable-screenshots";

    public static final String MINIMIZE_APP_AFTER_COPY_TO_CLIPBOARD_KEY = "minimize-app-after-copy-to-clipboard";

    public static final String VIBRATE_ON_SOME_ACTIONS_KEY = "vibrate-on-some-actions";
    public static final String HIDE_OTP_AUTOMATICALLY_KEY = "hide-otp-codes-automatically";

    public static final String PIN_ACCESS_KEY = "pin-access";

    public static final String FINGERPRINT_ACCESS_KEY = "fingerprint-access";

    public static final String AUTO_UPDATES_APP_KEY = "auto-updates-app";

    // 2FA Server accounts data (server and access token)
    public static final String TWO_FACTOR_AUTH_SERVER_LOCATION_KEY = "server";
    public static final String TWO_FACTOR_AUTH_TOKEN_KEY = "token";

    // 2FA last sync time (las correct sync time, last error sync (time and message)
    public static final String TWO_FACTOR_AUTH_CODES_LAST_SYNC_TIME_KEY = "2fa-last-sync-time";

    public static final String TWO_FACTOR_AUTH_CODES_LAST_SYNC_ERROR_TIME_KEY = "2fa-last-sync-error-time";
    public static final String TWO_FACTOR_AUTH_CODES_LAST_SYNC_ERROR_KEY = "2fa-last-sync-error";

    // 2FA last OTP generation code key prefix (the key is generated concatenating this prefix and the account ID)
    private static final String TWO_FACTOR_AUTH_ACCOUNT_LAST_USE_KEY_PREFIX = "2fa-last-use-";

    // Field in which loaded 2FA accounts are stored
    public static final String TWO_FACTOR_AUTH_ACCOUNTS_DATA_KEY = "2fa-accounts";
    public static final String TWO_FACTOR_AUTH_ACCOUNTS_DATA_LENGTH_KEY = "2fa-accounts-size";

    // Relevant fields from a 2FA account
    public static final String TWO_FACTOR_AUTH_ACCOUNT_DATA_ID_KEY = "id";
    public static final String TWO_FACTOR_AUTH_ACCOUNT_DATA_SERVICE_KEY = "service";
    public static final String TWO_FACTOR_AUTH_ACCOUNT_DATA_ACCOUNT_KEY = "account";
    public static final String TWO_FACTOR_AUTH_ACCOUNT_DATA_GROUP_KEY = "group_id";
    public static final String TWO_FACTOR_AUTH_ACCOUNT_DATA_ICON_KEY = "icon";
    public static final String TWO_FACTOR_AUTH_ACCOUNT_DATA_OTP_TYPE_KEY = "otp_type";
    public static final String TWO_FACTOR_AUTH_ACCOUNT_DATA_OTP_SECRET_KEY = "secret";
    public static final String TWO_FACTOR_AUTH_ACCOUNT_DATA_OTP_PASSWORD_LENGTH_KEY = "digits";
    public static final String TWO_FACTOR_AUTH_ACCOUNT_DATA_OTP_ALGORITHM_KEY = "algorithm";
    public static final String TWO_FACTOR_AUTH_ACCOUNT_DATA_PERIOD_KEY = "period";
    public static final String TWO_FACTOR_AUTH_ACCOUNT_DATA_COUNTER_KEY = "counter";

    public static final String[] TWO_FACTOR_AUTH_ACCOUNT_DATA_KEYS = new String[] {
        TWO_FACTOR_AUTH_ACCOUNT_DATA_ID_KEY,
        TWO_FACTOR_AUTH_ACCOUNT_DATA_SERVICE_KEY,
        TWO_FACTOR_AUTH_ACCOUNT_DATA_ACCOUNT_KEY,
        TWO_FACTOR_AUTH_ACCOUNT_DATA_GROUP_KEY,
        TWO_FACTOR_AUTH_ACCOUNT_DATA_ICON_KEY,
        TWO_FACTOR_AUTH_ACCOUNT_DATA_OTP_TYPE_KEY,
        TWO_FACTOR_AUTH_ACCOUNT_DATA_OTP_SECRET_KEY,
        TWO_FACTOR_AUTH_ACCOUNT_DATA_OTP_PASSWORD_LENGTH_KEY,
        TWO_FACTOR_AUTH_ACCOUNT_DATA_OTP_ALGORITHM_KEY,
        TWO_FACTOR_AUTH_ACCOUNT_DATA_PERIOD_KEY,
        TWO_FACTOR_AUTH_ACCOUNT_DATA_COUNTER_KEY,
    };

    // Relevant fields from a 2FA account group
    public static final String TWO_FACTOR_AUTH_GROUP_ID_KEY = "id";
    public static final String TWO_FACTOR_AUTH_GROUP_NAME_KEY = "name";

    public static SharedPreferences getDefaultSharedPreferences(@NotNull final Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    @SuppressLint("DefaultLocale")
    public static String getTwoFactorAccountLastUseKey(JSONObject object) {
        return String.format("%s-%d", TWO_FACTOR_AUTH_ACCOUNT_LAST_USE_KEY_PREFIX, object.optInt(TWO_FACTOR_AUTH_ACCOUNT_DATA_ID_KEY));
    }

    public static SharedPreferences.Editor deleteTwoFactorAccountLastUseKeys(@NotNull final SharedPreferences preferences, @Nullable SharedPreferences.Editor editor) {
        for (final String key : preferences.getAll().keySet()) {
            if (key.startsWith(TWO_FACTOR_AUTH_ACCOUNT_LAST_USE_KEY_PREFIX)) {
                if (editor == null) {
                    editor = preferences.edit();
                }
                editor.remove(key);
            }
        }
        return editor;
    }
    public static void deleteTwoFactorAccountLastUseKeys(@NotNull final Context context) {
        final SharedPreferences.Editor editor = deleteTwoFactorAccountLastUseKeys(getDefaultSharedPreferences(context), null);
        if (editor != null) {
            editor.apply();
        }
    }
}
