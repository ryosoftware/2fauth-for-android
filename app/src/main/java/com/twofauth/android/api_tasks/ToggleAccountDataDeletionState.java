package com.twofauth.android.api_tasks;

import android.content.Context;
import android.util.Log;

import com.twofauth.android.API;
import com.twofauth.android.Main;
import com.twofauth.android.database.TwoFactorAccount;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ToggleAccountDataDeletionState {
    public interface OnDataDeletedOrUndeletedListener {
        public abstract void onDataDeleted(boolean success, boolean synced);
        public abstract void onDataUndeleted(boolean success);
    }

    private static class ToggleAccountDataDeletionStateImplementation implements Main.OnBackgroundTaskExecutionListener {
        private final Context mContext;

        private final TwoFactorAccount mAccount;

        private final OnDataDeletedOrUndeletedListener mListener;

        private boolean mSuccess = false;
        private boolean mSynced = false;
        private final boolean mDeleting;

        ToggleAccountDataDeletionStateImplementation(@NotNull final Context context, @NotNull final TwoFactorAccount account, @Nullable final OnDataDeletedOrUndeletedListener listener) {
            mContext = context;
            mAccount = account;
            mDeleting = ! account.isDeleted();
            mListener = listener;
        }

        private void delete(@NotNull final SQLiteDatabase database) throws Exception {
            mAccount.setStatus(TwoFactorAccount.STATUS_DELETED);
            if (mAccount.getRemoteId() == 0) { mAccount.delete(database, mContext); }
            else { mAccount.save(database, mContext); }
            mSuccess = true;
            mSynced = ((mAccount.getRemoteId() == 0) || (mAccount.getServerIdentity().isSyncingImmediately() && API.syncAccount(database, mContext, mAccount, true)));
        }

        private void undelete(@NotNull final SQLiteDatabase database) throws Exception {
            mAccount.setStatus(TwoFactorAccount.STATUS_DEFAULT);
            mAccount.save(database, mContext);
            mSuccess = true;
            mSynced = (mAccount.getServerIdentity().isSyncingImmediately() && API.syncAccount(database, mContext, mAccount, true));
        }

        @Override
        public @Nullable Object onBackgroundTaskStarted(@Nullable final Object data) {
            try {
                final SQLiteDatabase database = Main.getInstance().getDatabaseHelper().open(true);
                if (database != null) {
                    try {
                        if (Main.getInstance().getDatabaseHelper().beginTransaction(database)) {
                            try {
                                if (mDeleting) {
                                    delete(database);
                                }
                                else {
                                    undelete(database);
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
                Log.e(Main.LOG_TAG_NAME, "Exception while trying to delete/undelete an account", e);
            }
            return null;
        }

        @Override
        public void onBackgroundTaskFinished(@Nullable final Object data) {
            if (mListener != null) {
                if (mDeleting) { mListener.onDataDeleted(mSuccess, mSynced); }
                else { mListener.onDataUndeleted(mSuccess); }
            }
        }
    }

    public static @NotNull Thread getBackgroundTask(@NotNull final Context context, @NotNull final TwoFactorAccount account, @Nullable OnDataDeletedOrUndeletedListener listener) {
        return Main.getInstance().getBackgroundTask(new ToggleAccountDataDeletionStateImplementation(context, account, listener));
    }
}
