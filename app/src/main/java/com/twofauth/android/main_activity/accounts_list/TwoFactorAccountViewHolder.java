package com.twofauth.android.main_activity.accounts_list;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
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
import com.twofauth.android.Database.TwoFactorAccount;
import com.twofauth.android.R;
import com.twofauth.android.SharedPreferencesUtilities;
import com.twofauth.android.StringUtils;
import com.twofauth.android.ThreadUtils;
import com.twofauth.android.VibratorUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

public class TwoFactorAccountViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
    private static final float ACTIVE_ITEM_OR_NO_OTHER_ACTIVE_ITEM_ALPHA = 1.0f;
    private static final float NOT_ACTIVE_ITEM_ALPHA = 0.4f;

    private static final float OTP_BLINK_ITEM_VISIBLE_ALPHA = 1.0f;
    private static final float OTP_BLINK_ITEM_NOT_VISIBLE_ALPHA = 0.3f;
    public static final long OTP_IS_NEAR_TO_ABOUT_TO_EXPIRE_TIME = 10 * DateUtils.SECOND_IN_MILLIS;
    public static final long OTP_IS_ABOUT_TO_EXPIRE_TIME = 5 * DateUtils.SECOND_IN_MILLIS;

    private static final long ON_CLICK_VIBRATION_INTERVAL = 30;
    private static final long ON_LONG_CLICK_VIBRATION_INTERVAL = 60;

    public interface OnViewHolderClickListener {
        public abstract void onClick(final int position);
    }

    private static class DateTimeUtils {
        public static Date getDateForwardFromNow(long milliseconds) {
            final Date date = new Date();
            date.setTime(date.getTime() + milliseconds);
            return date;
        }

        public static Date getDateBackFromNow(long milliseconds) {
            return getDateForwardFromNow(-milliseconds);
        }
    }

    private static class Utils {
        public static Activity getActivity(@NotNull final View view) {
            Context context = view.getContext();
            while (context instanceof ContextWrapper) {
                if (context instanceof Activity) {
                    return (Activity) context;
                }
                context = ((ContextWrapper) context).getBaseContext();
            }
            return null;
        }

        private static void copyToClipboard(@NotNull final Context context, @NotNull final String value, final boolean is_sensitive, @Nullable final Activity activity_to_be_finished) {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText(context.getString(R.string.otp_code), value);
                if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) && (is_sensitive)) {
                    PersistableBundle bundle = new PersistableBundle();
                    bundle.putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true);
                    clip.getDescription().setExtras(bundle);
                }
                if (activity_to_be_finished != null) {
                    clipboard.addPrimaryClipChangedListener(new ClipboardManager.OnPrimaryClipChangedListener() {
                        @Override
                        public void onPrimaryClipChanged() {
                            activity_to_be_finished.finish();
                        }
                    });
                }
                clipboard.setPrimaryClip(clip);
            }
        }

        public static void copyToClipboard(@NotNull final View view, @NotNull final String value, final boolean is_sensitive, final boolean finish_activity) {
            copyToClipboard(view.getContext(), value, is_sensitive, finish_activity ? getActivity(view) : null);
        }

        public static void copyToClipboard(@NotNull final Activity activity, @NotNull final String value, final boolean is_sensitive, final boolean finish_activity) {
            copyToClipboard(activity, value, is_sensitive, finish_activity ? activity : null);
        }
    }

    private final OnViewHolderClickListener mOnClickListener;
    private final TextView mService;
    private final TextView mAccount;
    private final TextView mGroup;
    private final ImageView mIcon;
    private final View mOtpContainer;
    private final TextView mOtp;
    private final TextView mOtpNext;
    private final TextView mOtpCounter;
    private final View mDataNotSynced;
    private final TextView mOtpError;

    private Animation mAnimation = null;

    public TwoFactorAccountViewHolder(@NotNull final View parent, @Nullable final OnViewHolderClickListener on_click_listener) {
        super(parent);
        mOnClickListener = on_click_listener;
        parent.setOnClickListener(this);
        parent.setOnLongClickListener(this);
        mService = (TextView) parent.findViewById(R.id.service);
        mAccount = (TextView) parent.findViewById(R.id.account);
        mGroup = (TextView) parent.findViewById(R.id.group);
        mIcon = (ImageView) parent.findViewById(R.id.icon);
        mOtpContainer = parent.findViewById(R.id.otp_container);
        mOtp = (TextView) parent.findViewById(R.id.otp);
        mOtpNext = (TextView) parent.findViewById(R.id.otp_next);
        mOtpCounter = (TextView) parent.findViewById(R.id.otp_counter);
        mDataNotSynced = parent.findViewById(R.id.account_data_not_synced);
        mOtpError = (TextView) parent.findViewById(R.id.otp_error);
    }

    private String getHiddenOtp(@NotNull final TwoFactorAccount account) {
        return StringUtils.toHiddenString(account.getPasswordLength());
    }

    private Animation getOtpAnimation() {
        if (mAnimation == null) {
            mAnimation = new AlphaAnimation(OTP_BLINK_ITEM_VISIBLE_ALPHA, OTP_BLINK_ITEM_NOT_VISIBLE_ALPHA);
            mAnimation.setDuration(500);
            mAnimation.setStartOffset(0);
            mAnimation.setRepeatMode(Animation.REVERSE);
            mAnimation.setRepeatCount(Animation.INFINITE);
        }
        return mAnimation;
    }

    private void setOtpAnimationByState(long millis_until_next_otp) {
        Animation otp_animation = mOtp.getAnimation();
        if ((otp_animation == null) && (millis_until_next_otp > 0) && (millis_until_next_otp < OTP_IS_ABOUT_TO_EXPIRE_TIME)) {
            mOtp.startAnimation(getOtpAnimation());
        }
        else if ((otp_animation != null) && ((millis_until_next_otp <= 0) || (millis_until_next_otp > OTP_IS_ABOUT_TO_EXPIRE_TIME))) {
            mOtp.clearAnimation();
        }
    }

    public void draw(@NotNull final Context context, @NotNull TwoFactorAccount account, final boolean show_otp, final boolean showing_other_otp, final TwoFactorAccountOptions options) {
        final boolean is_otp_type_supported = account.isOtpTypeSupported();
        final String otp = is_otp_type_supported ? show_otp ? account.getOtp() : getHiddenOtp(account) : null, otp_next = is_otp_type_supported && show_otp && options.isShowNextOtpCodeEnabled() ? account.getOtp(DateTimeUtils.getDateForwardFromNow(account.getPeriodInMillis())) : null, group = (account.getGroup() == null) ? null : account.getGroup().name;
        final long millis_until_next_otp = (is_otp_type_supported && show_otp) ? account.getMillisUntilNextOtp() : -1;
        mService.setText(account.getService());
        mAccount.setText(account.getUser());
        mGroup.setText(group);
        mGroup.setVisibility((group == null) || group.isEmpty() || (! options.isAccountGroupDisplayed()) ? View.GONE : View.VISIBLE);
        final Bitmap icon = account.getIconBitmap(context);
        mIcon.setImageBitmap(icon);
        mIcon.setVisibility(icon == null ? View.INVISIBLE : View.VISIBLE);

        mOtp.setText(options.isUngroupOtpCodeEnabled() ? options.ungroupOtp(otp) : otp);
        mOtp.setTextColor(context.getResources().getColor((millis_until_next_otp < 0) ? R.color.otp_hidden : millis_until_next_otp < OTP_IS_ABOUT_TO_EXPIRE_TIME ? R.color.otp_visible_last_seconds : millis_until_next_otp < OTP_IS_NEAR_TO_ABOUT_TO_EXPIRE_TIME ? R.color.otp_visible_near_of_last_seconds : R.color.otp_visible_normal, context.getTheme()));
        mOtp.setTag(millis_until_next_otp >= 0 ? otp : null);
        mOtpNext.setText(options.isUngroupOtpCodeEnabled() ? options.ungroupOtp(otp_next) : otp_next);
        mOtpNext.setVisibility((otp_next == null) || (millis_until_next_otp == Long.MAX_VALUE) ? View.GONE : View.VISIBLE);
        if (millis_until_next_otp == Long.MAX_VALUE) {
            mOtpCounter.setText(context.getString(R.string.hotp_counter, account.getCounter()));
        }
        else {
            setOtpAnimationByState(millis_until_next_otp);
        }
        mOtpCounter.setVisibility(millis_until_next_otp == Long.MAX_VALUE ? View.VISIBLE : View.GONE);
        mDataNotSynced.setVisibility(account.isNotSynced() ? View.VISIBLE : View.GONE);
        final boolean error = ((! is_otp_type_supported) || (otp == null));
        mOtpError.setText(is_otp_type_supported ? context.getString(millis_until_next_otp == Long.MAX_VALUE ? R.string.otp_generation_error_for_counter : R.string.otp_generation_error, account.getCounter()) : context.getString(R.string.otp_type_is_unsupported, account.getOtpType().toUpperCase(), account.getAlgorithm().toUpperCase()));
        mOtpError.setVisibility(error ? View.VISIBLE : View.GONE);
        mOtpContainer.setVisibility(error ? View.GONE : View.VISIBLE);
        itemView.setAlpha((show_otp || (! showing_other_otp)) ? ACTIVE_ITEM_OR_NO_OTHER_ACTIVE_ITEM_ALPHA : NOT_ACTIVE_ITEM_ALPHA);
    }

    public boolean copyToClipboard(@NotNull final View view) {
        final Context context = view.getContext();
        final boolean minimize_app_after_copy_to_clipboard = SharedPreferencesUtilities.getDefaultSharedPreferences(context).getBoolean(Constants.MINIMIZE_APP_AFTER_COPY_TO_CLIPBOARD_KEY, context.getResources().getBoolean(R.bool.minimize_app_after_copy_to_clipboard_default));
        Utils.copyToClipboard(view, mOtp.getTag().toString(), true, minimize_app_after_copy_to_clipboard);
        return minimize_app_after_copy_to_clipboard;
    }

    public void onClick(@NotNull final View view) {
        final int position = getBindingAdapterPosition();
        if ((position != RecyclerView.NO_POSITION) && (mOnClickListener != null)) {
            VibratorUtils.vibrate(view.getContext(), ON_CLICK_VIBRATION_INTERVAL);
            mOnClickListener.onClick(position);
        }
    }

    @Override
    public boolean onLongClick(@NotNull final View view) {
        if (mOtp.getTag() != null) {
            final boolean has_vibrated = VibratorUtils.vibrate(view.getContext(), ON_LONG_CLICK_VIBRATION_INTERVAL), app_has_been_minimized = copyToClipboard(view);
            if ((has_vibrated) && (app_has_been_minimized)) {
                ThreadUtils.sleep(ON_LONG_CLICK_VIBRATION_INTERVAL);
            }
            return true;
        }
        return false;
    }

    public static boolean copyToClipboard(@NotNull final Activity activity, @NotNull final TwoFactorAccount account) {
        final String otp_code = account.getOtp();
        if (otp_code != null) {
            final boolean minimize_app_after_copy_to_clipboard = SharedPreferencesUtilities.getDefaultSharedPreferences(activity).getBoolean(Constants.MINIMIZE_APP_AFTER_COPY_TO_CLIPBOARD_KEY, activity.getResources().getBoolean(R.bool.minimize_app_after_copy_to_clipboard_default));
            Utils.copyToClipboard(activity, otp_code, true, minimize_app_after_copy_to_clipboard);
            return minimize_app_after_copy_to_clipboard;
        }
        return false;
    }
    public static TwoFactorAccountViewHolder newInstance(@NotNull final View parent, @Nullable final OnViewHolderClickListener on_click_listener) {
        return new TwoFactorAccountViewHolder(parent, on_click_listener);
    }
}
