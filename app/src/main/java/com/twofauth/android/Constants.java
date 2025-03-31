package com.twofauth.android;

import android.net.Uri;

public class Constants {
    public static final String LOG_TAG_NAME = "2FAuth";

    public static final Uri GITHUB_REPO = Uri.parse("https://github.com/kslcsdalsadg/2fauth-for-android");

    // APP Keystore aliases and other data

    public static final String APP_DATA_ENCRYPTION_KEYSTORE_ENTRY_NAME = "encryption-key";
    public static final String FINGERPRINT_VALIDATED_KEYSTORE_ENTRY_NAME = "fingerprint-key";

    // 2FA Server accounts data (server and access token)

    public static final String TWO_FACTOR_AUTH_SERVER_LOCATION_KEY = "server";
    public static final String TWO_FACTOR_AUTH_TOKEN_KEY = "token";

    // 2FA last sync time (las correct sync time, last error sync (time and message)

    public static final String LAST_SYNC_TIME_KEY = "last-sync-time";
    public static final String LAST_SYNC_ERROR_TIME_KEY = "last-sync-error-time";
    public static final String LAST_SYNC_ERROR_KEY = "last-sync-error";

    // App options

    public static final String SYNC_ON_STARTUP_KEY = "sync-on-startup";
    public static final String SORT_ACCOUNTS_BY_LAST_USE_KEY = "sort-accounts-by-last-use";
    public static final String UNGROUP_OTP_CODE_KEY = "ungroup-otp-code";
    public static final String DISPLAY_ACCOUNT_GROUP_KEY = "display-account-group";
    public static final String DISABLE_SCREENSHOTS_KEY = "disable-screenshots";
    public static final String MINIMIZE_APP_AFTER_COPY_TO_CLIPBOARD_KEY = "minimize-app-after-copy-to-clipboard";
    public static final String HIDE_OTP_AUTOMATICALLY_KEY = "hide-otp-codes-automatically";
    public static final String SHOW_NEXT_OTP_CODE_KEY = "show-next-otp-code";
    public static final String HAPTIC_FEEDBACK_KEY = "haptic-feedback";
    public static final String PIN_CODE_KEY = "pin-code";
    public static final String USE_FINGERPRINT_INSTEAD_OF_PIN_CODE_KEY = "use-fingerprint-instead-of-pin-code";
    public static final String AUTO_UPDATE_APP_KEY = "auto-update-app";
    public static final String AUTO_UPDATE_APP_ONLY_IN_WIFI_KEY = "auto-update-app-only-in-wifi";

    // Relevant fields from a 2FA account

    public static final String TWO_FACTOR_AUTH_ACCOUNT_DATA_ID_KEY = "id";
    public static final String TWO_FACTOR_AUTH_ACCOUNT_DATA_SERVICE_KEY = "service";
    public static final String TWO_FACTOR_AUTH_ACCOUNT_DATA_USER_KEY = "account";
    public static final String TWO_FACTOR_AUTH_ACCOUNT_DATA_GROUP_KEY = "group_id";
    public static final String TWO_FACTOR_AUTH_ACCOUNT_DATA_ICON_KEY = "icon";
    public static final String TWO_FACTOR_AUTH_ACCOUNT_DATA_OTP_TYPE_KEY = "otp_type";
    public static final String TWO_FACTOR_AUTH_ACCOUNT_DATA_SECRET_KEY = "secret";
    public static final String TWO_FACTOR_AUTH_ACCOUNT_DATA_PASSWORD_LENGTH_KEY = "digits";
    public static final String TWO_FACTOR_AUTH_ACCOUNT_DATA_ALGORITHM_KEY = "algorithm";
    public static final String TWO_FACTOR_AUTH_ACCOUNT_DATA_PERIOD_KEY = "period";
    public static final String TWO_FACTOR_AUTH_ACCOUNT_DATA_COUNTER_KEY = "counter";

    // Relevant fields from a 2FA account group

    public static final String TWO_FACTOR_AUTH_GROUP_ID_KEY = "id";
    public static final String TWO_FACTOR_AUTH_GROUP_NAME_KEY = "name";
}
