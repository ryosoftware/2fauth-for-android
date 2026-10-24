package com.twofauth.android;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.TileService;

public class MainTileService extends TileService {
private static final int PENDING_INTENT_REQUEST_CODE = 10001;
    @SuppressLint("StartActivityAndCollapseDeprecated")
    private void showMainActivity(boolean collapse) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (collapse) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                PendingIntent pending_intent = PendingIntent.getActivity(this, PENDING_INTENT_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                startActivityAndCollapse(pending_intent);
            }
            else {
                startActivityAndCollapse(intent);
            }
        }
        else {
            startActivity(intent);
        }
    }

    @Override
    public void onClick() {
        super.onClick();

        if (isLocked()) {
            unlockAndRun(new Runnable() {
                @Override
                public void run() {
                    showMainActivity(false);
                }
            });
        }
        else {
            showMainActivity(true);
        }
    }
}