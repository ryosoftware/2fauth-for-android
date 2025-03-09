package com.twofauth.android;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.twofauth.android.main_service.ServerDataLoader;

public class MainService extends Service {
    public static final String ACTION_SERVICE_STARTED = MainService.class.getName() + ".ON_STARTED";
    public static final String ACTION_SERVICE_FINISHED = MainService.class.getName() + ".ON_FINISHED";
    public static final String ACTION_SERVICE_DATA_SYNCED = MainService.class.getName() + ".ON_DATA_LOADED";

    @Override
    public void onCreate() {
        super.onCreate();
        sendBroadcast(new Intent(ACTION_SERVICE_STARTED));
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(Constants.MAIN_SERVICE_PERSISTENT_NOTIFICATION_ID, getNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        }
        else {
            startForeground(Constants.MAIN_SERVICE_PERSISTENT_NOTIFICATION_ID, getNotification());
        }
        (new ServerDataLoader(this)).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sendBroadcast(new Intent(ACTION_SERVICE_FINISHED));
    }

    @Override
    public IBinder onBind(@Nullable final Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        final NotificationManager notification_manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notification_manager.getNotificationChannel(Constants.MAIN_SERVICE_NOTIFICATION_CHANNEL) == null) {
            NotificationChannel main_service_notification_channel = new NotificationChannel(Constants.MAIN_SERVICE_NOTIFICATION_CHANNEL, getString(R.string.sync_service_notifications_channel), NotificationManager.IMPORTANCE_MIN);
            main_service_notification_channel.setSound(null, null);
            main_service_notification_channel.setVibrationPattern(null);
            notification_manager.createNotificationChannel(main_service_notification_channel);
        }
    }

    private Notification getNotification() {
        createNotificationChannel();
        final NotificationCompat.Builder notification_builder = new NotificationCompat.Builder(getBaseContext(), Constants.MAIN_SERVICE_NOTIFICATION_CHANNEL);
        notification_builder.setContentText(getString(ServerDataLoader.getTwoFactorAuthCodesLastLoadTime(this) == 0 ? R.string.trying_to_load_2fa_codes : R.string.trying_to_refresh_2fa_codes));
        notification_builder.setSmallIcon(R.drawable.ic_notification_syncing);
        notification_builder.setContentIntent(PendingIntent.getActivity(getBaseContext(), Constants.MAIN_SERVICE_PERSISTENT_NOTIFICATION_ID, new Intent(getBaseContext(), MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        notification_builder.setShowWhen(false);
        return notification_builder.build();
    }

    public static boolean isRunning(@NonNull final Context context) {
        final ActivityManager activity_manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo running_service : activity_manager.getRunningServices(Integer.MAX_VALUE)) {
            if (running_service.service.getClassName().equals(MainService.class.getName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean canSyncServerData(@NonNull final Context context) {
        final String server = Constants.getDefaultSharedPreferences(context).getString(Constants.TWO_FACTOR_AUTH_SERVER_LOCATION_KEY, null), token = Constants.getDefaultSharedPreferences(context).getString(Constants.TWO_FACTOR_AUTH_TOKEN_KEY, null);
        return ((server != null) && (token != null));
    }

    public static boolean startService(@NonNull final Context context) {
        if ((canSyncServerData(context)) && (! isRunning(context))) {
            context.startForegroundService(new Intent(context, MainService.class));
            return true;
        }
        return false;
    }
}



