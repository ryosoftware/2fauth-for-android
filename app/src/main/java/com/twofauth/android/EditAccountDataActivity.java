package com.twofauth.android;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.twofauth.android.Authenticator.OnAuthenticatorFinishListener;
import com.twofauth.android.api_tasks.LoadAccountQR;
import com.twofauth.android.api_tasks.LoadAccountQR.OnQRLoadedListener;
import com.twofauth.android.database.TwoFactorAccount;
import com.twofauth.android.database.TwoFactorGroup;
import com.twofauth.android.database.TwoFactorIcon;
import com.twofauth.android.database.TwoFactorServerIdentity;
import com.twofauth.android.api_tasks.ToggleAccountDataDeletionState;
import com.twofauth.android.api_tasks.ToggleAccountDataDeletionState.OnDataDeletedOrUndeletedListener;
import com.twofauth.android.edit_account_data_activity.tasks.LoadAccountEditionNeededData;
import com.twofauth.android.edit_account_data_activity.tasks.LoadAccountEditionNeededData.OnAccountEditionNeededDataLoadedListener;
import com.twofauth.android.api_tasks.SaveAccountData;
import com.twofauth.android.api_tasks.SaveAccountData.OnDataSavedListener;
import com.twofauth.android.utils.Bitmaps;
import com.twofauth.android.utils.Clipboard;
import com.twofauth.android.utils.Lists;
import com.twofauth.android.utils.Strings;
import com.twofauth.android.utils.Threads;
import com.twofauth.android.utils.UI;
import com.twofauth.android.utils.Vibrator;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class EditAccountDataActivity extends BaseActivityWithTextController implements OnAccountEditionNeededDataLoadedListener, OnDataSavedListener, OnDataDeletedOrUndeletedListener, OnQRLoadedListener, OnAuthenticatorFinishListener, OnClickListener, OnItemSelectedListener {
    public static final String EXTRA_ACCOUNT_ID = "account-id";
    public static final String EXTRA_ACCOUNT_DATA = "account-data";

    private static enum AuthenticatedActions { SHOW_ADVANCED_DATA, COPY_SECRET_CODE_TO_CLIPBOARD, DELETE_ACCOUNT, ENABLE_ACCOUNT_EDITION, SHOW_QR };

    private static class ViewUtils {
        public static void setChildrenViewsOnClickListener(@NotNull final ViewGroup container, @Nullable final View.OnClickListener listener) {
            for (int i = 0; i < container.getChildCount(); i ++) { container.getChildAt(i).setOnClickListener(listener); }
        }

        public static String getSelected(@NotNull final ViewGroup container) {
            for (int i = 0; i < container.getChildCount(); i ++) {
                final View view = container.getChildAt(i);
                if (view.isSelected()) { return (String) view.getTag(); }
            }
            return null;
        }

        private static void setColors(@NotNull final Context context, @NotNull final Button button) {
            final Resources resources = context.getResources();
            final Theme theme = context.getTheme();
            final boolean is_selected = button.isSelected(), is_enabled = button.isEnabled();
            button.setBackgroundColor(resources.getColor(is_selected ? is_enabled ? R.color.otp_attribute_background_selected : R.color.otp_attribute_background_disabled_selected : is_enabled ? R.color.otp_attribute_background : R.color.otp_attribute_background_disabled, theme));
            button.setTextColor(resources.getColor(is_selected ? is_enabled ? R.color.otp_attribute_foreground_selected : R.color.otp_attribute_foreground_disabled_selected :  is_enabled ? R.color.otp_attribute_foreground : R.color.otp_attribute_foreground_disabled, theme));
        }

        public static void setSelected(@NotNull final Context context, @NotNull final ViewGroup container, @Nullable final String tag) {
            for (int i = 0; i < container.getChildCount(); i ++) {
                final Button button = (Button) container.getChildAt(i);
                final boolean is_selected = Objects.equals(tag, button.getTag());
                button.setSelected(is_selected);
                setColors(context, button);
            }
        }

        public static @Nullable String setSelected(@NotNull final Context context, final View view) {
            final String tag = (String) view.getTag();
            setSelected(context, (ViewGroup) view.getParent(), tag);
            return tag;
        }

        public static void setEnabled(@NotNull final Context context, @NotNull final ViewGroup container, final boolean enable) {
            for (int i = 0; i < container.getChildCount(); i ++) {
                final Button button = (Button) container.getChildAt(i);
                button.setEnabled(enable);
                setColors(context, button);
            }
        }
    }

    private static class TwoFactorServerIdentitiesUtils {
        public static @Nullable List<String> getNames(@Nullable final List<TwoFactorServerIdentity> server_identities) {
            if (! Lists.isEmptyOrNull(server_identities)) {
                List<String> names = new ArrayList<String>();
                for (final TwoFactorServerIdentity server_identity : server_identities) { names.add(server_identity.getTitle()); }
                return names;
            }
            return null;
        }

        public static int indexOf(@Nullable final List<TwoFactorServerIdentity> server_identities, final long id) {
            if (server_identities != null) {
                for (int i = server_identities.size() - 1; i >= 0; i --) {
                    if (server_identities.get(i).getRowId() == id) { return i; }
                }
            }
            return -1;
        }
    }

    private static class TwoFactorGroupsUtils {
        public static @Nullable List<String> getNames(@NotNull final Context context, @Nullable final List<TwoFactorGroup> groups) {
            if (! Lists.isEmptyOrNull(groups)) {
                List<String> names = new ArrayList<String>();
                for (final TwoFactorGroup group : groups) { names.add(group.isDeleted() ? context.getString(R.string.deleted_group, group.getName()) : group.getName()); }
                return names;
            }
            return null;
        }

        public static int indexOf(@Nullable final List<TwoFactorGroup> groups, final long id) {
            if (groups != null) {
                for (int i = groups.size() - 1; i >= 0; i --) {
                    if (groups.get(i).getRowId() == id) { return i; }
                }
            }
            return -1;
        }

        public static int indexOf(@Nullable final List<TwoFactorGroup> groups, @NotNull final String name) {
            if (groups != null) {
                for (int i = groups.size() - 1; i >= 0; i --) {
                    if (Strings.equals(groups.get(i).getName(), name, true)) { return i; }
                }
            }
            return -1;
        }
    }

    private TwoFactorAccount mInitialAccountData = null;
    private TwoFactorAccount mCurrentAccountData = null;

    private List<TwoFactorServerIdentity> mServerIdentities = null;
    private Map<Long, List<TwoFactorGroup>> mGroups = null;

    private Thread mDataLoader = null;

    private ImageView mIconImageView;
    private TextView mIconSourceTextView;
    private Button mSelectIconButton;
    private Button mDeleteIconButton;
    private Button mCopyIconToServerButton;
    private View mServerIdentityContainer;
    private Spinner mServerIdentitySpinner;
    private EditText mServiceEditText;
    private EditText mAccountEditText;
    private Spinner mGroupSpinner;
    private ViewGroup mOtpTypeContainer;
    private EditText mSecretEditText;
    private Button mCopySecretButton;
    private ViewGroup mDigitsContainer;
    private ViewGroup mAlgorithmContainer;
    private View mPeriodContainer;
    private EditText mPeriodEditText;
    private View mCounterContainer;
    private EditText mCounterEditText;
    private Button mToggleOtpGenerationAttributesButton;
    private View mOtpAttributesLayout;
    private FloatingActionButton mEditOrSaveAccountDataButton;
    private FloatingActionButton mDeleteOrUndeleteAccountDataButton;
    private FloatingActionButton mCloneAccountDataButton;
    private FloatingActionButton mShowQRCodeButton;
    private FloatingActionButton mToggleSubmenuVisibilityButton;
    private FloatingActionButton[] mSubmenuButtons;
    private View mWorking;
    private View mContents;

    private Authenticator mAuthenticator;

    private String mAccountQR;

    private boolean mEditing = false;

    @Override
    protected void onCreate(@Nullable final Bundle saved_instance_state) {
        super.onCreate(saved_instance_state);
        mAuthenticator = new Authenticator(this);
        setResult(Activity.RESULT_CANCELED);
        setContentView(R.layout.edit_account_data_activity);
        mIconImageView = (ImageView) findViewById(R.id.icon);
        mIconSourceTextView = (TextView) findViewById(R.id.icon_source);
        mSelectIconButton = (Button) findViewById(R.id.select_icon);
        mSelectIconButton.setOnClickListener(this);
        mDeleteIconButton = (Button) findViewById(R.id.delete_icon);
        mDeleteIconButton.setOnClickListener(this);
        mCopyIconToServerButton = (Button) findViewById(R.id.copy_icon_to_server);
        mCopyIconToServerButton.setOnClickListener(this);
        mServerIdentityContainer = findViewById(R.id.server_identity_layout);
        mServerIdentitySpinner = (Spinner) findViewById(R.id.server_identity_selector);
        mServerIdentitySpinner.setOnItemSelectedListener(this);
        mServiceEditText = (EditText) findViewById(R.id.service);
        mServiceEditText.addTextChangedListener(this);
        mAccountEditText = (EditText) findViewById(R.id.account);
        mAccountEditText.addTextChangedListener(this);
        mGroupSpinner = (Spinner) findViewById(R.id.group);
        mGroupSpinner.setOnItemSelectedListener(this);
        mOtpTypeContainer = (ViewGroup) findViewById(R.id.otp_types_container);
        ViewUtils.setChildrenViewsOnClickListener(mOtpTypeContainer, this);
        mSecretEditText = (EditText) findViewById(R.id.secret);
        mSecretEditText.addTextChangedListener(this);
        mCopySecretButton = (Button) findViewById(R.id.copy_secret);
        mCopySecretButton.setOnClickListener(this);
        mDigitsContainer = (ViewGroup) findViewById(R.id.digits_container);
        ViewUtils.setChildrenViewsOnClickListener(mDigitsContainer, this);
        mAlgorithmContainer = (ViewGroup) findViewById(R.id.algorithms_container);
        ViewUtils.setChildrenViewsOnClickListener(mAlgorithmContainer, this);
        mPeriodContainer = findViewById(R.id.period_block_container);
        mPeriodEditText = (EditText) findViewById(R.id.period);
        mPeriodEditText.addTextChangedListener(this);
        mCounterContainer = findViewById(R.id.counter_block_container);
        mCounterEditText = (EditText) findViewById(R.id.counter);
        mCounterEditText.addTextChangedListener(this);
        mToggleOtpGenerationAttributesButton = (Button) findViewById(R.id.toggle_otp_generation_attributes_visualization);
        mToggleOtpGenerationAttributesButton.setOnClickListener(this);
        mOtpAttributesLayout = findViewById(R.id.otp_generation_attributes_layout);
        mEditOrSaveAccountDataButton = (FloatingActionButton) findViewById(R.id.edit_or_save_account_data);
        mEditOrSaveAccountDataButton.setOnClickListener(this);
        mDeleteOrUndeleteAccountDataButton = (FloatingActionButton) findViewById(R.id.delete_or_undelete_account_data);
        mDeleteOrUndeleteAccountDataButton.setOnClickListener(this);
        mDeleteOrUndeleteAccountDataButton.setVisibility(View.GONE);
        mCloneAccountDataButton = (FloatingActionButton) findViewById(R.id.clone_account_data);
        mCloneAccountDataButton.setOnClickListener(this);
        mCloneAccountDataButton.setVisibility(View.GONE);
        mShowQRCodeButton = (FloatingActionButton) findViewById(R.id.show_qr_code);
        mShowQRCodeButton.setOnClickListener(this);
        mShowQRCodeButton.setVisibility(View.GONE);
        mToggleSubmenuVisibilityButton = (FloatingActionButton) findViewById(R.id.toggle_submenu);
        mToggleSubmenuVisibilityButton.setOnClickListener(this);
        mSubmenuButtons = new FloatingActionButton[] { mDeleteOrUndeleteAccountDataButton, mCloneAccountDataButton, mShowQRCodeButton };
        mWorking = findViewById(R.id.working);
        mContents = findViewById(R.id.contents);
        final Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_ACCOUNT_DATA)) { mDataLoader = LoadAccountEditionNeededData.getBackgroundTask(intent.getStringExtra(EXTRA_ACCOUNT_DATA), this); }
        else { mDataLoader = LoadAccountEditionNeededData.getBackgroundTask(intent.getLongExtra(EXTRA_ACCOUNT_ID, -1), this); }
        mDataLoader.start();;
    }

    @Override
    protected void onDestroy() {
        Threads.interrupt(mDataLoader);
        super.onDestroy();
    }

    // Process back button action
    // If the account data is changed, we notify this condition to the user then do not process activity finish immediately

    @Override
    protected void processOnBackPressed() {
        if (isChanged()) {
            UI.showConfirmDialog(this, mCurrentAccountData.inDatabase() ? R.string.cancel_account_edition_message : R.string.cancel_account_creation_message, R.string.continue_editing, R.string.exit, null, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
        }
        else {
            finish();
        }
    }

    // Entry point for the authenticated actions

    @Override
    public void onAuthenticationError(Object object) {}

    @Override
    public void onAuthenticationSuccess(Object object) {
        final AuthenticatedActions action = (AuthenticatedActions) object;
        if (action == AuthenticatedActions.SHOW_ADVANCED_DATA) { setShowOtpAttributesVisibility(true); }
        else if (action == AuthenticatedActions.COPY_SECRET_CODE_TO_CLIPBOARD) { copySecretToClipboard(); }
        else if (action == AuthenticatedActions.DELETE_ACCOUNT) { onDeleteOrUndeleteDataConfirmed(); }
        else if (action == AuthenticatedActions.SHOW_QR) { UI.showBase64ImageDialog(getActivity(), mAccountQR); }
        else if (action == AuthenticatedActions.ENABLE_ACCOUNT_EDITION) { enableEdition(); }
    }

    // Enable or disable edition

    private void enableEdition() {
        UI.animateIconChange(mEditOrSaveAccountDataButton, R.drawable.ic_actionbar_accept, Constants.BUTTON_SHOW_OR_HIDE_ANIMATION_DURATION, true);
        UI.hideSubmenuAndRelatedOptions(mToggleSubmenuVisibilityButton, Constants.SUBMENU_OPEN_OR_CLOSE_ANIMATION_DURATION, Constants.BUTTON_SHOW_OR_HIDE_ANIMATION_DURATION, mSubmenuButtons);
        setViewsAvailability(mEditing = true);
    }

    private void disableEdition() {
        UI.animateIconChange(mEditOrSaveAccountDataButton, R.drawable.ic_actionbar_edit, Constants.BUTTON_SHOW_OR_HIDE_ANIMATION_DURATION);
        UI.animateShowOrHide(mToggleSubmenuVisibilityButton, true, Constants.BUTTON_SHOW_OR_HIDE_ANIMATION_DURATION);
        mEditing = false;
    }

    // Copy secret code to clipboard

    private void copySecretToClipboard() {
        if (! isFinishedOrFinishing()) {
            Clipboard.copy(this, mCurrentAccountData.getSecret(), true, null);
        }
    }

    // Shows or hide period or counter OTP options according to OTP type

    private void setOtpTypeDependencies(@Nullable final String otp_type) {
        final boolean is_empty_or_steam_otp_type = Strings.isEmptyOrNull(otp_type) || TwoFactorAccount.isSteam(otp_type), is_hotp = TwoFactorAccount.isHotp(otp_type);
        for (int block_id : new int[] { R.id.digits_block_container, R.id.algorithms_block_container }) { findViewById(block_id).setVisibility(is_empty_or_steam_otp_type ? View.GONE : View.VISIBLE); }
        mPeriodContainer.setVisibility((is_empty_or_steam_otp_type || is_hotp) ? View.GONE : View.VISIBLE);
        mCounterContainer.setVisibility((is_empty_or_steam_otp_type || (! is_hotp)) ? View.GONE : View.VISIBLE);
    }

    // Check if account data is valid

    private boolean isValid() {
        if (! mCurrentAccountData.hasServerIdentity()) { return false; }
        if (Strings.isEmptyOrNull(mCurrentAccountData.getService())) { return false; }
        if (Strings.isEmptyOrNull(mCurrentAccountData.getAccount())) { return false; }
        if (Strings.isEmptyOrNull(mCurrentAccountData.getSecret())) { return false; }
        if (Strings.isEmptyOrNull(mCurrentAccountData.getOtpType())) { return false; }
        if (! mCurrentAccountData.isSteam()) {
            if (mCurrentAccountData.getOtpLength() == 0) { return false; }
            if (Strings.isEmptyOrNull(mCurrentAccountData.getAlgorithm())) { return false; }
            if (mCurrentAccountData.isHotp() && (mCurrentAccountData.getCounter() < 0)) { return false; }
            if ((mCurrentAccountData.isTotp()) && (mCurrentAccountData.getPeriod() == 0)) { return false; }
        }
        return true;
    }

    // Check if account data is changed, compared with the one loaded at Activity startup

    private boolean isChanged() {
        final long current_server_identity = mCurrentAccountData.hasServerIdentity() ? mCurrentAccountData.getServerIdentity().getRowId() : -1, initial_server_identity = mInitialAccountData.hasServerIdentity() ? mInitialAccountData.getServerIdentity().getRowId() : -1;
        if (current_server_identity != initial_server_identity) { return true; }
        if (! Strings.equals(mCurrentAccountData.getService(), mInitialAccountData.getService())) { return true; }
        if (! Strings.equals(mCurrentAccountData.getAccount(), mInitialAccountData.getAccount())) { return true; }
        final String current_group_name = mCurrentAccountData.hasGroup() ? mCurrentAccountData.getGroup().getName() : null, initial_group_name = mInitialAccountData.hasGroup() ? mInitialAccountData.getGroup().getName() : null;
        if (! Strings.equals(current_group_name, initial_group_name)) { return true; }
        if (mCurrentAccountData.hasIcon() && mCurrentAccountData.getIcon().isDirty()) { return true; }
        if (! Strings.equals(mCurrentAccountData.getSecret(), mInitialAccountData.getSecret())) { return true; }
        if (! Strings.equals(mCurrentAccountData.getOtpType(), mInitialAccountData.getOtpType())) { return true; }
        if (! mCurrentAccountData.isSteam()) {
            if (mCurrentAccountData.getOtpLength() != mInitialAccountData.getOtpLength()) { return true; }
            if (! Strings.equals(mCurrentAccountData.getAlgorithm(), mInitialAccountData.getAlgorithm())) { return true; }
            if (mCurrentAccountData.isHotp() && (mCurrentAccountData.getCounter() != mInitialAccountData.getCounter())) { return true; }
            if ((mCurrentAccountData.isTotp()) && (mCurrentAccountData.getPeriod() != mInitialAccountData.getPeriod())) { return true; }
        }
        return false;
    }

    // Set buttons state

    private void setButtonsAvailability() {
        final boolean buttons_available = (mDataLoader == null);
        mEditOrSaveAccountDataButton.setEnabled(buttons_available && (! mCurrentAccountData.isDeleted()) && ((! mEditing) || (isValid() && (isChanged() || (! mCurrentAccountData.inDatabase())))));
        mToggleSubmenuVisibilityButton.setEnabled(buttons_available);
        mDeleteOrUndeleteAccountDataButton.setEnabled(buttons_available);
        mCloneAccountDataButton.setEnabled(buttons_available && mCurrentAccountData.inDatabase() && (! mCurrentAccountData.isDeleted()));
        mShowQRCodeButton.setEnabled(buttons_available && (mCurrentAccountData.getRemoteId() != 0) && (! isChanged()));
    }

    // User interaction

    @Override
    public void onClick(final View view) {
        final int view_id = view.getId();
        Vibrator.vibrate(this, Constants.NORMAL_HAPTIC_FEEDBACK);
        if (view_id == R.id.select_icon) { startActivityFromIntent(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)); }
        else if (view_id == R.id.delete_icon) { deleteIcon(); }
        else if (view_id == R.id.copy_icon_to_server) { copyIconToServer(); }
        else if (view_id == R.id.toggle_otp_generation_attributes_visualization) { toggleOtpAttributesVisibility(); }
        else if (view_id == R.id.otp_type) { setOtpTypeDependencies((String) view.getTag()); mCurrentAccountData.setOtpType(ViewUtils.setSelected(this, view)); setButtonsAvailability(); }
        else if (view_id == R.id.digit) { mCurrentAccountData.setOtpLength(Integer.parseInt(ViewUtils.setSelected(this, view))); setButtonsAvailability(); }
        else if (view_id == R.id.algorithm) { mCurrentAccountData.setAlgorithm(ViewUtils.setSelected(this, view)); setButtonsAvailability(); }
        else if (view_id == R.id.copy_secret) { mAuthenticator.authenticate(this, AuthenticatedActions.COPY_SECRET_CODE_TO_CLIPBOARD); }
        else if (view_id == R.id.edit_or_save_account_data) { if (mEditing) { saveData(); }  else { mAuthenticator.authenticate(this, AuthenticatedActions.ENABLE_ACCOUNT_EDITION); } }
        else if (view_id == R.id.toggle_submenu) { UI.animateSubmenuOpenOrClose(mToggleSubmenuVisibilityButton, Constants.SUBMENU_OPEN_OR_CLOSE_ANIMATION_DURATION, mSubmenuButtons); }
        else if (view_id == R.id.delete_or_undelete_account_data) { deleteOrUnDeleteData(); }
        else if (view_id == R.id.clone_account_data) { cloneAccountData(); }
        else if (view_id == R.id.show_qr_code) { showQR(); }
    }

    @Override
    public void afterTextChanged(@NotNull final Editable editable) {
        final EditText edittext = (EditText) getCurrentFocus();
        if (edittext != null) {
            final int view_id = edittext.getId();
            if (view_id == R.id.service) { mCurrentAccountData.setService(editable.toString()); }
            else if (view_id == R.id.account) { mCurrentAccountData.setAccount(editable.toString()); }
            else if (view_id == R.id.secret) { mCurrentAccountData.setSecret(editable.toString()); mCopySecretButton.setEnabled(! Strings.isEmptyOrNull(mCurrentAccountData.getSecret())); }
            else if (view_id == R.id.period) { mCurrentAccountData.setPeriod(Integer.parseInt(editable.toString())); }
            else if (view_id == R.id.counter) { mCurrentAccountData.setCounter(Integer.parseInt(editable.toString())); }
            setButtonsAvailability();
        }
    }

    public void onItemSelected(@NotNull final AdapterView<?> parent, @NotNull final View view, final int position, final long id) {
        final int view_id = parent.getId();
        if (view_id == R.id.server_identity_selector) {
            final TwoFactorServerIdentity server_identity = (position < 0) ? null : mServerIdentities.get(position);
            final long current_server_identity_id = mCurrentAccountData.hasServerIdentity() ? mCurrentAccountData.getServerIdentity().getRowId() : -1, new_server_identity_id = (server_identity == null) ? -1 : server_identity.getRowId();
            if (current_server_identity_id != new_server_identity_id) {
                mCurrentAccountData.setServerIdentity(position < 0 ? null : mServerIdentities.get(position));
                mCurrentAccountData.setGroup(null);
                setSelectableGroups();
            }
        }
        else if (view_id == R.id.group) {
            if (position <= 0) { mCurrentAccountData.setGroup(null); }
            else if (position == 1) { addGroup(); }
            else { mCurrentAccountData.setGroup(getGroupsBySelectedServerIdentity().get(position - 2)); }
        }
        setButtonsAvailability();
    }

    @Override
    public void onNothingSelected(@NotNull final AdapterView<?> parent) {}

    // Function that is triggered when user selects a account icon

    @Override
    public void onActivityResult(@NotNull final ActivityResult result) {
        if (result.getResultCode() == RESULT_OK) {
            try {
                final Bitmap bitmap = Bitmaps.cropToSquare(this, result.getData().getData(), null);
                if (bitmap == null) { throw new Exception("Cannot load or crop image"); }
                setIcon(bitmap);
            }
            catch (Exception e) {
                UI.showToast(this, R.string.cannot_load_selected_image);
                Log.e(Main.LOG_TAG_NAME, "Exception while trying to load an user selected image", e);
            }
        }
    }

    private void setIcon(@NotNull final Bitmap bitmap) {
        if (! mCurrentAccountData.hasIcon()) { mCurrentAccountData.setIcon(new TwoFactorIcon()); }
        final TwoFactorIcon icon = mCurrentAccountData.getIcon();
        icon.setSource(null);
        icon.setBitmaps(bitmap, null, null);
        mIconSourceTextView.setText(R.string.icon_source_local_storage);
        mIconImageView.setImageBitmap(bitmap);
        for (final View view : new View[] { mIconSourceTextView, mDeleteIconButton, mIconImageView }) { view.setVisibility(View.VISIBLE); }
        mCopyIconToServerButton.setVisibility(View.GONE);
        setButtonsAvailability();
    }

    private void deleteIcon() {
        final TwoFactorIcon icon = mCurrentAccountData.getIcon();
        icon.setSource(null);
        icon.setBitmaps((Bitmap) null, (Bitmap) null, (Bitmap) null);
        for (final View view : new View[] { mIconSourceTextView, mDeleteIconButton, mCopyIconToServerButton, mIconImageView }) { view.setVisibility(View.GONE); }
        setButtonsAvailability();
    }

    private void copyIconToServer() {
        mCurrentAccountData.getIcon().setSourceId(null);
        setIcon(mCurrentAccountData.getIcon().getBitmap(this));
    }

    // Set initial form values and state

    private void setSelectableGroups() {
        final List<TwoFactorGroup> groups = getGroupsBySelectedServerIdentity();
        final List<String> groups_names = new ArrayList<String>();
        Lists.setItems(groups_names, new String[] { getString(R.string.no_group), getString(R.string.add_group) }, TwoFactorGroupsUtils.getNames(this, groups));
        final ArrayAdapter<String> groups_adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, groups_names);
        groups_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mGroupSpinner.setAdapter(groups_adapter);
        final int selected_group_index = TwoFactorGroupsUtils.indexOf(groups, mCurrentAccountData.hasGroup() ? mCurrentAccountData.getGroup().getRowId() : -1);
        mGroupSpinner.setSelection(selected_group_index < 0 ? 0 : selected_group_index + 2);
    }

    private void setEditableAccountData() {
        final boolean has_icon = (mCurrentAccountData.hasIcon() && mCurrentAccountData.getIcon().hasBitmaps(this));
        final String icon_source = has_icon ? mCurrentAccountData.getIcon().getSource() : null;
        mIconImageView.setImageBitmap(has_icon ? mCurrentAccountData.getIcon().getBitmap(this) : null);
        mIconSourceTextView.setText(API.ICON_SOURCE_DEFAULT.equals(icon_source) ? getString(R.string.icon_source_bubka) : API.ICON_SOURCE_DASHBOARD.equals(icon_source) ? getString(R.string.icon_source_dashboard_icons) : getString(R.string.icon_source_local_storage));
        mIconSourceTextView.setVisibility(has_icon ? View.VISIBLE : View.GONE);
        mDeleteIconButton.setVisibility(has_icon ? View.VISIBLE : View.GONE);
        mCopyIconToServerButton.setVisibility(API.ICON_SOURCE_DASHBOARD.equals(icon_source) ? View.VISIBLE : View.GONE);
        final ArrayAdapter<String> server_identities_adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, TwoFactorServerIdentitiesUtils.getNames(mServerIdentities));
        server_identities_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mServerIdentitySpinner.setAdapter(server_identities_adapter);
        final int selected_server_identity_index = TwoFactorServerIdentitiesUtils.indexOf(mServerIdentities, mCurrentAccountData.hasServerIdentity() ? mCurrentAccountData.getServerIdentity().getRowId() : -1);
        mServerIdentitySpinner.setSelection(Math.max(0, selected_server_identity_index));
        mServerIdentityContainer.setVisibility(server_identities_adapter.getCount() > 1 ? View.VISIBLE : View.GONE);
        mServiceEditText.setText(mCurrentAccountData.getService());
        mAccountEditText.setText(mCurrentAccountData.getAccount());
        setSelectableGroups();
        setShowOtpAttributesVisibility(! mCurrentAccountData.inDatabase());
        final String otp_type = mCurrentAccountData.getOtpType();
        ViewUtils.setSelected(this, mOtpTypeContainer, otp_type);
        mSecretEditText.setText(mCurrentAccountData.getSecret());
        ViewUtils.setSelected(this, mDigitsContainer, String.valueOf(mCurrentAccountData.getOtpLength()));
        ViewUtils.setSelected(this, mAlgorithmContainer, mCurrentAccountData.getAlgorithm());
        mPeriodEditText.setText(String.valueOf(mCurrentAccountData.getPeriod()));
        mCounterEditText.setText(String.valueOf(mCurrentAccountData.getCounter()));
        setOtpTypeDependencies(otp_type);
        mContents.setVisibility(View.VISIBLE);
    }

    // This function is triggered when database data that is needed to edit an account ha been loaded

    @Override
    public void onAccountEditionNeededDataLoaded(@Nullable final List<TwoFactorServerIdentity> server_identities, @Nullable final Map<Long, List<TwoFactorGroup>> groups, @Nullable TwoFactorAccount account) {
        if (! isFinishedOrFinishing()) {
            mDataLoader = null;
            if ((account == null) || Lists.isEmptyOrNull(server_identities)) {
                UI.showToast(this, R.string.error_while_loading_account_data);
                finish();
                return;
            }
            mServerIdentities = server_identities;
            mGroups = (groups == null) ? new HashMap<Long, List<TwoFactorGroup>>() : groups;
            mInitialAccountData = account;
            mInitialAccountData.setOtpType(account.inDatabase() ? account.getOtpType() : null);
            if (! mInitialAccountData.hasServerIdentity()) { mInitialAccountData.setServerIdentity(mServerIdentities.get(0)); }
            mDeleteOrUndeleteAccountDataButton.setImageResource(mInitialAccountData.isDeleted() ? R.drawable.ic_actionbar_undelete : R.drawable.ic_actionbar_delete);
            mCurrentAccountData = new TwoFactorAccount(mInitialAccountData);
            mEditing = ! mCurrentAccountData.inDatabase();
            mEditOrSaveAccountDataButton.setImageResource(mEditing ? R.drawable.ic_actionbar_accept : R.drawable.ic_actionbar_edit);
            mToggleSubmenuVisibilityButton.setVisibility(mEditing ? View.GONE : View.VISIBLE);
            if (mEditing) { for (final FloatingActionButton button : mSubmenuButtons) { button.setVisibility(View.GONE); } }
            setEditableAccountData();
            setViewsAvailability(mEditing);
            setButtonsAvailability();
        }
    }

    // Disables (or reenables) the form inputs when data is trying to be updated or already updated

    private void setViewsAvailability(final boolean enable) {
        mEditOrSaveAccountDataButton.setEnabled(enable && mEditing);
        mToggleSubmenuVisibilityButton.setEnabled(enable && mEditing);
        mDeleteOrUndeleteAccountDataButton.setEnabled(enable && mEditing);
        mServerIdentitySpinner.setEnabled(enable && mEditing);
        mServiceEditText.setEnabled(enable && mEditing);
        mAccountEditText.setEnabled(enable && mEditing);
        mGroupSpinner.setEnabled(enable && mEditing);
        mSelectIconButton.setEnabled(enable && mEditing);
        mDeleteIconButton.setEnabled(false);
        mCopyIconToServerButton.setEnabled(false);
        if (enable && mEditing && mCurrentAccountData.hasIcon() && mCurrentAccountData.getIcon().hasBitmaps(this)) {
            mDeleteIconButton.setEnabled(true);
            mCopyIconToServerButton.setEnabled(API.ICON_SOURCE_DASHBOARD.equals(mCurrentAccountData.getIcon().getSource()));
        }
        ViewUtils.setEnabled(this, mOtpTypeContainer, enable && mEditing);
        mSecretEditText.setInputType(enable && mEditing ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD : InputType.TYPE_TEXT_VARIATION_PASSWORD);
        mSecretEditText.setTransformationMethod(enable && mEditing ? null : new PasswordTransformationMethod());
        mSecretEditText.setEnabled(enable && mEditing);
        mCopySecretButton.setEnabled((mWorking.getVisibility() != View.VISIBLE) && (! Strings.isEmptyOrNull(mCurrentAccountData.getSecret())));
        ViewUtils.setEnabled(this, mDigitsContainer, enable && mEditing);
        ViewUtils.setEnabled(this, mAlgorithmContainer, enable && mEditing);
        mPeriodEditText.setEnabled(enable && mEditing);
        mCounterEditText.setEnabled(enable && mEditing);
    }

    private void onSyncingDataStarted() {
        mWorking.setVisibility(View.VISIBLE);
        mContents.setAlpha(Constants.BLUR_ALPHA);
        setViewsAvailability(false);
    }

    private void onSyncingDataFinished() {
        mWorking.setVisibility(View.GONE);
        mContents.setAlpha(1.0f);
        setViewsAvailability(true);
    }

    // Adds a group to the list of groups

    private List<TwoFactorGroup> getGroupsBySelectedServerIdentity() {
        final int selected_server_identity_index = mServerIdentitySpinner.getSelectedItemPosition();
        final long selected_identity_row_id = (selected_server_identity_index >= 0) ? mServerIdentities.get(selected_server_identity_index).getRowId() : -1;
        return (selected_server_identity_index < 0) ? null : mGroups.get(selected_identity_row_id);
    }

    private void addGroup() {
        final int selected_server_identity_index = mServerIdentitySpinner.getSelectedItemPosition();
        if (selected_server_identity_index >= 0) {
            UI.showEditTextDialog(this, R.string.add_group_dialog_title, R.string.add_group_dialog_message, "", 0, Constants.GROUP_NAME_VALID_REGEXP, R.string.accept, R.string.cancel, new UI.OnTextEnteredListener() {
                @Override
                public void onTextEntered(@NotNull final String name) {
                    final TwoFactorServerIdentity current_server_identity = mServerIdentities.get(selected_server_identity_index);
                    if (TwoFactorGroupsUtils.indexOf(mGroups.get(current_server_identity.getRowId()), name) < 0) {
                        final TwoFactorGroup group = new TwoFactorGroup();
                        group.setServerIdentity(current_server_identity);
                        group.setName(name);
                        mGroups.computeIfAbsent(current_server_identity.getRowId(), k -> new ArrayList<TwoFactorGroup>()).add(group);
                        mCurrentAccountData.setGroup(group);
                        mCurrentAccountData.setStatus(TwoFactorAccount.STATUS_NOT_SYNCED);
                        mGroupSpinner.setSelection(mGroups.get(current_server_identity.getRowId()).size() - 1 + 2);
                        setSelectableGroups();
                    }
                }
            });
        }
    }

    // Clones current account data

    private void cloneAccountData() {
        final String otp_type = mCurrentAccountData.getOtpType();
        final TwoFactorAccount account = new TwoFactorAccount(mCurrentAccountData);
        account.setService(getString(R.string.cloned_service, account.hasService() ? account.getService() : ""));
        account.setRowId(-1);
        account.setRemoteId(0);
        if (account.hasIcon()) {
            final TwoFactorIcon new_icon = new TwoFactorIcon(), source_icon = account.getIcon();
            new_icon.setSourceData(null, null);
            new_icon.setBitmaps(source_icon.getBitmap(this, null), source_icon.getBitmap(this, TwoFactorIcon.BitmapTheme.DARK), source_icon.getBitmap(this, TwoFactorIcon.BitmapTheme.LIGHT));
            account.setIcon(new_icon);
        }
        account.setStatus(TwoFactorAccount.STATUS_NOT_SYNCED);
        onAccountEditionNeededDataLoaded(mServerIdentities, mGroups, account);
        mCurrentAccountData.setOtpType(otp_type);
        ViewUtils.setSelected(this, mOtpTypeContainer, otp_type);
        setOtpTypeDependencies(otp_type);
    }

    // Shows or not advanced data (OTP type, secret, period...)
    // This rarely will be changed

    private void toggleOtpAttributesVisibility() {
        final boolean opt_attributes_layout_will_be_visible = (mOtpAttributesLayout.getVisibility() == View.GONE);
        if (opt_attributes_layout_will_be_visible) { mAuthenticator.authenticate(this, AuthenticatedActions.SHOW_ADVANCED_DATA); }
        else { setShowOtpAttributesVisibility(false); }
    }

    private void setShowOtpAttributesVisibility(final boolean show) {
        mToggleOtpGenerationAttributesButton.setText(show ? R.string.hide_otp_generation_attributes : R.string.show_otp_generation_attributes);
        Drawable drawable = ContextCompat.getDrawable(this, show ? R.drawable.ic_actionbar_opened : R.drawable.ic_actionbar_closed);
        if (drawable != null) { drawable = DrawableCompat.wrap(drawable); DrawableCompat.setTintList(drawable, mToggleOtpGenerationAttributesButton.getTextColors()); }
        mToggleOtpGenerationAttributesButton.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
        mOtpAttributesLayout.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    // Functions related with the save process

    @Override
    public void onDataSaved(@NotNull final TwoFactorAccount account, final boolean success, final boolean synced) {
        if (! isFinishedOrFinishing()) {
            UI.showToast(this, success ? synced ? R.string.account_data_has_been_saved_and_synced : R.string.account_data_has_been_saved_but_not_synced : R.string.error_while_saving_account_data);
            if (success) { setResult(Activity.RESULT_OK); }
            mInitialAccountData = new TwoFactorAccount(mCurrentAccountData);
            onSyncingDataFinished();
            disableEdition();
            setButtonsAvailability();
        }
    }

    private void onSaveDataConfirmed() {
        onSyncingDataStarted();
        SaveAccountData.getBackgroundTask(this, mCurrentAccountData, this).start();
    }

    private void saveData() {
        if (mInitialAccountData.inDatabase() && mInitialAccountData.isRemote() && (mInitialAccountData.getServerIdentity().getRowId() != mCurrentAccountData.getServerIdentity().getRowId())) {
            UI.showConfirmDialog(this, R.string.server_identity_changed_message, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    onSaveDataConfirmed();
                }
            });
        }
        else {
            onSaveDataConfirmed();
        }
    }

    // Functions related with the delete (and undelete) process

    @Override
    public void onDataDeleted(final boolean success, final boolean synced) {
        if (! isFinishedOrFinishing()) {
            UI.showToast(this, success ? synced ? R.string.account_data_has_been_deleted_and_synced : R.string.account_data_has_been_deleted_but_not_synced : R.string.error_while_deleting_account_data);
            if (success) {
                if (synced) {
                    setResult(Activity.RESULT_OK);
                    finish();
                }
                else {
                    UI.animateIconChange(mDeleteOrUndeleteAccountDataButton, R.drawable.ic_actionbar_undelete, Constants.BUTTON_SHOW_OR_HIDE_ANIMATION_DURATION);
                    onSyncingDataFinished();
                    setButtonsAvailability();
                }
            }
            else {
                onSyncingDataFinished();
            }
        }
    }

    @Override
    public void onDataUndeleted(final boolean success) {
        if (! isFinishedOrFinishing()) {
            UI.showToast(this, success ? R.string.account_data_has_been_undeleted : R.string.error_while_undeleting_account_data);
            if (success) {
                setResult(Activity.RESULT_OK);
                UI.animateIconChange(mDeleteOrUndeleteAccountDataButton, R.drawable.ic_actionbar_delete, Constants.BUTTON_SHOW_OR_HIDE_ANIMATION_DURATION);
            }
            onSyncingDataFinished();
            setButtonsAvailability();
        }
    }

    private void onDeleteOrUndeleteDataConfirmed() {
        onSyncingDataStarted();
        ToggleAccountDataDeletionState.getBackgroundTask(getActivity(), mCurrentAccountData, EditAccountDataActivity.this).start();
    }

    private void deleteOrUnDeleteData() {
        if (! mCurrentAccountData.isDeleted()) {
            UI.showConfirmDialog(this, R.string.delete_account_data_message, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mAuthenticator.authenticate(EditAccountDataActivity.this, AuthenticatedActions.DELETE_ACCOUNT);
                }
            });
        }
        else {
            onDeleteOrUndeleteDataConfirmed();
        }
    }

    // Functions related to the show QR code process

    @Override
    public void onQRLoaded(@Nullable final String qr) {
        if (! isFinishedOrFinishing()) {
            onSyncingDataFinished();
            setButtonsAvailability();
            mAccountQR = qr;
            if (mAccountQR == null) { UI.showToast(this, R.string.error_retrieving_qr_code); }
            else { mAuthenticator.authenticate(this, AuthenticatedActions.SHOW_QR); }
        }
    }

    private void showQR() {
        onSyncingDataStarted();
        LoadAccountQR.getBackgroundTask(mCurrentAccountData, EditAccountDataActivity.this).start();
    }
}
