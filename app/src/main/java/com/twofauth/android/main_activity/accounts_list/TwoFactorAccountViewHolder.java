package com.twofauth.android.main_activity.accounts_list;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.PersistableBundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bastiaanjansen.otp.HMACAlgorithm;
import com.bastiaanjansen.otp.TOTPGenerator;
import com.google.android.material.button.MaterialButton;
import com.twofauth.android.Constants;
import com.twofauth.android.R;
import com.twofauth.android.StringUtils;
import com.twofauth.android.main_service.ServerDataLoader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.File;
import java.time.Duration;

public class TwoFactorAccountViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
    private static final String TWO_FACTOR_AUTH_DATA_CACHED_ICON_KEY = Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_ICON_KEY + "_cached";
    private static final String TWO_FACTOR_AUTH_DATA_GENERATOR_KEY = Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_OTP_ALGORITHM_KEY + "_generator";
    private static final String OTP_TYPE_TOTP_VALUE = "totp";
    private static final String ALGORITHM_SHA512 = "sha512";
    private static final String ALGORITHM_SHA384 = "sha384";
    private static final String ALGORITHM_SHA256 = "sha256";
    private static final String ALGORITHM_SHA224 = "sha224";
    private static final String ALGORITHM_SHA1 = "sha1";

    private static final float ACTIVE_ITEM_OR_NO_OTHER_ACTIVE_ITEM_ALPHA = 1.0f;
    private static final float NOT_ACTIVE_ITEM_ALPHA = 0.4f;

    private static final float OTP_BLINK_ITEM_VISIBLE_ALPHA = 1.0f;
    private static final float OTP_BLINK_ITEM_NOT_VISIBLE_ALPHA = 0.3f;
    private static final long OTP_IS_ABOUT_TO_EXPIRE_TIME = 5 * DateUtils.SECOND_IN_MILLIS;

    public interface OnViewHolderClickListener {
        public abstract void onClick(final int position);
    }

    public static class Utils {
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

        public static void copyToClipboard(@NotNull final View view, @NotNull final String value, final boolean is_sensitive, final boolean finish_activity) {
            final Context context = view.getContext();
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText(context.getString(R.string.otp_code), value);
                if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) && (is_sensitive)) {
                    PersistableBundle bundle = new PersistableBundle();
                    bundle.putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true);
                    clip.getDescription().setExtras(bundle);
                }
                if (finish_activity) {
                    clipboard.addPrimaryClipChangedListener(new ClipboardManager.OnPrimaryClipChangedListener() {
                        @Override
                        public void onPrimaryClipChanged() {
                            final Activity activity = getActivity(view);
                            if (activity != null) {
                                activity.finish();
                            }
                        }
                    });
                }
                clipboard.setPrimaryClip(clip);
            }
        }
    }

    private final OnViewHolderClickListener mOnClickListener;
    private final TextView mService;
    private final TextView mAccount;
    private final TextView mGroup;
    private final ImageView mIcon;

    private final View mOtpContainer;
    private final TextView mOtp;
    private final ProgressBar mOtpTime;
    private final MaterialButton mOtpCopyToClipboard;
    private final TextView mOtpTypeUnsupported;

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
        mOtpTime = (ProgressBar) parent.findViewById(R.id.otp_time);
        mOtpCopyToClipboard = (MaterialButton) parent.findViewById(R.id.otp_copy_to_clipboard);
        mOtpCopyToClipboard.setOnClickListener(this);
        mOtpTypeUnsupported = (TextView) parent.findViewById(R.id.otp_type_unsupported);
    }

    private Bitmap getIcon(@NotNull final Context context, @NotNull JSONObject object) {
        try {
            if (! object.has(TWO_FACTOR_AUTH_DATA_CACHED_ICON_KEY)) {
                object.put(TWO_FACTOR_AUTH_DATA_CACHED_ICON_KEY, null);
                final File file = ServerDataLoader.getTwoFactorAuthIconPath(context, object);
                if ((file != null) && (file.exists())) {
                    object.put(TWO_FACTOR_AUTH_DATA_CACHED_ICON_KEY, BitmapFactory.decodeFile(file.getPath()));
                }
            }
            return (Bitmap) object.get(TWO_FACTOR_AUTH_DATA_CACHED_ICON_KEY);
        }
        catch (Exception e) {
            return null;
        }
    }

    private boolean isOtpSupported(@NotNull final JSONObject object) {
        return OTP_TYPE_TOTP_VALUE.equals(object.optString(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_OTP_TYPE_KEY));
    }

    private long getMillisUntilNextOtp(@NotNull final JSONObject object) {
        Object generator = initializeOtpGenerator(object);
        if (generator != null) {
            if (OTP_TYPE_TOTP_VALUE.equals(object.optString(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_OTP_TYPE_KEY))) {
                return ((TOTPGenerator) generator).durationUntilNextTimeWindow().toMillis();
            }
        }
        return -1;
    }

    private String getRevealedOtp(@NotNull final JSONObject object) {
        Object generator = initializeOtpGenerator(object);
        if (generator != null) {
            if (OTP_TYPE_TOTP_VALUE.equals(object.optString(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_OTP_TYPE_KEY))) {
                return ((TOTPGenerator) generator).now();
            }
        }
        return null;
    }

    private String getHiddenOtp(@NotNull JSONObject object) {
        return StringUtils.toHiddenString(object.optInt(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_OTP_PASSWORD_LENGTH_KEY));
    }

    private Animation getOtpAnimation() {
        if (mAnimation == null) {
            mAnimation = new AlphaAnimation(OTP_BLINK_ITEM_VISIBLE_ALPHA, OTP_BLINK_ITEM_NOT_VISIBLE_ALPHA);
            mAnimation.setDuration(750);
            mAnimation.setStartOffset(0);
            mAnimation.setRepeatMode(Animation.REVERSE);
            mAnimation.setRepeatCount(Animation.INFINITE);
        }
        return mAnimation;
    }

    public void draw(@NotNull final Context context, @NotNull JSONObject object, final boolean show_otp, final boolean showing_other_otp, final TwoFactorAccountOptions options) {
        final String otp = isOtpSupported(object) ? show_otp ? getRevealedOtp(object) : getHiddenOtp(object) : null, group = object.optString(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_GROUP_KEY);
        final long millis_until_next_otp = (show_otp && (otp != null)) ? getMillisUntilNextOtp(object) : -1;
        mService.setText(object.optString(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_SERVICE_KEY));
        mAccount.setText(object.optString(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_ACCOUNT_KEY));
        mGroup.setText(group);
        mGroup.setVisibility(group.isEmpty() || (! options.isAccountGroupDisplayed()) ? View.GONE : View.VISIBLE);
        final Bitmap icon = getIcon(context, object);
        mIcon.setImageBitmap(icon);
        mIcon.setVisibility(icon == null ? View.INVISIBLE : View.VISIBLE);
        mOtp.setText(options.isUngroupOtpCodeEnabled() ? options.ungroupOtp(otp) : otp);
        final int otp_color = context.getResources().getColor(millis_until_next_otp < 0 ? R.color.otp_hidden : millis_until_next_otp < OTP_IS_ABOUT_TO_EXPIRE_TIME ? R.color.otp_visible_last_seconds : R.color.otp_visible_normal, context.getTheme());
        final ColorStateList otp_color_state_list = ColorStateList.valueOf(otp_color);
        mOtp.setTextColor(otp_color);
        mOtp.setTag(millis_until_next_otp >= 0 ? otp : null);


        Animation otp_animation = mOtp.getAnimation();
        if ((otp_animation == null) && (millis_until_next_otp > 0) && (millis_until_next_otp < OTP_IS_ABOUT_TO_EXPIRE_TIME)) {
            mOtp.startAnimation(getOtpAnimation());
        }
        else if ((otp_animation != null) && ((millis_until_next_otp < 0) || (millis_until_next_otp > OTP_IS_ABOUT_TO_EXPIRE_TIME))) {
            mOtp.clearAnimation();
        }


        mOtpTime.setProgress(millis_until_next_otp >= 0 ? (int) (millis_until_next_otp / (10 * object.optInt(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_PERIOD_KEY, 1))) : 0);
        mOtpTime.setProgressTintList(otp_color_state_list);
        mOtpTime.setVisibility(millis_until_next_otp >= 0 ? View.VISIBLE : View.INVISIBLE);
        mOtpCopyToClipboard.setBackgroundTintList(otp_color_state_list);
        mOtpCopyToClipboard.setVisibility(millis_until_next_otp >= 0 && options.isShowCopyToClipboardButtonDisplayed() ? View.VISIBLE : View.GONE);
        mOtpContainer.setVisibility(otp != null ? View.VISIBLE : View.GONE);
        mOtpTypeUnsupported.setVisibility(otp == null ? View.VISIBLE : View.GONE);
        mOtpTypeUnsupported.setText(context.getString(R.string.otp_type_is_unsupported, object.optString(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_OTP_TYPE_KEY).toUpperCase()));
        itemView.setAlpha(show_otp || (! showing_other_otp) ? ACTIVE_ITEM_OR_NO_OTHER_ACTIVE_ITEM_ALPHA : NOT_ACTIVE_ITEM_ALPHA);
    }

    public void onClick(@NotNull final View view) {
        if (view.getId() == R.id.otp_copy_to_clipboard) {
            final Context context = view.getContext();
            final boolean minimize_app_after_copy_to_clipboard = Constants.getDefaultSharedPreferences(context).getBoolean(Constants.MINIMIZE_APP_AFTER_COPY_TO_CLIPBOARD_KEY, context.getResources().getBoolean(R.bool.minimize_app_after_copy_to_clipboard_default));
            Utils.copyToClipboard(view, mOtp.getTag().toString(), true, minimize_app_after_copy_to_clipboard);
            if (minimize_app_after_copy_to_clipboard) {
                return;
            }
        }
        if (mOnClickListener != null) {
            mOnClickListener.onClick(getBindingAdapterPosition());
        }
    }

    @Override
    public boolean onLongClick(@NotNull final View view) {
        if (mOtp.getTag() != null) {
            onClick(mOtpCopyToClipboard);
            return true;
        }
        return false;
    }

    private static Object initializeOtpGenerator(@NotNull final JSONObject object) {
        try {
            if (! object.has(TWO_FACTOR_AUTH_DATA_GENERATOR_KEY)) {
                if (OTP_TYPE_TOTP_VALUE.equals(object.optString(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_OTP_TYPE_KEY))) {
                    object.put(TWO_FACTOR_AUTH_DATA_GENERATOR_KEY, new TOTPGenerator.Builder(object.getString(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_OTP_SECRET_KEY)).withHOTPGenerator(builder -> {
                        final String algorithm = object.optString(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_OTP_ALGORITHM_KEY);
                        builder.withPasswordLength(object.optInt(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_OTP_PASSWORD_LENGTH_KEY));
                        builder.withAlgorithm(ALGORITHM_SHA512.equals(algorithm) ? HMACAlgorithm.SHA512 : ALGORITHM_SHA384.equals(algorithm) ? HMACAlgorithm.SHA384 : ALGORITHM_SHA256.equals(algorithm) ? HMACAlgorithm.SHA256 : ALGORITHM_SHA224.equals(algorithm) ? HMACAlgorithm.SHA224 : HMACAlgorithm.SHA1);
                    }).withPeriod(Duration.ofSeconds(object.getInt(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_PERIOD_KEY))).build());
                }
            }
        }
        catch (Exception e) {
            Log.e(Constants.LOG_TAG_NAME, "Exception while instancing OTP generator", e);
        }
        return object.opt(TWO_FACTOR_AUTH_DATA_GENERATOR_KEY);
    }

    public static long getMillisUntilNextOtpCompleteCycle(@NotNull final JSONObject object) {
        Object generator = initializeOtpGenerator(object);
        if (generator != null) {
            if (OTP_TYPE_TOTP_VALUE.equals(object.optString(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_OTP_TYPE_KEY))) {
                return ((TOTPGenerator) generator).durationUntilNextTimeWindow().toMillis() + ((TOTPGenerator) generator).getPeriod().toMillis();
            }
        }
        return -1;
    }

    public static TwoFactorAccountViewHolder newInstance(@NotNull final View parent, @Nullable final OnViewHolderClickListener on_click_listener) {
        return new TwoFactorAccountViewHolder(parent, on_click_listener);
    }
}
