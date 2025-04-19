package com.twofauth.android;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.twofauth.android.preferences_activity.MainPreferencesFragment;

import org.jetbrains.annotations.NotNull;

public class PreferencesActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable final Bundle saved_instance_state) {
        super.onCreate(saved_instance_state);
        setResult(Activity.RESULT_CANCELED);
        setContentView(R.layout.preferences_activity);
        setTitle(R.string.settings);
        if (saved_instance_state == null) {
            setFragment(new MainPreferencesFragment());
        }
    }

    public void setFragment(@NotNull final Fragment fragment) {
        final FragmentTransaction fragment_transaction = getSupportFragmentManager().beginTransaction().replace(R.id.contents, fragment);
        if (! (fragment instanceof MainPreferencesFragment)) {
            fragment_transaction.addToBackStack(null);
        }
        fragment_transaction.commit();
    }

    @Override
    protected void processOnBackPressed() {
        finish();
    }
}
