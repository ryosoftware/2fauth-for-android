package com.twofauth.android.api_tasks;

import android.util.Log;

import com.twofauth.android.Main;
import com.twofauth.android.database.TwoFactorServerIdentity;
import com.twofauth.android.utils.Lists;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LoadServerIdentities {
    public interface OnServerIdentitiesLoadedListener {
        public abstract void onServerIdentitiesLoaded(List<TwoFactorServerIdentity> server_identities);
    }

    private static class LoadServerIdentitiesDataImplementation implements Main.OnBackgroundTaskExecutionListener {
        private final OnServerIdentitiesLoadedListener mListener;

        private final List<TwoFactorServerIdentity> mServerIdentities = new ArrayList<TwoFactorServerIdentity>();

        LoadServerIdentitiesDataImplementation(@NotNull final OnServerIdentitiesLoadedListener listener) {
            mListener = listener;
        }

        @Override
        public @Nullable Object onBackgroundTaskStarted(@Nullable final Object data) {
            try {
                final SQLiteDatabase database = Main.getInstance().getDatabaseHelper().open(false);
                if (database != null) {
                    try {
                        Lists.setItems(mServerIdentities, Main.getInstance().getDatabaseHelper().getTwoFactorServerIdentitiesHelper().get(database));
                    }
                    finally {
                        Main.getInstance().getDatabaseHelper().close(database);
                    }
                }
            }
            catch (Exception e) {
                Log.e(Main.LOG_TAG_NAME, "Exception while trying to load server identities data", e);
            }
            return null;
        }

        @Override
        public void onBackgroundTaskFinished(@Nullable final Object data) {
            mListener.onServerIdentitiesLoaded(mServerIdentities);
        }
    }

    public static @NotNull Thread getBackgroundTask(@NotNull final OnServerIdentitiesLoadedListener listener) {
        return Main.getInstance().getBackgroundTask(new LoadServerIdentitiesDataImplementation(listener));
    }
}
