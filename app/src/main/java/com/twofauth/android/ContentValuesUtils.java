package com.twofauth.android;

import android.content.ContentValues;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ContentValuesUtils {
    public static String getAsString(@NotNull final ContentValues values, @NotNull final String name, @Nullable final String fallback) {
        final Object value = values.get(name);
        return (value == null) ? fallback : (String) value;
    }

    public static String getAsString(@NotNull final ContentValues values, @NotNull final String name) {
        return getAsString(values, name, null);
    }

    public static int getAsInteger(@NotNull final ContentValues values, @NotNull final String name, final int fallback) {
        final Object value = values.get(name);
        return (value == null) ? fallback : (int) value;
    }

    public static int getAsInteger(@NotNull final ContentValues values, @NotNull final String name) {
        return getAsInteger(values, name, 0);
    }

    public static long getAsLong(@NotNull final ContentValues values, @NotNull final String name, final long fallback) {
        final Object value = values.get(name);
        return (value == null) ? fallback : (long) value;
    }

    public static long getAsLong(@NotNull final ContentValues values, @NotNull final String name) {
        return getAsLong(values, name, 0);
    }

    public static boolean getAsBoolean(@NotNull final ContentValues values, @NotNull final String name, final boolean fallback) {
        final Object value = values.get(name);
        return (value == null) ? fallback : (value instanceof Integer) ? (((int) value) != 0) : (boolean) value;
    }

    public static boolean getAsBoolean(@NotNull final ContentValues values, @NotNull final String name) {
        return getAsBoolean(values, name, false);
    }
}
