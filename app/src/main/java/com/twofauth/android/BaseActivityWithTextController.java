package com.twofauth.android;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;

import com.twofauth.android.utils.UI;

import org.jetbrains.annotations.NotNull;

public abstract class BaseActivityWithTextController extends BaseActivity implements TextWatcher, OnGlobalLayoutListener {
    protected boolean mKeyboardVisible;

    protected void setListenForKeyboardPresence(final boolean listen_for_keyboard_presence) {
        final ViewTreeObserver observer = findViewById(android.R.id.content).getViewTreeObserver();
        if (listen_for_keyboard_presence) { mKeyboardVisible = UI.isKeyboardVisible(this); observer.addOnGlobalLayoutListener(this); }
        else { observer.removeOnGlobalLayoutListener(this); }
    }

    protected void onKeyboardVisibikityChange(final boolean visible) {}

    @Override
    public void onGlobalLayout() {
        final boolean keyboard_visible = UI.isKeyboardVisible(this);
        if (mKeyboardVisible != keyboard_visible) {
            mKeyboardVisible = keyboard_visible;
            onKeyboardVisibikityChange(keyboard_visible);
        }
    }

    @Override
    public void beforeTextChanged(@NotNull final CharSequence string, final int start, final int count, final int after) {}

    @Override
    public void onTextChanged(@NotNull final CharSequence string, final int start, final int before, final int count) {}

    @Override
    public void afterTextChanged(@NotNull final Editable editable) {}
}
