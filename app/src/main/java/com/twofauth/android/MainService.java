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

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.twofauth.android.database.TwoFactorServerIdentity;
import com.twofauth.android.main_service.tasks.ServerDataSynchronizer;
import com.twofauth.android.utils.Preferences;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

public class MainService extends Service {
    private static final String EXTRA_IS_APP_STARTUP = "app-startup";
    private static final String EXTRA_IDENTITY_TO_SYNCHRONIZE = "identities-to-synchronize";

    public static final String MAIN_SERVICE_NOTIFICATION_CHANNEL = "main-service";
    public static final int MAIN_SERVICE_PERSISTENT_NOTIFICATION_ID = 1001;

    private static final String LAST_SYNC_TIME_KEY = "last-sync-time";


    public enum SyncResultType { ERROR, NO_CHANGES, NO_IDENTITIES, UPDATED };

    public interface OnMainServiceStatusChangedListener {
        public abstract void onServiceStarted();

        public abstract void onServiceFinished(SyncResultType result_type);
    }

    private static final List<OnMainServiceStatusChangedListener> mListeners = new ArrayList<OnMainServiceStatusChangedListener>();

    private SyncResultType mSyncResultType = null;

    private static boolean mIsAppStartup;
    private static long mIdentityToSynchronize;

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= 34) { startForeground(MAIN_SERVICE_PERSISTENT_NOTIFICATION_ID, getNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC); }
        else { startForeground(MAIN_SERVICE_PERSISTENT_NOTIFICATION_ID, getNotification()); }
        onServiceStarted();
    }

    @Override
    public void onDestroy() {
        Preferences.getDefaultSharedPreferences(this).edit().putLong(LAST_SYNC_TIME_KEY, System.currentTimeMillis()).apply();
        onServiceFinished(mSyncResultType);
        super.onDestroy();
    }

    public int onStartCommand(@Nullable final Intent intent, final int flags, final int start_id) {
        mIsAppStartup = false;
        Thread task = null;
        if (intent != null) {
            if (intent.hasExtra(EXTRA_IS_APP_STARTUP)) {
                mIsAppStartup = intent.getBooleanExtra(EXTRA_IS_APP_STARTUP, false);
                if (mIsAppStartup) { task = ServerDataSynchronizer.getBackgroundTask(this, true); }
            }
            if ((task == null) && intent.hasExtra(EXTRA_IDENTITY_TO_SYNCHRONIZE)) {
                mIdentityToSynchronize = intent.getLongExtra(EXTRA_IDENTITY_TO_SYNCHRONIZE, ServerDataSynchronizer.SYNCHRONIZE_ALL_IDENTITIES);
                task = ServerDataSynchronizer.getBackgroundTask(this, mIdentityToSynchronize);
            }
        }
        if (task == null) {
            mIsAppStartup = false;
            mIdentityToSynchronize = ServerDataSynchronizer.SYNCHRONIZE_ALL_IDENTITIES;
            task = ServerDataSynchronizer.getBackgroundTask(this, mIdentityToSynchronize);
        }
        task.start();
        return super.onStartCommand(intent, flags, start_id);
    }

    @Override
    public IBinder onBind(@Nullable final Intent intent) {
        return null;
    }

    public void stopSelf(@NotNull final SyncResultType result_type) {
        mSyncResultType = result_type;
        stopSelf();
    }

    private void createNotificationChannel() {
        final NotificationManager notification_manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notification_manager.getNotificationChannel(MAIN_SERVICE_NOTIFICATION_CHANNEL) == null) {
            NotificationChannel main_service_notification_channel = new NotificationChannel(MAIN_SERVICE_NOTIFICATION_CHANNEL, getString(R.string.sync_service_notifications_channel), NotificationManager.IMPORTANCE_MIN);
            main_service_notification_channel.setSound(null, null);
            main_service_notification_channel.setVibrationPattern(null);
            notification_manager.createNotificationChannel(main_service_notification_channel);
        }
    }

    private @NotNull Notification getNotification() {
        createNotificationChannel();
        final NotificationCompat.Builder notification_builder = new NotificationCompat.Builder(getBaseContext(), MAIN_SERVICE_NOTIFICATION_CHANNEL);
        notification_builder.setContentText(getString(R.string.syncing_2fa_codes));
        notification_builder.setSmallIcon(R.drawable.ic_notification_syncing);
        notification_builder.setContentIntent(PendingIntent.getActivity(getBaseContext(), MAIN_SERVICE_PERSISTENT_NOTIFICATION_ID, new Intent(getBaseContext(), MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        notification_builder.setShowWhen(false);
        return notification_builder.build();
    }

    private static synchronized void onServiceStarted() {
        final List<OnMainServiceStatusChangedListener> listeners = new ArrayList<OnMainServiceStatusChangedListener>(mListeners);
        Main.getInstance().runOnUI(new Runnable() {
            @Override
            public void run() {
                for (final OnMainServiceStatusChangedListener listener : listeners) { listener.onServiceStarted(); }
            }
        });
    }

    private static synchronized void onServiceFinished(@Nullable final SyncResultType result_type) {
        final List<OnMainServiceStatusChangedListener> listeners = new ArrayList<OnMainServiceStatusChangedListener>(mListeners);
        Main.getInstance().runOnUI(new Runnable() {
            @Override
            public void run() {
                for (final OnMainServiceStatusChangedListener listener : listeners) { listener.onServiceFinished(result_type); }
            }
        });
    }

    public static synchronized void addListener(@NotNull final OnMainServiceStatusChangedListener listener) {
        if (! mListeners.contains(listener)) { mListeners.add(listener); } 
    }

    public static synchronized void removeListener(@NotNull final OnMainServiceStatusChangedListener listener) {
        mListeners.remove(listener);
    }

    public static boolean isRunning(@NotNull final Context context) {
        final ActivityManager activity_manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo running_service : activity_manager.getRunningServices(Integer.MAX_VALUE)) {
            if (running_service.service.getClassName().equals(MainService.class.getName())) { return true; }
        }
        return false;
    }

    public static boolean canSyncServerData(@NotNull final Context context) {
        return Preferences.getDefaultSharedPreferences(context).getInt(Constants.SERVER_IDENTITIES_COUNT_KEY, 0) > 0;
    }

    public static void startService(@NotNull final Context context, final boolean is_app_startup) {
        if ((canSyncServerData(context)) && (! isRunning(context))) { context.startForegroundService(new Intent(context, MainService.class).putExtra(EXTRA_IS_APP_STARTUP, is_app_startup)); }
    }

    public static void startService(@NotNull final Context context, @Nullable final TwoFactorServerIdentity server_identity) {
        if ((canSyncServerData(context)) && (! isRunning(context))) { context.startForegroundService(new Intent(context, MainService.class).putExtra(EXTRA_IDENTITY_TO_SYNCHRONIZE, server_identity == null ? ServerDataSynchronizer.SYNCHRONIZE_ALL_IDENTITIES : server_identity.getRowId())); }
    }

    public static void startService(@NotNull final Context context) {
        startService(context, false);
    }

    public static boolean isSyncingIdentity(@NotNull final Context context, TwoFactorServerIdentity server_identity) {
        if (isRunning(context)) {
            if (mIsAppStartup) { return server_identity.isSyncingOnStartup(); }
            return ((server_identity.getRowId() == mIdentityToSynchronize) || (mIdentityToSynchronize == ServerDataSynchronizer.SYNCHRONIZE_ALL_IDENTITIES));
        }
        return false;
    }

    public static long getLastSyncTime(@NotNull final Context context) {
        return Preferences.getDefaultSharedPreferences(context).getLong(LAST_SYNC_TIME_KEY, 0);
    }
}



