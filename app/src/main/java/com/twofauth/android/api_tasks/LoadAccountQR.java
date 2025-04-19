package com.twofauth.android.api_tasks;

import android.util.Log;

import com.twofauth.android.API;
import com.twofauth.android.Main;
import com.twofauth.android.database.TwoFactorAccount;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LoadAccountQR {
    public interface OnQRLoadedListener {
        public abstract void onQRLoaded(String qr);
    }

    private static class LoadAccountQRImplementation implements Main.OnBackgroundTaskExecutionListener {
        private final TwoFactorAccount mAccount;

        private final OnQRLoadedListener mListener;

        private String mQR = null;

        LoadAccountQRImplementation(@NotNull final TwoFactorAccount account, @NotNull final OnQRLoadedListener listener) {
            mAccount = account;
            mListener = listener;
        }

        @Override
        public @Nullable Object onBackgroundTaskStarted(@Nullable final Object data) {
            try {
                mQR = API.getQR(mAccount, true);
            }
            catch (Exception e) {
                Log.e(Main.LOG_TAG_NAME, "Exception while trying to load a QR", e);
            }
            return null;
        }

        @Override
        public void onBackgroundTaskFinished(@Nullable final Object data) {
            mListener.onQRLoaded(mQR);
        }
    }

    public static @NotNull Thread getBackgroundTask(@NotNull final TwoFactorAccount account, @NotNull OnQRLoadedListener listener) {
        return Main.getInstance().getBackgroundTask(new LoadAccountQRImplementation(account, listener));
    }
}
