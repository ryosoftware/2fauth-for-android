package com.twofauth.android.api_tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.twofauth.android.Constants;
import com.twofauth.android.Main;
import com.twofauth.android.database.TwoFactorAccount;
import com.twofauth.android.database.TwoFactorServerIdentity;
import com.twofauth.android.preferences_activity.tasks.LoadServerIdentitiesData.TwoFactorServerIdentityWithSyncDataAndAccountNumbers;
import com.twofauth.android.utils.Preferences;
import com.twofauth.android.utils.Strings;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SaveServerIdentityData {
    public interface OnServerIdentitySavedListener {
        public abstract void onServerIdentitySaved(boolean success);
    }

    private static class SaveServerIdentityDataImplementation implements Main.OnBackgroundTaskExecutionListener {
        private final Context mContext;

        private final TwoFactorServerIdentityWithSyncDataAndAccountNumbers mServerIdentity;

        private final OnServerIdentitySavedListener mListener;

        private boolean mSuccess = false;

        SaveServerIdentityDataImplementation(@NotNull final Context context, @NotNull final TwoFactorServerIdentityWithSyncDataAndAccountNumbers server_identity, @NotNull final OnServerIdentitySavedListener listener) {
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
                            final boolean in_database = mServerIdentity.storedData.inDatabase();
                            try {
                                final TwoFactorServerIdentity stored_server_identity = in_database ? Main.getInstance().getDatabaseHelper().getTwoFactorServerIdentitiesHelper().get(mServerIdentity.storedData.getRowId()) : null;
                                final boolean is_server_or_token_changed = ((stored_server_identity != null) && ((! Strings.equals(mServerIdentity.storedData.getServer(), stored_server_identity.getServer())) || (! Strings.equals(mServerIdentity.storedData.getToken(), stored_server_identity.getToken()))));
                                if (is_server_or_token_changed) {
                                    final List<TwoFactorAccount> accounts = Main.getInstance().getDatabaseHelper().getTwoFactorAccountsHelper().get(mServerIdentity.storedData, false);
                                    if (accounts != null) {
                                        for (final TwoFactorAccount account : accounts) {
                                            account.delete(database, mContext);
                                        }
                                    }
                                }
                                mServerIdentity.storedData.save(database, mContext);
                                mSuccess = true;
                                if (is_server_or_token_changed) { mServerIdentity.onDataDeleted(mContext); }
                            }
                            finally {
                                Main.getInstance().getDatabaseHelper().endTransaction(database, mSuccess);
                                if (! in_database) { final SharedPreferences preferences = Preferences.getDefaultSharedPreferences(mContext); preferences.edit().putInt(Constants.SERVER_IDENTITIES_COUNT_KEY, preferences.getInt(Constants.SERVER_IDENTITIES_COUNT_KEY, 0) + 1).apply(); }
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
            mListener.onServerIdentitySaved(mSuccess);
        }
    }

    public static Thread getBackgroundTask(@NotNull final Context context, @NotNull final TwoFactorServerIdentityWithSyncDataAndAccountNumbers server_identity, @NotNull OnServerIdentitySavedListener listener) {
        return Main.getInstance().getBackgroundTask(new SaveServerIdentityDataImplementation(context, server_identity, listener));
    }
}
