package com.twofauth.android.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.twofauth.android.Main;
import com.twofauth.android.R;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class UI {
    public static boolean allowAnimations = true;

    public interface OnSelectionDialogItemSelected {
        public abstract void onItemSelected(int position);
    }

    public interface OnAnimationEndListener {
        public abstract void onAnimationEnd(View view);
    }

    public interface OnTextEnteredListener {
        public abstract void onTextEntered(String text);
    }

    public static boolean isDarkModeActive(@NotNull final Context context) {
        return ((context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES);
    }

    public static boolean isKeyboardVisible(@NotNull final Activity activity) {
        final Rect rect = new Rect();
        final View root_view = activity.findViewById(android.R.id.content);
        root_view.getWindowVisibleDisplayFrame(rect);
        final int screen_height = root_view.getRootView().getHeight(), keypad_height = screen_height - rect.bottom;
        return (keypad_height > root_view.getRootView().getHeight() * 0.15);
    }

    public static boolean isInPortraitMode(@NotNull final Activity activity) {
        return activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    public static void showMessageDialog(@NotNull final Activity activity, @Nullable final String message) {
        if (message != null) {
            try {
                final AlertDialog.Builder alert_dialog_builder = new AlertDialog.Builder(activity);
                alert_dialog_builder.setTitle(R.string.information);
                alert_dialog_builder.setMessage(message);
                alert_dialog_builder.setPositiveButton(R.string.ok, (DialogInterface.OnClickListener) null);
                alert_dialog_builder.create().show();
            } catch (Exception e) {
                Log.e(Main.LOG_TAG_NAME, "Exception while trying to show a message dialog", e);
            }
        }
    }

    public static void showMessageDialog(@NotNull final Activity activity, final int message_id) {
        showMessageDialog(activity, message_id == 0 ? null : activity.getString(message_id));
    }

    public static void showConfirmDialog(@NotNull final Activity activity, @Nullable final String message, final int accept_button_text_id, final int cancel_button_text_id, @Nullable final DialogInterface.OnClickListener on_accept_button_click, @Nullable final DialogInterface.OnClickListener on_cancel_button_click) {
        try {
            if (message != null) {
                final AlertDialog.Builder alert_dialog_builder = new AlertDialog.Builder(activity);
                alert_dialog_builder.setTitle(R.string.warning);
                alert_dialog_builder.setMessage(message);
                alert_dialog_builder.setPositiveButton(accept_button_text_id == 0 ? R.string.accept : accept_button_text_id, on_accept_button_click);
                alert_dialog_builder.setNegativeButton(cancel_button_text_id == 0 ? R.string.cancel : cancel_button_text_id, on_cancel_button_click);
                alert_dialog_builder.create().show();
            }
        }
        catch (Exception e) {
            Log.e(Main.LOG_TAG_NAME, "Exception while trying to show a confirmation dialog", e);
        }
    }

    public static void showConfirmDialog(@NotNull final Activity activity, @Nullable final String message, final int accept_button_text_id, @Nullable final DialogInterface.OnClickListener on_accept_button_click) {
        showConfirmDialog(activity, message, accept_button_text_id, 0, on_accept_button_click, null);
    }

    public static void showConfirmDialog(@NotNull final Activity activity, final int message_id, final int accept_button_text_id, final int cancel_button_text_id, @Nullable final DialogInterface.OnClickListener on_accept_button_click, @Nullable final DialogInterface.OnClickListener on_cancel_button_click) {
        showConfirmDialog(activity, message_id == 0 ? null : activity.getString(message_id), accept_button_text_id, cancel_button_text_id, on_accept_button_click, on_cancel_button_click);
    }

    public static void showConfirmDialog(@NotNull final Activity activity, final int message_id, final int accept_button_text_id, @Nullable final DialogInterface.OnClickListener on_accept_button_click) {
        showConfirmDialog(activity, message_id, accept_button_text_id, 0, on_accept_button_click, null);
    }

    public static void showConfirmDialog(@NotNull final Activity activity, final int message_id, @Nullable final DialogInterface.OnClickListener on_accept_button_click) {
        showConfirmDialog(activity, message_id, 0, 0, on_accept_button_click, null);
    }

    public static void showBase64ImageDialog(@NotNull final Activity activity, @NotNull final String image, @Nullable final DialogInterface.OnClickListener on_accept_button_click) {
        try {
            final LayoutInflater inflater = LayoutInflater.from(activity);
            final View view = inflater.inflate(R.layout.alert_dialog_image, null);
            final WebView webview = (WebView) view.findViewById(R.id.webview);
            webview.loadData(String.format("<html><body><center><img width='300' height='300' src='%s'/></center></body></html>", image), "text/html", "UTF-8");
            final AlertDialog.Builder alert_dialog_builder = new AlertDialog.Builder(activity);
            alert_dialog_builder.setView(view);
            alert_dialog_builder.create().show();
        }
        catch (Exception e) {
            Log.e(Main.LOG_TAG_NAME, "Exception while trying to show a Base64 image in a dialog", e);
        }
    }

    public static void showBase64ImageDialog(@NotNull final Activity activity, @NotNull final String image) {
        showBase64ImageDialog(activity, image, null);
    }

    public static void showSelectItemFromListDialog(@NotNull final Activity activity, @Nullable final String title, @Nullable final String message, @NotNull final List<String> elements, @Nullable final String selected_element, final int accept_button_text_id, final int cancel_button_text_id, @NotNull final OnSelectionDialogItemSelected on_element_selected_listener, @Nullable final DialogInterface.OnClickListener on_cancel_button_click) {
        try {
            final AlertDialog.Builder alert_dialog_builder = new AlertDialog.Builder(activity);
            alert_dialog_builder.setTitle(title);
            alert_dialog_builder.setMessage(message);
            final View view = LayoutInflater.from(activity).inflate(R.layout.alert_dialog_spinner, null);
            final Spinner spinner = view.findViewById(R.id.spinner);
            final ArrayAdapter<String> spinner_adapter = new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_item, elements);
            spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(spinner_adapter);
            final int selected_element_index = selected_element == null ? -1 : elements.indexOf(selected_element);
            if (selected_element_index >= 0) { spinner.setSelection(selected_element_index); }
            alert_dialog_builder.setView(view);
            alert_dialog_builder.setPositiveButton(accept_button_text_id == 0 ? R.string.accept : accept_button_text_id, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(@NotNull final DialogInterface dialog, final int which) {
                    on_element_selected_listener.onItemSelected(spinner.getSelectedItemPosition());
                }
            });
            alert_dialog_builder.setNegativeButton(cancel_button_text_id == 0 ? R.string.cancel : cancel_button_text_id, on_cancel_button_click);
            alert_dialog_builder.create().show();
        }
        catch (Exception e) {
            Log.e(Main.LOG_TAG_NAME, "Exception while trying to show a Base64 image in a dialog", e);
        }
    }

    public static void showSelectItemFromListDialog(@NotNull final Activity activity, @Nullable final String title, @Nullable final String message, @NotNull final List<String> elements, final int accept_button_text_id, final int cancel_button_text_id, @NotNull final OnSelectionDialogItemSelected on_element_selected_listener, @Nullable final DialogInterface.OnClickListener on_cancel_button_click) {
        showSelectItemFromListDialog(activity, title, message, elements, null, accept_button_text_id, cancel_button_text_id, on_element_selected_listener, on_cancel_button_click);
    }

    public static void showSelectItemFromListDialog(@NotNull final Activity activity, @Nullable final String title, @Nullable final String message, @NotNull final List<String> elements, final int accept_button_text_id, final int cancel_button_text_id, @NotNull final OnSelectionDialogItemSelected on_element_selected_listener) {
        showSelectItemFromListDialog(activity, title, message, elements, null, accept_button_text_id, cancel_button_text_id, on_element_selected_listener, null);
    }

    public static void showSelectItemFromListDialog(@NotNull final Activity activity, final int title, int message_id, @NotNull final List<String> elements, @Nullable final String selected_element, final int accept_button_text_id, final int cancel_button_text_id, @NotNull final OnSelectionDialogItemSelected on_element_selected_listener, @Nullable final DialogInterface.OnClickListener on_cancel_button_click) {
        showSelectItemFromListDialog(activity, title == 0 ? null : activity.getString(title), message_id == 0 ? null : activity.getString(message_id), elements, selected_element, accept_button_text_id, cancel_button_text_id, on_element_selected_listener, on_cancel_button_click);
    }

    public static void showSelectItemFromListDialog(@NotNull final Activity activity, final int title, int message_id, @NotNull final List<String> elements, @Nullable final String selected_element, final int accept_button_text_id, final int cancel_button_text_id, @NotNull final OnSelectionDialogItemSelected on_element_selected_listener) {
        showSelectItemFromListDialog(activity, title == 0 ? null : activity.getString(title), message_id == 0 ? null : activity.getString(message_id), elements, selected_element, accept_button_text_id, cancel_button_text_id, on_element_selected_listener, null);
    }

    public static void showSelectItemFromListDialog(@NotNull final Activity activity, final int title, int message_id, @NotNull final List<String> elements, final int accept_button_text_id, final int cancel_button_text_id, @NotNull final OnSelectionDialogItemSelected on_element_selected_listener, @Nullable final DialogInterface.OnClickListener on_cancel_button_click) {
        showSelectItemFromListDialog(activity, title == 0 ? null : activity.getString(title), message_id == 0 ? null : activity.getString(message_id), elements, accept_button_text_id, cancel_button_text_id, on_element_selected_listener, on_cancel_button_click);
    }

    public static void showSelectItemFromListDialog(@NotNull final Activity activity, final int title, int message_id, @NotNull final List<String> elements, final int accept_button_text_id, final int cancel_button_text_id, @NotNull final OnSelectionDialogItemSelected on_element_selected_listener) {
        showSelectItemFromListDialog(activity, title == 0 ? null : activity.getString(title), message_id == 0 ? null : activity.getString(message_id), elements, accept_button_text_id, cancel_button_text_id, on_element_selected_listener, null);
    }

    public static void showEditTextDialog(@NotNull final Activity activity, @Nullable final String title, @Nullable final String message, @Nullable final String value, @Nullable final String hint, @Nullable final String regexp, final int accept_button_text_id, final int cancel_button_text_id, @NotNull final OnTextEnteredListener on_text_entered_listener) {
        try {
            final AlertDialog.Builder alert_dialog_builder = new AlertDialog.Builder(activity);
            alert_dialog_builder.setTitle(title);
            alert_dialog_builder.setMessage(message);
            final FrameLayout container = new FrameLayout(activity);
            final EditText input_text = new EditText(activity);
            final ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(getPixelsFromDp(activity, 20), 0, getPixelsFromDp(activity, 20), 0);
            input_text.setLayoutParams(params);
            input_text.setInputType(InputType.TYPE_CLASS_TEXT);
            input_text.setText(value);
            input_text.setHint(hint);
            container.addView(input_text);
            alert_dialog_builder.setView(container);
            alert_dialog_builder.setPositiveButton(accept_button_text_id == 0 ? R.string.accept : accept_button_text_id, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(@NotNull final DialogInterface dialog, final int which) {
                    on_text_entered_listener.onTextEntered(input_text.getText().toString().trim());
                }
            });
            alert_dialog_builder.setNegativeButton(cancel_button_text_id == 0 ? R.string.cancel : cancel_button_text_id, null);
            final AlertDialog alert_dialog = alert_dialog_builder.create();
            if (regexp != null) {
                alert_dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                       private boolean isValid(@NotNull final String value) {
                           return value.matches(regexp);
                       }
                       @Override
                       public void onShow(@NotNull final DialogInterface dialog) {
                           final Button accept_button = alert_dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                           accept_button.setEnabled(isValid(input_text.getText().toString().trim()));
                           input_text.addTextChangedListener(new TextWatcher() {
                               @Override
                               public void beforeTextChanged(@NotNull final CharSequence string, final int start, final int count, final int after) {}

                               @Override
                               public void onTextChanged(@NotNull final CharSequence string, final int start, final int before, final int count) {}

                               @Override
                               public void afterTextChanged(@NotNull final Editable edit) { accept_button.setEnabled(isValid(edit.toString().trim())); }
                           });
                       }
                });
            }
            alert_dialog.show();
        }
        catch (Exception e) {
            Log.e(Main.LOG_TAG_NAME, "Exception while trying to show a EditText dialog", e);
        }
    }

    public static void showEditTextDialog(@NotNull final Activity activity, final int title, final int message, final String value, final int hint, @Nullable final String regexp, final int accept_button_text_id, final int cancel_button_text_id, @NotNull final OnTextEnteredListener on_text_entered_listener) {
        showEditTextDialog(activity, title == 0 ? null : activity.getString(title), message == 0 ? null : activity.getString(message), value, hint == 0 ? null : activity.getString(hint), regexp, accept_button_text_id, cancel_button_text_id, on_text_entered_listener);
    }

    public static void showToast(@NotNull final Context context, final int message_id) {
        try {
            Toast.makeText(context, message_id, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(Main.LOG_TAG_NAME, "Exception while trying to show a toast", e);
        }
    }

    public static int getPixelsFromDp(@NotNull final Context context, final int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    public static int getSystemColor(@NotNull final Context context, final int resource_id) {
        TypedValue typed_value = new TypedValue();
        context.getTheme().resolveAttribute(resource_id, typed_value, true);
        try (TypedArray typed_array = context.obtainStyledAttributes(typed_value.data, new int[]{resource_id})) {
            return typed_array.getColor(0, -1);
        }
    }

    public static int getWidth(@NotNull final View view) {
        int width = view.getWidth();
        if (width == 0) {
            view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            width = view.getMeasuredWidth();
        }
        return width;
    }

    public static int getHeight(@NotNull final View view) {
        int height = view.getHeight();
        if (height == 0) {
            view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            height = view.getMeasuredHeight();
        }
        return height;
    }

    public static int getDistanceToBottom(@NotNull final Context context, @NotNull final View view) {
        int screen_height = context.getResources().getDisplayMetrics().heightPixels;
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        return screen_height - location[1] - getHeight(view);
    }

    private static void setViewMinimized(@NotNull final View view) {
        view.setAlpha(0f);
        view.setScaleX(0f);
        view.setScaleY(0f);
        view.setVisibility(View.VISIBLE);
    }

    public static void startInfiniteRotationAnimationLoop(final View view, final long duration) {
        if (allowAnimations) {
            final RotateAnimation animation = new RotateAnimation(0, 360, getWidth(view) / 2f, getHeight(view) / 2f);
            animation.setInterpolator(new LinearInterpolator());
            animation.setDuration(duration);
            animation.setRepeatCount(RotateAnimation.INFINITE);
            view.startAnimation(animation);
        }
    }

    public static long startRotationAnimation(@NotNull final View view, final int angle, final long duration, @Nullable final OnAnimationEndListener listener) {
        if (allowAnimations) {
            final float from = view.getRotation(), to = from + angle;
            final ObjectAnimator animation = ObjectAnimator.ofFloat(view, "rotation", from, to);
            final long real_duration = angle * duration / 360;
            animation.setDuration(real_duration);
            animation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(@NotNull final Animator animation) {
                    view.setRotation(to % 360);
                    if (listener != null) { listener.onAnimationEnd(view); }
                }
            });
            animation.start();
            return real_duration;
        }
        else if (listener != null) {
            listener.onAnimationEnd(view);
        }
        return 0;
    }

    public static void animateIconChange(@NotNull final FloatingActionButton button, final int new_icon, final long duration, final boolean toggle_enabled_state) {
        if (allowAnimations) {
            final AnimatorSet animator_set = new AnimatorSet();
            animator_set.playTogether(ObjectAnimator.ofFloat(button, "scaleX", 1f, 0f), ObjectAnimator.ofFloat(button, "scaleY", 1f, 0f));
            animator_set.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(@NotNull final Animator animation) {
                    button.setImageResource(new_icon);
                    if (toggle_enabled_state) { button.setEnabled(! button.isEnabled()); }
                    final AnimatorSet animator_set = new AnimatorSet();
                    animator_set.playTogether(ObjectAnimator.ofFloat(button, "scaleX", 0f, 1f), ObjectAnimator.ofFloat(button, "scaleY", 0f, 1f));
                    animator_set.setDuration(duration / 2);
                    animator_set.start();
                }
            });
            animator_set.setDuration(duration / 2);
            animator_set.start();
        }
        else {
            button.setImageResource(new_icon);
            if (toggle_enabled_state) { button.setEnabled(! button.isEnabled()); }
        }
    }

    public static void animateIconChange(@NotNull final FloatingActionButton button, final int new_icon, final long duration) {
        animateIconChange(button, new_icon , duration, false);
    }

    public static @Nullable ViewPropertyAnimator animateShowOrHide(@NotNull final View view, final boolean show, final long duration, @Nullable final OnAnimationEndListener listener) {
        if (allowAnimations) {
            if (show && (view.getVisibility() != View.VISIBLE)) { setViewMinimized(view); }
            final ViewPropertyAnimator animation = view.animate().alpha(1f).scaleX(show ? 1f : 0f).scaleY(show ? 1f : 0f).setDuration(duration);
            if (listener != null) {
                animation.setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(@NotNull final Animator animation) {
                        listener.onAnimationEnd(view);
                    }
                });
            }
            animation.start();
            return animation;
        }
        else {
            view.setVisibility(show ? View.VISIBLE : View.GONE);
            if (listener != null) { listener.onAnimationEnd(view); }
            return null;
        }
    }

    public static void animateShowOrHide(@NotNull final View view, final boolean show, final long duration) {
        animateShowOrHide(view, show, duration, null);
    }

    public static boolean isSubmenuOpened(@NotNull final View view) {
        return view.getRotation() == 180;
    }

    private static void animateSubmenuOpenOrClose(@NotNull final View view, final int angle, long duration, @Nullable final View[] related_options, @Nullable final OnAnimationEndListener listener) {
        final boolean open_submenu = ! isSubmenuOpened(view);
        if (related_options != null) {
            final long real_duration = startRotationAnimation(view, angle, duration, listener);
            for (final View related_option : related_options) {
                animateShowOrHide(related_option, open_submenu, real_duration);
            }
        }
    }

    public static void animateSubmenuOpenOrClose(@NotNull final View view, final int angle, final long duration, @Nullable final View[] related_options) {
        if (allowAnimations) {
            animateSubmenuOpenOrClose(view, angle, duration, related_options, null);
        }
        else {
            final boolean open_submenu = ! isSubmenuOpened(view);
            view.setRotation(open_submenu ? angle : 0);
            if (related_options != null) {
                for (final View related_option : related_options) {
                    related_option.setVisibility(open_submenu ? View.VISIBLE : View.GONE);
                }
            }
        }
    }

    public static void animateSubmenuOpenOrClose(@NotNull final View view, long duration, @Nullable final View[] related_options) {
        animateSubmenuOpenOrClose(view, 180, duration, related_options);
    }

    public static void hideSubmenuAndRelatedOptions(@NotNull final View view, final int angle, final long submenu_hide_animation_duration, final long button_hide_animation_duration, @Nullable final View[] related_options) {
        if (allowAnimations) {
            if (isSubmenuOpened(view)) {
                animateSubmenuOpenOrClose(view, angle, submenu_hide_animation_duration, related_options, new OnAnimationEndListener() {
                    @Override
                    public void onAnimationEnd(@NotNull final View view) {
                        animateShowOrHide(view, false, button_hide_animation_duration);
                    }
                });
            }
            else {
                animateShowOrHide(view, false, button_hide_animation_duration);
            }
        }
        else {
            view.setRotation(0);
            view.setVisibility(View.GONE);
            if (related_options != null) {
                for (final View related_option : related_options) {
                    related_option.setVisibility(View.GONE);
                }
            }
        }
    }

    public static void hideSubmenuAndRelatedOptions(@NotNull final View view, long submenu_hide_animation_duration, long button_hide_animation_duration, @Nullable final View[] related_options) {
        hideSubmenuAndRelatedOptions(view, 180, submenu_hide_animation_duration, button_hide_animation_duration, related_options);
    }
}
