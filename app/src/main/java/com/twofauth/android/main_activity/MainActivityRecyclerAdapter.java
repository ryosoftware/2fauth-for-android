package com.twofauth.android.main_activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.twofauth.android.Constants;
import com.twofauth.android.R;
import com.twofauth.android.main_activity.recycler_adapter.MainActivityRecyclerAdapterHandler;
import com.twofauth.android.main_activity.recycler_adapter.view_holders.TwoFactorAccountViewHolder.OnViewHolderClickListener;
import com.twofauth.android.main_activity.recycler_adapter.TwoFactorAccountOptions;
import com.twofauth.android.main_activity.recycler_adapter.view_holders.TwoFactorAccountViewHolder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;

public class MainActivityRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements OnViewHolderClickListener {
    private static final int TYPE_2FA_AUTH_ACCOUNT = 1;
    private static final MainActivityRecyclerAdapterHandler mHandler = new MainActivityRecyclerAdapterHandler();

    private TwoFactorAccountOptions mTwoFactorAccountOptions = null;

    private final List<JSONObject> mItems = new ArrayList<JSONObject>();

    private final Object mSynchronizationObject = new Object();
    private RecyclerView mRecyclerView = null;
    private View mNotEmptyView = null;
    private View mEmptyView = null;
    private int mActiveAccountPosition = -1;
    private boolean mResumed = false;

    public MainActivityRecyclerAdapter(final boolean resumed) {
        mResumed = resumed;
    }

    public MainActivityRecyclerAdapter() {
        this(false);
    }

    public MainActivityRecyclerAdapter(@Nullable final List<JSONObject> items, final boolean resumed) {
        this(resumed);
        setItems(items);
    }

    public MainActivityRecyclerAdapter(@Nullable final List<JSONObject> items) {
        this(items, false);
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
            mActiveAccountPosition = -1;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NotNull final ViewGroup parent, final int view_type) {
        final Context context = parent.getContext();
        if (view_type == TYPE_2FA_AUTH_ACCOUNT) {
            return TwoFactorAccountViewHolder.newInstance(LayoutInflater.from(context).inflate(R.layout.two_factor_auth_account_data, parent, false), this);
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

    public void setItems(final List<JSONObject> items) {
        synchronized (mSynchronizationObject) {
            mActiveAccountPosition = -1;
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

    public void onOptionsChanged() {
        synchronized (mSynchronizationObject) {
            mTwoFactorAccountOptions = null;
        }
        notifyDataSetChanged();
    }
    public void onClick(int position) {
        synchronized (mSynchronizationObject) {
            final Context context = mRecyclerView.getContext();
            final SharedPreferences preferences = Constants.getDefaultSharedPreferences(context);
            final int older_active_account_position = mActiveAccountPosition;
            mActiveAccountPosition = (older_active_account_position == position) ? -1 : position;
            mHandler.removeRedrawItemEachTimeToTimeMessages();
            //mHandler.sendRedrawItemsMessage(this, new int[] { older_active_account_position, mActiveAccountPosition });
            notifyDataSetChanged();
            if (mActiveAccountPosition != -1) {
                final JSONObject object = getItem(position);
                preferences.edit().putLong(Constants.getTwoFactorAccountLastUseKey(object), System.currentTimeMillis()).apply();
                mHandler.sendRedrawItemTimeToTimeMessage(this, mActiveAccountPosition, TwoFactorAccountViewHolder.getMillisUntilNextOtpCompleteCycle(object));
            }
            else if ((position != 0) && (preferences.getBoolean(Constants.SORT_ACCOUNTS_BY_LAST_USE_KEY, context.getResources().getBoolean(R.bool.sort_accounts_by_last_use_default)))) {
                mItems.add(0, mItems.remove(position));
                notifyItemMoved(position, 0);
            }
        }
    }

    public void onPause() {
        synchronized (mSynchronizationObject) {
            mHandler.removeAllMessages();
            mResumed = false;
            updateViewsVisibility();
        }
    }
    public void onResume() {
        synchronized (mSynchronizationObject) {
            mActiveAccountPosition = -1;
            mResumed = true;
            updateViewsVisibility();
        }
    }
}
