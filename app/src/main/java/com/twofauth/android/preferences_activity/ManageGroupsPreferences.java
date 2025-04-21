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
import com.twofauth.android.database.TwoFactorGroup;
import com.twofauth.android.database.TwoFactorServerIdentity;
import com.twofauth.android.preferences_activity.tasks.LoadGroupsData;
import com.twofauth.android.preferences_activity.tasks.LoadGroupsData.TwoFactorGroupWithReferencesInformation;
import com.twofauth.android.preferences_activity.tasks.LoadGroupsData.OnGroupsLoadedListener;
import com.twofauth.android.preferences_activity.tasks.SaveGroupData;
import com.twofauth.android.preferences_activity.tasks.SaveGroupData.OnDataSavedListener;
import com.twofauth.android.preferences_activity.ManageGroupPreferences.EditGroupResultType;
import com.twofauth.android.utils.Lists;
import com.twofauth.android.utils.Strings;
import com.twofauth.android.utils.UI;
import com.twofauth.android.utils.Vibrator;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ManageGroupsPreferences extends PreferenceFragmentCompat implements OnGroupsLoadedListener, OnDataSavedListener, OnMainServiceStatusChangedListener, OnPreferenceClickListener, OnClickListener, FragmentResultListener {
    private static class TwoFactorGroupsWithReferencesInformationComparator implements Comparator<TwoFactorGroupWithReferencesInformation> {
        @Override
        public int compare(@NotNull final TwoFactorGroupWithReferencesInformation group_1, @NotNull final TwoFactorGroupWithReferencesInformation group_2) {
            return Strings.compare(group_1.storedData.getName(), group_2.storedData.getName(), true);
        }
    }

    public static class TwoFactorGroupsUtils {
        public static int indexOf(@Nullable final List<TwoFactorGroupWithReferencesInformation> groups, @NotNull final String name) {
            if (groups != null) {
                for (int i = groups.size() - 1; i >= 0; i --) {
                    if (Strings.equals(groups.get(i).storedData.getName(), name, true)) { return i; }
                }
            }
            return -1;
        }
    }

    private boolean mRunningTasks = false;
    private boolean mAnimatingSyncDataButton = false;

    private final TwoFactorServerIdentity mServerIdentity;
    private final List<LoadGroupsData.TwoFactorGroupWithReferencesInformation> mGroups = new ArrayList<LoadGroupsData.TwoFactorGroupWithReferencesInformation>();

    private TwoFactorGroupWithReferencesInformation mActiveGroup;

    private FloatingActionButton mSyncDataButton;
    private FloatingActionButton mAddGroupDataButton;

    private final TwoFactorGroupsWithReferencesInformationComparator mComparator = new TwoFactorGroupsWithReferencesInformationComparator();

    private View mEmptyView = null;

    public ManageGroupsPreferences(@NotNull final TwoFactorServerIdentity server_identity) {
        super();
        mServerIdentity = server_identity;
        LoadGroupsData.getBackgroundTask(mServerIdentity, this).start();
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
        final View view = inflater.inflate(R.layout.manage_groups_preferences_fragment, container, false);
        final FrameLayout preferences_container = view.findViewById(R.id.contents);
        preferences_container.addView(super.onCreateView(inflater, preferences_container, saved_instance_state));
        return view;
    }

    @Override
    public void onViewCreated(@NotNull final View view, @Nullable final Bundle saved_instance_state) {
        super.onViewCreated(view, saved_instance_state);
        mEmptyView = view.findViewById(R.id.empty_view);
        mEmptyView.setVisibility(mGroups.isEmpty() ? View.VISIBLE : View.GONE);
        mSyncDataButton = (FloatingActionButton) view.findViewById(R.id.sync_server_data);
        mSyncDataButton.setOnClickListener(this);
        mAddGroupDataButton = (FloatingActionButton) view.findViewById(R.id.add_group_data);
        mAddGroupDataButton.setOnClickListener(this);
        getParentFragmentManager().setFragmentResultListener(ManageGroupPreferences.EDIT_GROUP, this, this);
        setSyncDataButtonsAvailability();
        setDivider(null);
    }

    @Override
    public void onCreatePreferences(@Nullable final Bundle saved_instance_state, @Nullable final String root_key) {
        setPreferencesFromResource(R.xml.manage_groups_preferences, root_key);
        onGroupsChanged();
    }

    private void onGroupsChanged() {
        initializePreferences();
        if (mEmptyView != null) { mEmptyView.setVisibility(mGroups.isEmpty() ? View.VISIBLE : View.GONE); }
    }

    private void initializePreferences() {
        final Context context = getPreferenceManager().getContext();
        final PreferenceScreen root_preference = getPreferenceScreen();
        root_preference.removeAll();
        mGroups.sort(mComparator);
        for (int i = 0; i < mGroups.size(); i ++) {
            final TwoFactorGroupWithReferencesInformation group = mGroups.get(i);
            final Preference preference = new Preference(context);
            preference.setTitle(group.storedData.getName());
            preference.setSummary(group.storedData.isDeleted() ? getString(R.string.pending_of_deletion) : group.storedData.isSynced() ? null : getString(R.string.pending_of_sync));
            preference.setKey(String.valueOf(i));
            preference.setOnPreferenceClickListener(this);
            root_preference.addPreference(preference);
        }
    }

    private void setSyncDataButtonsAvailability() {
        if (isAdded()) {
            final Context context = getContext();
            final boolean sync_process_running = MainService.isRunning(context) || mRunningTasks;
            mSyncDataButton.setEnabled(! sync_process_running);
            mAddGroupDataButton.setEnabled(! mRunningTasks);
            if ((sync_process_running) && (! mAnimatingSyncDataButton)) { UI.startInfiniteRotationAnimationLoop(mSyncDataButton, Constants.BUTTON_360_DEGREES_ROTATION_ANIMATION_DURATION); }
            else if ((! sync_process_running) && (mAnimatingSyncDataButton)) { mSyncDataButton.clearAnimation(); }
            mAnimatingSyncDataButton = sync_process_running;
        }
    }

    // Entry point for the load groups task finished

    public void onGroupsLoaded(@Nullable final List<TwoFactorGroupWithReferencesInformation> groups) {
        Lists.setItems(mGroups, groups);
        if (isAdded()) { onGroupsChanged(); }
    }

    public void onGroupsLoadError() {
        if (isAdded()) { UI.showToast(getContext(), R.string.cannot_process_request_due_to_an_internal_error); }
    }

    // Entry points for the synchronization process events

    @Override
    public void onServiceStarted() {
        if (isAdded()) { setSyncDataButtonsAvailability(); }
    }

    @Override
    public void onServiceFinished(MainService.SyncResultType result_type) {
        if (isAdded()) {
            if (result_type == MainService.SyncResultType.UPDATED) { LoadGroupsData.getBackgroundTask(mServerIdentity, this).start(); }
            setSyncDataButtonsAvailability();
        }
    }

    @Override
    public void onClick(@NotNull final View view) {
        final Context context = view.getContext();
        final int view_id = view.getId();
        Vibrator.vibrate(context, Constants.NORMAL_HAPTIC_FEEDBACK);
        if (view_id == R.id.add_group_data) { addGroup(); }
        else if (view_id == R.id.sync_server_data) { MainService.startService(context); }
    }

    @Override
    public boolean onPreferenceClick(@NotNull final Preference preference) {
        if (! mRunningTasks) {
            editGroup(mGroups.get(Integer.parseInt(preference.getKey())));
        }
        return true;
    }

    // Entry point to the edit group process

    private void editGroup(@NotNull final TwoFactorGroupWithReferencesInformation group) {
        ((PreferencesActivity) getActivity()).setFragment(new ManageGroupPreferences(mActiveGroup = group, mGroups));
    }

    @Override
    public void onFragmentResult(@NotNull final String request_key, @NotNull final Bundle result) {
        if (isAdded() && ManageGroupPreferences.EDIT_GROUP.equals(request_key)) {
            final EditGroupResultType result_type = (EditGroupResultType) result.getSerializable(ManageGroupPreferences.EDIT_GROUP_RESULT);
            if (result_type != null) {
                if (result_type == EditGroupResultType.DELETED.DELETED) { mGroups.remove(mActiveGroup); }
                onGroupsChanged();
            }
        }
    }

    // Entry point to the add group process

    private void addGroup() {
        UI.startRotationAnimation(mAddGroupDataButton, Constants.BUTTON_CLICK_ANIMATION_BEFORE_START_ACTION_DEGREES, Constants.BUTTON_360_DEGREES_ROTATION_ANIMATION_DURATION, new UI.OnAnimationEndListener() {
            @Override
            public void onAnimationEnd(View view) {
                if (isAdded()) {
                    UI.showEditTextDialog(getActivity(), R.string.add_group_dialog_title, R.string.add_group_dialog_message, null, 0, Constants.GROUP_NAME_VALID_REGEXP, R.string.accept, R.string.cancel, new UI.OnTextEnteredListener() {
                        @Override
                        public void onTextEntered(@NotNull final String name) {
                            addGroup(name);
                        }
                    });
                }
            }
        });
    }

    private void addGroup(@NotNull final String name) {
        if (isAdded() && (TwoFactorGroupsUtils.indexOf(mGroups, name) < 0)) {
            final TwoFactorGroup group = new TwoFactorGroup();
            group.setServerIdentity(mServerIdentity);
            group.setName(name);
            mRunningTasks = true;
            setSyncDataButtonsAvailability();
            SaveGroupData.getBackgroundTask(getContext(), group, ManageGroupsPreferences.this).start();
        }
    }

    @Override
    public void onDataSaved(TwoFactorGroupWithReferencesInformation group, boolean success, boolean synced) {
        if (isAdded()) {
            mRunningTasks = false;
            if (! success) UI.showToast(getContext(), R.string.cannot_process_request_due_to_an_internal_error);
            else { mGroups.add(group); onGroupsChanged(); }
            setSyncDataButtonsAvailability();
        }
    }
}
