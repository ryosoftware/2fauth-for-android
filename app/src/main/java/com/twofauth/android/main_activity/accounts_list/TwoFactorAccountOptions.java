package com.twofauth.android.main_activity.accounts_list;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

import com.twofauth.android.Constants;
import com.twofauth.android.R;
import com.twofauth.android.SharedPreferencesUtilities;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TwoFactorAccountOptions {
    private final boolean mUngroupOtpCode;
    private final boolean mHideOtpAutomatically;
    private final boolean mShowNextOtpCode;
    private final boolean mDisplayAccountGroup;

    public TwoFactorAccountOptions(@NotNull final Context context) {
        final SharedPreferences preferences = SharedPreferencesUtilities.getDefaultSharedPreferences(context);
        final Resources resources = context.getResources();
        mUngroupOtpCode = preferences.getBoolean(Constants.UNGROUP_OTP_CODE_KEY, resources.getBoolean(R.bool.ungroup_otp_code_default));
        mHideOtpAutomatically = preferences.getBoolean(Constants.HIDE_OTP_AUTOMATICALLY_KEY, resources.getBoolean(R.bool.hide_otp_codes_automatically_default));
        mShowNextOtpCode = preferences.getBoolean(Constants.SHOW_NEXT_OTP_CODE_KEY, resources.getBoolean(R.bool.show_next_otp_code_default));
        mDisplayAccountGroup = preferences.getBoolean(Constants.DISPLAY_ACCOUNT_GROUP_KEY, resources.getBoolean(R.bool.display_account_group_default));
    }

    public boolean isUngroupOtpCodeEnabled() {
        return mUngroupOtpCode;
    }

    public boolean isHideOtpAutomaticallyEnabled() {
        return mHideOtpAutomatically;
    }

    public boolean isShowNextOtpCodeEnabled() { return mShowNextOtpCode; }

    public boolean isAccountGroupDisplayed() {
        return mDisplayAccountGroup;
    }

    public String ungroupOtp(@Nullable final String otp) {
        if ((isUngroupOtpCodeEnabled()) && (otp != null)) {
            final int first_part_length = otp.length() / 2 + (otp.length() % 2 == 0 ? 0 : 1);
            return String.format("%s %s", otp.substring(0, first_part_length), otp.substring(first_part_length));
        }
        return null;
    }
}
