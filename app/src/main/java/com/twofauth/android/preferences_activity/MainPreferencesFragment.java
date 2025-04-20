package com.twofauth.android.preferences_activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentResultListener;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;

import com.twofauth.android.Authenticator;
import com.twofauth.android.Authenticator.OnAuthenticatorFinishListener;
import com.twofauth.android.Constants;
import com.twofauth.android.HtmlActivity;
import com.twofauth.android.Main;
import com.twofauth.android.PreferencesActivity;
import com.twofauth.android.R;
import com.twofauth.android.api_tasks.ResetAccountsLastUsage;
import com.twofauth.android.utils.Preferences;
import com.twofauth.android.utils.UI;
import com.twofauth.android.utils.Vibrator;
import com.twofauth.android.authenticator.AuthenticateWithBiometrics;
import com.twofauth.android.authenticator.AuthenticateWithBiometrics.OnBiometricAuthenticationFinishedListener;
import com.twofauth.android.authenticator.AuthenticateWithPin;
import com.twofauth.android.authenticator.AuthenticateWithPin.OnPinRequestFinishedListener;
import com.twofauth.android.preferences_activity.tasks.LoadServerIdentitiesData;
import com.twofauth.android.preferences_activity.tasks.LoadServerIdentitiesData.OnServerIdentitiesLoadedListener;
import com.twofauth.android.preferences_activity.tasks.LoadServerIdentitiesData.TwoFactorServerIdentityWithSyncDataAndAccountNumbers;
import com.twofauth.android.api_tasks.ResetAccountsLastUsage.OnAccountsLastUsageDoneListener;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainPreferencesFragment extends PreferenceFragmentCompat implements OnServerIdentitiesLoadedListener, OnAccountsLastUsageDoneListener, OnAuthenticatorFinishListener, OnPinRequestFinishedListener, OnBiometricAuthenticationFinishedListener, OnPreferenceClickListener, OnPreferenceChangeListener, FragmentResultListener {
    public static enum Theme { LIGHT, DARK };

    public static final String SERVER_IDENTITIES_CHANGED = "server-identities-changed";

    private static final String SERVER_IDENTITIES_KEY = "server-identities";
    private static final String RESET_ACCOUNTS_LAST_USE_KEY = "reset-accounts-last-use-data";
    private static final String PIN_ACCESS_ENABLED_KEY = "pin-access";
    private static final String GITHUB_REPO_KEY = "github-repo";
    private static final String OPEN_SOURCE_LICENSES_KEY = "open-source-licenses";
    private static final String APP_VERSION_KEY = "app-version";

    private final Intent mIntent = new Intent();

    private final ArrayList<String> mChanges = new ArrayList<String>();

    private final List<TwoFactorServerIdentityWithSyncDataAndAccountNumbers> mServerIdentities = new ArrayList<TwoFactorServerIdentityWithSyncDataAndAccountNumbers>();

    private Authenticator mAuthenticator;

    private boolean mLoadingServerIdentities = true;

    public MainPreferencesFragment() {
        super();
        mLoadingServerIdentities = true;
        LoadServerIdentitiesData.getBackgroundTask(this).start();
    }

    @Override
    public @NotNull View onCreateView(@NotNull LayoutInflater inflater, @NotNull ViewGroup container, @Nullable Bundle saved_instance_state) {
        final View view = inflater.inflate(R.layout.empty_preferences_fragment, container, false);
        final FrameLayout preferences_container = view.findViewById(R.id.contents);
        preferences_container.addView(super.onCreateView(inflater, preferences_container, saved_instance_state));
        return view;
    }

    @Override
    public void onViewCreated(@NotNull final View view, @Nullable final Bundle saved_instance_state) {
        super.onViewCreated(view, saved_instance_state);
        getParentFragmentManager().setFragmentResultListener(ServerIdentitiesPreferences.EDIT_IDENTITIES, this, this);
        setDivider(null);
    }

    @Override
    public void onCreatePreferences(@Nullable final Bundle saved_instance_state, @Nullable final String root_key) {
        setPreferencesFromResource(R.xml.main_preferences, root_key);
        initializePreferencesListeners();
        setSecurityPreferencesSummariesAndAvailability();
        mAuthenticator = new Authenticator(getActivity());
    }

    // Entry point for the Load Identities task finish

    @Override
    public void onServerIdentitiesLoaded(@Nullable final List<TwoFactorServerIdentityWithSyncDataAndAccountNumbers> server_identities) {
        if (isAdded()) {
            mLoadingServerIdentities = false;
            mServerIdentities.addAll(server_identities);
            findPreference(SERVER_IDENTITIES_KEY).setEnabled(true);
        }
    }

    @Override
    public void onServerIdentitiesLoadError() {
        if (isAdded()) { UI.showToast(getContext(), R.string.cannot_process_request_due_to_an_internal_error); }
    }

    private void setSecurityPreferencesSummariesAndAvailability() {
        if (isAdded()) {
            final Context context = getContext();
            final SharedPreferences preferences = Preferences.getDefaultSharedPreferences(context);
            ((CheckBoxPreference) findPreference(PIN_ACCESS_ENABLED_KEY)).setChecked(preferences.getBoolean(PIN_ACCESS_ENABLED_KEY, false));
            final CheckBoxPreference fingerprint_access_preference = (CheckBoxPreference) findPreference(Constants.USE_FINGERPRINT_INSTEAD_OF_PIN_CODE_KEY);
            if (preferences.getBoolean(Constants.USE_FINGERPRINT_INSTEAD_OF_PIN_CODE_KEY, false) && ((! AuthenticateWithBiometrics.canUseBiometrics(context)) || (! AuthenticateWithBiometrics.areBiometricsLinked()))) {
                preferences.edit().remove(Constants.USE_FINGERPRINT_INSTEAD_OF_PIN_CODE_KEY).apply();
                UI.showToast(context, R.string.fingerprint_validation_disabled_due_to_biometric_enrollment);
            }
            fingerprint_access_preference.setEnabled(preferences.contains(Constants.PIN_CODE_KEY));
            fingerprint_access_preference.setChecked(preferences.getBoolean(Constants.USE_FINGERPRINT_INSTEAD_OF_PIN_CODE_KEY, false));
        }
    }

    private void initializePreferencesListeners() {
        final Context context = getPreferenceManager().getContext();
        final SharedPreferences preferences = Preferences.getDefaultSharedPreferences(context);
        final Resources resources = getResources();
        findPreference(SERVER_IDENTITIES_KEY).setOnPreferenceClickListener(this);
        findPreference(SERVER_IDENTITIES_KEY).setEnabled(!mLoadingServerIdentities);
        findPreference(Constants.SORT_ACCOUNTS_BY_LAST_USE_KEY).setOnPreferenceChangeListener(this);
        findPreference(RESET_ACCOUNTS_LAST_USE_KEY).setOnPreferenceClickListener(this);
        final int min_columns = resources.getInteger(R.integer.accounts_list_min_columns), max_columns = resources.getInteger(R.integer.accounts_list_max_columns);
        ((SeekBarPreference) findPreference(Constants.ACCOUNTS_LIST_COLUMNS_IN_PORTRAIT_MODE_KEY)).setMin(min_columns);
        ((SeekBarPreference) findPreference(Constants.ACCOUNTS_LIST_COLUMNS_IN_PORTRAIT_MODE_KEY)).setMax(max_columns);
        findPreference(Constants.ACCOUNTS_LIST_COLUMNS_IN_PORTRAIT_MODE_KEY).setOnPreferenceChangeListener(this);
        ((SeekBarPreference) findPreference(Constants.ACCOUNTS_LIST_COLUMNS_IN_LANDSCAPE_MODE_KEY)).setMin(min_columns);
        ((SeekBarPreference) findPreference(Constants.ACCOUNTS_LIST_COLUMNS_IN_LANDSCAPE_MODE_KEY)).setMax(max_columns);
        findPreference(Constants.ACCOUNTS_LIST_COLUMNS_IN_LANDSCAPE_MODE_KEY).setOnPreferenceChangeListener(this);
        findPreference(Constants.UNGROUP_OTP_CODE_KEY).setOnPreferenceChangeListener(this);
        findPreference(Constants.DISPLAY_ACCOUNT_SERVER_IDENTITY_KEY).setOnPreferenceChangeListener(this);
        findPreference(Constants.DISPLAY_ACCOUNT_GROUP_KEY).setOnPreferenceChangeListener(this);
        findPreference(Constants.MINIMIZE_APP_AFTER_COPY_TO_CLIPBOARD_KEY).setOnPreferenceChangeListener(this);
        findPreference(Constants.HAPTIC_FEEDBACK_KEY).setOnPreferenceChangeListener(this);
        findPreference(Constants.HAPTIC_FEEDBACK_KEY).setVisible(Vibrator.canVibrate(context));
        findPreference(Constants.ALLOW_ANIMATIONS_KEY).setOnPreferenceChangeListener(this);
        findPreference(Constants.APP_THEME_KEY).setOnPreferenceClickListener(this);
        findPreference(Constants.APP_THEME_KEY).setSummary(getUserSelectedThemeLabel(context, getCurrentUserSelectedThemeValue(context)));
        findPreference(Constants.DISABLE_SCREENSHOTS_KEY).setOnPreferenceChangeListener(this);
        findPreference(Constants.HIDE_OTP_AUTOMATICALLY_KEY).setOnPreferenceChangeListener(this);
        findPreference(Constants.SHOW_NEXT_OTP_CODE_KEY).setOnPreferenceChangeListener(this);
        findPreference(PIN_ACCESS_ENABLED_KEY).setOnPreferenceChangeListener(this);
        ((CheckBoxPreference) findPreference(PIN_ACCESS_ENABLED_KEY)).setChecked(preferences.contains(Constants.PIN_CODE_KEY));
        findPreference(Constants.USE_FINGERPRINT_INSTEAD_OF_PIN_CODE_KEY).setOnPreferenceChangeListener(this);
        findPreference(Constants.USE_FINGERPRINT_INSTEAD_OF_PIN_CODE_KEY).setVisible(AuthenticateWithBiometrics.canUseBiometrics(context));
        findPreference(Constants.ALLOW_COPY_SERVER_ACCESS_TOKEN_KEY).setOnPreferenceChangeListener(this);
        findPreference(Constants.DOWNLOAD_ICONS_FROM_EXTERNAL_SOURCES_KEY).setOnPreferenceChangeListener(this);
        findPreference(GITHUB_REPO_KEY).setOnPreferenceClickListener(this);
        findPreference(Constants.AUTO_UPDATE_APP_KEY).setOnPreferenceChangeListener(this);
        findPreference(Constants.AUTO_UPDATE_APP_INCLUDE_PRERELEASES_KEY).setOnPreferenceChangeListener(this);
        findPreference(Constants.AUTO_UPDATE_APP_ONLY_IN_WIFI_KEY).setOnPreferenceChangeListener(this);
        findPreference(OPEN_SOURCE_LICENSES_KEY).setOnPreferenceClickListener(this);
        String app_version = getString(Main.getInstance().isPreRelease() ? R.string.app_build_version_number_prerelease : R.string.app_build_version_number, getString(R.string.app_version_name_value), resources.getInteger(R.integer.app_version_number_value));
        if (Main.getInstance().isDebugRelease()) { app_version = getString(R.string.app_version_is_debug_release, app_version); }
        findPreference(APP_VERSION_KEY).setSummary(app_version);
    }

    // We sent the changed setting keys to the caller via a Intent

    private void onSettingValueChanged(@NotNull final String[] keys) {
        synchronized (mIntent) {
            for (String key : keys) { mIntent.putExtra(key, true); }
            getActivity().setResult(Activity.RESULT_OK, mIntent);
        }
    }

    private void onSettingValueChanged(@NotNull final String key) {
        onSettingValueChanged(new String[] { key });
    }

    // This function is triggered when a fragment started from this is finish
    // Basically the only fragment we start here is the identities manager
    // In the case user has changed a identity, we notify to the caller, like other setting change

    @Override
    public void onFragmentResult(@NotNull final String request_key, @NotNull final Bundle result) {
        if (isAdded() && ServerIdentitiesPreferences.EDIT_IDENTITIES.equals(request_key) && result.getBoolean(ServerIdentitiesPreferences.EDIT_IDENTITIES_RESULT)) { onSettingValueChanged(SERVER_IDENTITIES_CHANGED); }
    }

    @Override
    public boolean onPreferenceClick(@NotNull final Preference preference) {
        final Context context = preference.getContext();
        if (SERVER_IDENTITIES_KEY.equals(preference.getKey())) {
            ((PreferencesActivity) getActivity()).setFragment(new ServerIdentitiesPreferences(mServerIdentities));
        }
        else if (RESET_ACCOUNTS_LAST_USE_KEY.equals(preference.getKey())) {
            onSettingValueChanged(Constants.SORT_ACCOUNTS_BY_LAST_USE_KEY);
            preference.setEnabled(false);
            ResetAccountsLastUsage.getBackgroundTask(this).start();
        }
        else if (Constants.APP_THEME_KEY.equals(preference.getKey())) {
            final String[] theme_names = new String[] { getString(R.string.dark_theme), getString(R.string.light_theme), getString(R.string.follow_system_theme) };
            final Theme[] theme_values = new Theme[] { Theme.DARK, Theme.LIGHT, null };
            UI.showSelectItemFromListDialog(getActivity(), R.string.theme, 0, Arrays.asList(theme_names), getUserSelectedThemeLabel(context, getCurrentUserSelectedThemeValue(context)), 0, 0, new UI.OnSelectionDialogItemSelected() {
                @Override
                public void onItemSelected(final int position) {
                    final Theme new_user_selected_theme = (position >= 0) ? theme_values[position] : null;
                    if (getCurrentUserSelectedThemeValue(context) != new_user_selected_theme) {
                        final SharedPreferences.Editor editor = Preferences.getDefaultSharedPreferences(context).edit();
                        editor.remove(Constants.APP_THEME_KEY);
                        if (new_user_selected_theme != null) { editor.putString(Constants.APP_THEME_KEY, theme_values[position].name()); }
                        editor.apply();
                        if (isAdded()) { findPreference(Constants.APP_THEME_KEY).setSummary(theme_names[position]); getActivity().recreate(); }
                    }
                }
            });
        }
        else if (GITHUB_REPO_KEY.equals(preference.getKey())) {
            HtmlActivity.openInWebBrowser(getActivity(), Constants.GITHUB_REPO);
        }
        else if (OPEN_SOURCE_LICENSES_KEY.equals(preference.getKey())) {
            startActivity(new Intent(context, HtmlActivity.class).putExtra(HtmlActivity.EXTRA_FILE_PATHNAME, "file:///android_asset/open-source-licenses.html"));
            return true;
        }
        return false;
    }

    // Entry point for the accounts last use reset tash finish

    @Override
    public void onAccountsLastUseResetDone(boolean success) {
        if (isAdded()) {
            UI.showToast(getContext(), success ? R.string.data_usage_has_been_deleted : R.string.data_usage_has_not_been_deleted);
            findPreference(RESET_ACCOUNTS_LAST_USE_KEY).setEnabled(true);
        }
    }

    @Override
    public boolean onPreferenceChange(@NotNull final Preference preference, final Object new_value) {
        if (Constants.HAPTIC_FEEDBACK_KEY.equals(preference.getKey())) {
            Vibrator.allowVibrator = (Boolean) new_value;
        }
        else if (Constants.ALLOW_ANIMATIONS_KEY.equals(preference.getKey())) {
            UI.allowAnimations = (Boolean) new_value;
        }
        else if (Constants.DISABLE_SCREENSHOTS_KEY.equals(preference.getKey())) {
            UI.showMessageDialog(getActivity(), R.string.change_will_be_applied_next_time_you_start_the_app);
        }
        else if (PIN_ACCESS_ENABLED_KEY.equals(preference.getKey())) {
            if ((boolean) new_value) { AuthenticateWithPin.request(getActivity(), this); }
            else { mAuthenticator.authenticate(this); }
            return false;
        }
        else if (Constants.USE_FINGERPRINT_INSTEAD_OF_PIN_CODE_KEY.equals(preference.getKey())) {
            if (! (boolean) new_value) { onSettingValueChanged(preference.getKey()); return true; }
            AuthenticateWithBiometrics.authenticate(getActivity(), this);
            return false;
        }
        onSettingValueChanged(preference.getKey());
        return true;
    }

    // This procedure is triggered when user tries to disable PIN access and has been properly authenticated
    // We disable PIN access and biometrics

    @Override
    public void onAuthenticationSuccess(@Nullable final Object object) {
        if (isAdded()) {
            final Context context = getContext();
            Preferences.getDefaultSharedPreferences(context).edit().remove(Constants.PIN_CODE_KEY).remove(Constants.USE_FINGERPRINT_INSTEAD_OF_PIN_CODE_KEY).putBoolean(PIN_ACCESS_ENABLED_KEY, false).apply();
            UI.showToast(context, R.string.pin_has_been_removed);
            onSettingValueChanged(new String[] { Constants.PIN_CODE_KEY, Constants.USE_FINGERPRINT_INSTEAD_OF_PIN_CODE_KEY });
            setSecurityPreferencesSummariesAndAvailability();
        }
    }

    @Override
    public void onAuthenticationError(@Nullable final Object object) {}

    // This procedure is triggered when user tries to enable PIN access and PIN has been set by the user

    @Override
    public void onPinRequestDone(@NotNull final String value) {
        if (isAdded()) {
            final Context context = getContext();
            SharedPreferences.Editor editor = Preferences.getDefaultSharedPreferences(context).edit();
            if (Preferences.putEncryptedString(context, editor, Constants.PIN_CODE_KEY, value)) {
                editor.putBoolean(PIN_ACCESS_ENABLED_KEY, true);
                editor.apply();
                UI.showToast(context, R.string.pin_has_been_set);
                onSettingValueChanged(Constants.PIN_CODE_KEY);
                setSecurityPreferencesSummariesAndAvailability();
            }
        }
    }

    @Override
    public void onPinRequestCancelled() {}

    // This procedure is triggered when user tries to enable fingerprint access and fingerprint has been recognized

    @Override
    public void onBiometricAuthenticationSucceeded() {
        if (isAdded()) {
            final Context context = getContext();
            if (AuthenticateWithBiometrics.linkBiometrics()) {
                Preferences.getDefaultSharedPreferences(context).edit().putBoolean(Constants.USE_FINGERPRINT_INSTEAD_OF_PIN_CODE_KEY, true).apply();
                onSettingValueChanged(Constants.USE_FINGERPRINT_INSTEAD_OF_PIN_CODE_KEY);
                setSecurityPreferencesSummariesAndAvailability();
            }
        }
    }

    @Override
    public void onBiometricAuthenticationError(final int error_code) {}

    private static @NotNull String getUserSelectedThemeLabel(@NotNull final Context context, @Nullable final Theme theme) {
        return context.getString((theme == null) ? R.string.follow_system_theme : (theme == Theme.DARK) ? R.string.dark_theme : R.string.light_theme);
    }

    public static @Nullable Theme getCurrentUserSelectedThemeValue(@NotNull final Context context) {
        final String value = Preferences.getDefaultSharedPreferences(context).getString(Constants.APP_THEME_KEY, null);
        return value == null ? null : Theme.valueOf(value);
    }
}
