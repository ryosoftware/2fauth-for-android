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
        if ((vibrator != null) && (vibrator.hasVibrator()) && (Constants.getDefaultSharedPreferences(context).getBoolean(Constants.VIBRATE_ON_SOME_ACTIONS_KEY, context.getResources().getBoolean(R.bool.vibrate_on_some_actions_default)))) {
            vibrator.vibrate(VibrationEffect.createOneShot(time, VibrationEffect.DEFAULT_AMPLITUDE));
            return true;
        }
        return false;
    }
}
