package com.twofauth.android.preferences_activity;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.twofauth.android.Constants;
import com.twofauth.android.R;
import com.twofauth.android.database.TwoFactorGroup;
import com.twofauth.android.preferences_activity.ManageGroupsPreferences.TwoFactorGroupsUtils;
import com.twofauth.android.preferences_activity.tasks.LoadGroupsData.TwoFactorGroupWithReferencesInformation;
import com.twofauth.android.preferences_activity.tasks.SaveGroupData;
import com.twofauth.android.preferences_activity.tasks.SaveGroupData.OnDataSavedListener;
import com.twofauth.android.utils.UI;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ManageGroupPreferences extends PreferenceFragmentCompat implements OnDataSavedListener, OnClickListener {
    public static final String EDIT_GROUP = "edit-group";
    public static final String EDIT_GROUP_RESULT = "result";

    public static enum EditGroupResultType { UPDATED, DELETED };

    private static enum EditGroupTask { CHANGE_NAME, DELETE_OR_UNDELETE };

    private static final String NAME_KEY = "name";

    private final TwoFactorGroupWithReferencesInformation mGroup;
    private final List<TwoFactorGroupWithReferencesInformation> mGroups;

    private EditGroupTask mRunningTasks = null;

    private View mWorkingView;
    private FloatingActionButton mEditGroupName;
    private FloatingActionButton mDeleteOrUndeleteGroup;

    public ManageGroupPreferences(@NotNull final TwoFactorGroupWithReferencesInformation group, @NotNull final List<TwoFactorGroupWithReferencesInformation> groups) {
        mGroup = group;
        mGroups = groups;
    }

    // Propagate results to the caller

    private void setFragmentResult(final @Nullable EditGroupResultType result) {
        final Bundle bundle = new Bundle();
        bundle.putSerializable(EDIT_GROUP_RESULT, result);
        getParentFragmentManager().setFragmentResult(EDIT_GROUP, bundle);
    }

    private void finish() {
        getActivity().getSupportFragmentManager().popBackStack();
    }

    @Override
    public @NotNull View onCreateView(@NotNull LayoutInflater inflater, @NotNull ViewGroup container, @Nullable Bundle saved_instance_state) {
        final View view = inflater.inflate(R.layout.manage_group_preferences_fragment, container, false);
        final FrameLayout preferences_container = view.findViewById(R.id.contents);
        preferences_container.addView(super.onCreateView(inflater, preferences_container, saved_instance_state));
        return view;
    }

    @Override
    public void onViewCreated(@NotNull final View view, @Nullable final Bundle saved_instance_state) {
        final Context context = view.getContext();
        super.onViewCreated(view, saved_instance_state);
        mWorkingView = view.findViewById(R.id.working);
        mEditGroupName = (FloatingActionButton) view.findViewById(R.id.edit_group_name);
        mEditGroupName.setOnClickListener(this);
        mDeleteOrUndeleteGroup = (FloatingActionButton) view.findViewById(R.id.delete_or_undelete_group_data);
        mDeleteOrUndeleteGroup.setImageResource(mGroup.storedData.isDeleted() ? R.drawable.ic_actionbar_undelete : R.drawable.ic_actionbar_delete);
        mDeleteOrUndeleteGroup.setOnClickListener(this);
        setButtonsAvailability();
        setDivider(null);
    }

    @Override
    public void onCreatePreferences(@Nullable final Bundle saved_instance_state, @Nullable final String root_key) {
        setPreferencesFromResource(R.xml.manage_group_preferences, root_key);
        initializePreferences();
    }

    private void initializePreferences() {
        findPreference(NAME_KEY).setTitle(mGroup.storedData.getName());
    }

    // Sets buttons state

    private void setButtonsAvailability() {
        final Context context = getContext();
        mWorkingView.setVisibility(mRunningTasks == null ? View.GONE : View.VISIBLE);
        mEditGroupName.setEnabled(mRunningTasks == null);
        mDeleteOrUndeleteGroup.setEnabled((mRunningTasks == null) && (mGroup.storedData.isDeleted() || (! mGroup.isReferenced)));
    }

    @Override
    public void onClick(@NotNull final View view) {
        final int view_id = view.getId();
        if (view_id == R.id.edit_group_name) { editGroupName(); }
        else if (view_id == R.id.delete_or_undelete_group_data) { deleteOrUndeleteGroup(); }
    }

    private void editGroupName() {
        UI.showEditTextDialog(getActivity(), R.string.edit_group_name_dialog_title, R.string.edit_group_name_dialog_message, mGroup.storedData.getName(), 0, Constants.GROUP_NAME_VALID_REGEXP, R.string.accept, R.string.cancel, new UI.OnTextEnteredListener() {
            @Override
            public void onTextEntered(@NotNull final String name) {
                if (TwoFactorGroupsUtils.indexOf(mGroups, name) < 0) {
                    mGroup.storedData.setName(name);
                    mRunningTasks = EditGroupTask.CHANGE_NAME;
                    setButtonsAvailability();
                    SaveGroupData.getBackgroundTask(getContext(), mGroup.storedData, ManageGroupPreferences.this).start();
                }
            }
        });
    }

    private void deleteOrUndeleteGroup() {
        if (mGroup.storedData.isDeleted()) { mGroup.storedData.setStatus(TwoFactorGroup.STATUS_DEFAULT); }
        else { mGroup.storedData.setStatus(TwoFactorGroup.STATUS_DELETED); }
        mRunningTasks = EditGroupTask.DELETE_OR_UNDELETE;
        setButtonsAvailability();
        SaveGroupData.getBackgroundTask(getContext(), mGroup.storedData, ManageGroupPreferences.this).start();
    }

    @Override
    public void onDataSaved(@NotNull final TwoFactorGroupWithReferencesInformation group, final boolean success, final boolean synced) {
        if (isAdded()) {
            if (! success) { UI.showToast(getContext(), R.string.cannot_process_request_due_to_an_internal_error); return; }
            findPreference(NAME_KEY).setTitle(mGroup.storedData.getName());
            if (mRunningTasks == EditGroupTask.DELETE_OR_UNDELETE) { UI.animateIconChange(mDeleteOrUndeleteGroup, mGroup.storedData.isDeleted() ? R.drawable.ic_actionbar_undelete : R.drawable.ic_actionbar_delete, Constants.BUTTON_SHOW_OR_HIDE_ANIMATION_DURATION); }
            setFragmentResult(mGroup.storedData.getRowId() < 0 ? EditGroupResultType.DELETED : EditGroupResultType.UPDATED);
            mRunningTasks = null;
            if (mGroup.storedData.getRowId() < 0) { finish(); }
            else { setButtonsAvailability(); }
        }
    }
}
