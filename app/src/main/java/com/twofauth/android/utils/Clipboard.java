package com.twofauth.android.utils;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.PersistableBundle;

import com.twofauth.android.R;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Clipboard {
    public static void copy(@NotNull final Context context, @NotNull final String value, final boolean is_sensitive, @Nullable final Activity activity_to_be_finished) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText(context.getString(R.string.otp_code), value);
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) && (is_sensitive)) {
                PersistableBundle bundle = new PersistableBundle();
                bundle.putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true);
                clip.getDescription().setExtras(bundle);
            }
            if (activity_to_be_finished != null) {
                clipboard.addPrimaryClipChangedListener(new ClipboardManager.OnPrimaryClipChangedListener() {
                    @Override
                    public void onPrimaryClipChanged() {
                        activity_to_be_finished.finish();
                    }
                });
            }
            clipboard.setPrimaryClip(clip);
        }
    }

    public static void copy(@NotNull final Activity activity, @NotNull final String value, final boolean is_sensitive, final boolean finish_activity) {
        copy(activity, value, is_sensitive, finish_activity ? activity : null);
    }
}
