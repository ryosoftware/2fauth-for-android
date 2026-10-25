package com.twofauth.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final @NotNull Context context, final @Nullable Intent intent) {
        String action = intent != null ? intent.getAction() : null;

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) MainTileService.requestListeningState(context);
    }
}