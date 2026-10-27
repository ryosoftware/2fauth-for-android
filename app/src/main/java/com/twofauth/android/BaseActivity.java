package com.twofauth.android;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.twofauth.android.preferences_activity.MainPreferencesFragment;
import com.twofauth.android.utils.UI;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BaseActivity extends AppCompatActivity implements ActivityResultCallback<ActivityResult> {
    public interface SynchronizedCallback {
        public abstract Object synchronizedCode(Object object);
    }

    protected final Object mSynchronizationObject = new Object();

    private boolean mOnResumeFirstExec = true;

    private boolean mDynamicColorsEnabled;

    private final ActivityResultLauncher<Intent> mActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this);

    protected void onCreate(@Nullable final Bundle saved_instance_state) {
        super.onCreate(saved_instance_state);
        Main.getInstance().setTheme();
        mDynamicColorsEnabled = MainPreferencesFragment.isDynamicColorsEnabled(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, new OnBackInvokedCallback() {
                @Override
                public void onBackInvoked() {
                    processOnBackPressed();
                }
            });
        }
    }

    private void setToolbarNavigation() {
        final View toolbar = findViewById(R.id.toolbar);
        if (toolbar instanceof Toolbar) { ((Toolbar) toolbar).setNavigationOnClickListener(view -> processOnBackPressed()); }
    }

    @Override
    public void setContentView(final int layout_res_id) {
        super.setContentView(layout_res_id);
        setToolbarNavigation();
    }

    @Override
    public void setContentView(final View view) {
        super.setContentView(view);
        setToolbarNavigation();
    }

    @Override
    public void setContentView(final View view, final ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        setToolbarNavigation();
    }

    protected void onPauseToHide() {}

    protected void onPause() {
        super.onPause();
        if (! isFinishedOrFinishing()) { onPauseToHide(); }
    }

    protected void onResumeFirstTime() {}

    @Override
    protected void onRestart() {
        super.onRestart();
        // Activities left in the back stack keep the palette they were created with until recreated
        if (mDynamicColorsEnabled != MainPreferencesFragment.isDynamicColorsEnabled(this)) { recreate(); }
    }

    protected void onResume() {
        super.onResume();
        if (mOnResumeFirstExec) {
            mOnResumeFirstExec = false;
            onResumeFirstTime();
        }
    }

    public boolean isFinishedOrFinishing() {
        return ((isFinishing()) || (isDestroyed()));
    }

    public BaseActivity getActivity() {
        return this;
    }

    protected void startActivityFromIntent(@NotNull final Intent intent, @Nullable final Bundle bundle) {
        if (bundle != null) { intent.putExtras(bundle); }
        mActivityResultLauncher.launch(intent);
    }

    protected void startActivityFromIntent(@NotNull final Intent intent) {
        startActivityFromIntent(intent, null);
    }

    protected void startActivityForResult(@NotNull final Class<?> activity_class, @Nullable final Bundle bundle) {
        startActivityFromIntent(new Intent(this, activity_class), bundle);
    }

    protected void startActivityForResult(@NotNull final Class<?> activity_class) {
        startActivityForResult(activity_class, null);
    }

    public void startActivity(@NotNull final Intent intent) {
        try {
            super.startActivity(intent);
        }
        catch (Exception e) {
            Log.e(BuildConfig.TAG, "Exception while trying to start an activity", e);
        }
    }

    @Override
    public void onActivityResult(@NotNull final ActivityResult result) {}

    protected boolean hasPermission(@NotNull final String permission) {
        return (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED);
    }

    protected void requestPermission(@NotNull final String permission, final int request_code) {
        requestPermissions(new String[] { permission }, request_code);
    }

    protected abstract void processOnBackPressed();

    @Override
    public void onBackPressed() {
        processOnBackPressed();
        super.onBackPressed();
    }

    public @Nullable Object executeSynchronized(@NotNull final SynchronizedCallback callback, @Nullable Object object) {
        synchronized (mSynchronizationObject) {
            return callback.synchronizedCode(object);
        }
    }

    public @Nullable Object executeSynchronized(@NotNull final SynchronizedCallback callback) {
        return executeSynchronized(callback, null);
    }
}
