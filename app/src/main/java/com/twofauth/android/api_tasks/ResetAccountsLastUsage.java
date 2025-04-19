package com.twofauth.android.api_tasks;

import android.content.ContentValues;
import android.util.Log;

import com.twofauth.android.Main;
import com.twofauth.android.database.TwoFactorAccount;
import com.twofauth.android.database.TwoFactorAccountsHelper;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ResetAccountsLastUsage {
    public interface OnAccountsLastUsageDoneListener {
        public abstract void onAccountsLastUseResetDone(boolean success);
    }

    private static class ResetAccountsLastUsageImplementation implements Main.OnBackgroundTaskExecutionListener {
        private final OnAccountsLastUsageDoneListener mListener;

        private boolean mSuccess = false;

        ResetAccountsLastUsageImplementation(@NotNull final OnAccountsLastUsageDoneListener listener) {
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
                                final ContentValues values = new ContentValues();
                                values.put(TwoFactorAccount.LAST_USE, 0);
                                database.update(TwoFactorAccountsHelper.TABLE_NAME, values, null, null);
                                mSuccess = true;
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
                Log.e(Main.LOG_TAG_NAME, "Exception while trying to load users data", e);
            }
            return null;
        }

        @Override
        public void onBackgroundTaskFinished(@Nullable final Object data) {
            mListener.onAccountsLastUseResetDone(mSuccess);
        }
    }

    public static @NotNull Thread getBackgroundTask(@NotNull OnAccountsLastUsageDoneListener listener) {
        return Main.getInstance().getBackgroundTask(new ResetAccountsLastUsageImplementation(listener));
    }
}
