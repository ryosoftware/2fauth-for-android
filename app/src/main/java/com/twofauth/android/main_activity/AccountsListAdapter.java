package com.twofauth.android.main_activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.twofauth.android.Constants;
import com.twofauth.android.ListUtils;
import com.twofauth.android.R;
import com.twofauth.android.RepeatingEvents;
import com.twofauth.android.RepeatingEvents.OnTick;
import com.twofauth.android.main_activity.accounts_list.TwoFactorAccountViewHolder.OnViewHolderClickListener;
import com.twofauth.android.main_activity.accounts_list.TwoFactorAccountOptions;
import com.twofauth.android.main_activity.accounts_list.TwoFactorAccountViewHolder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AccountsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements OnViewHolderClickListener, OnTick {
    private static final int TYPE_2FA_AUTH_ACCOUNT = 1;

    public interface OnOtpCodeVisibleStateChanged {
        public abstract void onOtpCodeBecomesVisible();
        public abstract void onOtpCodeShowAnimated(long interval_until_current_otp_cycle_ends, long cycle_time, boolean current_otp_cycle_ending);
        public abstract void onOtpCodeHidden();
    }

    private final Object mSynchronizationObject = new Object();
    private TwoFactorAccountOptions mTwoFactorAccountOptions = null;
    private final List<JSONObject> mItems = new ArrayList<JSONObject>();

    private final OnOtpCodeVisibleStateChanged mOnOtpCodeVisibleStateChanged;
    private RecyclerView mRecyclerView = null;
    private View mNotEmptyView = null;
    private View mEmptyView = null;
    private int mActiveAccountPosition = -1;
    private boolean mResumed = false;

    private final int mRepeatingEventsIdentifier = RepeatingEvents.obtainIdentifier();

    public AccountsListAdapter(@NotNull final OnOtpCodeVisibleStateChanged on_otp_visible_state_changes, final boolean resumed) {
        mOnOtpCodeVisibleStateChanged = on_otp_visible_state_changes;
        mResumed = resumed;
    }

    public AccountsListAdapter(@NotNull final OnOtpCodeVisibleStateChanged on_otp_visible_state_changes) {
        this(on_otp_visible_state_changes, false);
    }

    public AccountsListAdapter(@NotNull final OnOtpCodeVisibleStateChanged on_otp_visible_state_changes, @Nullable final List<JSONObject> items, final boolean resumed) {
        this(on_otp_visible_state_changes, resumed);
        setItems(items);
    }

    public AccountsListAdapter(@NotNull final OnOtpCodeVisibleStateChanged on_otp_visible_state_changes, @Nullable final List<JSONObject> items) {
        this(on_otp_visible_state_changes, items, false);
    }

    @Override
    public void onAttachedToRecyclerView(@NotNull final RecyclerView recycler_view) {
        super.onAttachedToRecyclerView(recycler_view);
        synchronized (mSynchronizationObject) {
            mRecyclerView = recycler_view;
            updateViewsVisibility();
        }
    }

    @Override
    public void onDetachedFromRecyclerView(@NotNull final RecyclerView recycler_view) {
        super.onDetachedFromRecyclerView(recycler_view);
        synchronized (mSynchronizationObject) {
            mRecyclerView = null;
            if (mActiveAccountPosition != -1) {
                RepeatingEvents.cancel(mRepeatingEventsIdentifier);
                onOtpCodeHidden();
                mActiveAccountPosition = -1;
            }
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NotNull final ViewGroup parent, final int view_type) {
        final Context context = parent.getContext();
        if (view_type == TYPE_2FA_AUTH_ACCOUNT) {
            return TwoFactorAccountViewHolder.newInstance(LayoutInflater.from(context).inflate(R.layout.two_factor_auth_account_item_data, parent, false), this);
        }
        throw new RuntimeException("There is no type that matches the type " + view_type + " + make sure your using types correctly");
    }

    @Override
    public void onBindViewHolder(@NotNull final RecyclerView.ViewHolder view_holder, final int position) {
        synchronized (mSynchronizationObject) {
            if (getItemViewType(position) == TYPE_2FA_AUTH_ACCOUNT) {
                final JSONObject object = getItem(position);
                if (mTwoFactorAccountOptions == null) {
                    mTwoFactorAccountOptions = new TwoFactorAccountOptions(mRecyclerView.getContext());
                }
                ((TwoFactorAccountViewHolder) view_holder).draw(mRecyclerView.getContext(), object, mActiveAccountPosition == position, mActiveAccountPosition != -1, mTwoFactorAccountOptions);
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setItems(final List<JSONObject> items) {
        synchronized (mSynchronizationObject) {
            if (mActiveAccountPosition != -1) {
                RepeatingEvents.cancel(mRepeatingEventsIdentifier);
                onOtpCodeHidden();
                mActiveAccountPosition = -1;
            }
            ListUtils.setItems(mItems, items);
            updateViewsVisibility();
        }
        notifyDataSetChanged();
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
        return mItems.size();
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

    @SuppressLint("NotifyDataSetChanged")
    public void onOptionsChanged() {
        synchronized (mSynchronizationObject) {
            mTwoFactorAccountOptions = null;
        }
        notifyDataSetChanged();
    }

    private void onOtpCodeAnimated(final boolean becoming_visible, final JSONObject object) {
        if (becoming_visible) {
            mOnOtpCodeVisibleStateChanged.onOtpCodeBecomesVisible();
        }
        final long interval_until_current_otp_cycle_ends = TwoFactorAccountViewHolder.getMillisUntilNextOtp(object);
        mOnOtpCodeVisibleStateChanged.onOtpCodeShowAnimated(interval_until_current_otp_cycle_ends, TwoFactorAccountViewHolder.getOtpMillis(object), interval_until_current_otp_cycle_ends <= TwoFactorAccountViewHolder.OTP_IS_ABOUT_TO_EXPIRE_TIME);
    }

    private void onOtpCodeHidden() {
        mOnOtpCodeVisibleStateChanged.onOtpCodeHidden();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void onClick(int position) {
        synchronized (mSynchronizationObject) {
            final Context context = mRecyclerView.getContext();
            final SharedPreferences preferences = Constants.getDefaultSharedPreferences(context);
            final int older_active_account_position = mActiveAccountPosition;
            mActiveAccountPosition = (older_active_account_position == position) ? -1 : position;
            if ((older_active_account_position != -1) && (mActiveAccountPosition != -1)) {
                notifyItemChanged(older_active_account_position);
            }
            else {
                notifyDataSetChanged();
            }
            if (mActiveAccountPosition == -1) {
                RepeatingEvents.cancel(mRepeatingEventsIdentifier);
                onOtpCodeHidden();
            }
            else {
                final JSONObject object = getItem(position);
                preferences.edit().putLong(Constants.getTwoFactorAccountLastUseKey(object), System.currentTimeMillis()).apply();
                onOtpCodeAnimated(older_active_account_position == -1, object);
                RepeatingEvents.start(mRepeatingEventsIdentifier, this, DateUtils.SECOND_IN_MILLIS, TwoFactorAccountViewHolder.getMillisUntilNextOtpCompleteCycle(object), object);
            }
        }
    }

    public void onTick(final int identifier, final long start_time, final long end_time, final long elapsed_time, @NotNull final Object object) {
        synchronized (mSynchronizationObject) {
            if (mActiveAccountPosition != -1) {
                if (start_time + elapsed_time < end_time) {
                    notifyItemChanged(mActiveAccountPosition);
                    onOtpCodeAnimated(false, (JSONObject) object);
                }
                else {
                    onClick(-1);
                }
            }
        }
    }
    public void onPause() {
        synchronized (mSynchronizationObject) {
            if (mActiveAccountPosition != -1) {
                RepeatingEvents.cancel(mRepeatingEventsIdentifier);
                onOtpCodeHidden();
                mActiveAccountPosition = -1;
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
}
