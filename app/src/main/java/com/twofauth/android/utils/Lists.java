package com.twofauth.android.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class Lists {
    public static <E> void addAll(@NotNull final List<E> dest, @Nullable final Collection<E> source) {
        if (source != null) { dest.addAll(source); }
    }

    public static <E> void addAll(@NotNull final List<E> dest, @Nullable final E[] source) {
        if (source != null) { dest.addAll(Arrays.asList(source)); }
    }

    public static <E> void addAll(@NotNull final List<E> dest, @Nullable final E[] source_1, @Nullable final Collection<E> source_2) {
        addAll(dest, source_1);
        addAll(dest, source_2);
    }

    public static <E> void setItems(@NotNull final List<E> dest, @Nullable final Collection<E> source) {
        dest.clear();
        addAll(dest, source);
    }

    public static <E> void setItems(@NotNull final List<E> dest, @Nullable final E[] source) {
        dest.clear();
        addAll(dest, source);
    }

    public static <E> void setItems(@NotNull final List<E> dest, @Nullable final E[] source_1, @Nullable final Collection<E> source_2) {
        dest.clear();
        addAll(dest, source_1, source_2);
    }

    public static <E> boolean isEmptyOrNull(@Nullable final List<E> list) {
        return ((list == null) || list.isEmpty());
    }

    public static <E> boolean isSizeGreaterOrEqualsTo(@Nullable final List<E> list, final int min_size) {
        return ((list != null) && ((min_size == 0) || (list.size() >= min_size)));
    }

    public static <E> int indexOf(@NotNull final List<E> elements, @Nullable final E element) {
        for (int i = 0, j = elements.size(); i < j; i ++) {
            if (elements.get(i) == element) { return i; }
        }
        return -1;
    }

    public static <E> int indexOf(@NotNull final E[] elements, @Nullable final E element) {
        for (int i = 0, j = elements.length; i < j; i ++) {
            if (elements[i] == element) { return i; }
        }
        return -1;
    }

    public static <E> int size(@Nullable final Collection<E> list) {
        return (list == null) ? 0 : list.size();
    }
}
