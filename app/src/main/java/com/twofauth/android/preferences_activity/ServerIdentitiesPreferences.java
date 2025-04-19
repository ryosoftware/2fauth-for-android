package com.twofauth.android.preferences_activity;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentResultListener;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.twofauth.android.Constants;
import com.twofauth.android.MainService;
import com.twofauth.android.MainService.OnMainServiceStatusChangedListener;
import com.twofauth.android.PreferencesActivity;
import com.twofauth.android.R;
import com.twofauth.android.main_service.TwoFactorServerIdentityWithSyncData;
import com.twofauth.android.preferences_activity.tasks.LoadServerIdentitiesData;
import com.twofauth.android.preferences_activity.ServerIdentityPreferences.EditIdentityResultType;
import com.twofauth.android.preferences_activity.tasks.LoadServerIdentitiesData.OnServerIdentitiesLoadedListener;
import com.twofauth.android.preferences_activity.tasks.LoadServerIdentitiesData.TwoFactorServerIdentityWithSyncDataAndAccountNumbers;
import com.twofauth.android.utils.Lists;
import com.twofauth.android.utils.Preferences;
import com.twofauth.android.utils.Strings;
import com.twofauth.android.utils.UI;
import com.twofauth.android.utils.Vibrator;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;

public class ServerIdentitiesPreferences extends PreferenceFragmentCompat implements OnServerIdentitiesLoadedListener, OnMainServiceStatusChangedListener, OnPreferenceClickListener, OnClickListener, FragmentResultListener {
    public static final String EDIT_IDENTITIES = "edit-server-identities";
    public static final String EDIT_IDENTITIES_RESULT = "result";

    private static class TwoFactorServerIdentitiesWithSyncDataAndAccountNumbersComparator implements Comparator<TwoFactorServerIdentityWithSyncDataAndAccountNumbers> {
        @Override
        public int compare(@NotNull final TwoFactorServerIdentityWithSyncDataAndAccountNumbers server_identity_1, @NotNull final TwoFactorServerIdentityWithSyncDataAndAccountNumbers server_identity_2) {
            return Strings.compare(server_identity_1.storedData.getTitle(), server_identity_2.storedData.getTitle(), true);
        }
    }

    private final Bundle mResultBundle = new Bundle();

    private boolean mAnimatingSyncDataButton = false;

    private final TwoFactorServerIdentitiesWithSyncDataAndAccountNumbersComparator mComparator = new TwoFactorServerIdentitiesWithSyncDataAndAccountNumbersComparator();

    private final List<TwoFactorServerIdentityWithSyncDataAndAccountNumbers> mServerIdentities;

    private TwoFactorServerIdentityWithSyncDataAndAccountNumbers mActiveServerIdentity;

    private FloatingActionButton mSyncDataButton;
    private FloatingActionButton mAddIdentityDataButton;

    private View mEmptyView;

    public ServerIdentitiesPreferences(@NotNull final List<TwoFactorServerIdentityWithSyncDataAndAccountNumbers> server_identities) {
        super();
        mServerIdentities = server_identities;
        mResultBundle.putBoolean(EDIT_IDENTITIES_RESULT, true);
    }

    @Override
    public void onPause() {
        MainService.removeListener(this);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        MainService.addListener(this);
        setSyncDataButtonsAvailability();
    }

    @Override
    public @NotNull View onCreateView(@NotNull LayoutInflater inflater, @NotNull ViewGroup container, @Nullable Bundle saved_instance_state) {
        final View view = inflater.inflate(R.layout.server_identities_preferences_fragment, container, false);
        final FrameLayout preferences_container = view.findViewById(R.id.contents);
        preferences_container.addView(super.onCreateView(inflater, preferences_container, saved_instance_state));
        return view;
    }

    @Override
    public void onViewCreated(@NotNull final View view, @Nullable final Bundle saved_instance_state) {
        super.onViewCreated(view, saved_instance_state);
        mEmptyView = view.findViewById(R.id.empty_view);
        mEmptyView.setVisibility(mServerIdentities.isEmpty() ? View.VISIBLE : View.GONE);
        mSyncDataButton = (FloatingActionButton) view.findViewById(R.id.sync_server_data);
        mSyncDataButton.setOnClickListener(this);
        mAddIdentityDataButton = (FloatingActionButton) view.findViewById(R.id.add_identity_data);
        mAddIdentityDataButton.setOnClickListener(this);
        getParentFragmentManager().setFragmentResultListener(ServerIdentityPreferences.EDIT_IDENTITY, this, this);
        setSyncDataButtonsAvailability();
        setDivider(null);
    }

    @Override
    public void onCreatePreferences(@Nullable final Bundle saved_instance_state, @Nullable final String root_key) {
        setPreferencesFromResource(R.xml.server_identities_preferences, root_key);
        initializePreferences();
    }

    private void initializePreferences() {
        final Context context = getPreferenceManager().getContext();
        final PreferenceScreen root_preference = getPreferenceScreen();
        root_preference.removeAll();
        mServerIdentities.sort(mComparator);
        for (int i = 0; i < mServerIdentities.size(); i ++) {
            final TwoFactorServerIdentityWithSyncData server_identity = mServerIdentities.get(i);
            final Preference preference = new Preference(context);
            preference.setTitle(server_identity.storedData.getTitle());
            preference.setSummary(getServerIdentitySyncDetails(context, server_identity));
            preference.setKey(String.valueOf(i));
            preference.setOnPreferenceClickListener(this);
            root_preference.addPreference(preference);
        }
    }

    private void setSyncDataButtonsAvailability() {
        if (isAdded()) {
            final Context context = getContext();
            final boolean sync_process_running = MainService.isRunning(context), can_start_sync_process = ((! sync_process_running) && MainService.canSyncServerData(context));
            mSyncDataButton.setEnabled(can_start_sync_process);
            if ((sync_process_running) && (! mAnimatingSyncDataButton)) { UI.startInfiniteRotationAnimationLoop(mSyncDataButton, Constants.BUTTON_360_DEGREES_ROTATION_ANIMATION_DURATION); }
            else if ((! sync_process_running) && (mAnimatingSyncDataButton)) { mSyncDataButton.clearAnimation(); }
            mAnimatingSyncDataButton = sync_process_running;
        }
    }

    // Entry point for the server identities task finish

    @Override
    public void onServerIdentitiesLoaded(@Nullable final List<TwoFactorServerIdentityWithSyncDataAndAccountNumbers> server_identities) {
        if (isAdded()) {
            Lists.setItems(mServerIdentities, server_identities);
            onServerIdentitiesChanged(false);
        }
    }

    // Entry points for the synchronization process events

    @Override
    public void onServiceStarted() {
        if (isAdded()) { setSyncDataButtonsAvailability(); }
    }

    @Override
    public void onServiceFinished(MainService.SyncResultType result_type) {
        if (isAdded()) {
            if (result_type == MainService.SyncResultType.UPDATED) { LoadServerIdentitiesData.getBackgroundTask(this).start(); }
            setSyncDataButtonsAvailability();
        }
    }

    // Gets the server identity synchronization status
    // We include information about last synchronization error time and error message or, if no error, last success synchronization time, if available

    private @NotNull String getServerIdentitySyncDetails(@NotNull final Context context, @NotNull final TwoFactorServerIdentityWithSyncData server_identity) {
        if (server_identity.hasSyncErrors(context)) { return getString(R.string.last_sync_error, server_identity.getLastSyncError(context), Strings.getDateTimeString(context, server_identity.getLastSyncErrorTime(context))); }
        else if (server_identity.hasBeenSynced(context)) { return getResources().getQuantityString(R.plurals.sync_details, server_identity.getLastSyncedAccounts(context), server_identity.getLastSyncedAccounts(context), Strings.getDateTimeString(context, server_identity.getLastSyncTime(context))); }
        return getString(R.string.account_never_synced);
    }

    @Override
    public void onClick(@NotNull final View view) {
        final Context context = view.getContext();
        final int view_id = view.getId();
        Vibrator.vibrate(context, Constants.NORMAL_HAPTIC_FEEDBACK);
        if (view_id == R.id.add_identity_data) { addServerIdentity(); }
        else if (view_id == R.id.sync_server_data) { MainService.startService(context); }
    }

    @Override
    public boolean onPreferenceClick(@NotNull final Preference preference) {
        editServerIdentity(mServerIdentities.get(Integer.parseInt(preference.getKey())));
        return true;
    }

    // Procedures related to the identity manager
    // If a identity is changed, we propagate this information to the caller, if is a relevant change

    private void onServerIdentitiesChanged(final boolean propagate_result) {
        if (propagate_result) { getParentFragmentManager().setFragmentResult(EDIT_IDENTITIES, mResultBundle); }
        Preferences.getDefaultSharedPreferences(getContext()).edit().putInt(Constants.SERVER_IDENTITIES_COUNT_KEY, mServerIdentities.size()).apply();
        initializePreferences();
        mEmptyView.setVisibility(mServerIdentities.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onFragmentResult(@NotNull final String request_key, @NotNull final Bundle result) {
        if (isAdded() && ServerIdentityPreferences.EDIT_IDENTITY.equals(request_key)) {
            final EditIdentityResultType result_type = (EditIdentityResultType) result.getSerializable(ServerIdentityPreferences.EDIT_IDENTITY_RESULT);
            if (result_type == EditIdentityResultType.DELETED) {
                mServerIdentities.remove(mActiveServerIdentity);
                onServerIdentitiesChanged(true);
            }
            else if ((result_type == EditIdentityResultType.RELEVANT_UPDATE) || (result_type == EditIdentityResultType.NOT_RELEVANT_UPDATE)) {
                final boolean is_new_identity = ! mServerIdentities.contains(mActiveServerIdentity);
                if (is_new_identity) { mServerIdentities.add(mActiveServerIdentity); }
                onServerIdentitiesChanged(is_new_identity || (result_type == EditIdentityResultType.RELEVANT_UPDATE));
            }
        }
    }

    private void editServerIdentity(@Nullable final TwoFactorServerIdentityWithSyncDataAndAccountNumbers server_identity) {
        mActiveServerIdentity = (server_identity == null) ? new TwoFactorServerIdentityWithSyncDataAndAccountNumbers() : server_identity;
        ((PreferencesActivity) getActivity()).setFragment(new ServerIdentityPreferences(mActiveServerIdentity, mServerIdentities));
    }

    // Open identity manager fragment after animate the add button (animations availability is enabled or disabled at UI class)

    private void addServerIdentity() {
        UI.startRotationAnimation(mAddIdentityDataButton, Constants.BUTTON_CLICK_ANIMATION_BEFORE_START_ACTION_DEGREES, Constants.BUTTON_360_DEGREES_ROTATION_ANIMATION_DURATION, new UI.OnAnimationEndListener() {
            @Override
            public void onAnimationEnd(View view) {
                editServerIdentity(null);
            }
        });
    }
}
