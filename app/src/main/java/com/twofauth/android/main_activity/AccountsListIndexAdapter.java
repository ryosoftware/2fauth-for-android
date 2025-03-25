package com.twofauth.android.main_activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.twofauth.android.ListUtils;
import com.twofauth.android.R;
import com.twofauth.android.RecyclerViewUtils;
import com.twofauth.android.main_activity.accounts_list_index.IndexEntryViewHolder;
import com.twofauth.android.main_activity.accounts_list_index.IndexEntryViewHolder.OnViewHolderClickListener;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AccountsListIndexAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements OnViewHolderClickListener {
    private static final int LETTER = 1;

    public interface OnIndexEntryClickListener {
        public abstract void onClick(char letter);
    }

    private final Object mSynchronizationObject = new Object();
    private final List<Character> mItems = new ArrayList<Character>();
    private int mActiveIndexPosition = RecyclerView.NO_POSITION;
    private RecyclerView mRecyclerView = null;
    private OnIndexEntryClickListener mOnIndexEntryClickListener = null;

    public AccountsListIndexAdapter() {}

    public AccountsListIndexAdapter(@Nullable final List<Character> items) {
        setItems(items);
    }

    @Override
    public void onAttachedToRecyclerView(@NotNull final RecyclerView recycler_view) {
        super.onAttachedToRecyclerView(recycler_view);
        synchronized (mSynchronizationObject) {
            mRecyclerView = recycler_view;
        }
    }

    @Override
    public void onDetachedFromRecyclerView(@NotNull final RecyclerView recycler_view) {
        super.onDetachedFromRecyclerView(recycler_view);
        synchronized (mSynchronizationObject) {
            mRecyclerView = null;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NotNull final ViewGroup parent, final int view_type) {
        final Context context = parent.getContext();
        return IndexEntryViewHolder.newInstance(LayoutInflater.from(context).inflate(R.layout.letter_item_data, parent, false), this);
    }

    @Override
    public void onBindViewHolder(@NotNull final RecyclerView.ViewHolder view_holder, final int position) {
        synchronized (mSynchronizationObject) {
            if (getItemViewType(position) == LETTER) {
                ((IndexEntryViewHolder) view_holder).draw(mRecyclerView.getContext(), getItem(position), position == mActiveIndexPosition);
            }
        }
    }

    public void setItems(@Nullable final List<Character> items) {
        synchronized (mSynchronizationObject) {
            ListUtils.setItems(mItems, items);
            mActiveIndexPosition = RecyclerView.NO_POSITION;
            RecyclerViewUtils.notifyDataSetChanged(this, mRecyclerView);
        }
    }

    private Character getItem(final int position) {
        synchronized (mSynchronizationObject) {
            return ((position >= 0) && (position < mItems.size())) ? mItems.get(position) : null;
        }
    }

    public int getItemViewType(final int position) {
        return LETTER;
    }

    @Override
    public int getItemCount() {
        synchronized (mSynchronizationObject) {
            return mItems.size();
        }
    }

    @Override
    public void onClick(final int position) {
        synchronized (mSynchronizationObject) {
            if ((setActiveIndexEntry(position)) && (mActiveIndexPosition != RecyclerView.NO_POSITION) && (mOnIndexEntryClickListener != null)) {
                mOnIndexEntryClickListener.onClick(getItem(mActiveIndexPosition));
            }
        }
    }

    private boolean setActiveIndexEntry(final int position) {
        synchronized (mSynchronizationObject) {
            if (mActiveIndexPosition != position) {
                final int older_active_index_position = mActiveIndexPosition;
                mActiveIndexPosition = ((position >= 0) && (position < getItemCount())) ? position : RecyclerView.NO_POSITION;
                RecyclerViewUtils.notifyItemChanged(this, mRecyclerView, new int[] { older_active_index_position, mActiveIndexPosition });
                return true;
            }
            return false;
        }
    }

    public void setActiveIndexEntry(final char letter) {
        synchronized (mSynchronizationObject) {
            if (setActiveIndexEntry(mItems.indexOf(letter))) {
                final LinearLayoutManager layout_manager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
                mRecyclerView.smoothScrollToPosition(mActiveIndexPosition);
            }
        }
    }

    public void setOnIndexClickListener(@Nullable final OnIndexEntryClickListener listener) {
        synchronized (mSynchronizationObject) {
            mOnIndexEntryClickListener = listener;
        }
    }
}