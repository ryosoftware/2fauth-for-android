package com.twofauth.android;

import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RecyclerViewUtils {
    public static void notifyDataSetChanged(@NotNull final RecyclerView.Adapter adapter, @Nullable final RecyclerView recycler_view) {
        if (recycler_view != null) {
            recycler_view.post(() -> adapter.notifyDataSetChanged());
        }
    }
    public static void notifyItemChanged(@NotNull final RecyclerView.Adapter adapter, @Nullable final RecyclerView recycler_view, final int position) {
        if ((recycler_view != null) && (position != RecyclerView.NO_POSITION)) {
            recycler_view.post(() -> adapter.notifyItemChanged(position));
        }
    }

    public static void notifyItemChanged(@NotNull final RecyclerView.Adapter adapter, @Nullable final RecyclerView recycler_view, final int[] positions) {
        if ((recycler_view != null) && (positions != null)) {
            for (int position : positions) {
                recycler_view.post(() -> adapter.notifyItemChanged(position));
            }
        }
    }
}
