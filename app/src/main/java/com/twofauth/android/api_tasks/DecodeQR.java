package com.twofauth.android.api_tasks;

import android.graphics.Bitmap;
import android.util.Log;

import com.twofauth.android.API;
import com.twofauth.android.Main;
import com.twofauth.android.database.TwoFactorAccount;
import com.twofauth.android.database.TwoFactorServerIdentity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

public class DecodeQR {
    public interface OnQRDecodedListener {
        public abstract void onQRDecoded(JSONObject object);
    }

    private static class DecodeQRImplementation implements Main.OnBackgroundTaskExecutionListener {
        private final TwoFactorServerIdentity mmServerIdentity;

        private final Bitmap mQR;

        private final OnQRDecodedListener mListener;

        private JSONObject mObject = null;

        DecodeQRImplementation(@NotNull final TwoFactorServerIdentity server_identity, @NotNull final Bitmap qr, @NotNull final OnQRDecodedListener listener) {
            mmServerIdentity = server_identity;
            mQR = qr;
            mListener = listener;
        }

        @Override
        public @Nullable Object onBackgroundTaskStarted(@Nullable final Object data) {
            try {
                mObject = API.decodeQR(mmServerIdentity, mQR);
            }
            catch (Exception e) {
                Log.e(Main.LOG_TAG_NAME, "Exception while trying to decode a QR code", e);
            }
            return null;
        }

        @Override
        public void onBackgroundTaskFinished(@Nullable final Object data) {
            mListener.onQRDecoded(mObject);
        }
    }

    public static @NotNull Thread getBackgroundTask(@NotNull final TwoFactorServerIdentity server_identity, @NotNull final Bitmap qr, @NotNull OnQRDecodedListener listener) {
        return Main.getInstance().getBackgroundTask(new DecodeQRImplementation(server_identity, qr, listener));
    }
}
