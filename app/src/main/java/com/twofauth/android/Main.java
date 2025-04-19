package com.twofauth.android;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.twofauth.android.preferences_activity.MainPreferencesFragment;
import com.twofauth.android.utils.Preferences;
import com.twofauth.android.utils.UI;
import com.twofauth.android.utils.Vibrator;

import org.jetbrains.annotations.NotNull;

public class Main extends Application implements LifecycleEventObserver {
    public static final String LOG_TAG_NAME = "2FAuth";

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
            mHandler.post(() -> { if (! isInterrupted()) { mListener.onBackgroundTaskFinished(data); } });
        }

        public void run() {
            Object final_data = null;
            try {
                final_data = mListener.onBackgroundTaskStarted(mInitialData);
            }
            catch (Exception e) {
                Log.e(LOG_TAG_NAME, "Exception while in background thread", e);
            }
            finally {
                if (! interrupted()) { finish(final_data); }
            }
        }
    }

    private static Main mMain;

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private boolean mDebugRelease;

    private DatabaseHelper mDatabaseHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        mMain = this;
        setTheme();
        mDebugRelease = getResources().getBoolean(R.bool.is_debug_version);
        final SharedPreferences preferences = Preferences.getDefaultSharedPreferences(this);
        final Resources resources = getResources();
        UI.allowAnimations = preferences.getBoolean(Constants.ALLOW_ANIMATIONS_KEY, resources.getBoolean(R.bool.allow_animations));
        Vibrator.allowVibrator = preferences.getBoolean(Constants.HAPTIC_FEEDBACK_KEY, resources.getBoolean(R.bool.haptic_feedback));
        preferences.edit().remove(LAST_APP_BACKGROUND_TIME_KEY).apply();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        mDatabaseHelper = new DatabaseHelper(this);
    }

    @Override
    public void onTerminate() {
        mDatabaseHelper.onTerminate();
        super.onTerminate();
    }

    public void setTheme() {
        final MainPreferencesFragment.Theme theme = MainPreferencesFragment.getCurrentUserSelectedThemeValue(this);
        AppCompatDelegate.setDefaultNightMode((theme == MainPreferencesFragment.Theme.DARK) ? AppCompatDelegate.MODE_NIGHT_YES : (theme == MainPreferencesFragment.Theme.LIGHT) ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    @Override
    public void onStateChanged(@NotNull final LifecycleOwner owner, @NotNull final Lifecycle.Event event) {
        if (event == Lifecycle.Event.ON_PAUSE) {
            Preferences.getDefaultSharedPreferences(this).edit().putLong(LAST_APP_BACKGROUND_TIME_KEY, SystemClock.elapsedRealtime()).apply();
        }
    }

    public void startObservingIfAppBackgrounded() {
        Preferences.getDefaultSharedPreferences(this).edit().remove(LAST_APP_BACKGROUND_TIME_KEY).apply();
    }

    public boolean stopObservingIfAppBackgrounded() {
        return Preferences.getDefaultSharedPreferences(this).contains(LAST_APP_BACKGROUND_TIME_KEY);
    }

    public @NotNull Thread getBackgroundTask(@NotNull final OnBackgroundTaskExecutionListener listener, @Nullable final Object data)
    {
        return new BackgroundTask(listener, data);
    }

    public @NotNull Thread getBackgroundTask(@NotNull final OnBackgroundTaskExecutionListener listener) { return getBackgroundTask(listener, null); }

    public void startBackgroundTask(@NotNull final OnBackgroundTaskExecutionListener listener, @Nullable final Object data) {
        final Thread thread = getBackgroundTask(listener, data);
        thread.start();
    }

    public void startBackgroundTask(@NotNull final OnBackgroundTaskExecutionListener listener) {
        startBackgroundTask(listener, null);
    }

    public void runOnUI(@NotNull final Runnable task) {
        mHandler.post(task);
    }

    public @NotNull DatabaseHelper getDatabaseHelper() {
        return mDatabaseHelper;
    }

    public boolean isDebugRelease() {
        return mDebugRelease;
    }

    public boolean isPreRelease() {
        final String app_name = getString(R.string.app_version_name_value);
        return (app_name.contains("-alpha") || app_name.contains("-beta") || (app_name.contains("-rc")));
    }

    public static Main getInstance() {
        return mMain;
    }
}
