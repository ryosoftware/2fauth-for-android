package com.twofauth.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.preference.PreferenceManager;

import org.jetbrains.annotations.NotNull;

public class Constants {
    public static final String LOG_TAG_NAME = "2FAuth";

    public static final Uri GITHUB_REPO = Uri.parse("https://github.com/kslcsdalsadg/2FAuth-for-android");

    public static final String MAIN_SERVICE_NOTIFICATION_CHANNEL = "main-service";
    public static final int MAIN_SERVICE_PERSISTENT_NOTIFICATION_ID = 1001;

    // App options
    public static final String UNGROUP_OTP_CODE_KEY = "ungroup-otp-code";
    public static final String DISPLAY_ACCOUNT_GROUP_KEY = "display-account-group";
    public static final String DISABLE_SCREENSHOTS_KEY = "disable-screenshots";

    public static final String MINIMIZE_APP_AFTER_COPY_TO_CLIPBOARD_KEY = "minimize-app-after-copy-to-clipboard";
    public static final String HIDE_OTP_AUTOMATICALLY_KEY = "hide-otp-codes-automatically";

    public static final String PIN_ACCESS_KEY = "pin-access";

    public static final String FINGERPRINT_ACCESS_KEY = "fingerprint-access";

    // 2FA Server accounts data
    public static final String TWO_FACTOR_AUTH_SERVER_LOCATION_KEY = "server";
    public static final String TWO_FACTOR_AUTH_TOKEN_KEY = "token";

    public static final String TWO_FACTOR_AUTH_CODES_LAST_SYNC_TIME_KEY = "2fa-last-sync-time";

    public static final String TWO_FACTOR_AUTH_CODES_LAST_SYNC_ERROR_TIME_KEY = "2fa-last-sync-error-time";
    public static final String TWO_FACTOR_AUTH_CODES_LAST_SYNC_ERROR_KEY = "2fa-last-sync-error";

    // 2FA Account fields
    public static final String TWO_FACTOR_AUTH_ACCOUNTS_DATA_KEY = "2fa-accounts";
    public static final String TWO_FACTOR_AUTH_ACCOUNTS_DATA_SIZE_KEY = "2fa-accounts-size";
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
    public static final String TWO_FACTOR_AUTH_GROUP_ID_KEY = "id";
    public static final String TWO_FACTOR_AUTH_GROUP_NAME_KEY = "name";

    public static SharedPreferences getDefaultSharedPreferences(@NotNull final Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
