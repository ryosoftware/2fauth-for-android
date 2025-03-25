package com.twofauth.android;

import android.content.Context;
import android.os.VibrationEffect;
import android.os.Vibrator;

import org.jetbrains.annotations.NotNull;

public class VibratorUtils {
    public static boolean canVibrate(@NotNull final Context context) {
        final Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        return ((vibrator != null) && (vibrator.hasVibrator()));
    }

    public static boolean vibrate(@NotNull final Context context, long time) {
        final Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if ((vibrator != null) && (vibrator.hasVibrator()) && (SharedPreferencesUtilities.getDefaultSharedPreferences(context).getBoolean(Constants.HAPTIC_FEEDBACK_KEY, context.getResources().getBoolean(R.bool.haptic_feedback_default)))) {
            vibrator.vibrate(VibrationEffect.createOneShot(time, VibrationEffect.DEFAULT_AMPLITUDE));
            return true;
        }
        return false;
    }
}
