package com.twofauth.android.main_activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import androidx.annotation.NonNull;

import com.twofauth.android.MainService;
import com.twofauth.android.MainService.SyncResultType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MainServiceStatusChangedBroadcastReceiver extends BroadcastReceiver {
    public interface OnMainServiceStatusChanged {
        public abstract void onServiceStarted();

        public abstract void onServiceFinished(SyncResultType result_type);

        public abstract void onDataSyncedFromServer(SyncResultType result_type);
    }
    private final OnMainServiceStatusChanged mListener;

    public MainServiceStatusChangedBroadcastReceiver(@NonNull final OnMainServiceStatusChanged listener) {
        mListener = listener;
    }
    public synchronized void onReceive(@NotNull final Context context, @Nullable final Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (MainService.ACTION_SERVICE_STARTED.equals(action)) {
                mListener.onServiceStarted();
            }
            else if (MainService.ACTION_SERVICE_FINISHED.equals(action)) {
                final String result_type = intent.getStringExtra(MainService.EXTRA_RESULT_TYPE);
                mListener.onServiceFinished(result_type == null ? null : SyncResultType.valueOf(result_type));
            }
            else if (MainService.ACTION_SERVICE_DATA_SYNCED.equals(action)) {
                final String result_type = intent.getStringExtra(MainService.EXTRA_RESULT_TYPE);
                mListener.onDataSyncedFromServer(result_type == null ? null : SyncResultType.valueOf(result_type));
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    public void enable(@NotNull final Context context) {
        final IntentFilter[] actions_filter = new IntentFilter[] { new IntentFilter(MainService.ACTION_SERVICE_STARTED), new IntentFilter(MainService.ACTION_SERVICE_FINISHED), new IntentFilter(MainService.ACTION_SERVICE_DATA_SYNCED) };
        for (IntentFilter action_filter : actions_filter) {
            if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) context.registerReceiver(this, action_filter, Context.RECEIVER_EXPORTED);
            else { context.registerReceiver(this, action_filter); }
        }
    }

    public void disable(@NotNull final Context context) {
        context.unregisterReceiver(this);
    }
}
