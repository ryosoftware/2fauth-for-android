package com.twofauth.android;

import android.os.SystemClock;

import org.jetbrains.annotations.Nullable;

public class ThreadUtils {
    public static void interrupt(@Nullable final Thread thread) {
        if (thread != null) {
            thread.interrupt();
        }
    }

    public static void sleep(final long time) {
        try {
            Thread.sleep(time);
        }
        catch (Exception e) {
        }
    }
}
