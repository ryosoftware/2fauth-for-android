package com.twofauth.android.main_activity;

import android.app.Activity;
import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import androidx.recyclerview.widget.RecyclerView;

import com.twofauth.android.Constants;
import com.twofauth.android.utils.Lists;
import com.twofauth.android.R;
import com.twofauth.android.utils.RecyclerViews;
import com.twofauth.android.RepeatingEvents;
import com.twofauth.android.RepeatingEvents.OnTickListener;
import com.twofauth.android.utils.Strings;
import com.twofauth.android.database.TwoFactorAccount;
import com.twofauth.android.main_activity.accounts_list.TwoFactorAccountViewHolder;
import com.twofauth.android.main_activity.accounts_list.TwoFactorAccountViewHolder.OnViewHolderClickListener;
import com.twofauth.android.main_activity.AccountsListIndexAdapter.OnIndexEntryClickListener;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements OnViewHolderClickListener, OnIndexEntryClickListener, OnTickListener {
    private static final int TYPE_2FA_AUTH_ACCOUNT = 1;

    public interface OnAccountNeedsToBeSynchronizedListener {
        public abstract void onAccountSynchronizationNeeded(TwoFactorAccount account);
    }

    public interface OnOtpCodeVisibleStateChangedListener {
        public abstract void onOtpCodeBecomesVisible(String otp_type);
        public abstract void onTotpCodeShowAnimated(long interval_until_current_otp_cycle_ends, long cycle_time);
        public abstract void onOtpCodeHidden();
    }

    public interface OnOtpAccountClickListener {
        public abstract void onOtpAccountClick(TwoFactorAccount account);
    }

    private static class AccountsListAdapterScroller extends LinearSmoothScroller {
        AccountsListAdapterScroller(@NotNull final Context context, final int position) {
            super(context);
            setTargetPosition(position);
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
        public void onScrollStateChanged(@NotNull final RecyclerView recycler_view, final int new_state) {
            super.onScrollStateChanged(recycler_view, new_state);

        }

        @Override
        public void onScrolled(@NotNull final RecyclerView recycler_view, final int dx, final int dy) {
            super.onScrolled(recycler_view, dx, dy);
            mAdapter.onFirstVisibleItemChanged();
        }
    }

    private AppearanceOptions mOptions = null;

    private final List<TwoFactorAccount> mItems = new ArrayList<TwoFactorAccount>();

    private final AccountsListIndexAdapter mAccountsListIndexAdapter;
    private final AccountsListScroller mAccountsListScroller = new AccountsListScroller(this);

    private final OnOtpCodeVisibleStateChangedListener mOnOtpCodeVisibleStateChangedListener;
    private final OnAccountNeedsToBeSynchronizedListener mOnAccountNeedsToBeSynchronizedListener;
    private final OnOtpAccountClickListener mOnOtpAccountClickListener;

    private RecyclerView mRecyclerView = null;

    private View mNotEmptyView = null;
    private View mEmptyView = null;

    private int mActiveAccountPosition = RecyclerView.NO_POSITION;

    private boolean mResumed = false;

    private final int mRepeatingEventsIdentifier = RepeatingEvents.obtainIdentifier();

    public AccountsListAdapter(@NotNull final OnOtpCodeVisibleStateChangedListener on_otp_visible_state_changes_listener, @Nullable final OnAccountNeedsToBeSynchronizedListener on_account_needs_to_be_synchronized_listener, @Nullable final OnOtpAccountClickListener on_otp_account_click_listener, @Nullable final AccountsListIndexAdapter accounts_list_index_adapter, final boolean resumed) {
        mOnOtpCodeVisibleStateChangedListener = on_otp_visible_state_changes_listener;
        mOnAccountNeedsToBeSynchronizedListener = on_account_needs_to_be_synchronized_listener;
        mOnOtpAccountClickListener = on_otp_account_click_listener;
        mAccountsListIndexAdapter = accounts_list_index_adapter;
        if (mAccountsListIndexAdapter != null) { mAccountsListIndexAdapter.setOnIndexClickListener(this); }
        mResumed = resumed;
    }

    @Override
    public synchronized void onAttachedToRecyclerView(@NotNull final RecyclerView recycler_view) {
        super.onAttachedToRecyclerView(recycler_view);
        mRecyclerView = recycler_view;
        mRecyclerView.addOnScrollListener(mAccountsListScroller);
        updateViewsVisibility();
    }

    @Override
    public synchronized void onDetachedFromRecyclerView(@NotNull final RecyclerView recycler_view) {
        super.onDetachedFromRecyclerView(recycler_view);
        mRecyclerView.removeOnScrollListener(mAccountsListScroller);
        mRecyclerView = null;
        onOtpCodeHidden();
    }

    @Override
    public @NotNull ViewHolder onCreateViewHolder(@NotNull final ViewGroup parent, final int view_type) {
        return TwoFactorAccountViewHolder.newInstance(LayoutInflater.from(parent.getContext()).inflate(R.layout.two_factor_auth_account_item_data, parent, false), this);
    }

    @Override
    public synchronized void onBindViewHolder(@NotNull final RecyclerView.ViewHolder view_holder, final int position) {
        if (getItemViewType(position) == TYPE_2FA_AUTH_ACCOUNT) {
            final TwoFactorAccount item = getItem(position);
            if (mOptions == null) { mOptions = new AppearanceOptions(mRecyclerView.getContext()); }
            ((TwoFactorAccountViewHolder) view_holder).draw(mRecyclerView.getContext(), item, mActiveAccountPosition == position, mActiveAccountPosition != RecyclerView.NO_POSITION, mOptions);
        }
    }

    private @Nullable Character getItemIndexCharacter(@NotNull final TwoFactorAccount account) {
        if (account.isPinned()) { return AccountsListIndexAdapter.PIN_LETTER; }
        final char index_entry = account.hasService() ? account.getService().charAt(0) : AccountsListIndexAdapter.NOT_LETTER;
        return Character.isLetter(index_entry) ? Character.toUpperCase(index_entry) : AccountsListIndexAdapter.NOT_LETTER;
    }

    private synchronized void setIndexItems(@Nullable final List<TwoFactorAccount> items) {
        if (mAccountsListIndexAdapter != null) {
            List<Character> index = null;
            if (items != null) {
                final Map<Character, Boolean> index_map = new HashMap<Character, Boolean>();
                for (final TwoFactorAccount item : items) {
                    index_map.put(getItemIndexCharacter(item), true);
                }
                index = new ArrayList<Character>(index_map.keySet());
                index.sort(new Comparator<Character>() {
                    @Override
                    public int compare(@NotNull final Character character1, @NotNull final Character character2) {
                        return Strings.compare(character1, character2);
                    }
                });
            }
            mAccountsListIndexAdapter.setItems(index);
            onFirstVisibleItemChanged();
        }
    }

    public synchronized void setItems(final List<TwoFactorAccount> items) {
        onOtpCodeHidden();
        Lists.setItems(mItems, items);
        setIndexItems(mItems);
        updateViewsVisibility();
        RecyclerViews.notifyDataSetChanged(this, mRecyclerView);
    }

    public synchronized void notifyItemChanged(TwoFactorAccount account) {
        final int position = mItems.indexOf(account);
        if (position >= 0) {
            notifyItemChanged(position);
        }
    }

    private @Nullable synchronized TwoFactorAccount getItem(final int position) {
        return ((position >= 0) && (position < mItems.size())) ? mItems.get(position) : null;
    }

    public int getItemViewType(final int position) {
        return TYPE_2FA_AUTH_ACCOUNT;
    }

    @Override
    public synchronized int getItemCount() {
        return mItems.size();
    }

    private synchronized void updateViewsVisibility() {
        if (mRecyclerView != null) {
            if (mEmptyView != null) {
                mEmptyView.setVisibility(mItems.isEmpty() && mResumed ? View.VISIBLE : View.GONE);
            }
            if (mNotEmptyView != null) {
                mNotEmptyView.setVisibility(mItems.isEmpty() || (! mResumed) ? View.GONE : View.VISIBLE);
            }
        }
    }

    public synchronized void setViews(@Nullable final View not_empty_view, @Nullable final View empty_view) {
        mNotEmptyView = not_empty_view;
        mEmptyView = empty_view;
        updateViewsVisibility();
    }

    public synchronized void onOptionsChanged(@Nullable final AppearanceOptions options) {
        mOptions = options;
        RecyclerViews.notifyDataSetChanged(this, mRecyclerView);
    }

    private void onAccountNeedsToBeSynchronized(@NotNull final TwoFactorAccount account) {
        if (mOnAccountNeedsToBeSynchronizedListener != null) {
            mOnAccountNeedsToBeSynchronizedListener.onAccountSynchronizationNeeded(account);
        }
    }

    private void onOtpCodeAnimated(final boolean becoming_visible, final TwoFactorAccount account) {
        if (becoming_visible) {
            if (mOnOtpCodeVisibleStateChangedListener != null) {
                mOnOtpCodeVisibleStateChangedListener.onOtpCodeBecomesVisible(account.getOtpType());
            }
        }
        if (mOnOtpCodeVisibleStateChangedListener != null) {
            mOnOtpCodeVisibleStateChangedListener.onTotpCodeShowAnimated(account.getMillisUntilNextOtp(), account.getOtpMillis());
        }
    }

    private void onOtpCodeHidden(final boolean force) {
        if ((force) || (mActiveAccountPosition != RecyclerView.NO_POSITION)) {
            RepeatingEvents.cancel(mRepeatingEventsIdentifier);
            mActiveAccountPosition = RecyclerView.NO_POSITION;
            if (mOnOtpCodeVisibleStateChangedListener != null) {
                mOnOtpCodeVisibleStateChangedListener.onOtpCodeHidden();
            }
        }
    }

    private void onOtpCodeHidden() {
        onOtpCodeHidden(false);
    }

    private synchronized void onFirstVisibleItemChanged() {
        if ((mAccountsListIndexAdapter != null) && (mRecyclerView != null)) {
            final TwoFactorAccount account = getItem(((LinearLayoutManager) mRecyclerView.getLayoutManager()).findFirstVisibleItemPosition());
            if (account != null) {
                mAccountsListIndexAdapter.setActiveIndexEntry(getItemIndexCharacter(account));
            }
        }
    }

    @Override
    public synchronized void onClick(@NotNull final Character letter) {
        for (int i = 0; i < mItems.size(); i ++) {
            final TwoFactorAccount account = mItems.get(i);
            if (letter.equals(getItemIndexCharacter(account))) {
                mRecyclerView.getLayoutManager().startSmoothScroll(new AccountsListAdapterScroller(mRecyclerView.getContext(), i));
                break;
            }
        }
    }

    @Override
    public synchronized void onClick(final int position) {
        final int older_active_account_position = mActiveAccountPosition;
        onOtpCodeHidden();
        mActiveAccountPosition = (older_active_account_position == position) ? RecyclerView.NO_POSITION : position;
        if ((older_active_account_position != RecyclerView.NO_POSITION) && (mActiveAccountPosition != RecyclerView.NO_POSITION)) { RecyclerViews.notifyItemChanged(this, mRecyclerView, older_active_account_position); }
        else { RecyclerViews.notifyDataSetChanged(this, mRecyclerView); }
        if (mActiveAccountPosition != RecyclerView.NO_POSITION) {
            final TwoFactorAccount account = getItem(position);
            final String otp_type = account.getOtpType();
            account.setLastUse(System.currentTimeMillis());
            if (Constants.OTP_TYPE_HOTP.equals(otp_type)) {
                account.setCounter(account.getCounter() + 1);
                onAccountNeedsToBeSynchronized(account);
            }
            RecyclerViews.notifyItemChanged(this, mRecyclerView, mActiveAccountPosition);
            onOtpCodeAnimated(true, account);
            if (Constants.OTP_TYPE_TOTP.equals(otp_type) || Constants.OTP_TYPE_STEAM.equals(otp_type)) { RepeatingEvents.start(mRepeatingEventsIdentifier, this, DateUtils.SECOND_IN_MILLIS, account.getMillisUntilNextOtpCompleteCycle(), account); }
        }
    }

    @Override
    public void onLongClick(final int position) {
        mOnOtpAccountClickListener.onOtpAccountClick(getItem(position));
    }

    @Override
    public synchronized void onTick(final int identifier, final long start_time, final long end_time, final long elapsed_time, @NotNull final Object object) {
        if (mActiveAccountPosition != RecyclerView.NO_POSITION) {
            if (start_time + elapsed_time < end_time) {
                RecyclerViews.notifyItemChanged(this, mRecyclerView, mActiveAccountPosition);
                onOtpCodeAnimated(false, (TwoFactorAccount) object);
            }
            else {
                onClick(RecyclerView.NO_POSITION);
            }
        }
    }

    public synchronized void onPause() {
        if (mActiveAccountPosition != RecyclerView.NO_POSITION) { onClick(-1); }
        mResumed = false;
        updateViewsVisibility();
    }

    public synchronized void onResume() {
        mResumed = true;
        updateViewsVisibility();
    }

    public synchronized boolean copyActiveAccountOtpCodeToClipboard(final Activity activity) {
        return ((mActiveAccountPosition != RecyclerView.NO_POSITION) && TwoFactorAccountViewHolder.copyToClipboard(activity, getItem(mActiveAccountPosition)));
    }
}
