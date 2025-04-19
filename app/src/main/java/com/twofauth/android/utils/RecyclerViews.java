package com.twofauth.android.utils;

import androidx.recyclerview.widget.RecyclerView.Adapter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RecyclerViews {
    public static void notifyDataSetChanged(@NotNull final Adapter adapter, @Nullable final androidx.recyclerview.widget.RecyclerView recycler_view) {
        if (recycler_view != null) { recycler_view.post(() -> adapter.notifyDataSetChanged()); }
    }

    public static void notifyItemChanged(@NotNull final Adapter adapter, @Nullable final androidx.recyclerview.widget.RecyclerView recycler_view, final int position) {
        if ((recycler_view != null) && (position != androidx.recyclerview.widget.RecyclerView.NO_POSITION)) { recycler_view.post(() -> adapter.notifyItemChanged(position)); }
    }

    public static void notifyItemChanged(@NotNull final Adapter adapter, @Nullable final androidx.recyclerview.widget.RecyclerView recycler_view, final int[] positions) {
        if ((recycler_view != null) && (positions != null)) {
            for (int position : positions) {
                recycler_view.post(() -> adapter.notifyItemChanged(position));
            }
        }
    }
}
