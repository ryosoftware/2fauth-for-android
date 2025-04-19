package com.twofauth.android.edit_account_data_activity.tasks;

import android.util.Log;

import com.twofauth.android.Main;
import com.twofauth.android.database.TwoFactorAccount;
import com.twofauth.android.database.TwoFactorGroup;
import com.twofauth.android.database.TwoFactorServerIdentity;
import com.twofauth.android.utils.JSON;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoadAccountEditionNeededData {
    public interface OnAccountEditionNeededDataLoadedListener {
        public abstract void onAccountEditionNeededDataLoaded(List<TwoFactorServerIdentity> server_identities, Map<Long, List<TwoFactorGroup>> groups, TwoFactorAccount account);
    }

    private static class LoadAccountEditionNeededDataImplementation implements Main.OnBackgroundTaskExecutionListener {
        private static class TwoFactorGroupUtils {
            private static Map<Long, List<TwoFactorGroup>> toMap(final List<TwoFactorGroup> groups) {
                final Map<Long, List<TwoFactorGroup>> map = new HashMap<Long, List<TwoFactorGroup>>();
                for (final TwoFactorGroup group : groups) {
                    final long id = group.getServerIdentity().getRowId();
                    if (! map.containsKey(id)) { map.put(id, new ArrayList<TwoFactorGroup>()); }
                    map.get(id).add(group);
                }
                map.put(-1L, groups);
                return map;
            }
        }

        private final long mId;
        private final String mData;

        private final OnAccountEditionNeededDataLoadedListener mListener;

        private List<TwoFactorServerIdentity> mServerIdentities;
        private TwoFactorAccount mAccount;
        private Map<Long, List<TwoFactorGroup>> mGroups;

        LoadAccountEditionNeededDataImplementation(final long id, @NotNull final OnAccountEditionNeededDataLoadedListener listener) {
            mId = id;
            mData = null;
            mListener = listener;
        }

        LoadAccountEditionNeededDataImplementation(@Nullable final String data, @NotNull final OnAccountEditionNeededDataLoadedListener listener) {
            mId = -1;
            mData = data;
            mListener = listener;
        }

        @Override
        public @Nullable Object onBackgroundTaskStarted(@Nullable final Object data) {
            try {
                final SQLiteDatabase database = Main.getInstance().getDatabaseHelper().open(false);
                if (database != null) {
                    try {
                        mServerIdentities = Main.getInstance().getDatabaseHelper().getTwoFactorServerIdentitiesHelper().get(database);
                        final List<TwoFactorGroup> groups = Main.getInstance().getDatabaseHelper().getTwoFactorGroupsHelper().get(database);
                        mGroups = (groups == null) ? null : TwoFactorGroupUtils.toMap(groups);
                        mAccount = (mId < 0) ? mData == null ? new TwoFactorAccount() : new TwoFactorAccount(database, JSON.toJSONObject(mData)) : Main.getInstance().getDatabaseHelper().getTwoFactorAccountsHelper().get(database, mId);
                    }
                    finally {
                        Main.getInstance().getDatabaseHelper().close(database);
                    }
                }
            }
            catch (Exception e) {
                Log.e(Main.LOG_TAG_NAME, "Exception while trying to load data needed by Edit Account Activity", e);
            }
            return null;
        }

        @Override
        public void onBackgroundTaskFinished(@Nullable final Object data) {
            mListener.onAccountEditionNeededDataLoaded(mServerIdentities, mGroups, mAccount);
        }
    }

    public static @NotNull Thread getBackgroundTask(final long id, @NotNull OnAccountEditionNeededDataLoadedListener listener) {
        return Main.getInstance().getBackgroundTask(new LoadAccountEditionNeededDataImplementation(id, listener));
    }

    public static @NotNull Thread getBackgroundTask(@Nullable final String data, @NotNull OnAccountEditionNeededDataLoadedListener listener) {
        return Main.getInstance().getBackgroundTask(new LoadAccountEditionNeededDataImplementation(data, listener));
    }
}
