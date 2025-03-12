package com.twofauth.android.preferences_activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.twofauth.android.Constants;
import com.twofauth.android.HtmlActivity;
import com.twofauth.android.MainService;
import com.twofauth.android.PreferencesActivity;
import com.twofauth.android.R;
import com.twofauth.android.StringUtils;
import com.twofauth.android.UiUtils;
import com.twofauth.android.main_activity.AuthenticWithBiometrics;
import com.twofauth.android.main_activity.AuthenticWithPin;
import com.twofauth.android.main_activity.MainServiceStatusChangedBroadcastReceiver;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class MainPreferencesFragment extends PreferenceFragmentCompat implements MainServiceStatusChangedBroadcastReceiver.OnMainServiceStatusChanged, Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener, AuthenticWithPin.OnPinAuthenticationFinished, AuthenticWithPin.OnPinRequestFinished, AuthenticWithBiometrics.OnBiometricAuthenticationFinished {
    public static final String EXTRA_CHANGED_SETTINGS = "changes";
    private static final String SYNC_DETAILS_KEY = "sync-details";
    private static final String PIN_ACCESS_ENABLED_KEY = "pin-access-enabled";
    private static final String GITHUB_REPO_KEY = "github-repo";
    private static final String OPEN_SOURCE_LICENSES_KEY = "open-source-licenses";
    private final Intent mIntent = new Intent();

    private final ArrayList<String> mChanges = new ArrayList<String>();

    private final MainServiceStatusChangedBroadcastReceiver mReceiver = new MainServiceStatusChangedBroadcastReceiver(this);

    @Override
    public void onCreatePreferences(@Nullable final Bundle saved_instance_state, @Nullable final String root_key) {
        setPreferencesFromResource(R.xml.preferences, root_key);
    }

    @Override
    public void onPause() {
        super.onPause();
        mReceiver.disable(getContext());
    }

    @Override
    public void onResume() {
        super.onResume();
        mReceiver.enable(getContext());
        setSyncDetailsPreferenceState();
    }

    @Override
    public void onServiceStarted() {
        setSyncDetailsPreferenceState();
    }

    @Override
    public void onServiceFinished() {
        setSyncDetailsPreferenceState();
    }

    @Override
    public void onDataSyncedFromServer() {}

    private void setSyncDetailsPreferenceState() {
        if (isAdded()) {
            final Context context = requireContext();
            final SharedPreferences preferences = Constants.getDefaultSharedPreferences(context);
	        final boolean is_service_running = MainService.isRunning(context);
  	        String last_sync_details = getString(R.string.sync_is_in_progress);
	        if (! is_service_running) {
                last_sync_details = getString(R.string.click_to_sync_data);
                if (preferences.contains(Constants.TWO_FACTOR_AUTH_CODES_LAST_SYNC_ERROR_TIME_KEY)) {
                    last_sync_details = getString(R.string.last_sync_error, preferences.getString(Constants.TWO_FACTOR_AUTH_CODES_LAST_SYNC_ERROR_KEY, null), StringUtils.getDateTimeString(context, preferences.getLong(Constants.TWO_FACTOR_AUTH_CODES_LAST_SYNC_ERROR_TIME_KEY, 0)), last_sync_details);
                }
                else if (preferences.contains(Constants.TWO_FACTOR_AUTH_CODES_LAST_SYNC_TIME_KEY)) {
                    final int number_of_accounts = preferences.getInt(Constants.TWO_FACTOR_AUTH_ACCOUNTS_DATA_SIZE_KEY, 0);
                    last_sync_details = getResources().getQuantityString(R.plurals.sync_details, number_of_accounts, number_of_accounts, StringUtils.getDateTimeString(context, preferences.getLong(Constants.TWO_FACTOR_AUTH_CODES_LAST_SYNC_TIME_KEY, 0)), last_sync_details);
                }
            }
            Preference sync_details_preference = findPreference(SYNC_DETAILS_KEY);
            sync_details_preference.setEnabled(MainService.canSyncServerData(context) && (! is_service_running));
            sync_details_preference.setSummary(last_sync_details);
        }
    }

    private void setDependenciesAvailability() {
        if (isAdded()) {
            final Context context = requireContext();
            final SharedPreferences preferences = Constants.getDefaultSharedPreferences(context);
            findPreference(Constants.TWO_FACTOR_AUTH_TOKEN_KEY).setEnabled(preferences.contains(Constants.TWO_FACTOR_AUTH_SERVER_LOCATION_KEY));
            setSyncDetailsPreferenceState();
            ((CheckBoxPreference) findPreference(PIN_ACCESS_ENABLED_KEY)).setChecked(preferences.getBoolean(PIN_ACCESS_ENABLED_KEY, false));
            final CheckBoxPreference fingerprint_access_preference = (CheckBoxPreference) findPreference(Constants.FINGERPRINT_ACCESS_KEY);
            if (fingerprint_access_preference != null) {
                fingerprint_access_preference.setEnabled(preferences.contains(Constants.PIN_ACCESS_KEY));
                fingerprint_access_preference.setChecked(preferences.getBoolean(Constants.FINGERPRINT_ACCESS_KEY, false));
            }
        }
    }

    private void initializePreferences(@NotNull final Context context) {
        final SharedPreferences preferences = Constants.getDefaultSharedPreferences(context);
        final EditTextPreference server_location = (EditTextPreference) findPreference(Constants.TWO_FACTOR_AUTH_SERVER_LOCATION_KEY), server_token = (EditTextPreference) findPreference(Constants.TWO_FACTOR_AUTH_TOKEN_KEY);
        server_location.setSummary(preferences.getString(Constants.TWO_FACTOR_AUTH_SERVER_LOCATION_KEY, getString(R.string.server_location_is_not_set)));
        server_location.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
            @Override
            public void onBindEditText(@NonNull EditText edit_text) {
                edit_text.setText(preferences.getString(Constants.TWO_FACTOR_AUTH_SERVER_LOCATION_KEY, null));
                edit_text.setSelection(edit_text.getText().length());
            }
        });
        server_location.setOnPreferenceChangeListener(this);

        server_token.setSummary(preferences.contains(Constants.TWO_FACTOR_AUTH_TOKEN_KEY) ? R.string.token_value_is_set_summary : R.string.token_value_is_not_set_summary);
        server_token.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
            public void onBindEditText(@NonNull final EditText edit_text) {
                edit_text.setHint(Constants.getDefaultSharedPreferences(edit_text.getContext()).contains(Constants.TWO_FACTOR_AUTH_TOKEN_KEY) ? getString(R.string.token_unchanged) : "");
                edit_text.setText(null);
            }
        });
        server_token.setOnPreferenceChangeListener(this);
        findPreference(SYNC_DETAILS_KEY).setOnPreferenceClickListener(this);
        findPreference(Constants.SORT_ACCOUNTS_BY_LAST_USE_KEY).setOnPreferenceChangeListener(this);
        findPreference(Constants.UNGROUP_OTP_CODE_KEY).setOnPreferenceChangeListener(this);
        findPreference(Constants.DISPLAY_ACCOUNT_GROUP_KEY).setOnPreferenceChangeListener(this);
        findPreference(Constants.SHOW_COPY_TO_CLIPBOARD_BUTTON_KEY).setOnPreferenceChangeListener(this);
        findPreference(Constants.MINIMIZE_APP_AFTER_COPY_TO_CLIPBOARD_KEY).setOnPreferenceChangeListener(this);
        findPreference(Constants.DISABLE_SCREENSHOTS_KEY).setOnPreferenceChangeListener(this);
        findPreference(Constants.HIDE_OTP_AUTOMATICALLY_KEY).setOnPreferenceChangeListener(this);
        findPreference(PIN_ACCESS_ENABLED_KEY).setOnPreferenceChangeListener(this);
        ((CheckBoxPreference) findPreference(PIN_ACCESS_ENABLED_KEY)).setChecked(preferences.contains(Constants.PIN_ACCESS_KEY));
        findPreference(Constants.FINGERPRINT_ACCESS_KEY).setOnPreferenceChangeListener(this);
        if (! AuthenticWithBiometrics.canUseBiometrics(context)) {
            findPreference(Constants.FINGERPRINT_ACCESS_KEY).getParent().removePreference(findPreference(Constants.FINGERPRINT_ACCESS_KEY));
        }
        findPreference(GITHUB_REPO_KEY).setOnPreferenceClickListener(this);
        findPreference(OPEN_SOURCE_LICENSES_KEY).setOnPreferenceClickListener(this);
        setDependenciesAvailability();
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle saved_instance_state) {
        super.onViewCreated(view, saved_instance_state);
        initializePreferences(view.getContext());
        setDivider(null);
    }

    private void onSettingValueChanged(@NotNull final String[] keys) {
        synchronized (mIntent) {
            boolean changed = false;
            for (String key : keys) {
                if (! mChanges.contains(key)) {
                    mChanges.add(key);
                    changed = true;
                }
            }
            if (changed) {
                mIntent.putStringArrayListExtra(EXTRA_CHANGED_SETTINGS, mChanges);
                getActivity().setResult(PreferencesActivity.RESULT_OK, mIntent);
            }
        }
    }
    private void onSettingValueChanged(@NotNull final String key) {
        onSettingValueChanged(new String[] { key });
    }
    @Override
    public boolean onPreferenceClick(@NonNull final Preference preference) {
        if (SYNC_DETAILS_KEY.equals(preference.getKey())) {
            MainService.startService(getContext());
        }
        else if (GITHUB_REPO_KEY.equals(preference.getKey())) {
            HtmlActivity.openInWebBrowser(getActivity(), Constants.GITHUB_REPO);
        }
        else if (OPEN_SOURCE_LICENSES_KEY.equals(preference.getKey())) {
            startActivity(new Intent(getContext(), HtmlActivity.class).putExtra(HtmlActivity.EXTRA_FILE_PATHNAME, "file:///android_asset/open-source-licenses.html"));
            return true;
        }
        return false;
    }

    private void removeDownloadedData(@NotNull final SharedPreferences.Editor editor, @NotNull final String primary_key) {
        if (! Constants.TWO_FACTOR_AUTH_TOKEN_KEY.equals(primary_key)) {
            editor.remove(Constants.TWO_FACTOR_AUTH_TOKEN_KEY);
        }
        editor.remove(Constants.TWO_FACTOR_AUTH_ACCOUNTS_DATA_KEY);
        editor.remove(Constants.TWO_FACTOR_AUTH_ACCOUNTS_DATA_SIZE_KEY);
        editor.remove(Constants.TWO_FACTOR_AUTH_CODES_LAST_SYNC_TIME_KEY);
        editor.remove(Constants.TWO_FACTOR_AUTH_CODES_LAST_SYNC_ERROR_KEY);
        editor.remove(Constants.TWO_FACTOR_AUTH_CODES_LAST_SYNC_ERROR_TIME_KEY);
    }

    private void onServerLocationChanged(@NotNull final Context context, @NotNull final String location) {
        final SharedPreferences preferences = Constants.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor editor = preferences.edit();
        int message_id;
        if (location.isEmpty()) {
            editor.remove(Constants.TWO_FACTOR_AUTH_SERVER_LOCATION_KEY);
            message_id = preferences.contains(Constants.TWO_FACTOR_AUTH_CODES_LAST_SYNC_TIME_KEY) ? R.string.synced_accounts_data_and_server_token_removed_due_to_server_location_removed : preferences.contains(Constants.TWO_FACTOR_AUTH_TOKEN_KEY) ? R.string.server_token_removed_due_to_server_location_removed : 0;
        }
        else {
            editor.putString(Constants.TWO_FACTOR_AUTH_SERVER_LOCATION_KEY, location);
            message_id = preferences.contains(Constants.TWO_FACTOR_AUTH_CODES_LAST_SYNC_TIME_KEY) ? R.string.synced_accounts_data_and_server_token_removed_due_to_server_location_changed : preferences.contains(Constants.TWO_FACTOR_AUTH_TOKEN_KEY) ? R.string.server_token_removed_due_to_server_location_changed : 0;
        }
        removeDownloadedData(editor, Constants.TWO_FACTOR_AUTH_SERVER_LOCATION_KEY);
        editor.apply();
        onSettingValueChanged(new String[] { Constants.TWO_FACTOR_AUTH_SERVER_LOCATION_KEY, Constants.TWO_FACTOR_AUTH_TOKEN_KEY });
        if (isAdded()) {
            findPreference(Constants.TWO_FACTOR_AUTH_SERVER_LOCATION_KEY).setSummary(location.isEmpty() ? getString(R.string.server_location_is_not_set) : location);
            findPreference(Constants.TWO_FACTOR_AUTH_TOKEN_KEY).setSummary(R.string.token_value_is_not_set_summary);
            setDependenciesAvailability();
            UiUtils.showMessageDialog(getActivity(), message_id);
        }
    }

    @Override
    public boolean onPreferenceChange(@NonNull final Preference preference, final Object new_value) {
        final Context context = preference.getContext();
        if (Constants.TWO_FACTOR_AUTH_SERVER_LOCATION_KEY.equals(preference.getKey())) {
            final String trimmed_new_value = new_value.toString().trim();
            if (! StringUtils.equals(trimmed_new_value, Constants.getDefaultSharedPreferences(context).getString(Constants.TWO_FACTOR_AUTH_SERVER_LOCATION_KEY, ""), true)) {
                if (trimmed_new_value.toLowerCase().startsWith("http:")) {
                    UiUtils.showConfirmDialog(getActivity(), R.string.server_http_is_insecure_warning, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            onServerLocationChanged(context, trimmed_new_value);
                        }
                    });
                    return false;
                }
                onServerLocationChanged(context, trimmed_new_value);
                return true;
            }
        }
        else if (Constants.TWO_FACTOR_AUTH_TOKEN_KEY.equals(preference.getKey())) {
            final String trimmed_new_value = new_value.toString().trim();
            if (! trimmed_new_value.isEmpty()) {
                final SharedPreferences.Editor editor = Constants.getDefaultSharedPreferences(context).edit();
                editor.putString(Constants.TWO_FACTOR_AUTH_TOKEN_KEY, trimmed_new_value);
                removeDownloadedData(editor, Constants.TWO_FACTOR_AUTH_TOKEN_KEY);
                editor.apply();
                findPreference(Constants.TWO_FACTOR_AUTH_TOKEN_KEY).setSummary(R.string.token_value_is_set_summary);
                onSettingValueChanged(Constants.TWO_FACTOR_AUTH_TOKEN_KEY);
                setDependenciesAvailability();
                return true;
            }
        }
        else if (Constants.SORT_ACCOUNTS_BY_LAST_USE_KEY.equals(preference.getKey())) {
            onSettingValueChanged(preference.getKey());
            return true;
        }
        else if (Constants.UNGROUP_OTP_CODE_KEY.equals(preference.getKey())) {
            onSettingValueChanged(preference.getKey());
            return true;
        }
        else if (Constants.DISPLAY_ACCOUNT_GROUP_KEY.equals(preference.getKey())) {
            onSettingValueChanged(preference.getKey());
            return true;
        }
        else if (Constants.SHOW_COPY_TO_CLIPBOARD_BUTTON_KEY.equals(preference.getKey())) {
            onSettingValueChanged(preference.getKey());
            return true;
        }
        else if (Constants.MINIMIZE_APP_AFTER_COPY_TO_CLIPBOARD_KEY.equals(preference.getKey())) {
            onSettingValueChanged(preference.getKey());
            return true;
        }
        else if (Constants.DISABLE_SCREENSHOTS_KEY.equals(preference.getKey())) {
            UiUtils.showMessageDialog(getActivity(), R.string.change_will_be_applied_next_time_you_start_the_app);
            onSettingValueChanged(preference.getKey());
            return true;
        }
        else if (Constants.HIDE_OTP_AUTOMATICALLY_KEY.equals(preference.getKey())) {
            onSettingValueChanged(preference.getKey());
            return true;
        }
        else if (PIN_ACCESS_ENABLED_KEY.equals(preference.getKey())) {
            if ((boolean) new_value) {
                AuthenticWithPin.request(getActivity(), this);
            }
            else {
                AuthenticWithPin.authenticate(getActivity(), this, Constants.getDefaultSharedPreferences(context).getString(Constants.PIN_ACCESS_KEY, null));
            }
        }
        else if (Constants.FINGERPRINT_ACCESS_KEY.equals(preference.getKey())) {
            if (! (boolean) new_value) {
                onSettingValueChanged(preference.getKey());
                return true;
            }
            AuthenticWithBiometrics.authenticate(getActivity(), this);
        }
        return false;
    }

    @Override
    public void onPinAuthenticationSucceeded() {
        final Context context = getContext();
        if (context != null) {
            SharedPreferences.Editor editor = Constants.getDefaultSharedPreferences(getContext()).edit();
            editor.remove(Constants.PIN_ACCESS_KEY);
            editor.putBoolean(Constants.FINGERPRINT_ACCESS_KEY, false);
            editor.putBoolean(PIN_ACCESS_ENABLED_KEY, false);
            editor.putBoolean(Constants.FINGERPRINT_ACCESS_KEY, false);
            editor.apply();
            UiUtils.showToast(context, R.string.pin_has_been_removed);
            setDependenciesAvailability();
            onSettingValueChanged(Constants.PIN_ACCESS_KEY);
        }
    }

    @Override
    public void onPinAuthenticationError(final boolean cancelled) {
        if (! cancelled) {
            UiUtils.showToast(getContext(), R.string.pin_is_not_valid);
        }
    }

    @Override
    public void onPinRequestDone(final String value) {
        final Context context = getContext();
        if (context != null) {
            SharedPreferences.Editor editor = Constants.getDefaultSharedPreferences(context).edit();
            editor.putString(Constants.PIN_ACCESS_KEY, value);
            editor.putBoolean(PIN_ACCESS_ENABLED_KEY, true);
            editor.apply();
            UiUtils.showToast(context, R.string.pin_has_been_set);
            onSettingValueChanged(Constants.PIN_ACCESS_KEY);
            setDependenciesAvailability();
        }
    }

    @Override
    public void onPinRequestCancelled() {}

    @Override
    public void onBiometricAuthenticationSucceeded() {
        final Context context = getContext();
        if (context != null) {
            SharedPreferences.Editor editor = Constants.getDefaultSharedPreferences(context).edit();
            editor.putBoolean(Constants.FINGERPRINT_ACCESS_KEY, true);
            editor.apply();
            onSettingValueChanged(Constants.FINGERPRINT_ACCESS_KEY);
            setDependenciesAvailability();
        }
    }

    @Override
    public void onBiometricAuthenticationError(final int error_code) {}
}
