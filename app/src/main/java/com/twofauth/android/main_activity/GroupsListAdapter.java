package com.twofauth.android.main_activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.twofauth.android.ListUtils;
import com.twofauth.android.R;
import com.twofauth.android.main_activity.groups_list.GroupViewHolder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GroupsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements GroupViewHolder.OnViewHolderClickListener {
    private static final int NO_ONE_ACTIVE_GROUP = -1;
    private static final int GROUP = 1;
    private final List<String> mItems = new ArrayList<String>();
    private final OnSelectedGroupChanges mOnSelectedGroupChanges;
    private final Object mSynchronizationObject = new Object();

    private RecyclerView mRecyclerView = null;
    private int mActiveGroupPosition = NO_ONE_ACTIVE_GROUP;

    public interface OnSelectedGroupChanges {
        public abstract void onSelectedGroupChanges(final String selected_group, final String previous_selected_group);
    }

    public GroupsListAdapter(@NotNull final OnSelectedGroupChanges on_selected_group_changes) {
        mOnSelectedGroupChanges = on_selected_group_changes;
    }

    public GroupsListAdapter(@Nullable final List<String> items, @NotNull final OnSelectedGroupChanges on_selected_group_changes) {
        this(on_selected_group_changes);
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
        if (view_type == GROUP) {
            return GroupViewHolder.newInstance(LayoutInflater.from(context).inflate(R.layout.account_group_item_data, parent, false), this);
        }
        throw new RuntimeException("There is no type that matches the type " + view_type + " + make sure your using types correctly");
    }

    @Override
    public void onBindViewHolder(@NotNull final RecyclerView.ViewHolder view_holder, final int position) {
        synchronized (mSynchronizationObject) {
            if (getItemViewType(position) == GROUP) {
                ((GroupViewHolder) view_holder).draw(mRecyclerView.getContext(), getItem(position), mActiveGroupPosition == position);
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setItems(@Nullable final List<String> items) {
        synchronized (mSynchronizationObject) {
            mActiveGroupPosition = NO_ONE_ACTIVE_GROUP;
            ListUtils.setItems(mItems, items);
        }
        notifyDataSetChanged();
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
            mActiveGroupPosition = (older_active_group_position == position) ? NO_ONE_ACTIVE_GROUP : position;
            if (older_active_group_position != NO_ONE_ACTIVE_GROUP) {
                notifyItemChanged(older_active_group_position);
            }
            if (mActiveGroupPosition != NO_ONE_ACTIVE_GROUP) {
                notifyItemChanged(mActiveGroupPosition);
            }
            mOnSelectedGroupChanges.onSelectedGroupChanges(mActiveGroupPosition == NO_ONE_ACTIVE_GROUP ? null : getItem(mActiveGroupPosition), older_active_group_position == NO_ONE_ACTIVE_GROUP ? null : getItem(older_active_group_position));
        }
    }

    public void setActiveGroup(@Nullable final String value) {
        synchronized (mSynchronizationObject) {
            int position = mItems.indexOf(value);
            position = ((position >= 0) && (position < getItemCount())) ? position : NO_ONE_ACTIVE_GROUP;
            if (mActiveGroupPosition != position) {
                onClick(position);
            }
        }
    }
}
