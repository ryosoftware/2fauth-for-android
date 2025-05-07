package com.twofauth.android.edit_account_data_activity.tasks;

import android.content.Context;
import android.util.Log;

import com.twofauth.android.Main;
import com.twofauth.android.api_tasks.SaveAccountData;
import com.twofauth.android.database.TwoFactorAccount;
import com.twofauth.android.database.TwoFactorGroup;
import com.twofauth.android.database.TwoFactorServerIdentity;
import com.twofauth.android.utils.JSON;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PinOrUnPinAccount {
    public interface OnAccountPinStatusChangedListener {
        public abstract void onAccountPinStatusChanged(TwoFactorAccount account, boolean success);
    }

    private static class PinOrUnPinAccountImplementation implements SaveAccountData.OnDataSavedListener {
        private final OnAccountPinStatusChangedListener mListener;

        PinOrUnPinAccountImplementation(@Nullable final OnAccountPinStatusChangedListener listener) { mListener = listener; }

        @Override
        public void onDataSaved(TwoFactorAccount account, boolean success, boolean synced) {
            if (mListener != null) { mListener.onAccountPinStatusChanged(account, success); }
        }
    }

    public static @NotNull Thread getBackgroundTask(@NotNull final Context context, @NotNull final TwoFactorAccount account, @Nullable OnAccountPinStatusChangedListener listener) {
        return SaveAccountData.getBackgroundTask(context, account, false, new PinOrUnPinAccountImplementation(listener));
    }
}
