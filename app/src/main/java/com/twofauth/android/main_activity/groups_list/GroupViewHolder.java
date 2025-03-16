package com.twofauth.android.main_activity.groups_list;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.twofauth.android.R;
import com.twofauth.android.UiUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GroupViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    public interface OnViewHolderClickListener {
        public abstract void onClick(final int position);
    }

    private final TextView mGroup;

    private final OnViewHolderClickListener mOnClickListener;

    public GroupViewHolder(@NotNull final View parent, @Nullable final OnViewHolderClickListener on_click_listener) {
        super(parent);
        mOnClickListener = on_click_listener;
        parent.setOnClickListener(this);
        mGroup = (TextView) parent.findViewById(R.id.group);
    }

    public void draw(@NotNull final Context context, @NotNull final String group, final boolean is_active) {
        final Resources resources = context.getResources();
        mGroup.setText(group);
        mGroup.setTextColor(is_active ? resources.getColor(R.color.accent_foreground, context.getTheme()) : UiUtils.getSystemColor(context, android.R.attr.textColorSecondary));
        mGroup.setSelected(is_active);
    }

    @Override
    public void onClick(@NotNull final View view) {
        if (mOnClickListener != null) {
            mOnClickListener.onClick(getBindingAdapterPosition());
        }
    }

    public static GroupViewHolder newInstance(@NotNull final View parent, @Nullable final OnViewHolderClickListener on_click_listener) {
        return new GroupViewHolder(parent, on_click_listener);
    }
}

