package com.twofauth.android.main_activity.groups_list;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.twofauth.android.Constants;
import com.twofauth.android.R;
import com.twofauth.android.database.TwoFactorGroup;
import com.twofauth.android.main_activity.AppearanceOptions;
import com.twofauth.android.utils.UI;
import com.twofauth.android.utils.Vibrator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GroupViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    public interface OnViewHolderClickListener {
        public abstract void onClick(final int position);
    }

    private final TextView mGroup;

    private final OnViewHolderClickListener mOnClickListener;

    @SuppressLint("WrongViewCast")
    public GroupViewHolder(@NotNull final View parent, @Nullable final OnViewHolderClickListener on_click_listener) {
        super(parent);
        mOnClickListener = on_click_listener;
        parent.setOnClickListener(this);
        mGroup = (TextView) parent.findViewById(R.id.group);
    }

    public void draw(@NotNull final Context context, @NotNull final TwoFactorGroup group, final boolean is_active, @NotNull final AppearanceOptions options) {
        final Resources resources = context.getResources();
        mGroup.setText(options.getServerIdentityAndGroupNames(context, group));
        mGroup.setTextColor(is_active ? resources.getColor(R.color.accent_foreground, context.getTheme()) : UI.getSystemColor(context, android.R.attr.textColorSecondary));
        mGroup.setSelected(is_active);
    }

    @Override
    public void onClick(@NotNull final View view) {
        final int position = getBindingAdapterPosition();
        if ((position != RecyclerView.NO_POSITION) && (mOnClickListener != null)) {
            Vibrator.vibrate(view.getContext(), Constants.NORMAL_HAPTIC_FEEDBACK);
            mOnClickListener.onClick(position);
        }
    }

    public static @NotNull GroupViewHolder newInstance(@NotNull final View parent, @Nullable final OnViewHolderClickListener on_click_listener) {
        return new GroupViewHolder(parent, on_click_listener);
    }
}

