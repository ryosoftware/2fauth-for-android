package com.twofauth.android.main_activity;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.twofauth.android.BaseActivity;
import com.twofauth.android.Constants;
import com.twofauth.android.R;
import com.twofauth.android.StringUtils;
import com.twofauth.android.UiUtils;
import com.twofauth.android.main_service.ServerDataLoader;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DataLoader extends Thread {
    public interface OnDataLoadListener {
        public abstract void onDataLoadSuccess(List<JSONObject> items);
        public abstract void onDataLoadError();
    }

    private class DataLoaderDisplayer implements Runnable, BaseActivity.SynchronizedCallback {
        private boolean mSuccess;
        private final List<JSONObject> mItems;

        private final List<String> mGroups;

        DataLoaderDisplayer(final boolean success, @Nullable final List<JSONObject> items, @Nullable final List<String> groups) {
            mSuccess = success;
            mItems = items;
            mGroups = groups;
        }

        private void displayGroupsBar() {
            final int currently_added_groups = mGroupsBar.getChildCount(), newly_added_groups = (mGroups == null) ? 0 : mGroups.size();
            if (currently_added_groups > newly_added_groups) {
                mGroupsBar.removeViews(newly_added_groups, currently_added_groups - newly_added_groups);
            }
            if (newly_added_groups != 0) {
                final Context context = mGroupsBar.getContext();
                final LayoutInflater layout_inflater = LayoutInflater.from(context);
                final int margin = UiUtils.getPixelsFromDp(context, 10);
                for (int i = 0; i < mGroups.size(); i ++) {
                    if (i >= currently_added_groups) {
                        final View view = layout_inflater.inflate(R.layout.account_group, mGroupsBar, false);
                        final ViewGroup.MarginLayoutParams layout_params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
                        layout_params.setMargins(margin, 0, i == mGroups.size() - 1 ? margin : 0, 0);
                        view.requestLayout();
                        view.setOnClickListener(mOnGroupButtonClickListener);
                        mGroupsBar.addView(view);
                    }
                    ((TextView) mGroupsBar.getChildAt(i).findViewById(R.id.group)).setText(mGroups.get(i));
                }
            }
            mGroupsBar.setVisibility(newly_added_groups == 0 ? View.GONE : View.VISIBLE);
        }

        @Override
        public Object synchronizedCode(Object object)
        {
            displayGroupsBar();
            mAdapter.setItems(mItems);
            return null;
        }

        @Override
        public void run() {
            try {
                if (mSuccess) {
                    mActivity.executeSynchronized(this, null);
                }
            }
            catch (Exception e) {
                mSuccess = false;
                Log.e(Constants.LOG_TAG_NAME, "Exception while trying to display data", e);
            }
            finally {
                if (mSuccess) {
                    mListener.onDataLoadSuccess(mItems);
                }
                else {
                    mListener.onDataLoadError();
                }
            }
        }
    }

    private final BaseActivity mActivity;

    private final MainActivityRecyclerAdapter mAdapter;

    private final ViewGroup mGroupsBar;

    private final View.OnClickListener mOnGroupButtonClickListener;

    private final OnDataLoadListener mListener;

    public DataLoader(@NotNull final BaseActivity activity, @NotNull final MainActivityRecyclerAdapter adapter, @NotNull final ViewGroup groups_bar, @NotNull final View.OnClickListener on_group_button_click_listener, @NotNull OnDataLoadListener listener) {
        mActivity = activity;
        mAdapter = adapter;
        mGroupsBar = groups_bar;
        mOnGroupButtonClickListener = on_group_button_click_listener;
        mListener = listener;
    }

    private List<String> getGroups(@Nullable final List<JSONObject> items) {
        final List<String> groups = new ArrayList<String>();
        if (items != null) {
            for (JSONObject object : items) {
                final String group = object.optString(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_GROUP_KEY);
                if ((! group.isEmpty()) && (! groups.contains(group))) {
                    groups.add(group);
                }
            }
            groups.sort(new Comparator<String>() {
                @Override
                public int compare(String string_1, String string_2) {
                    return StringUtils.compare(string_1, string_2, true);
                }
            });
        }
        return groups;
    }

    public void run() {
        boolean success = false;
        List<JSONObject> items = null;
        List<String>groups = null;
        try {
            items = ServerDataLoader.getTwoFactorAuthCodes(mActivity);
            groups = getGroups(items);
            success = true;
        }
        catch (Exception e) {
            Log.e(Constants.LOG_TAG_NAME, "Exception while trying to display data", e);
        }
        finally {
            if (! mActivity.isFinishedOrFinishing()) {
                mActivity.runOnUiThread(new DataLoaderDisplayer(success, items, groups));
            }
        }
    }
}
