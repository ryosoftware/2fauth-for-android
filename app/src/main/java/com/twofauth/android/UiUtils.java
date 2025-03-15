package com.twofauth.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.util.Log;
import android.util.TypedValue;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

public class UiUtils {
    public static void showMessageDialog(@Nullable final Activity activity, final int message_id) {
        if ((message_id != 0) && (activity != null)) {
            try {
                final AlertDialog.Builder alert_dialog_builder = new AlertDialog.Builder(activity);
                alert_dialog_builder.setTitle(R.string.information);
                alert_dialog_builder.setMessage(message_id);
                alert_dialog_builder.setPositiveButton(R.string.ok, (DialogInterface.OnClickListener) null);
                alert_dialog_builder.create().show();
            }
            catch (Exception e) {
                Log.e(Constants.LOG_TAG_NAME, "Exception while trying to show a message dialog", e);
            }
        }
    }

    public static void showConfirmDialog(@Nullable final Activity activity, @Nullable final String message, final int accept_button_text_id, @Nullable final DialogInterface.OnClickListener on_accept_button_click, @Nullable final DialogInterface.OnClickListener on_cancel_button_click) {
        if ((message != null) && (activity != null)) {
            try {
                final AlertDialog.Builder alert_dialog_builder = new AlertDialog.Builder(activity);
                alert_dialog_builder.setTitle(R.string.warning);
                alert_dialog_builder.setMessage(message);
                alert_dialog_builder.setPositiveButton(accept_button_text_id == 0 ? R.string.accept : accept_button_text_id, on_accept_button_click);
                alert_dialog_builder.setNegativeButton(R.string.cancel, on_cancel_button_click);
                alert_dialog_builder.create().show();
            }
            catch (Exception e) {
                Log.e(Constants.LOG_TAG_NAME, "Exception while trying to show a confirmation dialog", e);
            }
        }
    }

    public static void showConfirmDialog(@Nullable final Activity activity, @Nullable final String message, @Nullable final DialogInterface.OnClickListener
 on_accept_button_click, @Nullable final DialogInterface.OnClickListener on_cancel_button_click) {
        showConfirmDialog(activity, message, 0, on_accept_button_click, on_cancel_button_click);
    }

    public static void showConfirmDialog(@Nullable final Activity activity, @Nullable final String message, final int accept_button_text_id, @Nullable final DialogInterface.OnClickListener on_accept_button_click) {
        showConfirmDialog(activity, message, accept_button_text_id, on_accept_button_click, null);
    }

    public static void showConfirmDialog(@Nullable final Activity activity, @Nullable final String message, @Nullable final DialogInterface.OnClickListener on_accept_button_click) {
        showConfirmDialog(activity, message, 0, on_accept_button_click, null);
    }

    public static void showConfirmDialog(@Nullable final Activity activity, final int message_id, final int accept_button_text_id, @Nullable final DialogInterface.OnClickListener on_accept_button_click, @Nullable final DialogInterface.OnClickListener on_cancel_button_click) {
        showConfirmDialog(activity, message_id == 0 ? null : activity.getString(message_id), accept_button_text_id, on_accept_button_click, on_cancel_button_click);
    }

    public static void showConfirmDialog(@Nullable final Activity activity, final int message_id, @Nullable final DialogInterface.OnClickListener on_accept_button_click, @Nullable final DialogInterface.OnClickListener on_cancel_button_click) {
        showConfirmDialog(activity, message_id, 0, on_accept_button_click, on_cancel_button_click);
    }

    public static void showConfirmDialog(@Nullable final Activity activity, final int message_id, final int accept_button_text_id, @Nullable final DialogInterface.OnClickListener on_accept_button_click) {
        showConfirmDialog(activity, message_id, accept_button_text_id, on_accept_button_click, null);
    }

    public static void showConfirmDialog(@Nullable final Activity activity, final int message_id, @Nullable final DialogInterface.OnClickListener on_accept_button_click) {
        showConfirmDialog(activity, message_id, 0, on_accept_button_click, null);
    }

    public static void showToast(@Nullable final Context context, final int message_id) {
        if (context != null) {
            try {
                Toast.makeText(context, message_id, Toast.LENGTH_LONG).show();
            }
            catch (Exception e) {
                Log.e(Constants.LOG_TAG_NAME, "Exception while trying to show a toast", e);
            }
        }
    }

    public static int getPixelsFromDp(@NotNull final Context context, final int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    public static int getSystemColor(@NotNull final Context context, final int resource_id) {
        TypedValue typed_value = new TypedValue();
        context.getTheme().resolveAttribute(resource_id, typed_value, true);
        try (TypedArray typed_array = context.obtainStyledAttributes(typed_value.data, new int[] { resource_id })) {
            return typed_array.getColor(0, -1);
        }
    }
}
