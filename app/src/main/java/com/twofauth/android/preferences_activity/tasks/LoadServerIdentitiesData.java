package com.twofauth.android.preferences_activity.tasks;

import android.content.Context;
import android.util.Log;

import com.twofauth.android.Main;
import com.twofauth.android.database.TwoFactorGroup;
import com.twofauth.android.database.TwoFactorServerIdentity;
import com.twofauth.android.main_service.TwoFactorServerIdentityWithSyncData;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoadServerIdentitiesData {
    public interface OnServerIdentitiesLoadedListener {
        public abstract void onServerIdentitiesLoaded(List<TwoFactorServerIdentityWithSyncDataAndAccountNumbers> server_identities);
        public abstract void onServerIdentitiesLoadError();
    }

    public static class TwoFactorServerIdentityWithSyncDataAndAccountNumbers extends TwoFactorServerIdentityWithSyncData {
        private int mAccounts;
        private int mNotSyncedAccounts;

        TwoFactorServerIdentityWithSyncDataAndAccountNumbers(@NotNull final SQLiteDatabase database, @NotNull final TwoFactorServerIdentity server_identity) throws Exception {
            super(server_identity);
            mAccounts = Main.getInstance().getDatabaseHelper().getTwoFactorAccountsHelper().count(database, server_identity, false);
            mNotSyncedAccounts = Main.getInstance().getDatabaseHelper().getTwoFactorAccountsHelper().count(database, server_identity, true);
        }

        public TwoFactorServerIdentityWithSyncDataAndAccountNumbers() {
            super();
            mAccounts = mNotSyncedAccounts = 0;
        }

        public void onDataDeleted(@NotNull final Context context) {
            super.onDataDeleted(context);
            mAccounts = mNotSyncedAccounts = 0;
        }

        public boolean hasAccounts() {
            return mAccounts > 0;
        }

        public int countAccounts() {
            return mAccounts;
        }

        public boolean hasNotSyncedAccounts() {
            return mNotSyncedAccounts > 0;
        }

        public int countNotSyncedAccounts() {
            return mNotSyncedAccounts;
        }
    }

    private static class LoadServerIdentitiesDataImplementation implements Main.OnBackgroundTaskExecutionListener {
        private final OnServerIdentitiesLoadedListener mListener;

        private final List<TwoFactorServerIdentityWithSyncDataAndAccountNumbers> mServerIdentities = new ArrayList<TwoFactorServerIdentityWithSyncDataAndAccountNumbers>();

        private boolean mSuccess = false;

        LoadServerIdentitiesDataImplementation(@NotNull final OnServerIdentitiesLoadedListener listener) {
            mListener = listener;
        }

        @Override
        public @Nullable Object onBackgroundTaskStarted(@Nullable final Object data) {
            try {
                final SQLiteDatabase database = Main.getInstance().getDatabaseHelper().open(false);
                if (database != null) {
                    try {
                        for (final TwoFactorServerIdentity server_identity : Main.getInstance().getDatabaseHelper().getTwoFactorServerIdentitiesHelper().get(database)) {
                            mServerIdentities.add(new TwoFactorServerIdentityWithSyncDataAndAccountNumbers(database, server_identity));
                        }
                        mSuccess = true;
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
            if (mSuccess) { mListener.onServerIdentitiesLoaded(mServerIdentities); }
            else { mListener.onServerIdentitiesLoadError(); }
        }
    }

    public static @NotNull Thread getBackgroundTask(@NotNull OnServerIdentitiesLoadedListener listener) {
        return Main.getInstance().getBackgroundTask(new LoadServerIdentitiesDataImplementation(listener));
    }
}
