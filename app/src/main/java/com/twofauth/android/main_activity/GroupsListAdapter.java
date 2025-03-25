package com.twofauth.android.main_activity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.twofauth.android.ListUtils;
import com.twofauth.android.R;
import com.twofauth.android.RecyclerViewUtils;
import com.twofauth.android.main_activity.groups_list.GroupViewHolder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GroupsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements GroupViewHolder.OnViewHolderClickListener {
    private static final int GROUP = 1;
    private final List<String> mItems = new ArrayList<String>();
    private final OnSelectedGroupChangesListener mOnSelectedGroupChangesListener;
    private final Object mSynchronizationObject = new Object();

    private RecyclerView mRecyclerView = null;
    private int mActiveGroupPosition = RecyclerView.NO_POSITION;

    public interface OnSelectedGroupChangesListener {
        public abstract void onSelectedGroupChanges(final String selected_group, final String previous_selected_group);
    }

    public GroupsListAdapter(@NotNull final OnSelectedGroupChangesListener on_selected_group_changes_listener) {
        mOnSelectedGroupChangesListener = on_selected_group_changes_listener;
    }

    public GroupsListAdapter(@Nullable final List<String> items, @NotNull final OnSelectedGroupChangesListener on_selected_group_changes_listener) {
        this(on_selected_group_changes_listener);
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
        return GroupViewHolder.newInstance(LayoutInflater.from(context).inflate(R.layout.account_group_item_data, parent, false), this);
    }

    @Override
    public void onBindViewHolder(@NotNull final RecyclerView.ViewHolder view_holder, final int position) {
        synchronized (mSynchronizationObject) {
            if (getItemViewType(position) == GROUP) {
                ((GroupViewHolder) view_holder).draw(mRecyclerView.getContext(), getItem(position), mActiveGroupPosition == position);
            }
        }
    }

    public void setItems(@Nullable final List<String> items) {
        synchronized (mSynchronizationObject) {
            mActiveGroupPosition = RecyclerView.NO_POSITION;
            ListUtils.setItems(mItems, items);
        }
        RecyclerViewUtils.notifyDataSetChanged(this, mRecyclerView);
    }

    private String getItem(final int position) {
        synchronized (mSynchronizationObject) {
            return ((position >= 0) && (position < mItems.size())) ? mItems.get(position) : null;
        }
    }

    public int getItemViewType(final int position) {
        return GROUP;
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
            final Context context = mRecyclerView.getContext();
            final int older_active_group_position = mActiveGroupPosition;
            mActiveGroupPosition = (older_active_group_position == position) ? RecyclerView.NO_POSITION : position;
            RecyclerViewUtils.notifyItemChanged(this, mRecyclerView, new int[] { older_active_group_position, mActiveGroupPosition });
            mOnSelectedGroupChangesListener.onSelectedGroupChanges(mActiveGroupPosition == RecyclerView.NO_POSITION ? null : getItem(mActiveGroupPosition), older_active_group_position == RecyclerView.NO_POSITION ? null : getItem(older_active_group_position));
        }
    }

    public void setActiveGroup(@Nullable final String value) {
        synchronized (mSynchronizationObject) {
            int position = mItems.indexOf(value);
            position = ((position >= 0) && (position < getItemCount())) ? position : RecyclerView.NO_POSITION;
            if (mActiveGroupPosition != position) {
                onClick(position);
            }
        }
    }
}
