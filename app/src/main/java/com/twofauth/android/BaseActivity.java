package com.twofauth.android;

import android.os.Build;
import android.os.Bundle;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import androidx.appcompat.app.AppCompatActivity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BaseActivity extends AppCompatActivity {
    public interface SynchronizedCallback {
        public abstract Object synchronizedCode(Object object);
    }

    protected final Object mSynchronizationObject = new Object();

    protected void onCreate(@Nullable final Bundle saved_instance_state) {
        super.onCreate(saved_instance_state);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, new OnBackInvokedCallback() {
                @Override
                public void onBackInvoked() {
                    processOnBackPressed();
                }
            });
        }
    }

    protected abstract void processOnBackPressed();

    @Override
    public void onBackPressed() {
        processOnBackPressed();
        super.onBackPressed();
    }

    public boolean isFinishedOrFinishing() {
        return ((isFinishing()) || (isDestroyed()));
    }

    public Object executeSynchronized(@NotNull final SynchronizedCallback callback, @Nullable Object object) {
        synchronized (mSynchronizationObject) {
            return callback.synchronizedCode(object);
        }
    }

    public Object executeSynchronized(@NotNull final SynchronizedCallback callback) {
        return executeSynchronized(callback, null);
    }
}
