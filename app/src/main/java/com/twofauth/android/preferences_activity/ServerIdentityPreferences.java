package com.twofauth.android.preferences_activity;

import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.twofauth.android.Authenticator;
import com.twofauth.android.Authenticator.OnAuthenticatorFinishListener;
import com.twofauth.android.Constants;
import com.twofauth.android.HtmlActivity;
import com.twofauth.android.MainService;
import com.twofauth.android.MainService.OnMainServiceStatusChangedListener;
import com.twofauth.android.PreferencesActivity;
import com.twofauth.android.R;
import com.twofauth.android.database.TwoFactorServerIdentity;
import com.twofauth.android.api_tasks.DeleteServerIdentityData;
import com.twofauth.android.api_tasks.DeleteServerIdentityData.OnServerIdentityDeletedListener;
import com.twofauth.android.preferences_activity.tasks.LoadServerIdentityAndGroupsDataFromServer;
import com.twofauth.android.preferences_activity.tasks.LoadServerIdentityAndGroupsDataFromServer.OnServerIdentityAndGroupsDataLoadedFromServerListener;
import com.twofauth.android.preferences_activity.tasks.LoadServerIdentitiesData.TwoFactorServerIdentityWithSyncDataAndAccountNumbers;
import com.twofauth.android.api_tasks.SaveServerIdentityData;
import com.twofauth.android.api_tasks.SaveServerIdentityData.OnServerIdentitySavedListener;
import com.twofauth.android.utils.Clipboard;
import com.twofauth.android.utils.Preferences;
import com.twofauth.android.utils.Strings;
import com.twofauth.android.utils.UI;
import com.twofauth.android.utils.Vibrator;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ServerIdentityPreferences extends PreferenceFragmentCompat implements OnServerIdentityDeletedListener, OnServerIdentitySavedListener, OnServerIdentityAndGroupsDataLoadedFromServerListener, OnMainServiceStatusChangedListener, OnAuthenticatorFinishListener, OnPreferenceClickListener, OnPreferenceChangeListener, OnClickListener {
    public static final String EDIT_IDENTITY = "edit-server-identity";
    public static final String EDIT_IDENTITY_RESULT = "result";
    public static enum EditIdentityResultType { RELEVANT_UPDATE, NOT_RELEVANT_UPDATE, DELETED };

    private static final String LABEL_KEY = "label";
    private static final String SERVER_LOCATION_KEY = "server";
    private static final String TOKEN_KEY = "token";
    private static final String SYNC_ON_STARTUP_KEY = "sync-on-startup";
    private static final String SYNC_IMMEDIATELY_KEY = "sync-immediately";
    private static final String SERVER_LOADED_DATA_KEY = "server-loaded-data";
    private static final String NAME_KEY = "name";
    private static final String EMAIL_KEY = "email";
    private static final String IS_ADMIN_KEY = "is-admin";

    private static enum AuthenticatedActions { DELETE_SERVER_IDENTITY, ENABLE_SERVER_IDENTITY_EDITION, MANAGE_GROUPS, COPY_TOKEN_TO_CLIPBOARD };

    private boolean mAnimatingSyncDataButton = false;

    private final List<TwoFactorServerIdentityWithSyncDataAndAccountNumbers> mServerIdentities;

    private final TwoFactorServerIdentityWithSyncDataAndAccountNumbers mCurrentServerIdentity;
    private final TwoFactorServerIdentity mInitialServerIdentity;

    private View mWorkingView;
    private FloatingActionButton mEditOrSaveIdentityDataButton;
    private FloatingActionButton mToggleSubmenuVisibilityButton;
    private FloatingActionButton mDeleteIdentityDataButton;
    private FloatingActionButton mManageGroupsButton;
    private FloatingActionButton mSyncServerDataButton;
    private FloatingActionButton mCopyTokenToClipboardButton;
    private FloatingActionButton mOpenServerLocationButton;

    private boolean mEditing;
    private boolean mRunningTasks = false;

    private Authenticator mAuthenticator;

    private final OnBackPressedCallback mOnBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            onBackPressed();
        }
    };

    public ServerIdentityPreferences(@NotNull final TwoFactorServerIdentityWithSyncDataAndAccountNumbers server_identity, @NotNull final List<TwoFactorServerIdentityWithSyncDataAndAccountNumbers> server_identities) {
        super();
        mCurrentServerIdentity = server_identity;
        mInitialServerIdentity = new TwoFactorServerIdentity(mCurrentServerIdentity.storedData);
        mServerIdentities = server_identities;
        mEditing = ! mCurrentServerIdentity.storedData.inDatabase();
    }

    @Override
    public void onCreate(@Nullable final Bundle saved_instance_state) {
        super.onCreate(saved_instance_state);
        getActivity().getOnBackPressedDispatcher().addCallback(mOnBackPressedCallback);
    }

    @Override
    public void onPause() {
        MainService.removeListener(this);
        mOnBackPressedCallback.setEnabled(false);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        MainService.addListener(this);
        mOnBackPressedCallback.setEnabled(true);
        setButtonsAvailability();
    }

    private void finish() {
        getActivity().getSupportFragmentManager().popBackStack();
    }

    // Propagate results to the caller

    private void setFragmentResult(final @Nullable EditIdentityResultType result) {
        final Bundle bundle = new Bundle();
        bundle.putSerializable(EDIT_IDENTITY_RESULT, result);
        getParentFragmentManager().setFragmentResult(EDIT_IDENTITY, bundle);
    }

    private void setFragmentResultThenFinish(final @Nullable EditIdentityResultType result) {
        setFragmentResult(result);
        finish();
    }

    private void revertChangesThenGoBack() {
        mCurrentServerIdentity.storedData.setFrom(mInitialServerIdentity);
        setFragmentResultThenFinish(null);
    }

    // Entry point for the back pressed key event
    // If we are running a background task we do not return to the caller until background task ends
    // In other case, if server identity is changed we show a alert message before return to the caller

    public void onBackPressed() {
        if (! mRunningTasks) {
            if (isChanged()) {
                UI.showConfirmDialog(getActivity(), mCurrentServerIdentity.storedData.inDatabase() ? R.string.cancel_identity_edition_message : R.string.cancel_identity_creation_message, R.string.continue_editing, R.string.exit, null, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        revertChangesThenGoBack();
                    }
                });
            }
            else {
                finish();
            }
        }
    }

    @Override
    public @NotNull View onCreateView(@NotNull LayoutInflater inflater, @NotNull ViewGroup container, @Nullable Bundle saved_instance_state) {
        final View view = inflater.inflate(R.layout.server_identity_preferences_fragment, container, false);
        final FrameLayout preferences_container = view.findViewById(R.id.contents);
        preferences_container.addView(super.onCreateView(inflater, preferences_container, saved_instance_state));
        return view;
    }

    @Override
    public void onViewCreated(@NotNull final View view, @Nullable final Bundle saved_instance_state) {
        final Context context = view.getContext();
        super.onViewCreated(view, saved_instance_state);
        mWorkingView = view.findViewById(R.id.working);
        mEditOrSaveIdentityDataButton = (FloatingActionButton) view.findViewById(R.id.edit_or_save_server_identity_data);
        mEditOrSaveIdentityDataButton.setOnClickListener(this);
        mEditOrSaveIdentityDataButton.setImageResource(mEditing ? R.drawable.ic_actionbar_accept : R.drawable.ic_actionbar_edit);
        mToggleSubmenuVisibilityButton = (FloatingActionButton) view.findViewById(R.id.toggle_submenu);
        mToggleSubmenuVisibilityButton.setOnClickListener(this);
        mToggleSubmenuVisibilityButton.setVisibility(mEditing ? View.GONE : View.VISIBLE);
        mDeleteIdentityDataButton = (FloatingActionButton) view.findViewById(R.id.delete_server_identity_data);
        mDeleteIdentityDataButton.setVisibility(View.GONE);
        mDeleteIdentityDataButton.setOnClickListener(this);
        mManageGroupsButton = (FloatingActionButton) view.findViewById(R.id.manage_groups);
        mManageGroupsButton.setVisibility(View.GONE);
        mManageGroupsButton.setOnClickListener(this);
        mSyncServerDataButton = (FloatingActionButton) view.findViewById(R.id.sync_server_data);
        mSyncServerDataButton.setVisibility(View.GONE);
        mSyncServerDataButton.setOnClickListener(this);
        mCopyTokenToClipboardButton = (FloatingActionButton) view.findViewById(R.id.copy_token_to_clipboard);
        mCopyTokenToClipboardButton.setVisibility(View.GONE);
        mCopyTokenToClipboardButton.setOnClickListener(this);
        mOpenServerLocationButton = (FloatingActionButton) view.findViewById(R.id.open_server_location);
        mOpenServerLocationButton.setVisibility(View.GONE);
        mOpenServerLocationButton.setOnClickListener(this);
        setButtonsAvailability();
        setDivider(null);
    }

    @Override
    public void onCreatePreferences(@Nullable final Bundle saved_instance_state, @Nullable final String root_key) {
        setPreferencesFromResource(R.xml.server_identity_preferences, root_key);
        initializePreferences();
        mAuthenticator = new Authenticator(getActivity());
    }

    private void initializePreferences() {
        final EditTextPreference label_preference = (EditTextPreference) findPreference(LABEL_KEY), server_location_preference = (EditTextPreference) findPreference(SERVER_LOCATION_KEY), token_preference = (EditTextPreference) findPreference(TOKEN_KEY);
        final CheckBoxPreference sync_on_startup_preference = (CheckBoxPreference) findPreference(SYNC_ON_STARTUP_KEY), sync_immediately_preference = (CheckBoxPreference) findPreference(SYNC_IMMEDIATELY_KEY);
        label_preference.setSummary(mCurrentServerIdentity.storedData.hasLabel() ? mCurrentServerIdentity.storedData.getLabel() : getString(R.string.label_not_set));
        label_preference.setOnPreferenceClickListener(this);
        label_preference.setOnPreferenceChangeListener(this);
        token_preference.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
            public void onBindEditText(@NotNull final EditText edit_text) {
                edit_text.setText(mCurrentServerIdentity.storedData.getLabel());
                edit_text.setSelection(edit_text.getText().length());
            }
        });
        server_location_preference.setSummary(mCurrentServerIdentity.storedData.hasServer() ? mCurrentServerIdentity.storedData.getServer() : getString(R.string.server_location_is_not_set));
        server_location_preference.setOnPreferenceClickListener(this);
        server_location_preference.setOnPreferenceChangeListener(this);
        server_location_preference.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
            @Override
            public void onBindEditText(@NotNull EditText edit_text) {
                edit_text.setText(mCurrentServerIdentity.storedData.getServer());
                edit_text.setSelection(edit_text.getText().length());
            }
        });
        token_preference.setSummary(mCurrentServerIdentity.storedData.hasToken() ? R.string.token_value_is_set_summary : R.string.token_value_is_not_set_summary);
        token_preference.setOnPreferenceClickListener(this);
        token_preference.setOnPreferenceChangeListener(this);
        token_preference.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
            public void onBindEditText(@NotNull final EditText edit_text) {
                edit_text.setHint(mCurrentServerIdentity.storedData.hasToken() ? getString(R.string.token_unchanged) : "");
                edit_text.setText(null);
            }
        });
        token_preference.setEnabled(mCurrentServerIdentity.storedData.hasServer());
        sync_on_startup_preference.setChecked(mCurrentServerIdentity.storedData.isSyncingOnStartup());
        sync_on_startup_preference.setOnPreferenceChangeListener(this);
        sync_immediately_preference.setChecked(mCurrentServerIdentity.storedData.isSyncingImmediately());
        sync_immediately_preference.setOnPreferenceChangeListener(this);
        findPreference(NAME_KEY).setSummary(mCurrentServerIdentity.storedData.getName());
        findPreference(EMAIL_KEY).setSummary(mCurrentServerIdentity.storedData.getEmail());
        findPreference(IS_ADMIN_KEY).setSummary(mCurrentServerIdentity.storedData.isAdmin() ? R.string.yes : R.string.no);
        findPreference(SERVER_LOADED_DATA_KEY).setVisible(mCurrentServerIdentity.storedData.hasName());
    }

    // Entry points for the synchronization process events
    // We set the sync button state according to the synchronization process state

    @Override
    public void onServiceStarted() {
        setButtonsAvailability();
    }

    @Override
    public void onServiceFinished(MainService.SyncResultType result_type) { setButtonsAvailability(); }

    // Procedures that checks if a identity is changed

    private boolean isServerOrTokenChanged() {
        final TwoFactorServerIdentity current_server_identity = mCurrentServerIdentity.storedData;
        if (! Strings.equals(current_server_identity.getServer(), mInitialServerIdentity.getServer())) { return true; }
        if (! Strings.equals(current_server_identity.getToken(), mInitialServerIdentity.getToken())) { return true; }
        return false;
    }

    private boolean isChanged() {
        final TwoFactorServerIdentity current_server_identity = mCurrentServerIdentity.storedData;
        if (! Strings.similar(current_server_identity.getLabel(), mInitialServerIdentity.getLabel())) { return true; }
        if (isServerOrTokenChanged()) { return true; }
        if (current_server_identity.isSyncingOnStartup() != mInitialServerIdentity.isSyncingOnStartup()) { return true; }
        if (current_server_identity.isSyncingImmediately() != mInitialServerIdentity.isSyncingImmediately()) { return true; }
        return false;
    }

    private boolean isIdentityInUse() {
        final String server = mCurrentServerIdentity.storedData.getServer(), token = mCurrentServerIdentity.storedData.getToken();
        for (final TwoFactorServerIdentityWithSyncDataAndAccountNumbers server_identity : mServerIdentities) {
            if (Strings.equals(server, server_identity.storedData.getServer()) && Strings.equals(token, server_identity.storedData.getToken()) && (server_identity.storedData.getRowId() != mCurrentServerIdentity.storedData.getRowId())) { return true; }
        }
        return false;
    }

    // Sets buttons state (note that this enables or disables buttons but some buttons aren't visible if editing the identity)

    private void setButtonsAvailability() {
        final Context context = getContext();
        final boolean is_syncing_data = MainService.isRunning(context), can_save_data = mCurrentServerIdentity.storedData.hasServer() && mCurrentServerIdentity.storedData.hasToken(), can_sync_data = can_save_data && (! isServerOrTokenChanged()), is_syncing_this_identity = MainService.isSyncingIdentity(context, mCurrentServerIdentity.storedData);
        mWorkingView.setVisibility(mRunningTasks ? View.VISIBLE : View.GONE);
        mEditOrSaveIdentityDataButton.setEnabled((! mRunningTasks) && (! is_syncing_this_identity) && ((! mEditing) || (can_save_data && isChanged() && (! isIdentityInUse()))));
        mDeleteIdentityDataButton.setEnabled((! mRunningTasks) && (! is_syncing_this_identity) && mCurrentServerIdentity.storedData.inDatabase() && (! isChanged()));
        mManageGroupsButton.setEnabled((! mRunningTasks) && mCurrentServerIdentity.storedData.inDatabase());
        mSyncServerDataButton.setEnabled(can_sync_data && (! is_syncing_data) && mDeleteIdentityDataButton.isEnabled());
        if ((is_syncing_this_identity) && (! mAnimatingSyncDataButton)) { UI.startInfiniteRotationAnimationLoop(mSyncServerDataButton, Constants.BUTTON_360_DEGREES_ROTATION_ANIMATION_DURATION); }
        else if ((! is_syncing_this_identity) && (mAnimatingSyncDataButton)) { mSyncServerDataButton.clearAnimation(); }
        mAnimatingSyncDataButton = is_syncing_this_identity;
        mCopyTokenToClipboardButton.setEnabled(mCurrentServerIdentity.storedData.hasToken());
        mOpenServerLocationButton.setEnabled(mCurrentServerIdentity.storedData.hasServer());
    }

    // Entry points for the server identity parts changed

    private void onLabelChanged() {
        findPreference(LABEL_KEY).setSummary(mCurrentServerIdentity.storedData.hasLabel() ? mCurrentServerIdentity.storedData.getLabel() : getString(R.string.label_not_set));
        setButtonsAvailability();
    }

    private void onServerChanged() {
        final Preference server_location_preference = findPreference(SERVER_LOCATION_KEY), token_preference = findPreference(TOKEN_KEY);
        mCurrentServerIdentity.storedData.setToken(null);
        server_location_preference.setSummary(mCurrentServerIdentity.storedData.getServer());
        token_preference.setSummary(R.string.token_value_is_not_set_summary);
        token_preference.setEnabled(mCurrentServerIdentity.storedData.hasServer());
        setButtonsAvailability();
    }

    private void onTokenChanged() {
        findPreference(TOKEN_KEY).setSummary(mCurrentServerIdentity.storedData.hasToken() ? R.string.token_value_is_set_summary : R.string.token_value_is_not_set_summary);
        if (mCurrentServerIdentity.storedData.hasToken()) { refreshServerIdentity(); }
        setButtonsAvailability();
    }

    private void onOtherValueChanged() {
        setButtonsAvailability();
    }

    // Functions related with the save server identity data
    // If server/token have been changed, we notify about the accounts deletion before save

    private void saveServerIdentityDataConfirmed() {
        mRunningTasks = true;
        setButtonsAvailability();
        SaveServerIdentityData.getBackgroundTask(getContext(), mCurrentServerIdentity, this).start();
    }

    private void saveServerIdentityData() {
        if (isServerOrTokenChanged() && (mCurrentServerIdentity.hasNotSyncedAccounts() || mCurrentServerIdentity.hasAccounts())) {
            UI.showConfirmDialog(getActivity(), mCurrentServerIdentity.hasNotSyncedAccounts() ? R.string.accounts_and_not_synced_accounts_will_be_removed : R.string.accounts_will_be_removed, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    saveServerIdentityDataConfirmed();
                }
            });
        }
        else {
            saveServerIdentityDataConfirmed();
        }
    }

    @Override
    public void onServerIdentitySaved(boolean success) {
        if (isAdded()) {
            mRunningTasks = false;
            UI.showToast(getContext(), success ? R.string.server_identity_data_saved : R.string.cannot_process_request_due_to_an_internal_error);
            if (success) {
                setFragmentResult(isServerOrTokenChanged() ? EditIdentityResultType.RELEVANT_UPDATE : EditIdentityResultType.NOT_RELEVANT_UPDATE);
                mInitialServerIdentity.setFrom(mCurrentServerIdentity.storedData);
                disableEdition();
                setButtonsAvailability();
            }
        }
    }

    // Functions related to the delete server identity data
    // We request authentication before do it

    private void deleteServerIdentityDataConfirmed() {
        mRunningTasks = true;
        setButtonsAvailability();
        DeleteServerIdentityData.getBackgroundTask(getContext(), mCurrentServerIdentity, this).start();
    }

    private void deleteServerIdentityData() {
        final FragmentActivity activity = getActivity();
        UI.showConfirmDialog(activity, mCurrentServerIdentity.hasNotSyncedAccounts() ? R.string.server_identity_and_accounts_and_not_synced_accounts_will_be_removed : mCurrentServerIdentity.hasAccounts() ? R.string.server_identity_and_accounts_will_be_removed : R.string.server_identity_will_be_removed, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mAuthenticator.authenticate(ServerIdentityPreferences.this, AuthenticatedActions.DELETE_SERVER_IDENTITY);
            }
        });
    }

    @Override
    public void onServerIdentityDeleted(boolean success) {
        if (isAdded()) {
            mRunningTasks = false;
            UI.showToast(getContext(), success ? R.string.server_identity_has_been_deleted : R.string.cannot_process_request_due_to_an_internal_error);
            if (success) { setFragmentResultThenFinish(EditIdentityResultType.DELETED); }
            else { setButtonsAvailability(); }
        }
    }

    // Functions related to the refresh server identity data
    // We do not set the running task flag to true due to this is a non block task (maybe the server is not accessible at this moment)

    private void refreshServerIdentity() {
        LoadServerIdentityAndGroupsDataFromServer.getBackgroundTask(mCurrentServerIdentity.storedData, this).start();
    }

    public void onServerIdentityAndGroupsDataLoadedFromServer(final boolean success) {
        if (isAdded()) {
            findPreference(SERVER_LOADED_DATA_KEY).setVisible(success || mCurrentServerIdentity.storedData.hasName());
            findPreference(NAME_KEY).setSummary(mCurrentServerIdentity.storedData.getName());
            findPreference(EMAIL_KEY).setSummary(mCurrentServerIdentity.storedData.getEmail());
            findPreference(IS_ADMIN_KEY).setSummary(mCurrentServerIdentity.storedData.isAdmin() ? R.string.yes : R.string.no);
            setButtonsAvailability();
        }
    }

    // User interaction

    @Override
    public void onClick(@NotNull final View view) {
        final int view_id = view.getId();
        final Context context = view.getContext();
        Vibrator.vibrate(context, Constants.NORMAL_HAPTIC_FEEDBACK);
        if (view_id == R.id.edit_or_save_server_identity_data) {
            if (! mEditing) { mAuthenticator.authenticate(this, AuthenticatedActions.ENABLE_SERVER_IDENTITY_EDITION); }
            else { saveServerIdentityData(); }
        }
        else if (view_id == R.id.toggle_submenu) {
            UI.animateSubmenuOpenOrClose(mToggleSubmenuVisibilityButton, Constants.SUBMENU_OPEN_OR_CLOSE_ANIMATION_DURATION, Preferences.getDefaultSharedPreferences(context).getBoolean(Constants.ALLOW_COPY_SERVER_ACCESS_TOKEN_KEY, context.getResources().getBoolean(R.bool.allow_copy_server_access_tokens)) ? new FloatingActionButton[] { mDeleteIdentityDataButton, mManageGroupsButton, mSyncServerDataButton, mCopyTokenToClipboardButton, mOpenServerLocationButton } : new FloatingActionButton[] { mDeleteIdentityDataButton, mManageGroupsButton, mSyncServerDataButton, mOpenServerLocationButton });
        }
        else if (view_id == R.id.delete_server_identity_data) {
            deleteServerIdentityData();
        }
        else if (view_id == R.id.sync_server_data) {
            MainService.startService(context, mCurrentServerIdentity.storedData);
        }
        else if (view_id == R.id.copy_token_to_clipboard) {
            mAuthenticator.authenticate(this, AuthenticatedActions.COPY_TOKEN_TO_CLIPBOARD);
        }
        else if (view_id == R.id.manage_groups) {
            mAuthenticator.authenticate(this, AuthenticatedActions.MANAGE_GROUPS);
        }
        else if (view_id == R.id.open_server_location) {
            HtmlActivity.openInWebBrowser(getActivity(), Uri.parse(mCurrentServerIdentity.storedData.getServer()));
        }
    }

    // If edition is disabled we discard change events

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        return ! mEditing;
    }

    @Override
    public boolean onPreferenceChange(@NotNull final Preference preference, @Nullable final Object new_value) {
        if (mEditing) {
            if (LABEL_KEY.equals(preference.getKey())) {
                final String trimmed_new_value = new_value.toString().trim();
                if (! Strings.equals(trimmed_new_value, mCurrentServerIdentity.storedData.getLabel())) {
                    mCurrentServerIdentity.storedData.setLabel(trimmed_new_value);
                    onLabelChanged();
                    return true;
                }
            }
            else if (SERVER_LOCATION_KEY.equals(preference.getKey())) {
                final String trimmed_new_value = new_value.toString().trim();
                if (! Strings.equals(trimmed_new_value, mCurrentServerIdentity.storedData.getServer())) {
                    if (trimmed_new_value.toLowerCase().startsWith("http:")) {
                        UI.showConfirmDialog(getActivity(), R.string.server_http_is_insecure_warning, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mCurrentServerIdentity.storedData.setServer(trimmed_new_value);
                                onServerChanged();
                            }
                        });
                        return false;
                    }
                    mCurrentServerIdentity.storedData.setServer(trimmed_new_value);
                    onServerChanged();
                    return true;
                }
            }
            else if (TOKEN_KEY.equals(preference.getKey())) {
                final String trimmed_new_value = new_value.toString().trim();
                if (! trimmed_new_value.isEmpty()) {
                    mCurrentServerIdentity.storedData.setToken(trimmed_new_value);
                    onTokenChanged();
                    return true;
                }
            }
            else if (SYNC_ON_STARTUP_KEY.equals(preference.getKey())) {
                mCurrentServerIdentity.storedData.setSyncOnStartup((Boolean) new_value);
                onOtherValueChanged();
                return true;
            }
            else if (SYNC_IMMEDIATELY_KEY.equals(preference.getKey())) {
                mCurrentServerIdentity.storedData.setSyncImmediately((Boolean) new_value);
                onOtherValueChanged();
                return true;
            }
        }
        return false;
    }

    // Events related to the authentication

    @Override
    public void onAuthenticationError(@Nullable final Object object) {}

    @Override
    public void onAuthenticationSuccess(@Nullable final Object object) {
        final AuthenticatedActions action = (AuthenticatedActions) object;
        if (action == AuthenticatedActions.DELETE_SERVER_IDENTITY) { deleteServerIdentityDataConfirmed(); }
        else if (action == AuthenticatedActions.ENABLE_SERVER_IDENTITY_EDITION) { enableEdition(); }
        else if (action == AuthenticatedActions.MANAGE_GROUPS) { manageGroups(); }
        else if (action == AuthenticatedActions.COPY_TOKEN_TO_CLIPBOARD) { copyTokenToClipboard(); }
    }

    private void manageGroups() {
        ((PreferencesActivity) getActivity()).setFragment(new ManageGroupsPreferences(mCurrentServerIdentity.storedData));
    }

    private void enableEdition() {
        if (isAdded()) {
            UI.animateIconChange(mEditOrSaveIdentityDataButton, R.drawable.ic_actionbar_accept, Constants.BUTTON_SHOW_OR_HIDE_ANIMATION_DURATION, true);
            UI.hideSubmenuAndRelatedOptions(mToggleSubmenuVisibilityButton, Constants.SUBMENU_OPEN_OR_CLOSE_ANIMATION_DURATION, Constants.BUTTON_SHOW_OR_HIDE_ANIMATION_DURATION, new FloatingActionButton[] { mDeleteIdentityDataButton, mManageGroupsButton, mSyncServerDataButton, mCopyTokenToClipboardButton, mOpenServerLocationButton });
            mEditing = true;
        }
    }

    private void disableEdition() {
        if (isAdded()) {
            UI.animateIconChange(mEditOrSaveIdentityDataButton, R.drawable.ic_actionbar_edit, Constants.BUTTON_SHOW_OR_HIDE_ANIMATION_DURATION);
            UI.animateShowOrHide(mToggleSubmenuVisibilityButton, true, Constants.BUTTON_SHOW_OR_HIDE_ANIMATION_DURATION);
            mEditing = false;
        }
    }

    private void copyTokenToClipboard() {
        Clipboard.copy(getContext(), mCurrentServerIdentity.storedData.getToken(), true, null);
    }
}

