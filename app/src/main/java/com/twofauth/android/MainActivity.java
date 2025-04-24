package com.twofauth.android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;

import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;

import android.text.Editable;
import android.text.format.DateUtils;
import android.view.View;
import android.view.View.OnClickListener;

import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.twofauth.android.Authenticator.OnAuthenticatorFinishListener;
import com.twofauth.android.api_tasks.SaveAccountData;
import com.twofauth.android.database.TwoFactorAccount;
import com.twofauth.android.database.TwoFactorGroup;
import com.twofauth.android.database.TwoFactorServerIdentity;
import com.twofauth.android.main_activity.AccountsListAdapter.OnOtpAccountClickListener;
import com.twofauth.android.main_activity.AccountsListAdapter.OnOtpCodeVisibleStateChangedListener;
import com.twofauth.android.main_activity.AccountsListAdapter.OnAccountNeedsToBeSynchronizedListener;
import com.twofauth.android.main_activity.AccountsListIndexAdapter;
import com.twofauth.android.main_activity.AppearanceOptions;
import com.twofauth.android.main_activity.accounts_list.TwoFactorAccountViewHolder;
import com.twofauth.android.main_activity.GroupsListAdapter.OnSelectedGroupChangesListener;
import com.twofauth.android.main_activity.tasks.CheckForAppUpdates;
import com.twofauth.android.main_activity.tasks.CheckForAppUpdates.AppVersionData;
import com.twofauth.android.main_activity.tasks.DataLoaderAndFilterer;
import com.twofauth.android.main_activity.tasks.DataLoaderAndFilterer.OnDataLoadedListener;
import com.twofauth.android.main_activity.tasks.DataLoaderAndFilterer.OnDataFilteredListener;
import com.twofauth.android.main_activity.tasks.CheckForAppUpdates.OnCheckForUpdatesListener;
import com.twofauth.android.api_tasks.SaveAccountData.OnDataSavedListener;
import com.twofauth.android.main_activity.GroupsListAdapter;
import com.twofauth.android.MainService.SyncResultType;
import com.twofauth.android.MainService.OnMainServiceStatusChangedListener;

import com.twofauth.android.main_activity.AccountsListAdapter;
import com.twofauth.android.main_activity.FabButtonShowOrHide;
import com.twofauth.android.preferences_activity.MainPreferencesFragment;
import com.twofauth.android.utils.Lists;
import com.twofauth.android.utils.Preferences;
import com.twofauth.android.utils.Strings;
import com.twofauth.android.utils.Threads;
import com.twofauth.android.utils.UI;
import com.twofauth.android.utils.UI.OnSelectionDialogItemSelected;
import com.twofauth.android.utils.Vibrator;

import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ProgressBar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends BaseActivityWithTextController implements OnMainServiceStatusChangedListener, OnOtpCodeVisibleStateChangedListener, OnAccountNeedsToBeSynchronizedListener, OnOtpAccountClickListener, OnSelectedGroupChangesListener, OnDataLoadedListener, OnDataFilteredListener, OnCheckForUpdatesListener, OnDataSavedListener, OnAuthenticatorFinishListener, ActivityResultCallback<ActivityResult>, OnClickListener {
    private static final String LAST_CHECK_FOR_UPDATES_TIME_KEY = "last-check-for-updates-time";
    private static final long CHECK_FOR_UPDATES_INTERVAL = 12 * DateUtils.HOUR_IN_MILLIS;

    private static final String LAST_NOTIFIED_APP_UPDATED_VERSION_KEY = "last-notified-app-updated-version";
    private static final String LAST_NOTIFIED_APP_UPDATED_TIME_KEY = "last-notified-app-updated-time";
    private static final long NOTIFY_SAME_APP_VERSION_UPDATE_INTERVAL = DateUtils.DAY_IN_MILLIS;

    private static final String HOW_INTERACT_WITH_ACCOUNTS_MESSAGE_ALREADY_DISPLAYED_KEY = "how-interact-with-accounts-message-already-displayed";

    private final AccountsListIndexAdapter mAccountsListIndexAdapter = new AccountsListIndexAdapter();
    private final AccountsListAdapter mAccountsListAdapter = new AccountsListAdapter(this, this, this, mAccountsListIndexAdapter, false);;
    private final GroupsListAdapter mGroupsListAdapter = new GroupsListAdapter(this);

    private Thread mDataLoaderAndFilterer = null;

    private long mLastLoadTime = 0;

    private Authenticator mAuthenticator;

    private List<TwoFactorServerIdentity> mServerIdentities = null;
    private Map<Long, List<TwoFactorGroup>> mGroups = null;
    private Map<Long, List<TwoFactorAccount>> mAccounts = null;
    private boolean mAlphaSortedAccounts = false;

    private TwoFactorServerIdentity mActiveServerIdentity = null;
    private TwoFactorGroup mActiveGroup = null;
    private String mText = null;
    private List<TwoFactorAccount> mFilteredAccounts = null;

    private String mStartedActivityForResult;

    private boolean mUnlocked = false;

    private FabButtonShowOrHide mFabButtonShowOrHide;

    private boolean mAnimatingSyncDataButton = false;

    private FloatingActionButton mSyncServerDataButton;
    private FloatingActionButton mOpenAppSettingsButton;
    private FloatingActionButton mCopyToClipboardButton;
    private FloatingActionButton mAddAccountDataButton;
    private RecyclerView mAccountsListRecyclerView;
    private RecyclerView mGroupsListRecyclerView;
    private View mEmptyView;
    private ProgressBar mRemainingOtpTimeProgressBar;
    private View mServerIdentitySelector;
    private EditText mFilterTextEditText;
    private View mAccountsListHeader;
    private View mAccountsListIndexContainer;

    @SuppressLint("CutPasteId")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuthenticator = new Authenticator(this);
        final SharedPreferences preferences = Preferences.getDefaultSharedPreferences(this);
        if (preferences.getBoolean(Constants.DISABLE_SCREENSHOTS_KEY, getResources().getBoolean(R.bool.disable_screenshots))) { getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE); }
        setContentView(R.layout.main_activity);
        mAccountsListHeader = findViewById(R.id.accounts_list_header);
        mAccountsListIndexContainer = findViewById(R.id.accounts_list_index_container);
        final RecyclerView accounts_index_recycler_view = (RecyclerView) findViewById(R.id.accounts_list_index);
        accounts_index_recycler_view.setLayoutManager(new LinearLayoutManager(this));
        accounts_index_recycler_view.setAdapter(mAccountsListIndexAdapter);
        ((SimpleItemAnimator) accounts_index_recycler_view.getItemAnimator()).setSupportsChangeAnimations(false);
        mAccountsListRecyclerView = (RecyclerView) findViewById(R.id.accounts_list);
        final Resources resources = getResources();
        mAccountsListRecyclerView.setLayoutManager(new GridLayoutManager(this, resources.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? preferences.getInt(Constants.ACCOUNTS_LIST_COLUMNS_IN_PORTRAIT_MODE_KEY, resources.getInteger(R.integer.accounts_list_columns_portrait)) : preferences.getInt(Constants.ACCOUNTS_LIST_COLUMNS_IN_LANDSCAPE_MODE_KEY, resources.getInteger(R.integer.accounts_list_columns_landscape))));
        mAccountsListRecyclerView.setAdapter(mAccountsListAdapter);
        ((SimpleItemAnimator) mAccountsListRecyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        mGroupsListRecyclerView = (RecyclerView) findViewById(R.id.groups_list);
        mGroupsListRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mGroupsListRecyclerView.setAdapter(mGroupsListAdapter);
        ((SimpleItemAnimator) mGroupsListRecyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        mGroupsListRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NotNull final Rect out_rect, @NotNull final View view, @NotNull final RecyclerView parent, @NotNull final RecyclerView.State state) {
                super.getItemOffsets(out_rect, view, parent, state);
                out_rect.right = (parent.getChildAdapterPosition(view) == parent.getAdapter().getItemCount() - 1) ? 0 : UI.getPixelsFromDp(getBaseContext(), 10);
            }
        });
        mEmptyView = findViewById(R.id.empty_view);
        mSyncServerDataButton = (FloatingActionButton) findViewById(R.id.sync_server_data);
        mSyncServerDataButton.setOnClickListener(this);
        mOpenAppSettingsButton = (FloatingActionButton) findViewById(R.id.open_app_settings);
        mOpenAppSettingsButton.setOnClickListener(this);
        mCopyToClipboardButton = (FloatingActionButton) findViewById(R.id.copy_to_clipboard);
        mCopyToClipboardButton.setOnClickListener(this);
        mCopyToClipboardButton.setTag(null);
        mAddAccountDataButton = (FloatingActionButton) findViewById(R.id.add_account_data);
        mAddAccountDataButton.setOnClickListener(this);
        mFabButtonShowOrHide = new FabButtonShowOrHide(mAccountsListRecyclerView, false, new FloatingActionButton[] { mSyncServerDataButton, mAddAccountDataButton, mOpenAppSettingsButton }, null, FabButtonShowOrHide.DisplayState.VISIBLE);
        mRemainingOtpTimeProgressBar = (ProgressBar) findViewById(R.id.otp_time);
        mFilterTextEditText = (EditText) findViewById(R.id.filter_text);
        mFilterTextEditText.addTextChangedListener(this);
        mServerIdentitySelector = findViewById(R.id.server_identity_selector);
        mServerIdentitySelector.setOnClickListener(this);
        preferences.edit().remove(Constants.FILTERING_BY_SERVER_IDENTITY_KEY).remove(Constants.FILTERING_BY_GROUP_KEY).apply();
        MainService.startService(this, true);
        if (preferences.getLong(LAST_CHECK_FOR_UPDATES_TIME_KEY, 0) + CHECK_FOR_UPDATES_INTERVAL < System.currentTimeMillis()) { checkForAppUpdates(); }
    }

    @Override
    public void onDestroy() {
        Threads.interrupt(mDataLoaderAndFilterer);
        super.onDestroy();
    }

    // Show or hide functions
    // On Show we try to unlock the device then set the buttons state then, on first show, load data
    // On hide, we lock the app then, if activity is not being finish, we hide accounts (and header and list index)

    private void loadDataOrEnterAppSettings() {
        if (! MainService.canSyncServerData(this) && (mDataLoaderAndFilterer == null)) { openAppSettingsActivity(); }
        loadData();
    }

    @Override
    public void onResumeFirstTime() { loadDataOrEnterAppSettings(); }

    @Override
    public void onResume() {
        super.onResume();
        if (MainService.getLastSyncTime(this) > mLastLoadTime) { loadData(); }
        setListenForKeyboardPresence(true);
        MainService.addListener(this);
        setSyncDataButtonAvailability();
        setAccountsListIndexBounds();
        unlock();
    }

    @Override
    protected void onPauseToHide() {
        super.onPauseToHide();
        mAccountsListAdapter.onPause();
        mAccountsListHeader.setVisibility(View.GONE);
        mAccountsListIndexContainer.setVisibility(View.GONE);
    }

    @Override
    protected void onPause() {
        setListenForKeyboardPresence(false);
        MainService.removeListener(this);
        mUnlocked = false;
        super.onPause();
    }

    // Cosmetic functions to show accounts list index only in portrait mode and keyboard not visible

    private void setAccountsListIndexBounds() {
        final ViewGroup.MarginLayoutParams button_params = (ViewGroup.MarginLayoutParams) mOpenAppSettingsButton.getLayoutParams();
        int button_height = UI.getHeight(mOpenAppSettingsButton) + button_params.topMargin + button_params.bottomMargin, number_of_visible_buttons = 3;
        if (mCopyToClipboardButton.getTag() != null) { number_of_visible_buttons ++; }
        ViewGroup.MarginLayoutParams list_index_container_params = (ViewGroup.MarginLayoutParams) mAccountsListIndexContainer.getLayoutParams();
        list_index_container_params.bottomMargin = number_of_visible_buttons * button_height + button_params.topMargin + button_params.bottomMargin;
        mAccountsListIndexContainer.setLayoutParams(list_index_container_params);
    }

    private void setAccountsListIndexVisibility() {
        final boolean accounts_list_index_container_will_be_visible = ((! Lists.isEmptyOrNull(mFilteredAccounts)) && mAlphaSortedAccounts && mUnlocked && (! mKeyboardVisible) && UI.isInPortraitMode(this));
        if (! accounts_list_index_container_will_be_visible) { UI.animateShowOrHide(mAccountsListIndexContainer, false, Constants.BUTTON_SHOW_OR_HIDE_ANIMATION_DURATION); }
        mFabButtonShowOrHide.setOtherViews(accounts_list_index_container_will_be_visible ? new View[] { mAccountsListIndexContainer } : null, true);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void onConfigurationChanged(@NotNull final Configuration new_config) {
        final SharedPreferences preferences = Preferences.getDefaultSharedPreferences(this);
        final Resources resources = getResources();
        super.onConfigurationChanged(new_config);
        ((GridLayoutManager) mAccountsListRecyclerView.getLayoutManager()).setSpanCount(UI.isInPortraitMode(this) ? preferences.getInt(Constants.ACCOUNTS_LIST_COLUMNS_IN_PORTRAIT_MODE_KEY, resources.getInteger(R.integer.accounts_list_columns_portrait)) : preferences.getInt(Constants.ACCOUNTS_LIST_COLUMNS_IN_LANDSCAPE_MODE_KEY, resources.getInteger(R.integer.accounts_list_columns_landscape)));
        mAccountsListRecyclerView.getAdapter().notifyDataSetChanged();
        setAccountsListIndexVisibility();
        setAccountsListIndexBounds();
    }

    @Override
    protected void onKeyboardVisibilityChange(final boolean visible) { setAccountsListIndexVisibility(); }

    // Functions related to app unlock (PIN / Fingerprint)
    // Unlock function is triggered each time activity becomes visible.
    // If activity is locked and a lock is enabled, we show the unlock dialog.
    // If authentication succeeded, we show accounts list (and headers and, if available, the list index)

    public void onAuthenticationSuccess(@Nullable final Object object) {
        mUnlocked = true;
        mAccountsListAdapter.onResume();
        mAccountsListHeader.setVisibility((mAccounts != null) && (! Lists.isEmptyOrNull(mAccounts.get(-1L))) ? View.VISIBLE : View.GONE);
        setAccountsListIndexVisibility();
        final SharedPreferences preferences = Preferences.getDefaultSharedPreferences(this);
        if (! preferences.getBoolean(HOW_INTERACT_WITH_ACCOUNTS_MESSAGE_ALREADY_DISPLAYED_KEY, false)) {
            preferences.edit().putBoolean(HOW_INTERACT_WITH_ACCOUNTS_MESSAGE_ALREADY_DISPLAYED_KEY, true).apply();
            UI.showMessageDialog(this, R.string.how_interact_with_accounts);
        }
    }

    public void onAuthenticationError(@Nullable final Object object) { finish(); }

    private void unlock() {
        if (mUnlocked) { onAuthenticationSuccess(null); }
        else { mAuthenticator.authenticate(this, null); }
    }

    // User interaction

    @Override
    public void onClick(@NotNull final View view) {
        final int view_id = view.getId();
        final boolean has_vibrated = Vibrator.vibrate(this, view_id == R.id.copy_to_clipboard ? Constants.LONG_HAPTIC_FEEDBACK : Constants.NORMAL_HAPTIC_FEEDBACK);
        if (view_id == R.id.server_identity_selector) { showSelectServerIdentityDialog(); }
        else if (view_id == R.id.sync_server_data) { MainService.startService(this, mActiveServerIdentity); }
        else if (view_id == R.id.add_account_data) { openAddAccountDataActivity(); }
        else if (view_id == R.id.open_app_settings) { openAppSettingsActivity(); }
        else if ((view_id == R.id.copy_to_clipboard) && mAccountsListAdapter.copyActiveAccountOtpCodeToClipboard(this) && has_vibrated) { Threads.sleep(Constants.LONG_HAPTIC_FEEDBACK); }
    }

    // Open settings after animate the button (animations availability is enabled or disabled at UI class)

    private void openAppSettingsActivity() {
        UI.startRotationAnimation(mOpenAppSettingsButton, Constants.BUTTON_CLICK_ANIMATION_BEFORE_START_ACTION_DEGREES, Constants.BUTTON_360_DEGREES_ROTATION_ANIMATION_DURATION, new UI.OnAnimationEndListener() {
            @Override
            public void onAnimationEnd(View view) {
                startActivityForResult(PreferencesActivity.class);
            }
        });
    }

    // Open add account data activity after animate the button (animations availability is enabled or disabled at UI class)

    private void openAddAccountDataActivity() {
        UI.startRotationAnimation(mAddAccountDataButton, Constants.BUTTON_CLICK_ANIMATION_BEFORE_START_ACTION_DEGREES, Constants.BUTTON_360_DEGREES_ROTATION_ANIMATION_DURATION, new UI.OnAnimationEndListener() {
            @Override
            public void onAnimationEnd(View view) {
                startActivityForResult(AddAccountDataActivity.class);
            }
        });
    }

    // Saves account data, in a separate Thread, when account needs to be saved (HOTP account and counter increased)
    // If sync immediately is enabled, the server synchronization process is also done by the thread

    @Override
    public void onAccountSynchronizationNeeded(@NotNull final TwoFactorAccount account) {
        SaveAccountData.getBackgroundTask(this, account, null).start();
    }

    @Override
    public void onDataSaved(@NotNull final TwoFactorAccount account, final boolean success, final boolean synced) {
        if ((! isFinishedOrFinishing()) && success) { mAccountsListAdapter.notifyItemChanged(account); }
    }

    // Events related to OTP codes

    @Override
    public void onOtpCodeBecomesVisible(@NotNull final String otp_type) {
        mRemainingOtpTimeProgressBar.setVisibility(TwoFactorAccount.isHotp(otp_type) ? View.INVISIBLE : View.VISIBLE);
        if (mFabButtonShowOrHide.getDisplayState() != FabButtonShowOrHide.DisplayState.HIDDEN) { UI.animateShowOrHide(mCopyToClipboardButton, true, Constants.BUTTON_SHOW_OR_HIDE_ANIMATION_DURATION); }
        mFabButtonShowOrHide.setFloatingActionButtons(new FloatingActionButton[] { mSyncServerDataButton, mAddAccountDataButton, mOpenAppSettingsButton, mCopyToClipboardButton });
        mCopyToClipboardButton.setTag(true);
        setAccountsListIndexBounds();
    }

    @Override
    public void onTotpCodeShowAnimated(final long interval_until_current_otp_cycle_ends, final long cycle_time) {
        mRemainingOtpTimeProgressBar.setProgress(Math.max(0, (int) ((100 * interval_until_current_otp_cycle_ends) / cycle_time)));
        mRemainingOtpTimeProgressBar.setProgressTintList(ColorStateList.valueOf(getResources().getColor(interval_until_current_otp_cycle_ends < TwoFactorAccountViewHolder.OTP_IS_ABOUT_TO_EXPIRE_TIME ? R.color.otp_visible_last_seconds : interval_until_current_otp_cycle_ends < TwoFactorAccountViewHolder.OTP_IS_NEAR_TO_ABOUT_TO_EXPIRE_TIME ? R.color.otp_visible_near_of_last_seconds : R.color.otp_visible_normal, getTheme())));
    }

    @Override
    public void onOtpCodeHidden() {
        mRemainingOtpTimeProgressBar.setVisibility(View.INVISIBLE);
        mFabButtonShowOrHide.setFloatingActionButtons(new FloatingActionButton[] { mSyncServerDataButton, mAddAccountDataButton, mOpenAppSettingsButton });
        UI.animateShowOrHide(mCopyToClipboardButton, false, Constants.BUTTON_SHOW_OR_HIDE_ANIMATION_DURATION, new UI.OnAnimationEndListener() {
            @Override
            public void onAnimationEnd(View view) {
                mCopyToClipboardButton.setTag(null);
                setAccountsListIndexBounds();
            }
        });
    }

    @Override
    public void onOtpAccountClick(@NotNull final TwoFactorAccount account) {
        final Bundle bundle = new Bundle();
        bundle.putLong(EditAccountDataActivity.EXTRA_ACCOUNT_ID, account.getRowId());
        startActivityForResult(EditAccountDataActivity.class, bundle);
    }

    // Set Fab buttons availability

    private void setSyncDataButtonAvailability() {
        if (! isFinishedOrFinishing()) {
            final boolean syncing_or_loading_data = ((mDataLoaderAndFilterer != null) || MainService.isRunning(this)), can_sync_data = MainService.canSyncServerData(this);
            mSyncServerDataButton.setEnabled((! syncing_or_loading_data) && can_sync_data);
            mAddAccountDataButton.setEnabled(can_sync_data);
            if (syncing_or_loading_data && (! mAnimatingSyncDataButton)) { UI.startInfiniteRotationAnimationLoop(mSyncServerDataButton, Constants.BUTTON_360_DEGREES_ROTATION_ANIMATION_DURATION); }
            else if ((! syncing_or_loading_data) && (mAnimatingSyncDataButton)) { mSyncServerDataButton.clearAnimation(); }
            mAnimatingSyncDataButton = syncing_or_loading_data;
        }
    }

    // Events related to the synchronization process
    // When synchronization process ends we launch load data process then change the buttons (related to the sync process) availability

    public void onServiceStarted() {
        setSyncDataButtonAvailability();
    }

    public void onServiceFinished(@Nullable final SyncResultType result_type) {
        setSyncDataButtonAvailability();
        loadData();
    }

    // Functions related to the data load and filter process
    // This process is started first time app in launched and each time synchronization ends
    // If load data ends with success, we clear filter options and list adapter options
    // In any case, we set the buttons availability and list index accordly to current state

    private void loadData() {
        Threads.interrupt(mDataLoaderAndFilterer);
        mDataLoaderAndFilterer = DataLoaderAndFilterer.getBackgroundTask(this, mActiveServerIdentity, mActiveGroup, mText, this, this);
        setSyncDataButtonAvailability();
        mDataLoaderAndFilterer.start();
    }

    @Override
    public void onDataLoadError() {
        int i = 0;
        int j = i;
        int k = j;
    }

    @Override
    public void onDataLoadSuccess(@Nullable final List<TwoFactorServerIdentity> server_identities, @Nullable final Map<Long, List<TwoFactorGroup>> groups, @Nullable final Map<Long, List<TwoFactorAccount>> accounts, final boolean alpha_sorted_accounts) {
        if (! isFinishedOrFinishing()) {
            mLastLoadTime = MainService.getLastSyncTime(this);
            mServerIdentities = server_identities;
            mGroups = groups;
            mAccounts = accounts;
            mAlphaSortedAccounts = alpha_sorted_accounts;
            mServerIdentitySelector.setVisibility(Lists.isSizeGreaterOrEqualsTo(mServerIdentities, 2) ? View.VISIBLE : View.GONE);
            mAccountsListHeader.setVisibility(((mAccounts != null) && (! Lists.isEmptyOrNull(mAccounts.get(-1L)))) && mUnlocked ? View.VISIBLE : View.GONE);
        }
    }

    // Filter process follows the load data process, but also will be manually started
    // Note that, on filter success, empty-view and not-empty-view are the same if any filter applied

    private void filterData() {
        Threads.interrupt(mDataLoaderAndFilterer);
        if (mAccounts != null) { (mDataLoaderAndFilterer = DataLoaderAndFilterer.getBackgroundTask(this, mAccounts.get(-1L), mActiveServerIdentity, mActiveGroup, mText, this)).start(); }
    }

    @Override
    public void onDataFiltered(final boolean any_filter_applied, @Nullable final List<TwoFactorAccount> accounts) {
        if (! isFinishedOrFinishing()) {
            mAccountsListAdapter.setItems(mFilteredAccounts = accounts);
            mAccountsListAdapter.setViews(mAccountsListRecyclerView, any_filter_applied ? mAccountsListRecyclerView : mEmptyView);
            final List<TwoFactorGroup> groups = (mActiveServerIdentity == null) ? (mGroups == null) ? null : mGroups.get(-1L) : mGroups.get(mActiveServerIdentity.getRowId());
            mGroupsListAdapter.setItems(groups, mActiveGroup);
            mGroupsListRecyclerView.setVisibility(Lists.isEmptyOrNull(groups) ? View.GONE : View.VISIBLE);
            if (any_filter_applied) { mEmptyView.setVisibility(View.GONE); }
            mDataLoaderAndFilterer = null;
            setAccountsListIndexVisibility();
            setSyncDataButtonAvailability();
        }
    }

    private void showSelectServerIdentityDialog() {
        final List<String> server_identities = new ArrayList<String>();
        server_identities.add(getString(R.string.no_filter_by_server_identity));
        for (final TwoFactorServerIdentity server_identity : mServerIdentities) { server_identities.add(server_identity.getTitle()); }
        UI.showSelectItemFromListDialog(this, 0, R.string.select_a_server_identity_to_filter, server_identities, mActiveServerIdentity == null ? null : mActiveServerIdentity.getTitle(), R.string.accept, R.string.cancel, new OnSelectionDialogItemSelected() {
            @Override
            public void onItemSelected(final int position) {
                final TwoFactorServerIdentity active_server_identity = ((position <= 0) || (position > mServerIdentities.size())) ? null : mServerIdentities.get(position - 1);
                if (mActiveServerIdentity != active_server_identity) {
                    mActiveServerIdentity = active_server_identity;
                    Preferences.getDefaultSharedPreferences(getBaseContext()).edit().putBoolean(Constants.FILTERING_BY_SERVER_IDENTITY_KEY, mActiveServerIdentity != null).apply();
                    onAppearanceOptionsChanged();
                    filterData();
                }
            }
        });
    }

    public void onSelectedGroupChanges(@Nullable final TwoFactorGroup active_group, @Nullable final TwoFactorGroup previous_active_group) {
        final long active_group_id = (active_group == null) ? -1 : active_group.getRowId(), previous_active_group_id = (mActiveGroup == null) ? -1 : mActiveGroup.getRowId();
        if (active_group_id != previous_active_group_id)  {
            mActiveGroup = active_group;
            Preferences.getDefaultSharedPreferences(this).edit().putBoolean(Constants.FILTERING_BY_GROUP_KEY, mActiveGroup != null).apply();
            onAppearanceOptionsChanged();
            filterData();
        }
    }

    @Override
    public void afterTextChanged(@NotNull final Editable editable) {
        final String text = mFilterTextEditText.getText().toString();
        if (! Strings.equals(mText, text, true)) {
            mText = text;
            filterData();
        }
    }

    // Entry point for the related activities launch

    @Override
    protected void startActivityForResult(@NotNull final Class<?> activity_class, @Nullable final Bundle bundle) {
        Main.getInstance().startObservingIfAppBackgrounded();
        mStartedActivityForResult = activity_class.getName();
        super.startActivityForResult(activity_class, bundle);
    }

    // Entry point for the related activities end
    // When the settings activity ends, if changed settings includes server-identities, we try to start synchronization process or, if not possible, we reload accounts data
    // If the activity that ends isn't the settings activity, we reload accounts data
    // Note that, if app has not paused while related activity execution, we maintains the app unlocked

    private void onAppearanceOptionsChanged() {
        final AppearanceOptions options = new AppearanceOptions(this);
        mAccountsListAdapter.onOptionsChanged(options);
        mGroupsListAdapter.onOptionsChanged(options);
    }

    @Override
    public void onActivityResult(@NotNull final ActivityResult result) {
        if ((result.getResultCode() == Activity.RESULT_OK) && (! isFinishedOrFinishing())) {
            if (PreferencesActivity.class.getName().equals(mStartedActivityForResult)) {
                final Intent intent = result.getData();
                if (intent != null) {
                    if (intent.getBooleanExtra(MainPreferencesFragment.SERVER_IDENTITIES_CHANGED, false)) {
                        if (! MainService.isRunning(this)) {
                            if (MainService.canSyncServerData(this)) { MainService.startService(this); }
                            else { loadData(); }
                        }
                    }
                    else if (intent.getBooleanExtra(Constants.SORT_ACCOUNTS_BY_LAST_USE_KEY, false)) {
                        loadData();
                    }
                    onAppearanceOptionsChanged();
                }
            }
            else if (EditAccountDataActivity.class.getName().equals(mStartedActivityForResult) || AddAccountDataActivity.class.getName().equals(mStartedActivityForResult)) {
                loadData();
            }
        }
        mUnlocked = ! Main.getInstance().stopObservingIfAppBackgrounded();
        onConfigurationChanged(getResources().getConfiguration());
    }

    // If back button pressed and we are filtering by any kind of data, we reset the filter
    // In other case, we ends the activity

    @Override
    protected void processOnBackPressed() {
        if ((mActiveServerIdentity != null) || (mActiveGroup != null) || (! mFilterTextEditText.getText().toString().isEmpty())) {
            mFilterTextEditText.setText(null);
            mActiveServerIdentity = null;
            mActiveGroup = null;
            Preferences.getDefaultSharedPreferences(this).edit().remove(Constants.FILTERING_BY_SERVER_IDENTITY_KEY).remove(Constants.FILTERING_BY_GROUP_KEY).apply();
            onAppearanceOptionsChanged();
            filterData();
        }
        else {
            finish();
        }
    }

    // Functions related to the search for updates process
    // If a update is available we show a dialog with info and user decides if update is installed
    // We do not notify the same update all the time but each time to time

    private void checkForAppUpdates() {
        if (Preferences.getDefaultSharedPreferences(this).getBoolean(Constants.AUTO_UPDATE_APP_KEY, getResources().getBoolean(R.bool.auto_update_app))) { CheckForAppUpdates.getBackgroundTask(this, this).start(); }
    }

    public void onCheckForUpdatesFinished(@NotNull final File downloaded_app_file, @NotNull final AppVersionData downloaded_app_version)
    {
        if (! isFinishedOrFinishing()) { onCheckForUpdatesFinished(this, false, downloaded_app_file, downloaded_app_version); }
    }

    public static void onCheckForUpdatesFinished(@NotNull final Activity activity, final boolean force, @NotNull final File downloaded_app_file, @NotNull final AppVersionData downloaded_app_version)
    {
        final SharedPreferences preferences = Preferences.getDefaultSharedPreferences(activity);
        final SharedPreferences.Editor editor = preferences.edit();
        final long now = System.currentTimeMillis();
        editor.putLong(LAST_CHECK_FOR_UPDATES_TIME_KEY, now);
        if ((force) || (downloaded_app_version.code != preferences.getInt(LAST_NOTIFIED_APP_UPDATED_VERSION_KEY, 0)) || (preferences.getLong(LAST_NOTIFIED_APP_UPDATED_TIME_KEY, 0) + NOTIFY_SAME_APP_VERSION_UPDATE_INTERVAL < System.currentTimeMillis())) {
            editor.putInt(LAST_NOTIFIED_APP_UPDATED_VERSION_KEY, downloaded_app_version.code).putLong(LAST_NOTIFIED_APP_UPDATED_TIME_KEY, now);
            UI.showConfirmDialog(activity, activity.getString(R.string.there_is_an_update_version, activity.getString(Main.getInstance().isPreRelease() ? R.string.app_build_version_number_prerelease : R.string.app_build_version_number, activity.getString(R.string.app_version_name_value), activity.getResources().getInteger(R.integer.app_version_number_value)), activity.getString(downloaded_app_version.preRelease ? R.string.app_build_version_number_prerelease : R.string.app_build_version_number, downloaded_app_version.name, downloaded_app_version.code)), R.string.install_now, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    activity.startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(FileProvider.getUriForFile(activity, activity.getPackageName() + ".provider", downloaded_app_file), "application/vnd.android.package-archive").setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION));
                }
            });
        }
        editor.apply();
    }
}
