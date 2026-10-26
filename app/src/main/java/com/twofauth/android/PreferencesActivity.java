package com.twofauth.android;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.appbar.MaterialToolbar;

import com.twofauth.android.preferences_activity.MainPreferencesFragment;
import com.twofauth.android.preferences_activity.ManageGroupsPreferences;
import com.twofauth.android.preferences_activity.ServerIdentitiesPreferences;
import com.twofauth.android.preferences_activity.ServerIdentityPreferences;

import org.jetbrains.annotations.NotNull;

public class PreferencesActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable final Bundle saved_instance_state) {
        super.onCreate(saved_instance_state);
        setResult(Activity.RESULT_CANCELED);
        setContentView(R.layout.preferences_activity);
        setTitle(R.string.settings);
        final MaterialToolbar toolbar = (MaterialToolbar) findViewById(R.id.toolbar);
        // Up goes through the dispatcher so the preference screens' own back handling (back stack, unsaved changes) applies
        toolbar.setNavigationOnClickListener(view -> getOnBackPressedDispatcher().onBackPressed());
        // A screen also resumes when the one above it is popped, so this keeps the title right in both directions
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentResumed(@NotNull final FragmentManager fragment_manager, @NotNull final Fragment fragment) {
                if (fragment.getId() == R.id.contents) { toolbar.setTitle(getTitleFor(fragment)); }
            }
        }, false);
        if (saved_instance_state == null) {
            setFragment(new MainPreferencesFragment());
        }
    }

    private int getTitleFor(@Nullable final Fragment fragment) {
        if (fragment instanceof ServerIdentitiesPreferences) { return R.string.server_identities; }
        if (fragment instanceof ServerIdentityPreferences) { return R.string.server_identity; }
        if (fragment instanceof ManageGroupsPreferences) { return R.string.groups; }
        return R.string.settings;
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
