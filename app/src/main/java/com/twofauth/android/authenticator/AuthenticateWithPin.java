package com.twofauth.android.authenticator;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import com.twofauth.android.Constants;
import com.twofauth.android.R;
import com.twofauth.android.utils.Strings;
import com.twofauth.android.utils.UI;
import com.twofauth.android.utils.Vibrator;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AuthenticateWithPin {
    private static final int MIN_PIN_LENGTH = 4;
    private static final int MAX_PIN_LENGTH = 8;
    private static final int MAX_ATTEMPTS = 3;

    public interface OnPinAuthenticationFinishedListener {
        public abstract void onPinAuthenticationSucceeded();
        public abstract void onPinAuthenticationError(boolean cancelled);
    }

    public interface OnPinRequestFinishedListener {
        public abstract void onPinRequestDone(String value);
        public abstract void onPinRequestCancelled();
    }

    private static class PinData {
        public int pin = 0;

        public int digits = 0;
        public boolean accepted = false;

        PinData() {}
    }

    public static void authenticate(@NotNull final FragmentActivity activity, @NotNull final OnPinAuthenticationFinishedListener callback, final String current_pin) {
        final PinData pin_data = new PinData();
        final AlertDialog.Builder dialog_builder = new AlertDialog.Builder(activity);
        final LayoutInflater inflater = activity.getLayoutInflater();
        final View dialog_view = inflater.inflate(R.layout.pin_dialog, null), button_delete = dialog_view.findViewById(R.id.button_delete);
        final TextView pin_textview = (TextView) dialog_view.findViewById(R.id.pin);
        final List<View> buttons = new ArrayList<View>(Arrays.asList(new View[] { dialog_view.findViewById(R.id.button_0), dialog_view.findViewById(R.id.button_1), dialog_view.findViewById(R.id.button_2), dialog_view.findViewById(R.id.button_3), dialog_view.findViewById(R.id.button_4), dialog_view.findViewById(R.id.button_5), dialog_view.findViewById(R.id.button_6), dialog_view.findViewById(R.id.button_7), dialog_view.findViewById(R.id.button_8), dialog_view.findViewById(R.id.button_9), button_delete }));
        dialog_builder.setView(dialog_view);
        dialog_builder.setTitle(R.string.pin_access_dialog_title);
        ((TextView) dialog_view.findViewById(R.id.message)).setText(R.string.pin_access_enter_current_pin);
        dialog_builder.setPositiveButton(activity.getString(R.string.accept), (DialogInterface.OnClickListener) null);
        dialog_builder.setNegativeButton(activity.getString(R.string.cancel), (DialogInterface.OnClickListener) null);
        dialog_builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(@NotNull final DialogInterface dialog) {
                final Object tag = pin_textview.getTag();
                if ((tag instanceof Boolean) && ((boolean) tag)) { callback.onPinAuthenticationSucceeded(); }
                else { callback.onPinAuthenticationError(tag == null); }
            }
        });
        final AlertDialog dialog = dialog_builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialog) {
                final Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                button.setTag(1);
                button.setEnabled(false);
                button.setOnClickListener(new View.OnClickListener() {
                    private void cleanPinData() {
                        pin_data.pin = 0;
                        pin_data.digits = 0;
                        pin_textview.setText(null);
                        button_delete.setEnabled(false);
                        ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                    }

                    @Override
                    public void onClick(@NotNull final View view) {
                        if (Strings.equals(String.valueOf(pin_data.pin), current_pin)) {
                            pin_textview.setTag(true);
                            dialog.dismiss();
                        }
                        else {
                            final int attempts = (int) view.getTag();
                            if (attempts < MAX_ATTEMPTS) {
                                cleanPinData();
                                view.setTag(attempts + 1);
                            }
                            else {
                                pin_textview.setTag(false);
                                dialog.dismiss();
                            }
                            UI.showToast(activity, R.string.pin_is_not_valid);
                        }
                    }
                });
            }
        });
        for (final View button : buttons) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(@NotNull final View view) {
                    final boolean is_button_delete = (view.getId() == R.id.button_delete);
                    Vibrator.vibrate(view.getContext(), Constants.SHORT_HAPTIC_FEEDBACK);
                    if (is_button_delete) {
                        pin_data.pin /= 10;
                        pin_data.digits --;
                    }
                    else {
                        pin_data.pin = pin_data.pin * 10 + buttons.indexOf(view);
                        pin_data.digits ++;
                    }
                    pin_textview.setText(Strings.toHiddenString(pin_data.digits));
                    button_delete.setEnabled(pin_data.digits > 0);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(pin_data.digits >= MIN_PIN_LENGTH);
                }
            });
        }
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        button_delete.setEnabled(false);
    }

    public static void request(@NotNull final FragmentActivity activity, @NotNull final OnPinRequestFinishedListener callback) {
        final PinData[] pins_data = new PinData[] { new PinData(), new PinData() };
        final AlertDialog.Builder dialog_builder = new AlertDialog.Builder(activity);
        final LayoutInflater inflater = activity.getLayoutInflater();
        final View dialog_view = inflater.inflate(R.layout.pin_dialog, null), button_delete = dialog_view.findViewById(R.id.button_delete);
        final TextView pin_textview = (TextView) dialog_view.findViewById(R.id.pin), message_textview = (TextView) dialog_view.findViewById(R.id.message);
        final List<View> buttons = new ArrayList<View>(Arrays.asList(new View[] { dialog_view.findViewById(R.id.button_0), dialog_view.findViewById(R.id.button_1), dialog_view.findViewById(R.id.button_2), dialog_view.findViewById(R.id.button_3), dialog_view.findViewById(R.id.button_4), dialog_view.findViewById(R.id.button_5), dialog_view.findViewById(R.id.button_6), dialog_view.findViewById(R.id.button_7), dialog_view.findViewById(R.id.button_8), dialog_view.findViewById(R.id.button_9), button_delete }));
        dialog_builder.setView(dialog_view);
        dialog_builder.setTitle(R.string.pin_access_dialog_title);
        message_textview.setText(R.string.pin_access_enter_new_pin);
        dialog_builder.setPositiveButton(activity.getString(R.string.accept), (DialogInterface.OnClickListener) null);
        dialog_builder.setNegativeButton(activity.getString(R.string.cancel), (DialogInterface.OnClickListener) null);
        dialog_builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(@NotNull final DialogInterface dialog) {
                final Object tag = pin_textview.getTag();
                if ((tag instanceof Boolean) && ((boolean) tag)) { callback.onPinRequestDone(String.valueOf(pins_data[0].pin)); }
                else { callback.onPinRequestCancelled(); }
            }
        });
        final AlertDialog dialog = dialog_builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialog) {
                final Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                button.setTag(0);
                button.setEnabled(false);
                button.setOnClickListener(new View.OnClickListener() {
                    private void cleanPinData(final int index) {
                        pins_data[index].pin = 0;
                        pins_data[index].digits = 0;
                        pin_textview.setText(null);
                        for (View button : buttons) {
                            button.setEnabled(true);
                        }
                        button_delete.setEnabled(false);
                        ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                    }

                    @Override
                    public void onClick(@NotNull final View view) {
                        final int attempt = (int) view.getTag();
                        if (attempt == 0) {
                            message_textview.setText(R.string.pin_access_enter_new_pin_again);
                            cleanPinData(1);
                            view.setTag(1);
                        }
                        else {
                            final boolean success = ((pins_data[0].pin == pins_data[1].pin) && (pins_data[0].digits == pins_data[1].digits));
                            if (! success) {
                                UI.showToast(activity, R.string.pin_and_its_repeat_does_not_match);
                            }
                            pin_textview.setTag(success);
                            dialog.dismiss();
                        }
                    }
                });
            }
        });
        for (final View button : buttons) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(@NotNull final View view) {
                    final PinData pin_data = pins_data[(int) dialog.getButton(DialogInterface.BUTTON_POSITIVE).getTag()];
                    final boolean is_button_delete = (view.getId() == R.id.button_delete);
                    Vibrator.vibrate(view.getContext(), Constants.SHORT_HAPTIC_FEEDBACK);
                    if (is_button_delete) {
                        pin_data.pin /= 10;
                        pin_data.digits --;
                    }
                    else {
                        pin_data.pin = pin_data.pin * 10 + buttons.indexOf(view);
                        pin_data.digits ++;
                    }
                    for (View button : buttons) {
                        button.setEnabled(pin_data.digits < MAX_PIN_LENGTH);
                    }
                    pin_textview.setText(Strings.toHiddenString(pin_data.digits));
                    button_delete.setEnabled(pin_data.digits > 0);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(pin_data.digits >= MIN_PIN_LENGTH);
                }
            });
        }
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        button_delete.setEnabled(false);
    }
}
