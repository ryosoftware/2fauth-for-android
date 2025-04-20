package com.twofauth.android.api_tasks;

import android.util.Log;

import com.twofauth.android.API;
import com.twofauth.android.Main;
import com.twofauth.android.database.TwoFactorServerIdentity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class LoadServerIdentity {
    public interface OnServerIdentityLoadedListener {
        public abstract void onServerIdentityLoaded(boolean success);
    }

    private static class LoadServerIdentityImplementation implements Main.OnBackgroundTaskExecutionListener {
        private final OnServerIdentityLoadedListener mListener;

        private final TwoFactorServerIdentity mServerIdentity;

        private boolean mSuccess = false;

        LoadServerIdentityImplementation(@NotNull final TwoFactorServerIdentity server_identity, @NotNull final OnServerIdentityLoadedListener listener) {
            mServerIdentity = server_identity;
            mListener = listener;
        }

        @Override
        public @Nullable Object onBackgroundTaskStarted(@Nullable final Object data) {
            try {
                mSuccess = (mServerIdentity.isSyncingImmediately() && API.refreshIdentityData(mServerIdentity, true));
            }
            catch (Exception e) {
                Log.e(Main.LOG_TAG_NAME, "Exception while trying to refresh server identity data", e);
            }
            return null;
        }

        @Override
        public void onBackgroundTaskFinished(@Nullable final Object data) {
            mListener.onServerIdentityLoaded(mSuccess);
        }
    }

    public static @NotNull Thread getBackgroundTask(@NotNull final TwoFactorServerIdentity server_identity, @NotNull OnServerIdentityLoadedListener listener) {
        return Main.getInstance().getBackgroundTask(new LoadServerIdentityImplementation(server_identity, listener));
    }
}
