package com.twofauth.android.utils;

import android.content.Context;
import android.os.VibrationEffect;

import com.twofauth.android.Constants;
import com.twofauth.android.R;

import org.jetbrains.annotations.NotNull;

public class Vibrator {
    public static boolean allowVibrator = true;

    public static boolean canVibrate(@NotNull final Context context) {
        final android.os.Vibrator vibrator = (android.os.Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        return ((vibrator != null) && (vibrator.hasVibrator()));
    }

    public static boolean vibrate(@NotNull final Context context, long time) {
        final android.os.Vibrator vibrator = (android.os.Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if ((vibrator != null) && vibrator.hasVibrator() && allowVibrator) {
            vibrator.vibrate(VibrationEffect.createOneShot(time, VibrationEffect.DEFAULT_AMPLITUDE));
            return true;
        }
        return false;
    }
}
