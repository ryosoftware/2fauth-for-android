package com.twofauth.android.main_activity;

import android.view.View;
import android.view.ViewPropertyAnimator;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.twofauth.android.Constants;
import com.twofauth.android.utils.Lists;
import com.twofauth.android.utils.UI;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FabButtonShowOrHide {
    public static enum DisplayState { VISIBLE, HIDDEN };

    private static class RecyclerViewOnScrollListener extends RecyclerView.OnScrollListener {
        private final FabButtonShowOrHide mOwner;
        private boolean mTakeIntoAccountAutomaticScrollEvents = true;

        private boolean mShowOnIdle = false;

        RecyclerViewOnScrollListener(@NotNull final FabButtonShowOrHide owner) {
            mOwner = owner;
        }

        public void setTakeIntoAccountAutomaticScrollEvents(final boolean take_into_account_automatic_scroll_events) {
            mTakeIntoAccountAutomaticScrollEvents = take_into_account_automatic_scroll_events;
        }

        @Override
        public void onScrollStateChanged(@NotNull final RecyclerView recycler_view, final int new_state) {
            super.onScrollStateChanged(recycler_view, new_state);
            if ((new_state == RecyclerView.SCROLL_STATE_IDLE) && mShowOnIdle) {
                mShowOnIdle = false;
                mOwner.show();
            }
            else if ((new_state == RecyclerView.SCROLL_STATE_DRAGGING) || (new_state == RecyclerView.SCROLL_STATE_SETTLING)) {
                mShowOnIdle |= ((new_state == RecyclerView.SCROLL_STATE_DRAGGING) || mTakeIntoAccountAutomaticScrollEvents);
                if (mShowOnIdle) {
                    mOwner.hide();
                }
            }
        }

        @Override
        public void onScrolled(@NotNull final RecyclerView recycler_view, final int dx, final int dy) {
            super.onScrolled(recycler_view, dx, dy);
        }
    }

    private final List<FloatingActionButton> mFloatingActionButtons = new ArrayList<FloatingActionButton>();
    private final List<View> mOtherViews = new ArrayList<View>();

    private final Map<View, ViewPropertyAnimator> mActiveVisualizationAnimations = new HashMap<View, ViewPropertyAnimator>();
    private final Map<View, ViewPropertyAnimator> mActiveHidingAnimations = new HashMap<View, ViewPropertyAnimator>();

    private DisplayState mDisplayState;

    public FabButtonShowOrHide(@NotNull final RecyclerView recycler_view, final boolean take_into_account_automatic_scroll_events, @NotNull final FloatingActionButton[] floating_action_buttons, @Nullable final View[] other_views, @Nullable final DisplayState initial_display_state) {
        final RecyclerViewOnScrollListener mRecyclerViewOnScrollListener = new RecyclerViewOnScrollListener(this);
        mRecyclerViewOnScrollListener.setTakeIntoAccountAutomaticScrollEvents(take_into_account_automatic_scroll_events);
        recycler_view.addOnScrollListener(mRecyclerViewOnScrollListener);
        setFloatingActionButtons(floating_action_buttons);
        setOtherViews(other_views);
        mDisplayState = initial_display_state;
    }

    public FabButtonShowOrHide(@NotNull final RecyclerView recycler_view, final boolean take_into_account_automatic_scroll_events, @NotNull final FloatingActionButton[] floating_action_buttons, @Nullable final View[] other_views) {
        this(recycler_view, take_into_account_automatic_scroll_events, floating_action_buttons, other_views, null);
    }

    private void cancelAnimation(@NotNull final View view) {
        final boolean cancelling_visualization_animations = (mDisplayState == DisplayState.VISIBLE);
        final Map<View, ViewPropertyAnimator> active_animations_map = cancelling_visualization_animations ? mActiveVisualizationAnimations : mActiveHidingAnimations;
        final ViewPropertyAnimator animation = active_animations_map.remove(view);
        if (animation != null) { animation.cancel(); }
        view.setVisibility(cancelling_visualization_animations ? View.VISIBLE : View.GONE);
    }

    private void cancelAnimations() {
        final boolean cancelling_visualization_animations = (mDisplayState == DisplayState.VISIBLE);
        final Map<View, ViewPropertyAnimator> active_animations_map = cancelling_visualization_animations ? mActiveVisualizationAnimations : mActiveHidingAnimations;
        final List<View> views = new ArrayList<View>(active_animations_map.keySet());
        for (final View view : views) { cancelAnimation(view); }
        active_animations_map.clear();
    }

    private synchronized void onAnimationStarted(@NotNull final View view, @Nullable final ViewPropertyAnimator animation) {
        if (animation != null) {
            final Map<View, ViewPropertyAnimator> active_animations_map = (mDisplayState == DisplayState.VISIBLE) ? mActiveVisualizationAnimations : mActiveHidingAnimations;
            active_animations_map.put(view, animation);
        }
    }

    private synchronized void onAnimationFinished(@Nullable final View view, @NotNull final DisplayState state) {
        final Map<View, ViewPropertyAnimator> active_animations_map = (state == DisplayState.VISIBLE) ? mActiveVisualizationAnimations : mActiveHidingAnimations;
        active_animations_map.remove(view);
    }

    private void hide(@NotNull final View view) {
        final ViewPropertyAnimator animation = UI.animateShowOrHide(view, false, Constants.BUTTON_SHOW_OR_HIDE_ANIMATION_DURATION, new UI.OnAnimationEndListener() {
            @Override
            public void onAnimationEnd(@NotNull final View view) {
                onAnimationFinished(view, DisplayState.HIDDEN);
            }
        });
        onAnimationStarted(view, animation);
    }

    private synchronized void hide() {
        if (mDisplayState != DisplayState.HIDDEN) {
            cancelAnimations();
            mDisplayState = DisplayState.HIDDEN;
            for (final FloatingActionButton floating_action_button : mFloatingActionButtons) { hide(floating_action_button); }
            for (final View view : mOtherViews) { hide(view); }
        }
    }

    private void show(@NotNull final View view) {
        final ViewPropertyAnimator animation = UI.animateShowOrHide(view, true, Constants.BUTTON_SHOW_OR_HIDE_ANIMATION_DURATION, new UI.OnAnimationEndListener() {
            @Override
            public void onAnimationEnd(@NotNull final View view) {
                onAnimationFinished(view, DisplayState.VISIBLE);
            }
        });
        onAnimationStarted(view, animation);
    }

    private synchronized void show() {
        if (mDisplayState != DisplayState.VISIBLE) {
            cancelAnimations();
            mDisplayState = DisplayState.VISIBLE;
            for (final FloatingActionButton floating_action_button : mFloatingActionButtons) { show(floating_action_button); }
            for (final View view : mOtherViews) { show(view); }
        }
    }

    private void setViewVisibility(@NotNull final View view) {
        if (mDisplayState == DisplayState.VISIBLE) { show(view); }
        else { hide(view); }
    }

    public synchronized void setFloatingActionButtons(@Nullable FloatingActionButton[] floating_action_buttons, final boolean set_visibility_according_to_current_state) {
        for (final FloatingActionButton button : mFloatingActionButtons) {
            if ((floating_action_buttons == null) || (Lists.indexOf(floating_action_buttons, button) < 0)) { cancelAnimation(button); }
        }
        if (set_visibility_according_to_current_state && (floating_action_buttons != null)) {
            for (final FloatingActionButton button : floating_action_buttons) {
                setViewVisibility(button);
            }
        }
        Lists.setItems(mFloatingActionButtons, floating_action_buttons);
    }

    public synchronized void setFloatingActionButtons(@Nullable FloatingActionButton[] floating_action_buttons) {
        setFloatingActionButtons(floating_action_buttons, false);
    }

    public synchronized void setOtherViews(@Nullable final View[] other_views, final boolean set_visibility_according_to_current_state) {
        for (final View view : mOtherViews) {
            if ((other_views == null) || (Lists.indexOf(other_views, view) < 0)) { cancelAnimation(view); }
        }
        if (set_visibility_according_to_current_state && (other_views != null)) {
            for (final View view : other_views) {
                setViewVisibility(view);
            }
        }
        Lists.setItems(mOtherViews, other_views);
    }

    public synchronized void setOtherViews(@Nullable final View[] other_views) {
        setOtherViews(other_views, false);
    }

    public DisplayState getDisplayState() {
        return mDisplayState;
    }
}
