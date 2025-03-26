package com.twofauth.android.main_activity.tasks;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
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
    
    private static final String APP_VERSION_ENTRY_NAME = "version";
    private static final String APP_VERSION_NAME_ENTRY_NAME = "name";
    private static final String APP_VERSION_CODE_ENTRY_NAME = "code";
    private static final String APP_APKS_ENTRY_NAME = "apks";
    private static final String APP_DEFAULT_APK_ENTRY_NAME = "default";
    
    private static final String ASSETS_ENTRY_NAME = "assets";
    private static final String ASSET_NAME_ENTRY_NAME = "name";
    private static final String ENTRY_LOCATION_ENTRY_NAME = "browser_download_url";
    private static final String ENTRY_SIZE_ENTRY_NAME = "size";


    private static final String APK_VERSION_DETAILS_NAME = "app-release-data.json";

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

    private static class DownloadeableAppVersionData {
        private final String name;
        private final AppVersionData version;

        DownloadeableAppVersionData(@NotNull final String _name, @NotNull final AppVersionData _version) {
            name = _name;
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

        private JSONObject getReleaseAssetFileData(@NotNull final JSONObject object, @NotNull final String asset_name) throws Exception {
            final JSONArray assets = object.optJSONArray(ASSETS_ENTRY_NAME);
            if (assets != null) {
                for (int i = 0; i < assets.length(); i ++) {
                    final JSONObject asset = assets.getJSONObject(i);
                    if (asset.optString(ASSET_NAME_ENTRY_NAME).equals(asset_name)) {
                        return asset;
                    }
                }
            }
            return null;
        }

        private HttpURLConnection getAssetFileConnection(@NotNull final JSONObject object, final boolean raise_exception_on_error) throws Exception {
            final String location = object.optString(ENTRY_LOCATION_ENTRY_NAME);
            if (! location.isEmpty()) {
                final HttpURLConnection connection = HttpUtils.get(new URL(location));
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    return connection;
                }
                else if (raise_exception_on_error) {
                    throw new Exception(connection.getResponseMessage());
                }
            }
            return null;
        }

        private String getAssetFileContents(@NotNull final JSONObject object, @NotNull final String asset_name, final boolean raise_exception_on_error) throws Exception {
            final JSONObject asset_data_object = getReleaseAssetFileData(object, asset_name);
            final HttpURLConnection connection = asset_data_object == null ? null : getAssetFileConnection(asset_data_object, raise_exception_on_error);
            return connection == null ? null : HttpUtils.getContentString(connection);

        }

        private boolean downloadAssetFile(@NotNull final JSONObject object, @NotNull final String asset_name, @NotNull final File file, final boolean raise_exception_on_error) throws Exception {
            final JSONObject asset_data_object = getReleaseAssetFileData(object, asset_name);
            if (asset_data_object != null) {
                final HttpURLConnection connection = getAssetFileConnection(asset_data_object, raise_exception_on_error);
                if (connection != null) {
                    HttpUtils.saveContent(connection, file);
                    if (file.length() == asset_data_object.optInt(ENTRY_SIZE_ENTRY_NAME)) {
                        return true;
                    }
                    file.delete();
                }
                else if (raise_exception_on_error) {
                    throw new Exception(connection.getResponseMessage());
                }
            }
            return false;
        }

        private DownloadeableAppVersionData getLatestReleaseVersionData(@NotNull final JSONObject object, final boolean raise_exception_on_error) throws Exception {
            final JSONObject release_data_object = JsonUtils.StringToJsonObject(getAssetFileContents(object, APK_VERSION_DETAILS_NAME, raise_exception_on_error));
            if ((release_data_object != null) && (release_data_object.has(APP_VERSION_ENTRY_NAME)) && (release_data_object.has(APP_APKS_ENTRY_NAME))) {
                final JSONObject version_object = release_data_object.getJSONObject(APP_VERSION_ENTRY_NAME), apks_object = release_data_object.getJSONObject(APP_APKS_ENTRY_NAME);
                final String[] supported_abis = Build.SUPPORTED_ABIS;
                final String apk_asset_name = ((supported_abis == null) || (supported_abis.length == 0)) ? APP_DEFAULT_APK_ENTRY_NAME : apks_object.optString(supported_abis[0], apks_object.getString(APP_DEFAULT_APK_ENTRY_NAME));
                return new DownloadeableAppVersionData(apk_asset_name, new AppVersionData(version_object.getString(APP_VERSION_NAME_ENTRY_NAME), version_object.getInt(APP_VERSION_CODE_ENTRY_NAME)));
            }
            return null;
        }
        
        private File getApkLocalFile() throws Exception {
            return new File(mActivity.getExternalFilesDir("downloads"), "app-release.apk");
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
                            final DownloadeableAppVersionData latest_release_version_data = getLatestReleaseVersionData(object, true);
                            AppVersionData latest_downloaded_version_data = getLastDownloadedAppVersionData(preferences);
                            if ((latest_downloaded_version_data != null) && ((latest_downloaded_version_data.code < installed_app_version_code) || ((latest_release_version_data != null) && (latest_downloaded_version_data.code < latest_release_version_data.version.code)))) {
                                deleteLastDownloadedAppData(preferences, apk_local_file);        
                                latest_downloaded_version_data = null;                 
                            }
                            if ((latest_release_version_data != null) && (latest_release_version_data.version.code > installed_app_version_code)) {
                                boolean there_is_an_app_update = (latest_downloaded_version_data != null);
                                if ((latest_downloaded_version_data == null) && (downloadAssetFile(object, latest_release_version_data.name, apk_local_file, true))) {
                                    setLastDownloadedAppData(preferences, latest_release_version_data.version);
                                    latest_downloaded_version_data = latest_release_version_data.version;
                                    there_is_an_app_update = true;
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
