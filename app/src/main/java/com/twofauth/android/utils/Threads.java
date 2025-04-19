package com.twofauth.android.utils;

import android.util.Log;

import com.twofauth.android.Main;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Threads {
    public static void sleep(final long time) {
        if (time > 0) {
            try {
                Thread.sleep(time);
            }
            catch (Exception e) {
            }
        }
    }

    public static @NotNull Thread getCurrent() {
        return Thread.currentThread();
    }

    public static void interrupt(@NotNull final Thread[] threads) {
        for (final Thread thread : threads) {
            if (thread != null) { thread.interrupt(); }
        }
    }

    public static void interrupt(@Nullable final Thread thread) { interrupt(new Thread[] { thread }); }

    public static void printStackTrace(@Nullable final String initial_message, @Nullable final String final_message) {
        if (initial_message != null) { Log.d(Main.LOG_TAG_NAME, initial_message); }
        for (final StackTraceElement element : Thread.currentThread().getStackTrace()) { Log.d(Main.LOG_TAG_NAME, element.toString()); }
        if (final_message != null) { Log.d(Main.LOG_TAG_NAME, final_message); }
    }
}
