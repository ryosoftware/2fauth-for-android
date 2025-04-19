package com.twofauth.android.main_activity;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.twofauth.android.database.TwoFactorGroup;
import com.twofauth.android.utils.Lists;
import com.twofauth.android.R;
import com.twofauth.android.utils.RecyclerViews;
import com.twofauth.android.main_activity.groups_list.GroupViewHolder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GroupsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements GroupViewHolder.OnViewHolderClickListener {
    private static final int GROUP = 1;

    private AppearanceOptions mOptions = null;

    private final List<TwoFactorGroup> mItems = new ArrayList<TwoFactorGroup>();
    private final OnSelectedGroupChangesListener mOnSelectedGroupChangesListener;

    private RecyclerView mRecyclerView = null;

    private int mActiveGroupPosition = RecyclerView.NO_POSITION;

    public interface OnSelectedGroupChangesListener {
        public abstract void onSelectedGroupChanges(TwoFactorGroup selected_group, TwoFactorGroup previous_selected_group);
    }

    public GroupsListAdapter(@NotNull final OnSelectedGroupChangesListener on_selected_group_changes_listener) {
        mOnSelectedGroupChangesListener = on_selected_group_changes_listener;
    }

    public GroupsListAdapter(@Nullable final List<TwoFactorGroup> items, @NotNull final OnSelectedGroupChangesListener on_selected_group_changes_listener) {
        this(on_selected_group_changes_listener);
        setItems(items);
    }

    @Override
    public synchronized void onAttachedToRecyclerView(@NotNull final RecyclerView recycler_view) {
        super.onAttachedToRecyclerView(recycler_view);
        mRecyclerView = recycler_view;
    }

    @Override
    public synchronized void onDetachedFromRecyclerView(@NotNull final RecyclerView recycler_view) {
        super.onDetachedFromRecyclerView(recycler_view);
        mRecyclerView = null;
    }

    @Override
    public @NotNull ViewHolder onCreateViewHolder(@NotNull final ViewGroup parent, final int view_type) {
        return GroupViewHolder.newInstance(LayoutInflater.from(parent.getContext()).inflate(R.layout.account_group_item_data, parent, false), this);
    }

    @Override
    public synchronized void onBindViewHolder(@NotNull final RecyclerView.ViewHolder view_holder, final int position) {
        if (getItemViewType(position) == GROUP) {
            if (mOptions == null) { mOptions = new AppearanceOptions(mRecyclerView.getContext()); }
            ((GroupViewHolder) view_holder).draw(mRecyclerView.getContext(), getItem(position), mActiveGroupPosition == position, mOptions);
        }
    }

    public synchronized void setItems(@Nullable final List<TwoFactorGroup> items) {
        Lists.setItems(mItems, items);
        mActiveGroupPosition = RecyclerView.NO_POSITION;
        RecyclerViews.notifyDataSetChanged(this, mRecyclerView);
    }

    public synchronized void setItems(@Nullable final List<TwoFactorGroup> items, @Nullable final TwoFactorGroup selected_group) {
        Lists.setItems(mItems, items);
        final int index = mItems.indexOf(selected_group);
        mActiveGroupPosition = index < 0 ? RecyclerView.NO_POSITION : index;
        RecyclerViews.notifyDataSetChanged(this, mRecyclerView);
    }

    private synchronized TwoFactorGroup getItem(final int position) {
        return ((position >= 0) && (position < mItems.size())) ? mItems.get(position) : null;
    }

    public int getItemViewType(final int position) {
        return GROUP;
    }

    @Override
    public synchronized int getItemCount() {
        return mItems.size();
    }

    public synchronized void onOptionsChanged(@Nullable final AppearanceOptions options) {
        mOptions = options;
        RecyclerViews.notifyDataSetChanged(this, mRecyclerView);
    }

    @Override
    public synchronized void onClick(final int position) {
        final int older_active_group_position = mActiveGroupPosition;
        mActiveGroupPosition = (older_active_group_position == position) ? RecyclerView.NO_POSITION : position;
        RecyclerViews.notifyItemChanged(this, mRecyclerView, new int[] { older_active_group_position, mActiveGroupPosition });
        mOnSelectedGroupChangesListener.onSelectedGroupChanges((mActiveGroupPosition == RecyclerView.NO_POSITION) ? null : getItem(mActiveGroupPosition), (older_active_group_position == RecyclerView.NO_POSITION) ? null : getItem(older_active_group_position));
    }

    public synchronized void setActiveGroup(@Nullable final TwoFactorGroup group) {
        int position = mItems.indexOf(group);
        position = ((position >= 0) && (position < getItemCount())) ? position : RecyclerView.NO_POSITION;
        if (mActiveGroupPosition != position) { onClick(position); }
    }
}
