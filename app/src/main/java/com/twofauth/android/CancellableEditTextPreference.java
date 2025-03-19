package com.twofauth.android;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;

public class CancellableEditTextPreference extends EditTextPreference implements Preference.OnPreferenceClickListener {
    private OnPreferenceClickListener mOnClickListener = null;
    public CancellableEditTextPreference(@NonNull final Context context, @Nullable final AttributeSet attrs, final int def_style_attr, final int def_style_res) {
        super(context, attrs, def_style_attr, def_style_res);
        super.setOnPreferenceClickListener(this);
    }

    public CancellableEditTextPreference(@NonNull final Context context, @Nullable final AttributeSet attrs, final int def_style_attr) {
        super(context, attrs, def_style_attr);
        super.setOnPreferenceClickListener(this);
    }

    public CancellableEditTextPreference(@NonNull final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
        super.setOnPreferenceClickListener(this);
    }

    public CancellableEditTextPreference(@NonNull final Context context) {
        super(context);
        super.setOnPreferenceClickListener(this);
    }

    protected void onClick() {}

    @Override
    public boolean onPreferenceClick(@NonNull final Preference preference) {
        if (mOnClickListener != null) {
            if (! mOnClickListener.onPreferenceClick(preference)) {
                super.onClick();
                return false;
            }
            return true;
        }
        return false;
    }

    public void setOnPreferenceClickListener(OnPreferenceClickListener onPreferenceClickListener) {
        mOnClickListener = onPreferenceClickListener;
    }

    public OnPreferenceClickListener getOnPreferenceClickListener() {
        return mOnClickListener;
    }

    public void showDialog() {
        super.onClick();
    }
}
