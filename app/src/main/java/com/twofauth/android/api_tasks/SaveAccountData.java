package com.twofauth.android.api_tasks;

import android.content.Context;
import android.util.Log;

import com.twofauth.android.API;
import com.twofauth.android.Main;
import com.twofauth.android.database.TwoFactorAccount;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SaveAccountData {
    public interface OnDataSavedListener {
        public abstract void onDataSaved(TwoFactorAccount account, boolean success, boolean synced);
    }

    private static class SaveAccountDataImplementation implements Main.OnBackgroundTaskExecutionListener {
        private final Context mContext;

        private final TwoFactorAccount mAccount;

        private final OnDataSavedListener mListener;

        private boolean mSuccess = false;
        private boolean mSynced = false;

        SaveAccountDataImplementation(@NotNull final Context context, @NotNull final TwoFactorAccount account, @Nullable final OnDataSavedListener listener) {
            mContext = context;
            mAccount = account;
            mListener = listener;
        }

        @Override
        public @Nullable Object onBackgroundTaskStarted(@Nullable final Object data) {
            try {
                final SQLiteDatabase database = Main.getInstance().getDatabaseHelper().open(true);
                if (database != null) {
                    try {
                        if (Main.getInstance().getDatabaseHelper().beginTransaction(database)) {
                            try {
                                final TwoFactorAccount stored_account = mAccount.inDatabase() ? Main.getInstance().getDatabaseHelper().getTwoFactorAccountsHelper().get(mAccount.getRowId()) : null;
                                if ((stored_account != null) && stored_account.isRemote() && (mAccount.getServerIdentity().getRowId() != stored_account.getServerIdentity().getRowId())) {
                                    stored_account.setStatus(TwoFactorAccount.STATUS_DELETED);
                                    stored_account.save(database, mContext);
                                    mAccount.setRowId(-1);
                                    mAccount.setRemoteId(0);
                                    mAccount.setStatus(TwoFactorAccount.STATUS_NOT_SYNCED);
                                }
                                mAccount.save(database, mContext);
                                mSuccess = true;
                                if (mAccount.getServerIdentity().isSyncingImmediately()) {
                                    mSynced = true;
                                    if ((stored_account != null) && (stored_account.getRowId() != mAccount.getRowId())) { mSynced &= API.syncAccount(database, mContext, stored_account, true); }
                                    mSynced &= API.syncAccount(database, mContext, mAccount, true);
                                }
                            }
                            finally {
                                Main.getInstance().getDatabaseHelper().endTransaction(database, mSuccess);
                            }
                        }
                    }
                    finally {
                        Main.getInstance().getDatabaseHelper().close(database);
                    }
                }
            }
            catch (Exception e) {
                Log.e(Main.LOG_TAG_NAME, "Exception while trying to store/synchronize an account", e);
            }
            return null;
        }

        @Override
        public void onBackgroundTaskFinished(@Nullable final Object data) {
            if (mListener != null) { mListener.onDataSaved(mAccount, mSuccess, mSynced); }
        }
    }

    public static @NotNull Thread getBackgroundTask(@NotNull final Context context, @NotNull final TwoFactorAccount account, @Nullable OnDataSavedListener listener) {
        return Main.getInstance().getBackgroundTask(new SaveAccountDataImplementation(context, account, listener));
    }
}
