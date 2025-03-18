package com.twofauth.android.main_activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.twofauth.android.Constants;
import com.twofauth.android.ListUtils;
import com.twofauth.android.R;
import com.twofauth.android.RecyclerViewUtils;
import com.twofauth.android.RepeatingEvents;
import com.twofauth.android.RepeatingEvents.OnTick;
import com.twofauth.android.StringUtils;
import com.twofauth.android.main_activity.accounts_list.TwoFactorAccountOptions;
import com.twofauth.android.main_activity.accounts_list.TwoFactorAccountViewHolder;
import com.twofauth.android.main_activity.accounts_list.TwoFactorAccountViewHolder.OnViewHolderClickListener;
import com.twofauth.android.main_activity.AccountsListIndexAdapter.OnIndexEntryClickListener;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements OnViewHolderClickListener, OnIndexEntryClickListener, OnTick {
    private static final int TYPE_2FA_AUTH_ACCOUNT = 1;

    private static final Character NOT_LETTER_ITEMS_ENTRY_VALUE = '#';

    public interface OnOtpCodeVisibleStateChanged {
        public abstract void onOtpCodeBecomesVisible();
        public abstract void onOtpCodeShowAnimated(long interval_until_current_otp_cycle_ends, long cycle_time, boolean current_otp_cycle_ending);
        public abstract void onOtpCodeHidden();
    }

    private static class AccountsListAdapterScroller extends LinearSmoothScroller {
        AccountsListAdapterScroller(@NotNull final Context context) {
            super(context);
        }

        @Override
        protected int getVerticalSnapPreference() {
            return SNAP_TO_START;
        }
    }

    private static class AccountsListScroller extends RecyclerView.OnScrollListener {
        private final AccountsListAdapter mAdapter;

        AccountsListScroller(@NotNull final AccountsListAdapter adapter) {
            mAdapter = adapter;
        }
        @Override
        public void onScrollStateChanged(@NonNull final RecyclerView recycler_view, final int new_state) {
            super.onScrollStateChanged(recycler_view, new_state);

        }

        @Override
        public void onScrolled(@NonNull final RecyclerView recycler_view, final int dx, final int dy) {
            super.onScrolled(recycler_view, dx, dy);
            mAdapter.onFirstVisibleItemChanged();
        }
    }
    private final Object mSynchronizationObject = new Object();
    private TwoFactorAccountOptions mTwoFactorAccountOptions = null;
    private final List<JSONObject> mItems = new ArrayList<JSONObject>();
    private final AccountsListIndexAdapter mAccountsListIndexAdapter;
    private final AccountsListScroller mAccountsListScroller = new AccountsListScroller(this);
    private final OnOtpCodeVisibleStateChanged mOnOtpCodeVisibleStateChanged;
    private RecyclerView mRecyclerView = null;
    private View mNotEmptyView = null;
    private View mEmptyView = null;
    private int mActiveAccountPosition = RecyclerView.NO_POSITION;
    private boolean mResumed = false;
    private final int mRepeatingEventsIdentifier = RepeatingEvents.obtainIdentifier();

    private AccountsListAdapterScroller mAccountsListAdapterScroller = null;

    public AccountsListAdapter(@NotNull final OnOtpCodeVisibleStateChanged on_otp_visible_state_changes, @Nullable final AccountsListIndexAdapter accounts_list_index_adapter, final boolean resumed) {
        mOnOtpCodeVisibleStateChanged = on_otp_visible_state_changes;
        mAccountsListIndexAdapter = accounts_list_index_adapter;
        if (mAccountsListIndexAdapter != null) {
            mAccountsListIndexAdapter.setOnIndexClickListener(this);
        }
        mResumed = resumed;
    }

    @Override
    public void onAttachedToRecyclerView(@NotNull final RecyclerView recycler_view) {
        super.onAttachedToRecyclerView(recycler_view);
        synchronized (mSynchronizationObject) {
            mRecyclerView = recycler_view;
            mRecyclerView.addOnScrollListener(mAccountsListScroller);
            updateViewsVisibility();
        }
    }

    @Override
    public void onDetachedFromRecyclerView(@NotNull final RecyclerView recycler_view) {
        super.onDetachedFromRecyclerView(recycler_view);
        synchronized (mSynchronizationObject) {
            mRecyclerView.removeOnScrollListener(mAccountsListScroller);
            mRecyclerView = null;
            onOtpCodeHidden();
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NotNull final ViewGroup parent, final int view_type) {
        final Context context = parent.getContext();
        return TwoFactorAccountViewHolder.newInstance(LayoutInflater.from(context).inflate(R.layout.two_factor_auth_account_item_data, parent, false), this);
    }

    @Override
    public void onBindViewHolder(@NotNull final RecyclerView.ViewHolder view_holder, final int position) {
        synchronized (mSynchronizationObject) {
            if (getItemViewType(position) == TYPE_2FA_AUTH_ACCOUNT) {
                final JSONObject object = getItem(position);
                if (mTwoFactorAccountOptions == null) {
                    mTwoFactorAccountOptions = new TwoFactorAccountOptions(mRecyclerView.getContext());
                }
                ((TwoFactorAccountViewHolder) view_holder).draw(mRecyclerView.getContext(), object, mActiveAccountPosition == position, mActiveAccountPosition != RecyclerView.NO_POSITION, mTwoFactorAccountOptions);
            }
        }
    }

    private Character getItemIndexCharacter(@NotNull final JSONObject object) {
        final char index_entry = object.optString(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_SERVICE_KEY).charAt(0);
        return Character.isLetter(index_entry) ? Character.toUpperCase(index_entry) : NOT_LETTER_ITEMS_ENTRY_VALUE;
    }

    private void setIndexItems(@Nullable final List<JSONObject> items) {
        if (mAccountsListIndexAdapter != null) {
            List<Character> index_items = null;
            if (items != null) {
                final Map<Character, Boolean> index_map = new HashMap<Character, Boolean>();
                for (final JSONObject object : items) {
                    index_map.put(getItemIndexCharacter(object), true);
                }
                index_items = new ArrayList<Character>(index_map.keySet());
                index_items.sort(new Comparator<Character>() {
                    @Override
                    public int compare(@NotNull final Character character1, @NotNull final Character character2) {
                        return StringUtils.compare(character1, character2);
                    }
                });
            }
            mAccountsListIndexAdapter.setItems(index_items);
            onFirstVisibleItemChanged();
        }
    }

    public void setItems(final List<JSONObject> items) {
        synchronized (mSynchronizationObject) {
            onOtpCodeHidden();
            ListUtils.setItems(mItems, items);
            setIndexItems(mItems);
            updateViewsVisibility();
        }
        RecyclerViewUtils.notifyDataSetChanged(this, mRecyclerView);
    }

    private JSONObject getItem(final int position) {
        synchronized (mSynchronizationObject) {
            return ((position >= 0) && (position < mItems.size())) ? mItems.get(position) : null;
        }
    }

    public int getItemViewType(final int position) {
        return TYPE_2FA_AUTH_ACCOUNT;
    }

    @Override
    public int getItemCount() {
        synchronized (mSynchronizationObject) {
            return mItems.size();
        }
    }

    private void updateViewsVisibility() {
        synchronized (mSynchronizationObject) {
            if (mRecyclerView != null) {
                if (mEmptyView != null) {
                    mEmptyView.setVisibility(mItems.isEmpty() && mResumed ? View.VISIBLE : View.GONE);
                }
                if (mNotEmptyView != null) {
                    mNotEmptyView.setVisibility(mItems.isEmpty() || (! mResumed) ? View.GONE : View.VISIBLE);
                }
            }
        }
    }
    public void setViews(@Nullable final View not_empty_view, @Nullable final View empty_view) {
        synchronized (mSynchronizationObject) {
            mNotEmptyView = not_empty_view;
            mEmptyView = empty_view;
            updateViewsVisibility();
        }
    }

    public void onOptionsChanged() {
        synchronized (mSynchronizationObject) {
            mTwoFactorAccountOptions = null;
        }
        RecyclerViewUtils.notifyDataSetChanged(this, mRecyclerView);
    }

    private void onOtpCodeAnimated(final boolean becoming_visible, final JSONObject object) {
        if (becoming_visible) {
            mOnOtpCodeVisibleStateChanged.onOtpCodeBecomesVisible();
        }
        final long interval_until_current_otp_cycle_ends = TwoFactorAccountViewHolder.getMillisUntilNextOtp(object);
        mOnOtpCodeVisibleStateChanged.onOtpCodeShowAnimated(interval_until_current_otp_cycle_ends, TwoFactorAccountViewHolder.getOtpMillis(object), interval_until_current_otp_cycle_ends <= TwoFactorAccountViewHolder.OTP_IS_ABOUT_TO_EXPIRE_TIME);
    }

    private void onOtpCodeHidden(final boolean force) {
        if ((force) || (mActiveAccountPosition != RecyclerView.NO_POSITION)) {
            RepeatingEvents.cancel(mRepeatingEventsIdentifier);
            mOnOtpCodeVisibleStateChanged.onOtpCodeHidden();
            mActiveAccountPosition = RecyclerView.NO_POSITION;
        }
    }

    private void onOtpCodeHidden() {
        onOtpCodeHidden(false);
    }

    private void onFirstVisibleItemChanged() {
        if (mAccountsListIndexAdapter != null) {
            synchronized (mSynchronizationObject) {
                if (mRecyclerView != null) {
                    final JSONObject object = getItem(((LinearLayoutManager) mRecyclerView.getLayoutManager()).findFirstVisibleItemPosition());
                    if (object != null) {
                        mAccountsListIndexAdapter.setActiveIndexEntry(getItemIndexCharacter(object));
                    }
                }
            }
        }
    }
    public void onClick(final char letter) {
        synchronized (mSynchronizationObject) {
            for (int i = 0; i < mItems.size(); i ++) {
                final JSONObject object = mItems.get(i);
                if (getItemIndexCharacter(object) == letter) {
                    if (mAccountsListAdapterScroller == null) {
                        mAccountsListAdapterScroller = new AccountsListAdapterScroller(mRecyclerView.getContext());
                    }
                    mAccountsListAdapterScroller.setTargetPosition(i);
                    mRecyclerView.getLayoutManager().startSmoothScroll(mAccountsListAdapterScroller);
                    break;
                }
            }
        }
    }

    public void onClick(final int position) {
        synchronized (mSynchronizationObject) {
            final Context context = mRecyclerView.getContext();
            final SharedPreferences preferences = Constants.getDefaultSharedPreferences(context);
            final int older_active_account_position = mActiveAccountPosition;
            mActiveAccountPosition = (older_active_account_position == position) ? RecyclerView.NO_POSITION : position;
            if ((older_active_account_position != RecyclerView.NO_POSITION) && (mActiveAccountPosition != RecyclerView.NO_POSITION)) {
                RecyclerViewUtils.notifyItemChanged(this, mRecyclerView, older_active_account_position);
            }
            else {
                RecyclerViewUtils.notifyDataSetChanged(this, mRecyclerView);
            }
            if (mActiveAccountPosition == RecyclerView.NO_POSITION) {
                onOtpCodeHidden(older_active_account_position != RecyclerView.NO_POSITION);
            }
            else {
                final JSONObject object = getItem(position);
                preferences.edit().putLong(Constants.getTwoFactorAccountLastUseKey(object), System.currentTimeMillis()).apply();
                RecyclerViewUtils.notifyItemChanged(this, mRecyclerView, mActiveAccountPosition);
                onOtpCodeAnimated(older_active_account_position == RecyclerView.NO_POSITION, object);
                RepeatingEvents.start(mRepeatingEventsIdentifier, this, DateUtils.SECOND_IN_MILLIS, TwoFactorAccountViewHolder.getMillisUntilNextOtpCompleteCycle(object), object);
            }
        }
    }

    public void onTick(final int identifier, final long start_time, final long end_time, final long elapsed_time, @NotNull final Object object) {
        synchronized (mSynchronizationObject) {
            if (mActiveAccountPosition != RecyclerView.NO_POSITION) {
                if (start_time + elapsed_time < end_time) {
                    RecyclerViewUtils.notifyItemChanged(this, mRecyclerView, mActiveAccountPosition);
                    onOtpCodeAnimated(false, (JSONObject) object);
                }
                else {
                    onClick(RecyclerView.NO_POSITION);
                }
            }
        }
    }
    public void onPause() {
        synchronized (mSynchronizationObject) {
            if (mActiveAccountPosition != RecyclerView.NO_POSITION) {
                onOtpCodeHidden();
            }
            mResumed = false;
            updateViewsVisibility();
        }
    }
    public void onResume() {
        synchronized (mSynchronizationObject) {
            mResumed = true;
            updateViewsVisibility();
        }
    }

    public void copyActiveAccountOtpCodeToClipboard(final Activity activity) {
        synchronized (mSynchronizationObject) {
            if (mActiveAccountPosition != RecyclerView.NO_POSITION) {
                TwoFactorAccountViewHolder.copyToClipboard(activity, getItem(mActiveAccountPosition));
            }
        }
    }
}
