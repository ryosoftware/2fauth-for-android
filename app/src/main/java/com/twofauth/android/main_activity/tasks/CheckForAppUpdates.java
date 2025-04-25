package com.twofauth.android.main_activity.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;

import com.twofauth.android.Constants;
import com.twofauth.android.utils.HTTP;
import com.twofauth.android.utils.JSON;
import com.twofauth.android.Main;
import com.twofauth.android.utils.Lists;
import com.twofauth.android.utils.Network;
import com.twofauth.android.utils.Preferences;
import com.twofauth.android.R;
import com.twofauth.android.utils.Strings;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class CheckForAppUpdates {
    private static final String LAST_DOWNLOADED_APP_VERSION_NAME_KEY = "last-downloaded-app-version-name";
    private static final String LAST_DOWNLOADED_APP_VERSION_CODE_KEY = "last-downloaded-app-version-code";
    private static final String LAST_DOWNLOADED_APP_VERSION_IS_PRERELEASE_KEY = "last-downloaded-app-version-is-prerelease";

    private static final String LATEST_RELEASES_URL = "https://api.github.com/repos/kslcsdalsadg/2fauth-for-android/releases?per_page=10&page=%PAGE%";

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
        public abstract void onCheckForUpdatesUpdateFound(File downloaded_app_file, AppVersionData downloaded_app_version);
        public abstract void onCheckForUpdatesNoUpdates();
        public abstract void onCheckForUpdatesError(String error);
    }

    public static class AppVersionData {
        public final String name;
        public final int code;
        public final boolean preRelease;

        AppVersionData(@NotNull final String _name, final int _code, final boolean pre_release) {
            name = _name;
            code = _code;
            preRelease = pre_release;
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
        private final Context mContext;
        private final boolean mForce;
        private final OnCheckForUpdatesListener mListener;

        public CheckForAppUpdatesImplementation(@NotNull final Context context, final boolean force, @NotNull final OnCheckForUpdatesListener listener) {
            mContext = context;
            mForce = force;
            mListener = listener;
        }

        private @Nullable JSONObject getLatestReleaseData(final boolean allow_to_return_prerelease, final boolean raise_exception_on_error) throws Exception {
            JSONObject last_release = null;
            int page = 1;
            do {
                final HttpURLConnection connection = HTTP.get(new URL(LATEST_RELEASES_URL.replace("%PAGE%", String.valueOf(page ++))));
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    final List<JSONObject> releases = JSON.toListOfJSONObjects(HTTP.getContentString(connection));
                    if (Lists.isEmptyOrNull(releases)) { break; }
                    for (final JSONObject release : releases) {
                        if ((! release.optBoolean(DRAFT_ENTRY_NAME, false)) && (allow_to_return_prerelease || (! release.optBoolean(PRE_RELEASE_ENTRY_NAME, true)))) {
                            last_release = release;
                            break;
                        }
                    }
                }
                else if (raise_exception_on_error) {
                    throw new Exception(connection.getResponseMessage());
                }
                else {
                    break;
                }
            } while (last_release == null);
            return last_release;
        }

        private @Nullable JSONObject getReleaseAssetFileData(@NotNull final JSONObject object, @NotNull final String asset_name) throws Exception {
            final JSONArray assets = object.optJSONArray(ASSETS_ENTRY_NAME);
            if (assets != null) {
                for (int i = 0; i < assets.length(); i ++) {
                    final JSONObject asset = assets.getJSONObject(i);
                    if (asset.optString(ASSET_NAME_ENTRY_NAME).equals(asset_name)) { return asset; }
                }
            }
            return null;
        }

        private @Nullable HttpURLConnection getAssetFileConnection(@NotNull final JSONObject object, final boolean raise_exception_on_error) throws Exception {
            final String location = object.optString(ENTRY_LOCATION_ENTRY_NAME);
            if (! location.isEmpty()) {
                final HttpURLConnection connection = HTTP.get(new URL(location));
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) { return connection; }
                else if (raise_exception_on_error) { throw new Exception(connection.getResponseMessage()); }
            }
            return null;
        }

        private @Nullable String getAssetFileContents(@NotNull final JSONObject object, @NotNull final String asset_name, final boolean raise_exception_on_error) throws Exception {
            final JSONObject asset_data_object = getReleaseAssetFileData(object, asset_name);
            final HttpURLConnection connection = asset_data_object == null ? null : getAssetFileConnection(asset_data_object, raise_exception_on_error);
            return (connection == null) ? null : HTTP.getContentString(connection);

        }

        private boolean downloadAssetFile(@NotNull final JSONObject object, @NotNull final String asset_name, @NotNull final File file, final boolean raise_exception_on_error) throws Exception {
            final JSONObject asset_data_object = getReleaseAssetFileData(object, asset_name);
            if (asset_data_object != null) {
                final HttpURLConnection connection = getAssetFileConnection(asset_data_object, raise_exception_on_error);
                if (connection != null) {
                    HTTP.saveContent(connection, file);
                    if (file.length() == asset_data_object.optInt(ENTRY_SIZE_ENTRY_NAME)) { return true; }
                    file.delete();
                }
                else if (raise_exception_on_error) {
                    throw new Exception(connection.getResponseMessage());
                }
            }
            return false;
        }

        private @Nullable DownloadeableAppVersionData getLatestReleaseVersionData(@NotNull final JSONObject object, final boolean raise_exception_on_error) throws Exception {
            final JSONObject release_data_object = JSON.toJSONObject(getAssetFileContents(object, APK_VERSION_DETAILS_NAME, raise_exception_on_error));
            if ((release_data_object != null) && (release_data_object.has(APP_VERSION_ENTRY_NAME)) && (release_data_object.has(APP_APKS_ENTRY_NAME))) {
                final JSONObject version_object = release_data_object.getJSONObject(APP_VERSION_ENTRY_NAME), apks_object = release_data_object.getJSONObject(APP_APKS_ENTRY_NAME);
                final String[] supported_abis = Build.SUPPORTED_ABIS;
                final String apk_asset_name = ((supported_abis == null) || (supported_abis.length == 0) || (! apks_object.has(supported_abis[0])) || Strings.isEmptyOrNull(apks_object.optString(supported_abis[0], ""))) ? apks_object.getString(APP_DEFAULT_APK_ENTRY_NAME) : apks_object.getString(supported_abis[0]);
                return new DownloadeableAppVersionData(apk_asset_name, new AppVersionData(version_object.getString(APP_VERSION_NAME_ENTRY_NAME), version_object.getInt(APP_VERSION_CODE_ENTRY_NAME), object.optBoolean(PRE_RELEASE_ENTRY_NAME, false)));
            }
            return null;
        }

        private @NotNull File getApkLocalFile() throws Exception {
            return new File(mContext.getExternalFilesDir("downloads"), "app-release.apk");
        }

        private @Nullable AppVersionData getLastDownloadedAppVersionData(@NotNull final SharedPreferences preferences) {
            return preferences.contains(LAST_DOWNLOADED_APP_VERSION_CODE_KEY) ? new AppVersionData(preferences.getString(LAST_DOWNLOADED_APP_VERSION_NAME_KEY, null), preferences.getInt(LAST_DOWNLOADED_APP_VERSION_CODE_KEY, 0), preferences.getBoolean(LAST_DOWNLOADED_APP_VERSION_IS_PRERELEASE_KEY, false)) : null;
        }

        private void setLastDownloadedAppData(@NotNull final SharedPreferences preferences, @NotNull final AppVersionData downloaded_app_data) {
            preferences.edit().putInt(LAST_DOWNLOADED_APP_VERSION_CODE_KEY, downloaded_app_data.code).putString(LAST_DOWNLOADED_APP_VERSION_NAME_KEY, downloaded_app_data.name).apply();
        }

        private void deleteLastDownloadedAppData(@NotNull final SharedPreferences preferences, @Nullable final File file) {
            preferences.edit().remove(LAST_DOWNLOADED_APP_VERSION_CODE_KEY).remove(LAST_DOWNLOADED_APP_VERSION_NAME_KEY).apply();
            if (file != null) { file.delete(); }
        }

        @Override
        public Object onBackgroundTaskStarted(@Nullable final Object data) {
            try {
                final Resources resources = mContext.getResources();
                if (! Main.getInstance().isDebugRelease()) {
                    final SharedPreferences preferences = Preferences.getDefaultSharedPreferences(mContext);
                    if (mForce || (! preferences.getBoolean(Constants.AUTO_UPDATE_APP_ONLY_IN_WIFI_KEY, resources.getBoolean(R.bool.auto_update_app_only_in_wifi))) || Network.isWifiConnected(mContext)) {
                        final File apk_local_file = getApkLocalFile();
                        final JSONObject object = getLatestReleaseData(preferences.getBoolean(Constants.AUTO_UPDATE_APP_INCLUDE_PRERELEASES_KEY, resources.getBoolean(R.bool.auto_update_app_include_prereleases)), true);
                        if (object != null) {
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
                Log.e(Main.LOG_TAG_NAME, "Exception while checking for app updates", e);
                return e;
            }
            return null;
        }

        @Override
        public void onBackgroundTaskFinished(@Nullable final Object data) {
            if (data == null) { mListener.onCheckForUpdatesNoUpdates(); }
            else if (data instanceof DownloadeableAppVersionData) { mListener.onCheckForUpdatesUpdateFound(((DownloadedAppVersionData) data).file, ((DownloadedAppVersionData) data).version); }
            else { mListener.onCheckForUpdatesError(((Exception) data).getMessage()); }
        }
    }

    public static @NotNull Thread getBackgroundTask(@NotNull final Context context, final boolean force, @NotNull final OnCheckForUpdatesListener listener) {
        return Main.getInstance().getBackgroundTask(new CheckForAppUpdatesImplementation(context, force, listener));
    }

    public static @NotNull Thread getBackgroundTask(@NotNull final Context context, @NotNull final OnCheckForUpdatesListener listener) {
        return getBackgroundTask(context, false, listener);
    }
}
