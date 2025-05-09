package com.twofauth.android.main_activity.accounts_list;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Build;
import android.os.PersistableBundle;
import android.text.format.DateUtils;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.twofauth.android.Constants;
import com.twofauth.android.R;
import com.twofauth.android.main_activity.AppearanceOptions;
import com.twofauth.android.utils.Clipboard;
import com.twofauth.android.utils.Preferences;
import com.twofauth.android.utils.Strings;
import com.twofauth.android.utils.Threads;
import com.twofauth.android.utils.Vibrator;
import com.twofauth.android.database.TwoFactorAccount;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

public class TwoFactorAccountViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
    public static final long OTP_IS_NEAR_TO_ABOUT_TO_EXPIRE_TIME = 10 * DateUtils.SECOND_IN_MILLIS;
    public static final long OTP_IS_ABOUT_TO_EXPIRE_TIME = 5 * DateUtils.SECOND_IN_MILLIS;

    public interface OnViewHolderClickListener {
        public abstract void onClick(int position);
        public abstract void onLongClick(int position);
    }

    private static class DateTimeUtils {
        public static @NotNull Date getDateForwardFromNow(long milliseconds) {
            final Date date = new Date();
            date.setTime(date.getTime() + milliseconds);
            return date;
        }

        public static @NotNull Date getDateBackFromNow(long milliseconds) {
            return getDateForwardFromNow(-milliseconds);
        }
    }

    private static class Utils {
        public static @Nullable Activity getActivity(@NotNull final View view) {
            Context context = view.getContext();
            while (context instanceof ContextWrapper) {
                if (context instanceof Activity) { return (Activity) context; }
                context = ((ContextWrapper) context).getBaseContext();
            }
            return null;
        }
    }

    private final OnViewHolderClickListener mOnClickListener;
    private final TextView mService;
    private final TextView mAccount;
    private final TextView mServerIdentityAndGroupNames;
    private final ImageView mIcon;
    private final View mOtpContainer;
    private final TextView mOtp;
    private final TextView mOtpNext;
    private final TextView mOtpCounter;
    private final View mAccountPinned;
    private final View mNewAccount;
    private final View mAccountDataNotSynced;
    private final View mAccountDataDeleted;
    private final TextView mOtpError;

    private Animation mAnimation = null;

    @SuppressLint("WrongViewCast")
    public TwoFactorAccountViewHolder(@NotNull final View parent, @Nullable final OnViewHolderClickListener on_click_listener) {
        super(parent);
        mOnClickListener = on_click_listener;
        parent.setOnClickListener(this);
        parent.setOnLongClickListener(this);
        mService = (TextView) parent.findViewById(R.id.service);
        mAccount = (TextView) parent.findViewById(R.id.account);
        mServerIdentityAndGroupNames = (TextView) parent.findViewById(R.id.group);
        mIcon = (ImageView) parent.findViewById(R.id.icon);
        mOtpContainer = parent.findViewById(R.id.otp_container);
        mOtp = (TextView) parent.findViewById(R.id.otp);
        mOtpNext = (TextView) parent.findViewById(R.id.otp_next);
        mOtpCounter = (TextView) parent.findViewById(R.id.otp_counter);
        mAccountPinned = (ImageView) parent.findViewById(R.id.account_pinned);
        mNewAccount = (TextView) parent.findViewById(R.id.account_new);
        mAccountDataNotSynced = parent.findViewById(R.id.account_data_not_synced);
        mAccountDataDeleted = parent.findViewById(R.id.account_data_deleted);
        mOtpError = (TextView) parent.findViewById(R.id.otp_error);
    }

    private @Nullable String getHiddenOtp(@NotNull final TwoFactorAccount account) {
        return Strings.toHiddenString(account.getOtpLength());
    }

    private @NotNull Animation getOtpAnimation() {
        if (mAnimation == null) {
            mAnimation = new AlphaAnimation(1f, 0.25f);
            mAnimation.setDuration(DateUtils.SECOND_IN_MILLIS / 2);
            mAnimation.setStartOffset(0);
            mAnimation.setRepeatMode(Animation.REVERSE);
            mAnimation.setRepeatCount(Animation.INFINITE);
        }
        return mAnimation;
    }

    private void setOtpAnimationByState(final long millis_until_next_otp) {
        final Animation otp_animation = mOtp.getAnimation();
        if ((otp_animation == null) && (millis_until_next_otp > 0) && (millis_until_next_otp < OTP_IS_NEAR_TO_ABOUT_TO_EXPIRE_TIME)) { mOtp.startAnimation(getOtpAnimation()); }
        else if ((otp_animation != null) && ((millis_until_next_otp <= 0) || (millis_until_next_otp > OTP_IS_NEAR_TO_ABOUT_TO_EXPIRE_TIME))) { mOtp.clearAnimation(); }
    }

    public void draw(@NotNull final Context context, @NotNull final TwoFactorAccount account, final boolean show_otp, final boolean showing_other_otp, final AppearanceOptions options) {
        final boolean is_otp_type_supported = account.isOtpTypeSupported();
        final String otp = is_otp_type_supported ? show_otp ? account.getOtp() : getHiddenOtp(account) : null, otp_next = is_otp_type_supported && show_otp && options.isShowNextOtpCodeEnabled() ? account.getOtp(DateTimeUtils.getDateForwardFromNow(account.getPeriodInMillis())) : null;
        final long millis_until_next_otp = (is_otp_type_supported && show_otp) ? account.getMillisUntilNextOtp() : -1;
        mService.setText(account.getService());
        mAccount.setText(account.getAccount());
        final String server_identity_and_group_names = options.getServerIdentityAndGroupNames(context, account);
        mServerIdentityAndGroupNames.setText(server_identity_and_group_names);
        mServerIdentityAndGroupNames.setVisibility(server_identity_and_group_names == null ? View.GONE : View.VISIBLE);
        final Bitmap bitmap = account.hasIcon() ? account.getIcon().getBitmap(context) : null;
        mIcon.setImageBitmap(bitmap);
        mIcon.setVisibility(bitmap == null ? View.INVISIBLE : View.VISIBLE);
        mOtp.setText(options.isUngroupOtpCodeEnabled() ? options.ungroupOtp(otp) : otp);
        mOtp.setTextColor(context.getResources().getColor((millis_until_next_otp < 0) ? R.color.otp_hidden : millis_until_next_otp < OTP_IS_ABOUT_TO_EXPIRE_TIME ? R.color.otp_visible_last_seconds : millis_until_next_otp < OTP_IS_NEAR_TO_ABOUT_TO_EXPIRE_TIME ? R.color.otp_visible_near_of_last_seconds : R.color.otp_visible_normal, context.getTheme()));
        mOtp.setTag(millis_until_next_otp >= 0 ? otp : null);
        mOtpNext.setText(options.isUngroupOtpCodeEnabled() ? options.ungroupOtp(otp_next) : otp_next);
        mOtpNext.setVisibility((otp_next == null) || (millis_until_next_otp == Long.MAX_VALUE) ? View.GONE : View.VISIBLE);
        if (millis_until_next_otp == Long.MAX_VALUE) { mOtpCounter.setText(context.getString(R.string.hotp_counter, account.getCounter())); }
        else { setOtpAnimationByState(millis_until_next_otp); }
        mOtpCounter.setVisibility(millis_until_next_otp == Long.MAX_VALUE ? View.VISIBLE : View.GONE);
        mAccountPinned.setVisibility(account.isPinned() ? View.VISIBLE : View.GONE);
        mNewAccount.setVisibility(options.isNewAccount(context, account) ? View.VISIBLE : View.GONE);
        mAccountDataNotSynced.setVisibility(account.isSynced() && ((! account.hasGroup()) || account.getGroup().isSynced()) && ((! account.hasIcon()) || account.getIcon().isSynced()) ? View.GONE : View.VISIBLE);
        mAccountDataDeleted.setVisibility(account.isDeleted() ? View.VISIBLE : View.GONE);
        final boolean error = ((! is_otp_type_supported) || (otp == null));
        mOtpError.setText(is_otp_type_supported ? context.getString(millis_until_next_otp == Long.MAX_VALUE ? R.string.otp_generation_error_for_counter : R.string.otp_generation_error, account.getCounter()) : context.getString(R.string.otp_type_is_unsupported, account.getOtpType().toUpperCase(), account.getAlgorithm().toUpperCase()));
        mOtpError.setVisibility(error ? View.VISIBLE : View.GONE);
        mOtpContainer.setVisibility(error ? View.GONE : View.VISIBLE);
        itemView.setAlpha((show_otp || (! showing_other_otp)) ? 1f : Constants.BLUR_ALPHA);
    }

    public boolean copyToClipboard(@NotNull final View view) {
        final Context context = view.getContext();
        final boolean minimize_app_after_copy_to_clipboard = Preferences.getDefaultSharedPreferences(context).getBoolean(Constants.MINIMIZE_APP_AFTER_COPY_TO_CLIPBOARD_KEY, context.getResources().getBoolean(R.bool.minimize_app_after_copy_to_clipboard));
        Clipboard.copy(context, mOtp.getTag().toString(), true, minimize_app_after_copy_to_clipboard ? Utils.getActivity(view) : null);
        return minimize_app_after_copy_to_clipboard;
    }

    @Override
    public void onClick(@NotNull final View view) {
        final int position = getBindingAdapterPosition();
        if ((position != RecyclerView.NO_POSITION) && (mOnClickListener != null)) {
            Vibrator.vibrate(view.getContext(), Constants.NORMAL_HAPTIC_FEEDBACK);
            mOnClickListener.onClick(position);
        }
    }

    @Override
    public boolean onLongClick(@NotNull final View view) {
        if (mOtp.getTag() != null) {
            final boolean has_vibrated = Vibrator.vibrate(view.getContext(), Constants.LONG_HAPTIC_FEEDBACK), app_has_been_minimized = copyToClipboard(view);
            if (has_vibrated && app_has_been_minimized) { Threads.sleep(Constants.LONG_HAPTIC_FEEDBACK); }
            return true;
        }
        final int position = getBindingAdapterPosition();
        if ((position != RecyclerView.NO_POSITION) && (mOnClickListener != null)) {
            Vibrator.vibrate(view.getContext(), Constants.LONG_HAPTIC_FEEDBACK);
            mOnClickListener.onLongClick(position);
            return true;
        }
        return false;
    }

    public static boolean copyToClipboard(@NotNull final Activity activity, @NotNull final TwoFactorAccount account, final boolean minimize_app_after_copy_to_clipboard) {
        final String otp_code = account.getOtp();
        if (otp_code != null) {
            Clipboard.copy(activity, otp_code, true, minimize_app_after_copy_to_clipboard);
            return minimize_app_after_copy_to_clipboard;
        }
        return false;
    }

    public static boolean copyToClipboard(@NotNull final Activity activity, @NotNull final TwoFactorAccount account) {
        return copyToClipboard(activity, account, Preferences.getDefaultSharedPreferences(activity).getBoolean(Constants.MINIMIZE_APP_AFTER_COPY_TO_CLIPBOARD_KEY, activity.getResources().getBoolean(R.bool.minimize_app_after_copy_to_clipboard)));
    }

    public static @NotNull TwoFactorAccountViewHolder newInstance(@NotNull final View parent, @Nullable final OnViewHolderClickListener on_click_listener) {
        return new TwoFactorAccountViewHolder(parent, on_click_listener);
    }
}
