package com.twofauth.android;

import android.app.Application;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

public class Main extends Application implements LifecycleEventObserver {
    private static Main mMain;

    @Override
    public void onCreate() {
        super.onCreate();
        mMain = this;
        Constants.getDefaultSharedPreferences(this).edit().remove(Constants.LAST_APP_BACKGROUND_TIME_KEY).apply();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    @Override
    public void onStateChanged(@NonNull final LifecycleOwner owner, @NonNull final Lifecycle.Event event) {
        if (event == Lifecycle.Event.ON_PAUSE) {
            Constants.getDefaultSharedPreferences(this).edit().putLong(Constants.LAST_APP_BACKGROUND_TIME_KEY, SystemClock.elapsedRealtime()).apply();
        }
    }

    public void startObservingIfAppBackgrounded() {
        Constants.getDefaultSharedPreferences(this).edit().remove(Constants.LAST_APP_BACKGROUND_TIME_KEY).apply();
    }

    public boolean stopObservingIfAppBackgrounded() {
        return Constants.getDefaultSharedPreferences(this).contains(Constants.LAST_APP_BACKGROUND_TIME_KEY);
    }

    public static Main getInstance() {
        return mMain;
    }
}
