package com.twofauth.android.api_tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.twofauth.android.Constants;
import com.twofauth.android.Main;
import com.twofauth.android.database.TwoFactorAccount;
import com.twofauth.android.database.TwoFactorGroup;
import com.twofauth.android.preferences_activity.tasks.LoadServerIdentitiesData.TwoFactorServerIdentityWithSyncDataAndAccountNumbers;
import com.twofauth.android.utils.Preferences;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DeleteServerIdentityData {
    public interface OnServerIdentityDeletedListener {
        public abstract void onServerIdentityDeleted(boolean success);
    }

    private static class DeleteServerIdentityDataImplementation implements Main.OnBackgroundTaskExecutionListener {
        private final Context mContext;

        private final TwoFactorServerIdentityWithSyncDataAndAccountNumbers mServerIdentity;

        private final OnServerIdentityDeletedListener mListener;

        private boolean mSuccess = false;

        DeleteServerIdentityDataImplementation(@NotNull final Context context, @NotNull final TwoFactorServerIdentityWithSyncDataAndAccountNumbers server_identity, @NotNull final OnServerIdentityDeletedListener listener) {
            mContext = context;
            mServerIdentity = server_identity;
            mListener = listener;
        }

        @Override
        public Object onBackgroundTaskStarted(@Nullable final Object data) {
            try {
                final SQLiteDatabase database = Main.getInstance().getDatabaseHelper().open(true);
                if (database != null) {
                    try {
                        if (Main.getInstance().getDatabaseHelper().beginTransaction(database)) {
                            try {
                                final List<TwoFactorAccount> accounts = Main.getInstance().getDatabaseHelper().getTwoFactorAccountsHelper().get(database, mServerIdentity.storedData);
                                if (accounts != null) {
                                    for (final TwoFactorAccount account : accounts) {
                                        account.delete(database, mContext);
                                    }
                                }
                                final List<TwoFactorGroup> groups = Main.getInstance().getDatabaseHelper().getTwoFactorGroupsHelper().get(database, mServerIdentity.storedData);
                                if (groups != null) {
                                    for (final TwoFactorGroup group : groups) {
                                        group.delete(database, mContext);
                                    }
                                }
                                mServerIdentity.storedData.delete(database, mContext);
                                mSuccess = true;
                                mServerIdentity.onDataDeleted(mContext);
                            }
                            finally {
                                final SharedPreferences preferences = Preferences.getDefaultSharedPreferences(mContext);
                                Main.getInstance().getDatabaseHelper().endTransaction(database, mSuccess);
                                preferences.edit().putInt(Constants.SERVER_IDENTITIES_COUNT_KEY, preferences.getInt(Constants.SERVER_IDENTITIES_COUNT_KEY, 1) - 1).apply();
                            }
                        }
                    }
                    finally {
                        Main.getInstance().getDatabaseHelper().close(database);
                    }
                }
            }
            catch (Exception e) {
                Log.e(Main.LOG_TAG_NAME, "Exception while trying to store/synchronize a server identity", e);
            }
            return null;
        }

        @Override
        public void onBackgroundTaskFinished(@Nullable final Object data) {
            mListener.onServerIdentityDeleted(mSuccess);
        }
    }

    public static Thread getBackgroundTask(@NotNull final Context context, @NotNull final TwoFactorServerIdentityWithSyncDataAndAccountNumbers server_identity, @NotNull OnServerIdentityDeletedListener listener) {
        return Main.getInstance().getBackgroundTask(new DeleteServerIdentityDataImplementation(context, server_identity, listener));
    }
}
