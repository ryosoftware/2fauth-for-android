package com.twofauth.android.main_activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.text.format.DateUtils;

import com.twofauth.android.Constants;
import com.twofauth.android.R;
import com.twofauth.android.database.TwoFactorAccount;
import com.twofauth.android.database.TwoFactorGroup;
import com.twofauth.android.utils.Preferences;
import com.twofauth.android.utils.Strings;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AppearanceOptions {
    private final long mNewAccountPeriod;
    private final boolean mUngroupOtpCode;
    private final int mUngroupedOtpCodePartSize;
    private final boolean mHideOtpAutomatically;
    private final boolean mShowNextOtpCode;
    private final boolean mDisplayAccountServerIdentity;
    private final boolean mDisplayAccountGroup;

    public AppearanceOptions(@NotNull final Context context) {
        final SharedPreferences preferences = Preferences.getDefaultSharedPreferences(context);
        final Resources resources = context.getResources();
        mNewAccountPeriod = preferences.getLong(Constants.NEW_ACCOUNT_PERIOD_KEY, resources.getInteger(R.integer.new_account_period_in_minutes) * DateUtils.MINUTE_IN_MILLIS);
        mUngroupOtpCode = preferences.getBoolean(Constants.UNGROUP_OTP_CODE_KEY, resources.getBoolean(R.bool.ungroup_otp_code));
        mUngroupedOtpCodePartSize = preferences.getInt(Constants.UNGROUPED_OTP_CODE_PART_SIZE_KEY, resources.getInteger(R.integer.ungrouped_otp_code_part_size));
        mHideOtpAutomatically = preferences.getBoolean(Constants.HIDE_OTP_AUTOMATICALLY_KEY, resources.getBoolean(R.bool.hide_otp_codes_automatically));
        mShowNextOtpCode = preferences.getBoolean(Constants.SHOW_NEXT_OTP_CODE_KEY, resources.getBoolean(R.bool.show_next_otp_code));
        mDisplayAccountServerIdentity = (preferences.getBoolean(Constants.DISPLAY_ACCOUNT_SERVER_IDENTITY_KEY, resources.getBoolean(R.bool.display_account_server_identity)) && (preferences.getInt(Constants.SERVER_IDENTITIES_COUNT_KEY, 0) > 1) && (! preferences.getBoolean(Constants.FILTERING_BY_SERVER_IDENTITY_KEY, false)));
        mDisplayAccountGroup = (preferences.getBoolean(Constants.DISPLAY_ACCOUNT_GROUP_KEY, resources.getBoolean(R.bool.display_account_group)) && (! preferences.getBoolean(Constants.FILTERING_BY_GROUP_KEY, false)));
    }

    public boolean isUngroupOtpCodeEnabled() {
        return mUngroupOtpCode;
    }

    public boolean isHideOtpAutomaticallyEnabled() {
        return mHideOtpAutomatically;
    }

    public boolean isShowNextOtpCodeEnabled() { return mShowNextOtpCode; }

    public boolean isAccountServerIdentityDisplayed() { return mDisplayAccountServerIdentity; }

    public boolean isAccountGroupDisplayed() {
        return mDisplayAccountGroup;
    }

    public @Nullable String ungroupOtp(@Nullable final String otp) {
        if (isUngroupOtpCodeEnabled() && (! Strings.isEmptyOrNull(otp))) {
            final StringBuilder string_builder = new StringBuilder();
            int from = 0, part_size = (mUngroupedOtpCodePartSize <= 0) ? otp.length() / 2 + (otp.length() % 2 == 0 ? 0 : 1) : mUngroupedOtpCodePartSize;
            while (from < otp.length()) {
                if (from > 0) { string_builder.append(' '); }
                final int to = Math.min(from + part_size, otp.length());
                string_builder.append(otp.substring(from, to));
                from = to;
            }
            return string_builder.toString();
        }
        return null;
    }

    private @Nullable String getServerIdentityAndGroupNames(@NotNull final Context context, @Nullable final String server_identity_title, @Nullable final String group_name) {
        return isAccountServerIdentityDisplayed() ? context.getString(group_name == null ? R.string.server_identity_label : R.string.server_identity_and_group_label, server_identity_title, group_name) : group_name == null ? null : context.getString(R.string.group_label, null, group_name);
    }

    public @Nullable String getServerIdentityAndGroupNames(@NotNull final Context context, @NotNull final TwoFactorAccount account) {
        return getServerIdentityAndGroupNames(context, account.getServerIdentity().getTitle(), account.hasGroup() && account.getGroup().hasName() && mDisplayAccountGroup ? account.getGroup().getName() : null);
    }

    public @Nullable String getServerIdentityAndGroupNames(@NotNull final Context context, @NotNull final TwoFactorGroup group) {
        return getServerIdentityAndGroupNames(context, group.getServerIdentity().getTitle(), group.hasName() ? group.getName() : null);
    }

    public boolean isNewAccount(@NotNull final Context context, @NotNull final TwoFactorAccount account) {
        return account.getAddTime() + mNewAccountPeriod > System.currentTimeMillis();
    }
}
