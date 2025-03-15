package com.twofauth.android.main_activity;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;

import com.twofauth.android.BaseActivity;
import com.twofauth.android.Constants;
import com.twofauth.android.R;
import com.twofauth.android.StringUtils;
import com.twofauth.android.UiUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DataFilterer extends Thread
{
    public interface OnDataFilteredListener {
        public abstract void onDataFilterSuccess(boolean any_filter_applied);
        public abstract void onDataFilterError();
    }

    private class DataFiltererDisplayer implements Runnable, BaseActivity.SynchronizedCallback {
        private boolean mSuccess;
        private final List<JSONObject> mItems;

        DataFiltererDisplayer(final boolean success, @Nullable List<JSONObject> items) {
            mSuccess = success;
            mItems = items;
        }

        private void displayGroupsBar() {
            final Resources resources = mActivity.getResources();
            final int active_text_color = resources.getColor(R.color.accent_foreground, mActivity.getTheme()), not_active_textcolor = UiUtils.getSystemColor(mActivity, android.R.attr.textColorSecondary);
            for (int i = 0; i < mGroupsBar.getChildCount(); i ++) {
                final View group_view = mGroupsBar.getChildAt(i), group_textview = group_view.findViewById(R.id.group);
                final boolean is_active = ((TextView) group_textview).getText().toString().equals(mActiveGroup);
                group_view.setBackground(is_active ? AppCompatResources.getDrawable(mActivity, R.drawable.border_frame_solid) : AppCompatResources.getDrawable(mActivity, R.drawable.border_frame_transparent));
                ((TextView) group_textview).setTextColor(is_active ? active_text_color : not_active_textcolor);
            }
        }

        @Override
        public Object synchronizedCode(Object object)
        {
            mAdapter.setItems(mItems);
            displayGroupsBar();
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
                Log.e(Constants.LOG_TAG_NAME, "Exception while trying to display filtered data", e);
            }
            finally {
                if (mSuccess) {
                    mListener.onDataFilterSuccess((mActiveGroup != null) && (! mActiveGroup.isEmpty()) && (mFilter != null) && (! mFilter.isEmpty()));
                }
                else {
                    mListener.onDataFilterError();
                }
            }
        }
    }
    private final BaseActivity mActivity;
    private final MainActivityRecyclerAdapter mAdapter;
    private final ViewGroup mGroupsBar;

    private final List<JSONObject> mItems;
    private final String mActiveGroup;

    private final String mFilter;

    private final OnDataFilteredListener mListener;

    public DataFilterer(@NotNull final BaseActivity activity, @NotNull final MainActivityRecyclerAdapter adapter, @NotNull final ViewGroup groups_bar, @NotNull final List<JSONObject> items, @Nullable final String active_group, @Nullable final String filter, @NotNull final OnDataFilteredListener listener) {
        mActivity = activity;
        mAdapter = adapter;
        mGroupsBar = groups_bar;
        mItems = items;
        mActiveGroup = active_group;
        mFilter = filter;
        mListener = listener;
    }

    private boolean isVisible(JSONObject object) {
        if ((mActiveGroup == null) || (mActiveGroup.equals(object.optString(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_GROUP_KEY)))) {
            if ((mFilter != null) && (! mFilter.isEmpty())) {
                boolean in = false;
                if (StringUtils.in(object.optString(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_SERVICE_KEY, ""), mFilter, true)) {
                    in = true;
                }
                else if (StringUtils.in(object.optString(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_ACCOUNT_KEY, ""), mFilter, true)) {
                    in = true;
                }
                return in;
            }
            return true;
        }
        return false;
    }
    private List<JSONObject> getItems() {
        List<JSONObject> items = new ArrayList<JSONObject>();
        for (JSONObject object : mItems) {
            if (isVisible(object)) {
                items.add(object);
            }
        }
        return items;
    }

    public void run() {
        boolean success = false;
        List<JSONObject> items = null;
        try {
            items = getItems();
            success = true;
        }
        catch (Exception e) {
            Log.e(Constants.LOG_TAG_NAME, "Exception while trying to filter data", e);
        }
        finally {
            if (! mActivity.isFinishedOrFinishing()) {
                mActivity.runOnUiThread(new DataFiltererDisplayer(success, items));
            }
        }
    }
}
