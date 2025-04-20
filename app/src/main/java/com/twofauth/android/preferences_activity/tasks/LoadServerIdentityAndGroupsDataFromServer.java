package com.twofauth.android.preferences_activity.tasks;

import android.util.Log;

import com.twofauth.android.API;
import com.twofauth.android.Main;
import com.twofauth.android.database.TwoFactorServerIdentity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class LoadServerIdentityAndGroupsData {
    public interface OnServerIdentityAndGroupsDataLoadedListener {
        public abstract void onServerIdentityAndGroupsDataLoaded(boolean success);
    }

    private static class LoadServerIdentityAndGroupsImplementation implements Main.OnBackgroundTaskExecutionListener {
        private final OnServerIdentityAndGroupsDataLoadedListener mListener;

        private final TwoFactorServerIdentity mServerIdentity;

        private boolean mSuccess = false;

        LoadServerIdentityAndGroupsImplementation(@NotNull final TwoFactorServerIdentity server_identity, @NotNull final OnServerIdentityAndGroupsDataLoadedListener listener) {
            mServerIdentity = server_identity;
            mListener = listener;
        }

        @Override
        public @Nullable Object onBackgroundTaskStarted(@Nullable final Object data) {
            try {
                API.refreshIdentityData(mServerIdentity, true);
                API.getGroups(mServerIdentity, true);
                mSuccess = true;
            }
            catch (Exception e) {
                Log.e(Main.LOG_TAG_NAME, "Exception while trying to refresh server identity data", e);
            }
            return null;
        }

        @Override
        public void onBackgroundTaskFinished(@Nullable final Object data) {
            mListener.onServerIdentityAndGroupsDataLoaded(mSuccess);
        }
    }

    public static @NotNull Thread getBackgroundTask(@NotNull final TwoFactorServerIdentity server_identity, @NotNull OnServerIdentityAndGroupsDataLoadedListener listener) {
        return Main.getInstance().getBackgroundTask(new LoadServerIdentityAndGroupsImplementation(server_identity, listener));
    }
}
