package com.twofauth.android.preferences_activity.tasks;

import android.util.Log;

import com.twofauth.android.Main;
import com.twofauth.android.database.TwoFactorGroup;
import com.twofauth.android.database.TwoFactorServerIdentity;
import com.twofauth.android.utils.Lists;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LoadGroupsData {
    public interface OnGroupsLoadedListener {
        public abstract void onGroupsLoaded(List<TwoFactorGroupWithReferencesInformation> groups);
        public abstract void onGroupsLoadError();
    }

    public static class TwoFactorGroupWithReferencesInformation {
        public final TwoFactorGroup storedData;

        public final boolean isReferenced;

        TwoFactorGroupWithReferencesInformation(@NotNull final SQLiteDatabase database, @NotNull final TwoFactorGroup group) {
            storedData = group;
            isReferenced = ((group.getRowId() >= 0) && group.isReferenced(database));
        }
    }

    private static class LoadGroupsDataImplementation implements Main.OnBackgroundTaskExecutionListener {
        private final TwoFactorServerIdentity mServerIdentity;

        private final OnGroupsLoadedListener mListener;

        private List<TwoFactorGroupWithReferencesInformation> mGroups = null;
        private boolean mSuccess = false;

        LoadGroupsDataImplementation(@NotNull final TwoFactorServerIdentity server_identity, @NotNull final OnGroupsLoadedListener listener) {
            mServerIdentity = server_identity;
            mListener = listener;
        }

        @Override
        public @Nullable Object onBackgroundTaskStarted(@Nullable final Object data) {
            try {
                final SQLiteDatabase database = Main.getInstance().getDatabaseHelper().open(false);
                if (database != null) {
                    try {
                        final List<TwoFactorGroup> groups = Main.getInstance().getDatabaseHelper().getTwoFactorGroupsHelper().get(database, mServerIdentity);
                        if (!Lists.isEmptyOrNull(groups)) {
                            mGroups = new ArrayList<TwoFactorGroupWithReferencesInformation>();
                            for (final TwoFactorGroup group : groups) {
                                mGroups.add(new TwoFactorGroupWithReferencesInformation(database, group));
                            }
                        }
                        mSuccess = true;
                    }
                    finally {
                        Main.getInstance().getDatabaseHelper().close(database);
                    }
                }
            }
            catch (Exception e) {
                Log.e(Main.LOG_TAG_NAME, "Exception while trying to load groups data", e);
            }
            return null;
        }

        @Override
        public void onBackgroundTaskFinished(@Nullable final Object data) {
            if (mSuccess) { mListener.onGroupsLoaded(mGroups); }
            else { mListener.onGroupsLoadError(); }
        }
    }

    public static @NotNull Thread getBackgroundTask(@NotNull final TwoFactorServerIdentity server_identity, @NotNull OnGroupsLoadedListener listener) {
        return Main.getInstance().getBackgroundTask(new LoadGroupsDataImplementation(server_identity, listener));
    }
}
