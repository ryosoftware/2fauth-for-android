package com.twofauth.android;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ListUtils {
    public static <E> void setItems(@NotNull final List<E> dest, @Nullable final Collection<E> source) {
        dest.clear();
        if (source != null) {
            dest.addAll(source);
        }
    }

    public static <E> void setItems(@NotNull final List<E> dest, @Nullable final List<E> source) {
        dest.clear();
        if (source != null) {
            dest.addAll(source);
        }
    }

    public static <E> void setItems(@NotNull final List<E> dest, @Nullable final E[] source) {
        dest.clear();
        if (source != null) {
            dest.addAll(Arrays.asList(source));
        }
    }
}
