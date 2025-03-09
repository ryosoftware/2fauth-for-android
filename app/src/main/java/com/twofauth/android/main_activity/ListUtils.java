package com.twofauth.android.main_activity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class ListUtils {
    public static <E> void setItems(@NotNull final List<E> dest, @Nullable final List<E> source) {
        dest.clear();
        if (source != null) {
            dest.addAll(source);
        }
    }
}
