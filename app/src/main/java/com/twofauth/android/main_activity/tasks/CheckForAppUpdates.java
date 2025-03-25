package com.twofauth.android.main_activity.tasks;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.Log;

import com.twofauth.android.BaseActivity;
import com.twofauth.android.Constants;
import com.twofauth.android.HttpUtils;
import com.twofauth.android.JsonUtils;
import com.twofauth.android.Main;
import com.twofauth.android.NetworkUtils;
import com.twofauth.android.SharedPreferencesUtilities;
import com.twofauth.android.R;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;

public class CheckForAppUpdates {
    private static final String LAST_DOWNLOADED_APP_VERSION_NAME_KEY = "last-downloaded-app-version-name";
    private static final String LAST_DOWNLOADED_APP_VERSION_CODE_KEY = "last-downloaded-app-version-code";
    private static final String LATEST_RELEASE_URL = "https://api.github.com/repos/kslcsdalsadg/2fauth-for-android/releases/latest";
    private static final String DRAFT_ENTRY_NAME = "draft";
    private static final String PRE_RELEASE_ENTRY_NAME = "prerelease";
    private static final String NOTES_ENTRY_NAME = "body";
    private static final String APP_VERSION_ENTRY_NAME = "version";
    private static final String APP_VERSION_NAME_ENTRY_NAME = "name";
    private static final String APP_VERSION_CODE_ENTRY_NAME = "code";
    
    private static final String ASSETS_ENTRY_NAME = "assets";
    private static final String ASSET_NAME_ENTRY_NAME = "name";
    private static final String APK_ENTRY_LOCATION_ENTRY_NAME = "browser_download_url";
    private static final String APK_ENTRY_SIZE_ENTRY_NAME = "size";
    private static final String APK_NAME = "app-release.apk";

    public interface OnCheckForUpdatesListener {
        public abstract void onCheckForUpdatesFinished(File downloaded_app_file, AppVersionData downloaded_app_version);
    }

    public static class AppVersionData {
        public final String name;
        public final int code;

        AppVersionData(@NotNull final String _name, final int _code) {
            name = _name;
            code = _code;
        }
    }

    private static class DownloadedAppVersionData {
        private final File file;
        private final AppVersionData version;

        DownloadedAppVersionData(@Nullable final File _file, @Nullable final AppVersionData _version) {
            file = _file;
            version = _version;
        }
    }

    private static class CheckForAppUpdatesImplementation implements Main.OnBackgroundTaskExecutionListener {
        private final BaseActivity mActivity;
        private final OnCheckForUpdatesListener mListener;

        public CheckForAppUpdatesImplementation(@NotNull final BaseActivity activity, @NotNull final OnCheckForUpdatesListener listener) {
            mActivity = activity;
            mListener = listener;
        }

        private JSONObject getLatestReleaseData(final boolean raise_exception_on_error) throws Exception {
            final HttpURLConnection connection = HttpUtils.get(new URL(LATEST_RELEASE_URL));
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                return JsonUtils.StringToJsonObject(HttpUtils.getContentString(connection));
            }
            else if (raise_exception_on_error) {
                throw new Exception(connection.getResponseMessage());
            }
            return null;
        }

        private AppVersionData getLatestReleaseVersionData(@NotNull final JSONObject object) throws Exception {
            if (object.has(NOTES_ENTRY_NAME)) {
                final JSONObject notes_object = JsonUtils.StringToJsonObject(object.optString(NOTES_ENTRY_NAME, null));
                if ((notes_object != null) && (notes_object.has(APP_VERSION_ENTRY_NAME))) {
                    final JSONObject version_object = notes_object.getJSONObject(APP_VERSION_ENTRY_NAME);
                    return new AppVersionData(version_object.optString(APP_VERSION_NAME_ENTRY_NAME, null), version_object.optInt(APP_VERSION_CODE_ENTRY_NAME, 0));
                }
            }
            return null;
        }

        private JSONObject getLatestApkFileData(@NotNull final JSONObject object) throws Exception {
            final JSONArray assets = object.optJSONArray(ASSETS_ENTRY_NAME);
            if (assets != null) {
                for (int i = 0; i < assets.length(); i ++) {
                    final JSONObject asset = assets.getJSONObject(i);
                    if (asset.optString(ASSET_NAME_ENTRY_NAME).equals(APK_NAME)) {
                        return asset;
                    }
                }
            }
            return null;
        }

        private boolean downloadLatestApkFileData(@NotNull final JSONObject apk_asset_object, @NotNull final File file) throws Exception {
            final String location = apk_asset_object.optString(APK_ENTRY_LOCATION_ENTRY_NAME);
            if (! location.isEmpty()) {
                final HttpURLConnection connection = HttpUtils.get(new URL(location));
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    HttpUtils.saveContent(connection, file);
                    if (file.length() == apk_asset_object.optInt(APK_ENTRY_SIZE_ENTRY_NAME)) {
                        return true;
                    }
                    file.delete();
                }
            }
            return false;
        }

        private File getApkLocalFile() throws Exception {
            return new File(mActivity.getExternalFilesDir("downloads"), APK_NAME);
        }

        private AppVersionData getLastDownloadedAppVersionData(@NotNull final SharedPreferences preferences) {
            return preferences.contains(LAST_DOWNLOADED_APP_VERSION_CODE_KEY) ? new AppVersionData(preferences.getString(LAST_DOWNLOADED_APP_VERSION_NAME_KEY, null), preferences.getInt(LAST_DOWNLOADED_APP_VERSION_CODE_KEY, 0)) : null;
        }

        private void setLastDownloadedAppData(@NotNull final SharedPreferences preferences, @NotNull final AppVersionData downloaded_app_data) {
            preferences.edit().putInt(LAST_DOWNLOADED_APP_VERSION_CODE_KEY, downloaded_app_data.code).putString(LAST_DOWNLOADED_APP_VERSION_NAME_KEY, downloaded_app_data.name).apply();
        }

        private void deleteLastDownloadedAppData(@NotNull final SharedPreferences preferences, @Nullable final File file) {
            preferences.edit().remove(LAST_DOWNLOADED_APP_VERSION_CODE_KEY).remove(LAST_DOWNLOADED_APP_VERSION_NAME_KEY).apply();
            if (file != null) {
                file.delete();
            }
        }

        @Override
        public Object onBackgroundTaskStarted(@Nullable final Object data) {
            try {
                final Resources resources = mActivity.getResources();
                if (! resources.getBoolean(R.bool.is_debug_version)) {
                    final SharedPreferences preferences = SharedPreferencesUtilities.getDefaultSharedPreferences(mActivity);
                    if ((! preferences.getBoolean(Constants.AUTO_UPDATE_APP_ONLY_IN_WIFI_KEY, resources.getBoolean(R.bool.auto_update_app_only_in_wifi_default))) || NetworkUtils.isWifiConnected(mActivity)) {
                        final File apk_local_file = getApkLocalFile();
                        final JSONObject object = getLatestReleaseData(true);
                        if ((! object.optBoolean(DRAFT_ENTRY_NAME, true)) && (! object.optBoolean(PRE_RELEASE_ENTRY_NAME, true))) {
                            final int installed_app_version_code = resources.getInteger(R.integer.app_version_number_value);
                            final AppVersionData latest_release_version_data = getLatestReleaseVersionData(object);
                            AppVersionData latest_downloaded_version_data = getLastDownloadedAppVersionData(preferences);
                            if ((latest_downloaded_version_data != null) && ((latest_downloaded_version_data.code < installed_app_version_code) || ((latest_release_version_data != null) && (latest_downloaded_version_data.code < latest_release_version_data.code)))) {
                                deleteLastDownloadedAppData(preferences, apk_local_file);        
                                latest_downloaded_version_data = null;                 
                            }
                            if ((latest_release_version_data != null) && (latest_release_version_data.code > installed_app_version_code)) {
                                boolean there_is_an_app_update = (latest_downloaded_version_data != null);
                                if (latest_downloaded_version_data == null) {
                                    final JSONObject apk_asset = getLatestApkFileData(object);
                                    if ((apk_asset != null) && (downloadLatestApkFileData(apk_asset, apk_local_file))) {
                                        setLastDownloadedAppData(preferences, latest_release_version_data);
                                        latest_downloaded_version_data = latest_release_version_data;
                                        there_is_an_app_update = true;
                                    }
                                }
                                if (there_is_an_app_update) {
                                    return new DownloadedAppVersionData(apk_local_file, latest_downloaded_version_data);
                                }
                            }
                        }
                    }
                }
            }
            catch (Exception e) {
                Log.e(Constants.LOG_TAG_NAME, "Exception while checking for app updates", e);
            }
            return null;
        }


        @Override
        public void onBackgroundTaskFinished(@Nullable final Object data) {
            if ((! mActivity.isFinishedOrFinishing()) && (data != null)) {
                mListener.onCheckForUpdatesFinished(((DownloadedAppVersionData) data).file, ((DownloadedAppVersionData) data).version);
            }
        }
    }

    public static Thread getBackgroundTask(@NotNull final BaseActivity activity, @NotNull final OnCheckForUpdatesListener listener) {
        return Main.getInstance().getBackgroundTask(new CheckForAppUpdatesImplementation(activity, listener));
    }
}
