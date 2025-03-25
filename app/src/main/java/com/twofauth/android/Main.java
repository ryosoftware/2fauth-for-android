package com.twofauth.android;

import android.app.Application;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import org.jetbrains.annotations.NotNull;

public class Main extends Application implements LifecycleEventObserver {
    private static final String LAST_APP_BACKGROUND_TIME_KEY = "last-app-background-time";

    public interface OnBackgroundTaskExecutionListener {
        public abstract Object onBackgroundTaskStarted(Object data);
        public abstract void onBackgroundTaskFinished(Object data);
    }

    private class BackgroundTask extends Thread {
        private final OnBackgroundTaskExecutionListener mListener;
        private final Object mInitialData;

        BackgroundTask(@NotNull final OnBackgroundTaskExecutionListener listener, @Nullable final Object initial_data) {
            mListener = listener;
            mInitialData = initial_data;
        }

        private void finish(@Nullable final Object data) {
            mHandler.post(() -> mListener.onBackgroundTaskFinished(data));
        }

        public void run() {
            Object final_data = null;
            try {
                final_data = mListener.onBackgroundTaskStarted(mInitialData);
            }
            catch (Exception e) {
                Log.e(Constants.LOG_TAG_NAME, "Exception while in background thread", e);
            }
            finally {
                if (! interrupted()) {
                    finish(final_data);
                }
            }
        }
    }

    private static Main mMain;

    private final Handler mHandler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
        mMain = this;
        SharedPreferencesUtilities.getDefaultSharedPreferences(this).edit().remove(LAST_APP_BACKGROUND_TIME_KEY).apply();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        Database.initialize(this);
    }

    @Override
    public void onStateChanged(@NonNull final LifecycleOwner owner, @NonNull final Lifecycle.Event event) {
        if (event == Lifecycle.Event.ON_PAUSE) {
            SharedPreferencesUtilities.getDefaultSharedPreferences(this).edit().putLong(LAST_APP_BACKGROUND_TIME_KEY, SystemClock.elapsedRealtime()).apply();
        }
    }

    public void startObservingIfAppBackgrounded() {
        SharedPreferencesUtilities.getDefaultSharedPreferences(this).edit().remove(LAST_APP_BACKGROUND_TIME_KEY).apply();
    }

    public boolean stopObservingIfAppBackgrounded() {
        return SharedPreferencesUtilities.getDefaultSharedPreferences(this).contains(LAST_APP_BACKGROUND_TIME_KEY);
    }

    public Thread getBackgroundTask(@NotNull final OnBackgroundTaskExecutionListener listener, @Nullable final Object data)
    {
        return new BackgroundTask(listener, data);
    }

    public Thread getBackgroundTask(@NotNull final OnBackgroundTaskExecutionListener listener) { return getBackgroundTask(listener, null); }

    public static Main getInstance() {
        return mMain;
    }
}
