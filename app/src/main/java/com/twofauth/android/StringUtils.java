package com.twofauth.android;

import android.content.Context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DateFormat;
import java.util.Date;

public class StringUtils {
    public static int compare(@Nullable final String string_1, @Nullable final String string_2, final boolean ignore_case) {
        if (string_1 == null) {
            return (string_2 == null) ? 0 : -1;
        }
        return ignore_case ? string_1.compareToIgnoreCase(string_2) : string_1.compareTo(string_2);
    }

    public static int compare(@Nullable final String string_1, @Nullable final String string_2) {
        return compare(string_1, string_2, false);
    }

    public static boolean equals(@Nullable final String string_1, @Nullable final String string_2, final boolean ignore_case) {
        if (string_1 == null) {
            return string_2 == null;
        }
        return ignore_case ? string_1.equalsIgnoreCase(string_2) : string_1.equals(string_2);
    }

    public static boolean equals(@Nullable final String string_1, @Nullable final String string_2) {
        return equals(string_1, string_2, false);
    }

    public static boolean in(@NotNull final String string, @NotNull final String part, final boolean ignore_case) {
        if (ignore_case) {
            return string.toLowerCase().contains(part.toLowerCase());
        }
        return string.contains(part);
    }

    public static boolean in(@NotNull final String string, @NotNull final String part) {
        return in(string, part, false);
    }

    public static String toHiddenString(final int length) {
        if (length != 0) {
            final StringBuilder value = new StringBuilder();
            for (int i = 0; i < length; i ++) {
                value.append("\u25CF");
            }
            return value.toString();
        }
        return null;
    }

    public static String toHiddenString(@Nullable final String value) {
        return toHiddenString(value == null ? 0 : value.length());
    }

    public static String getDateTimeString(@NotNull final Context context, final long time) {
        final DateFormat date_format = DateFormat.getDateInstance(DateFormat.MEDIUM), time_format = DateFormat.getTimeInstance(DateFormat.MEDIUM);
        final Date date = new Date(time);
        return context.getString(R.string.date_time, date_format.format(date), time_format.format(date));
    }
}
