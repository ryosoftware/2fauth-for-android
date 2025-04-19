package com.twofauth.android.main_activity.accounts_list_index;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.twofauth.android.Constants;
import com.twofauth.android.R;
import com.twofauth.android.utils.UI;
import com.twofauth.android.utils.Vibrator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IndexEntryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

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
        mLetter.setTextColor(is_active ? resources.getColor(R.color.accent_foreground, context.getTheme()) : UI.getSystemColor(context, android.R.attr.textColorSecondary));
        mLetter.setTypeface(null, is_active ? Typeface.BOLD : Typeface.NORMAL);
        mLetter.setSelected(is_active);
    }

    @Override
    public void onClick(@NotNull final View view) {
        final int position = getBindingAdapterPosition();
        if ((position != RecyclerView.NO_POSITION) && (mOnClickListener != null)) {
            Vibrator.vibrate(view.getContext(), Constants.NORMAL_HAPTIC_FEEDBACK);
            mOnClickListener.onClick(position);
        }
    }

    public static @NotNull IndexEntryViewHolder newInstance(@NotNull final View parent, @Nullable final OnViewHolderClickListener on_click_listener) {
        return new IndexEntryViewHolder(parent, on_click_listener);
    }
}
