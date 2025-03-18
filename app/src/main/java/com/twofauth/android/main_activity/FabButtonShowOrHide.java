package com.twofauth.android.main_activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;
import android.view.ViewAnimationUtils;

import java.util.List;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.twofauth.android.ListUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FabButtonShowOrHide {
    public enum DisplayState { VISIBLE, HIDDEN };

    private static class RecyclerViewOnScrollListener extends RecyclerView.OnScrollListener {
        private final FabButtonShowOrHide mOwner;

        RecyclerViewOnScrollListener(@NotNull final FabButtonShowOrHide owner) {
            mOwner = owner;
        }

        @Override
        public void onScrollStateChanged(@NonNull final RecyclerView recycler_view, int new_state) {
            super.onScrollStateChanged(recycler_view, new_state);
            if (new_state == RecyclerView.SCROLL_STATE_IDLE) {
                mOwner.show();
            }
            else {
                mOwner.hide();
            }
        }

        @Override
        public void onScrolled(@NonNull final RecyclerView recycler_view, final int dx, final int dy) {
            super.onScrolled(recycler_view, dx, dy);
        }
    }

    private final Object mSynchronizationObject = new Object();
    private final List<FloatingActionButton> mFloatingActionButtons = new ArrayList<FloatingActionButton>();
    private final List<View> mOtherViews = new ArrayList<View>();
    private final List<Animator> mActiveAnimations = new ArrayList<Animator>();

    private DisplayState mDisplayState = null;
    private final RecyclerViewOnScrollListener mRecyclerViewOnScrollListener = new RecyclerViewOnScrollListener(this);

    public FabButtonShowOrHide(@NotNull final RecyclerView recycler_view, @NotNull final FloatingActionButton[] floating_action_buttons, @Nullable final View[] other_views) {
        setFloatingActionButtons(floating_action_buttons);
        setOtherViews(other_views);
        recycler_view.addOnScrollListener(mRecyclerViewOnScrollListener);
    }

    public FabButtonShowOrHide(@NotNull final RecyclerView recycler_view, @NotNull final FloatingActionButton[] floating_action_buttons) {
        this(recycler_view, floating_action_buttons, null);
    }

    public FabButtonShowOrHide(@NotNull final RecyclerView recycler_view) {
        this(recycler_view, null);
    }

    private void cancelAnimations() {
        synchronized (mSynchronizationObject) {
            while (! mActiveAnimations.isEmpty()) {
                final Animator animation = mActiveAnimations.remove(0);
                if (animation.isRunning()) {
                    animation.end();
                }
            }
        }
    }

    private void startAnimation(@NotNull final Animator animation) {
        synchronized (mSynchronizationObject) {
            mActiveAnimations.add(animation);
        }
        animation.start();
    }

    private boolean removeAnimation(@NotNull final Animator animation) {
        synchronized (mSynchronizationObject) {
            return mActiveAnimations.remove(animation);
        }
    }

    private void hide(@NotNull final View view) {
        final int cx = view.getMeasuredWidth() / 2, cy = view.getMeasuredHeight() / 2, radius = view.getWidth() / 2;
        Animator animation = ViewAnimationUtils.createCircularReveal(view, cx, cy, radius, 0);
        animation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (removeAnimation(animation)) {
                    view.setVisibility(View.GONE);
                }
            }
        });
        startAnimation(animation);
    }
    private void hide() {
        synchronized (mSynchronizationObject) {
            for (final FloatingActionButton floating_action_button : mFloatingActionButtons) {
                floating_action_button.hide();
            }
            cancelAnimations();
            for (final View view : mOtherViews) {
                hide(view);
            }
            mDisplayState = DisplayState.HIDDEN;
        }
    }

    private void show(@NotNull final View view) {
        final int cx = view.getMeasuredWidth() / 2, cy = view.getMeasuredHeight() / 2, radius = Math.max(view.getWidth(), view.getHeight()) / 2;
        Animator animation = ViewAnimationUtils.createCircularReveal(view, cx, cy, 0, radius);
        view.setVisibility(View.VISIBLE);
        animation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                removeAnimation(animation);
            }
        });
        startAnimation(animation);
    }

    private void show() {
        synchronized (mSynchronizationObject) {
            for (final FloatingActionButton floating_action_button : mFloatingActionButtons) {
                floating_action_button.show();
            }
            cancelAnimations();
            for (final View view : mOtherViews) {
                show(view);
            }
            mDisplayState = DisplayState.VISIBLE;
        }
    }

    public void setFloatingActionButtons(@Nullable FloatingActionButton[] floating_action_buttons) {
        synchronized (mSynchronizationObject) {
            cancelAnimations();
            ListUtils.setItems(mFloatingActionButtons, floating_action_buttons);
        }
    }

    public void setOtherViews(@Nullable final View[] other_views) {
        synchronized (mSynchronizationObject) {
            cancelAnimations();
            ListUtils.setItems(mOtherViews, other_views);
        }
    }

    public DisplayState getDisplayState() {
        return mDisplayState;
    }
}
