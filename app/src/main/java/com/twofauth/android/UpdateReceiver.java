package com.twofauth.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UpdateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final @NotNull Context context, final @Nullable Intent intent) {
        String action = intent != null ? intent.getAction() : null;

        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) MainTileService.requestListeningState(context);
    }
}