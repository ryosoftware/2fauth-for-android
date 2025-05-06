package com.twofauth.android.main_activity;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.twofauth.android.utils.Lists;
import com.twofauth.android.R;
import com.twofauth.android.utils.RecyclerViews;
import com.twofauth.android.main_activity.accounts_list_index.IndexEntryViewHolder;
import com.twofauth.android.main_activity.accounts_list_index.IndexEntryViewHolder.OnViewHolderClickListener;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AccountsListIndexAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder> implements OnViewHolderClickListener {
    public static final Character PIN_LETTER = ' ';
    public static final Character NOT_LETTER = '#';

    private static final int LETTER = 1;

    public interface OnIndexEntryClickListener {
        public abstract void onClick(Character letter);
    }

    private final List<Character> mItems = new ArrayList<Character>();
    private int mActiveIndexPosition = androidx.recyclerview.widget.RecyclerView.NO_POSITION;
    private androidx.recyclerview.widget.RecyclerView mRecyclerView = null;
    private OnIndexEntryClickListener mOnIndexEntryClickListener = null;

    public AccountsListIndexAdapter() {}

    public AccountsListIndexAdapter(@Nullable final List<Character> items) {
        setItems(items);
    }

    @Override
    public synchronized void onAttachedToRecyclerView(@NotNull final androidx.recyclerview.widget.RecyclerView recycler_view) {
        super.onAttachedToRecyclerView(recycler_view);
        mRecyclerView = recycler_view;
    }

    @Override
    public synchronized void onDetachedFromRecyclerView(@NotNull final androidx.recyclerview.widget.RecyclerView recycler_view) {
        super.onDetachedFromRecyclerView(recycler_view);
        mRecyclerView = null;
    }

    @Override
    public @NotNull ViewHolder onCreateViewHolder(@NotNull final ViewGroup parent, final int view_type) {
        return IndexEntryViewHolder.newInstance(LayoutInflater.from(parent.getContext()).inflate(R.layout.letter_item_data, parent, false), this);
    }

    @Override
    public synchronized void onBindViewHolder(@NotNull final androidx.recyclerview.widget.RecyclerView.ViewHolder view_holder, final int position) {
        if (getItemViewType(position) == LETTER) {
            final Character letter = getItem(position);
            ((IndexEntryViewHolder) view_holder).draw(mRecyclerView.getContext(), PIN_LETTER.equals(letter) ? null : letter, position == mActiveIndexPosition);
        }
    }

    public synchronized void setItems(@Nullable final List<Character> items) {
        Lists.setItems(mItems, items);
        mActiveIndexPosition = androidx.recyclerview.widget.RecyclerView.NO_POSITION;
        RecyclerViews.notifyDataSetChanged(this, mRecyclerView);
    }

    private @Nullable Character getItem(final int position) {
        return ((position >= 0) && (position < mItems.size())) ? mItems.get(position) : null;
    }

    public int getItemViewType(final int position) {
        return LETTER;
    }

    @Override
    public synchronized int getItemCount() {
        return mItems.size();
    }

    @Override
    public synchronized void onClick(final int position) {
        if ((setActiveIndexEntry(position)) && (mActiveIndexPosition != androidx.recyclerview.widget.RecyclerView.NO_POSITION) && (mOnIndexEntryClickListener != null)) { mOnIndexEntryClickListener.onClick(getItem(mActiveIndexPosition)); }
    }

    private synchronized boolean setActiveIndexEntry(final int position) {
        if (mActiveIndexPosition != position) {
            final int older_active_index_position = mActiveIndexPosition;
            mActiveIndexPosition = ((position >= 0) && (position < getItemCount())) ? position : RecyclerView.NO_POSITION;
            RecyclerViews.notifyItemChanged(this, mRecyclerView, new int[] { older_active_index_position, mActiveIndexPosition });
            return true;
        }
        return false;
    }

    public synchronized void setActiveIndexEntry(final char letter) {
        if (setActiveIndexEntry(mItems.indexOf(letter))) { mRecyclerView.smoothScrollToPosition(mActiveIndexPosition); }
    }

    public synchronized void setOnIndexClickListener(@Nullable final OnIndexEntryClickListener listener) {
        mOnIndexEntryClickListener = listener;
    }
}