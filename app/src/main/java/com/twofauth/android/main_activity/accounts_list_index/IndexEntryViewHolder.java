package com.twofauth.android.main_activity.accounts_list_index;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.twofauth.android.Constants;
import com.twofauth.android.R;
import com.twofauth.android.main_activity.AccountsListIndexAdapter;
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

    private static Drawable[] mPins = new Drawable[] { null, null, null, null };

    public IndexEntryViewHolder(@NotNull final View parent, @Nullable final OnViewHolderClickListener on_click_listener) {
        super(parent);
        mOnClickListener = on_click_listener;
        parent.setOnClickListener(this);
        mLetter = (TextView) parent.findViewById(R.id.letter);
    }

    private @NotNull Drawable getPin(@NotNull final Context context, final boolean is_active) {
        final boolean is_dark_theme_active = UI.isDarkModeActive(context);
        final int index = (is_dark_theme_active ? 2 : 0) + (is_active ? 1 : 0);
        if (mPins[index] == null) { mPins[index] = ContextCompat.getDrawable(context, R.drawable.ic_pinned).mutate(); mPins[index].setTint(mLetter.getCurrentTextColor()); }
        return mPins[index];
    }

    public void draw(@NotNull final Context context, @Nullable final Character letter, final boolean is_active) {
        mLetter.setText(letter == null ? null : letter.toString().toUpperCase());
        mLetter.setTextColor(context.getResources().getColor(is_active ? R.color.floating_action_buttons_foreground : R.color.floating_action_buttons_foreground_disabled, context.getTheme()));
        mLetter.setTypeface(null, is_active ? Typeface.BOLD : Typeface.NORMAL);
        mLetter.setCompoundDrawablesWithIntrinsicBounds(letter == null ? getPin(context, is_active) : null, null, null, null);
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
