package com.twofauth.android.main_activity.accounts_list_index;

import android.content.Context;
import android.content.res.Resources;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.twofauth.android.R;
import com.twofauth.android.UiUtils;
import com.twofauth.android.VibratorUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IndexEntryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    private static final long ON_CLICK_VIBRATION_INTERVAL = 30;

    public interface OnViewHolderClickListener {
        public abstract void onClick(int position);
    }

    private final OnViewHolderClickListener mOnClickListener;

    private final TextView mLetter;

    public IndexEntryViewHolder(@NotNull final View parent, @Nullable final OnViewHolderClickListener on_click_listener) {
        super(parent);
        mOnClickListener = on_click_listener;
        parent.setOnClickListener(this);
        mLetter = (TextView) parent.findViewById(R.id.letter);
    }

    public void draw(@NotNull final Context context, final Character letter, final boolean is_active) {
        final Resources resources = context.getResources();
        mLetter.setText(letter.toString().toUpperCase());
        mLetter.setTextColor(is_active ? resources.getColor(R.color.accent_foreground, context.getTheme()) : UiUtils.getSystemColor(context, android.R.attr.textColorSecondary));
        mLetter.setSelected(is_active);
    }

    @Override
    public void onClick(@NotNull final View view) {
        final int position = getBindingAdapterPosition();
        if ((position != RecyclerView.NO_POSITION) && (mOnClickListener != null)) {
            VibratorUtils.vibrate(view.getContext(), ON_CLICK_VIBRATION_INTERVAL);
            mOnClickListener.onClick(position);
        }
    }

    public static IndexEntryViewHolder newInstance(@NotNull final View parent, @Nullable final OnViewHolderClickListener on_click_listener) {
        return new IndexEntryViewHolder(parent, on_click_listener);
    }
}
