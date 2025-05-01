package com.twofauth.android.api_tasks;

import android.content.Context;
import android.util.Log;

import com.twofauth.android.API;
import com.twofauth.android.Main;
import com.twofauth.android.database.TwoFactorAccount;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UndeleteAccountData {
    public interface OnDataUndeletedListener {
        public abstract void onDataUndeleted(TwoFactorAccount account, boolean success, boolean synced);
    }

    private static class UndeleteAccountDataImplementation implements Main.OnBackgroundTaskExecutionListener {
        private final Context mContext;

        private final TwoFactorAccount mAccount;

        private final OnDataUndeletedListener mListener;

        private boolean mSuccess = false;
        private boolean mSynced = false;

        UndeleteAccountDataImplementation(@NotNull final Context context, @NotNull final TwoFactorAccount account, @Nullable final OnDataUndeletedListener listener) {
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
                                mAccount.setStatus(TwoFactorAccount.STATUS_DEFAULT);
                                mAccount.save(database, mContext);
                                mSuccess = true;
                                mSynced = (mAccount.getServerIdentity().isSyncingImmediately() && API.synchronizeAccount(database, mContext, mAccount, true, true));
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
                mListener.onDataUndeleted(mAccount, mSuccess, mSynced);
            }
        }
    }

    public static @NotNull Thread getBackgroundTask(@NotNull final Context context, @NotNull final TwoFactorAccount account, @Nullable OnDataUndeletedListener listener) {
        return Main.getInstance().getBackgroundTask(new UndeleteAccountDataImplementation(context, account, listener));
    }
}
