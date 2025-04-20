package com.twofauth.android;

import android.net.Uri;
import android.text.format.DateUtils;

public class Constants {
    public static final Uri GITHUB_REPO = Uri.parse("https://github.com/ryosoftware/2fauth-for-android");

    // APP Keystore aliases and other data

    public static final String APP_DATA_ENCRYPTION_KEYSTORE_ENTRY_NAME = "encryption-key";
    public static final String FINGERPRINT_VALIDATED_KEYSTORE_ENTRY_NAME = "fingerprint-key";

    // App options

    public static final String SORT_ACCOUNTS_BY_LAST_USE_KEY = "sort-accounts-by-last-use";
    public static final String ACCOUNTS_LIST_COLUMNS_IN_PORTRAIT_MODE_KEY = "accounts-list-columns-portrait";
    public static final String ACCOUNTS_LIST_COLUMNS_IN_LANDSCAPE_MODE_KEY = "accounts-list-columns-portrait";
    public static final String UNGROUP_OTP_CODE_KEY = "ungroup-otp-code";
    public static final String DISPLAY_ACCOUNT_SERVER_IDENTITY_KEY = "display-account-server-identity";
    public static final String DISPLAY_ACCOUNT_GROUP_KEY = "display-account-group";
    public static final String DISABLE_SCREENSHOTS_KEY = "disable-screenshots";
    public static final String MINIMIZE_APP_AFTER_COPY_TO_CLIPBOARD_KEY = "minimize-app-after-copy-to-clipboard";
    public static final String HIDE_OTP_AUTOMATICALLY_KEY = "hide-otp-codes-automatically";
    public static final String SHOW_NEXT_OTP_CODE_KEY = "show-next-otp-code";
    public static final String HAPTIC_FEEDBACK_KEY = "haptic-feedback";
    public static final String ALLOW_ANIMATIONS_KEY = "allow-animations";
    public static final String APP_THEME_KEY = "theme";
    public static final String PIN_CODE_KEY = "pin-code";
    public static final String USE_FINGERPRINT_INSTEAD_OF_PIN_CODE_KEY = "use-fingerprint-instead-of-pin-code";
    public static final String ALLOW_COPY_SERVER_ACCESS_TOKEN_KEY = "allow-copy-server-access-tokens";
    public static final String DOWNLOAD_ICONS_FROM_EXTERNAL_SOURCES_KEY = "download-icons-from-external-sources";
    public static final String AUTO_UPDATE_APP_KEY = "auto-update-app";
    public static final String AUTO_UPDATE_APP_INCLUDE_PRERELEASES_KEY = "auto-update-app-include-prereleases";
    public static final String AUTO_UPDATE_APP_ONLY_IN_WIFI_KEY = "auto-update-app-only-in-wifi";

    // App Runtime options

    public static final String FILTERING_BY_SERVER_IDENTITY_KEY = "filtering-by-server-identity";
    public static final String FILTERING_BY_GROUP_KEY = "filtering-by-group";
    public static final String SERVER_IDENTITIES_COUNT_KEY = "server-identities-count";

    // Relevant fields from a 2FA account

    public static final String ACCOUNT_DATA_ID_KEY = "id";
    public static final String ACCOUNT_DATA_SERVICE_KEY = "service";
    public static final String ACCOUNT_DATA_USER_KEY = "account";
    public static final String ACCOUNT_DATA_GROUP_KEY = "group_id";
    public static final String ACCOUNT_DATA_ICON_KEY = "icon";
    public static final String ACCOUNT_DATA_OTP_TYPE_KEY = "otp_type";
    public static final String ACCOUNT_DATA_SECRET_KEY = "secret";
    public static final String ACCOUNT_DATA_OTP_LENGTH_KEY = "digits";
    public static final String ACCOUNT_DATA_ALGORITHM_KEY = "algorithm";
    public static final String ACCOUNT_DATA_PERIOD_KEY = "period";
    public static final String ACCOUNT_DATA_COUNTER_KEY = "counter";

    // OTP Options

    public static final String OTP_TYPE_TOTP = "totp";
    public static final String OTP_TYPE_HOTP = "hotp";
    public static final String OTP_TYPE_STEAM = "steamtotp";

    public static final String ALGORITHM_SHA512 = "sha512";
    public static final String ALGORITHM_SHA384 = "sha384";
    public static final String ALGORITHM_SHA256 = "sha256";
    public static final String ALGORITHM_SHA224 = "sha224";
    public static final String ALGORITHM_SHA1 = "sha1";
    public static final String ALGORITHM_MD5 = "md5";

    public static final String DEFAULT_OTP_TYPE = OTP_TYPE_TOTP;
    public static final String DEFAULT_ALGORITHM = ALGORITHM_SHA1;
    public static final int DEFAULT_OTP_LENGTH = 6;
    public static final int DEFAULT_PERIOD = 30;
    public static final int DEFAULT_COUNTER = 0;

    // Relevant fields from a 2FA account group

    public static final String GROUP_DATA_ID_KEY = "id";
    public static final String GROUP_DATA_NAME_KEY = "name";

    public static final String GROUP_NAME_VALID_REGEXP = "^[\\p{L}0-9 ]+$";

    // Relevant fields from a 2FA account icon

    public static final String ICON_DATA_ID_KEY = "filename";

    // Relevant fields from a 2FA QR code

    public static final String QR_DATA_ISSUER_KEY = "issuer";

    // Relevant fields from a 2FA user

    public static final String USER_DATA_ID_KEY = "id";
    public static final String USER_DATA_NAME_KEY = "name";
    public static final String USER_DATA_EMAIL_KEY = "email";
    public static final String USER_DATA_IS_ADMIN_KEY = "is_admin";

    // Other App constants

    public static final long BUTTON_360_DEGREES_ROTATION_ANIMATION_DURATION = 2 * DateUtils.SECOND_IN_MILLIS;
    public static final long BUTTON_SHOW_OR_HIDE_ANIMATION_DURATION = BUTTON_360_DEGREES_ROTATION_ANIMATION_DURATION / 4;
    public static final long SUBMENU_OPEN_OR_CLOSE_ANIMATION_DURATION = BUTTON_360_DEGREES_ROTATION_ANIMATION_DURATION / 3;

    public static final int BUTTON_CLICK_ANIMATION_BEFORE_START_ACTION_DEGREES = 180;

    public static final long SHORT_HAPTIC_FEEDBACK = 15;
    public static final long NORMAL_HAPTIC_FEEDBACK = 30;
    public static final long LONG_HAPTIC_FEEDBACK = 60;

    public static final float BLUR_ALPHA = 0.4f;
}
