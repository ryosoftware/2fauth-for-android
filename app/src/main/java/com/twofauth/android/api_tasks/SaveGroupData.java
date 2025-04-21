package com.twofauth.android.api_tasks;

import android.content.Context;
import android.util.Log;

import com.twofauth.android.API;
import com.twofauth.android.Main;
import com.twofauth.android.database.TwoFactorAccount;
import com.twofauth.android.database.TwoFactorGroup;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SaveGroupData {
    public interface OnDataSavedListener {
        public abstract void onDataSaved(TwoFactorGroup group, boolean success, boolean synced);
    }

    private static class SaveGroupDataImplementation implements Main.OnBackgroundTaskExecutionListener {
        private final Context mContext;

        private final TwoFactorGroup mGroup;

        private final OnDataSavedListener mListener;

        private boolean mSuccess = false;
        private boolean mSynced = false;

        SaveGroupDataImplementation(@NotNull final Context context, @NotNull final TwoFactorGroup group, @Nullable final OnDataSavedListener listener) {
            mContext = context;
            mGroup = group;
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
                                mGroup.save(database, mContext);
                                mSuccess = true;
                                if (mGroup.getServerIdentity().isSyncingImmediately()) { mSynced = API.syncGroup(database, mContext, mGroup, true); }
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
                Log.e(Main.LOG_TAG_NAME, "Exception while trying to store/synchronize a group", e);
            }
            return null;
        }

        @Override
        public void onBackgroundTaskFinished(@Nullable final Object data) {
            if (mListener != null) { mListener.onDataSaved(mGroup, mSuccess, mSynced); }
        }
    }

    public static @NotNull Thread getBackgroundTask(@NotNull final Context context, @NotNull final TwoFactorGroup group, @Nullable OnDataSavedListener listener) {
        return Main.getInstance().getBackgroundTask(new SaveGroupDataImplementation(context, group, listener));
    }
}
