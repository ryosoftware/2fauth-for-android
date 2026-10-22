package com.twofauth.android.preferences_activity;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.google.android.material.slider.Slider;
import com.twofauth.android.R;

// SeekBarPreference only supports the platform SeekBar; this stores the same int value through a Material 3 Slider
public class SliderPreference extends Preference {
    private int mMin = 0;
    private int mMax = 100;
    private int mValue = 0;

    public SliderPreference(@NonNull final Context context, @Nullable final AttributeSet attrs, final int def_style_attr, final int def_style_res) {
        super(context, attrs, def_style_attr, def_style_res);
        final TypedArray attributes = context.obtainStyledAttributes(attrs, new int[] { android.R.attr.max }, def_style_attr, def_style_res);
        mMax = attributes.getInt(0, mMax);
        attributes.recycle();
        setLayoutResource(R.layout.preference_slider);
        setSelectable(false);
    }

    public SliderPreference(@NonNull final Context context, @Nullable final AttributeSet attrs, final int def_style_attr) {
        this(context, attrs, def_style_attr, 0);
    }

    public SliderPreference(@NonNull final Context context, @Nullable final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SliderPreference(@NonNull final Context context) {
        this(context, null);
    }

    // A value stored under different limits is pulled into the new range, so the slider never shows an out-of-range value
    public void setMin(final int min) {
        mMin = min;
        setValue(mValue);
        notifyChanged();
    }

    public void setMax(final int max) {
        mMax = max;
        setValue(mValue);
        notifyChanged();
    }

    private int clamp(final int value) {
        return Math.max(mMin, Math.min(mMax, value));
    }

    public int getValue() {
        return mValue;
    }

    public void setValue(final int value) {
        final int clamped_value = clamp(value);
        if (clamped_value != mValue) {
            mValue = clamped_value;
            persistInt(clamped_value);
            notifyChanged();
        }
    }

    @Override
    protected Object onGetDefaultValue(@NonNull final TypedArray attributes, final int index) {
        return attributes.getInt(index, 0);
    }

    @Override
    protected void onSetInitialValue(@Nullable final Object default_value) {
        mValue = getPersistedInt(default_value == null ? 0 : (Integer) default_value);
    }

    @Override
    public void onBindViewHolder(@NonNull final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        final Slider slider = (Slider) holder.findViewById(R.id.slider);
        slider.clearOnChangeListeners();
        slider.setValueFrom(mMin);
        slider.setValueTo(mMax);
        slider.setValue(clamp(mValue));
        slider.setEnabled(isEnabled());
        slider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull final Slider slider, final float value, final boolean from_user) {
                final int new_value = Math.round(value);
                if (from_user && (new_value != mValue)) {
                    if (callChangeListener(new_value)) { setValue(new_value); }
                    else { slider.setValue(mValue); }
                }
            }
        });
    }
}
