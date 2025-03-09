package com.twofauth.android.main_activity;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.jetbrains.annotations.NotNull;

public class FabButtonShowOrHide implements View.OnScrollChangeListener {
    private static final int HIDE_THRESHOLD = 18;

    private final FloatingActionButton[] mFloatingActionButtons;
    private int mScrolledDistance = 0;
    private boolean mControlsVisible = true;

    public FabButtonShowOrHide(@NotNull final RecyclerView recycler_view, @NotNull final FloatingActionButton[] floating_action_buttons) {
        mFloatingActionButtons = new FloatingActionButton[floating_action_buttons.length];
        System.arraycopy(floating_action_buttons, 0, mFloatingActionButtons, 0, floating_action_buttons.length);
        recycler_view.setOnScrollChangeListener(this);
    }

    public FabButtonShowOrHide(@NotNull final RecyclerView recycler_view, @NotNull final FloatingActionButton floating_action_button) {
        this(recycler_view, new FloatingActionButton[] { floating_action_button });
    }

    private void hide() {
        for (FloatingActionButton floating_action_button : mFloatingActionButtons) {
            floating_action_button.hide();
        }
        mControlsVisible = false;
        mScrolledDistance = 0;
    }

    private void show() {
        for (FloatingActionButton floating_action_button : mFloatingActionButtons) {
            floating_action_button.show();
        }
        mControlsVisible = true;
        mScrolledDistance = 0;
    }

    @Override
    public synchronized void onScrollChange(@NotNull final View view, final int scroll_x, final int scroll_y, final int old_scroll_x, final int old_scroll_y) {
        final int scrolled_distance = scroll_y - old_scroll_y;
        if ((mScrolledDistance > HIDE_THRESHOLD) && (mControlsVisible)) {
            hide();
        }
        else if ((mScrolledDistance < -HIDE_THRESHOLD) && (! mControlsVisible)) {
            show();
        }
        if (((mControlsVisible) && (scrolled_distance > 0)) || ((! mControlsVisible) && (scrolled_distance < 0))) {
            mScrolledDistance += scrolled_distance;
        }
    }
}
