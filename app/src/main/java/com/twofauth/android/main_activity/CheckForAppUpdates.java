package com.twofauth.android.main_activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.twofauth.android.BaseActivity;
import com.twofauth.android.Constants;
import com.twofauth.android.HttpUtils;
import com.twofauth.android.JsonUtils;
import com.twofauth.android.R;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class CheckForAppUpdates extends Thread {
    private static final String LAST_DOWNLOADED_APP_VERSION_KEY = "last-downloaded-app-version";
    private static final String LATEST_RELEASE_URL = "https://api.github.com/repos/kslcsdalsadg/2fauth-for-android/releases/latest";
    private static final String APP_VERSION_ENTRY_NAME = "tag_name";
    private static final String DRAFT_ENTRY_NAME = "draft";
    private static final String PRE_RELEASE_ENTRY_NAME = "prerelease";
    private static final String ASSETS_ENTRY_NAME = "assets";
    private static final String ASSET_NAME_ENTRY_NAME = "name";
    private static final String APK_ENTRY_LOCATION_ENTRY_NAME = "browser_download_url";
    private static final String APK_ENTRY_SIZE_ENTRY_NAME = "size";
    private static final String APK_NAME = "app-release.apk";

    public interface OnCheckForUpdatesListener {
        public abstract void onCheckForUpdatesFinished(@Nullable final File apk_local_file);
    }

    private static class FileUtilities {
        public static void copy(@NotNull final File source, @NotNull final File destination) throws Exception {
            try (final InputStream in = new FileInputStream(source)) {
                try (final OutputStream out = new FileOutputStream(destination)) {
                    byte[] buffer = new byte[8192];
                    int length;
                    while ((length = in.read(buffer)) > 0) {
                        out.write(buffer, 0, length);
                    }
                }
            }
        }
    }
    private class CheckForUpdatesFinishedNotifier implements Runnable {
        private final File mFile;

        CheckForUpdatesFinishedNotifier(@Nullable final File file) {
            mFile = file;
        }

        public void run() {
            mListener.onCheckForUpdatesFinished(mFile);
        }
    }

    private final BaseActivity mActivity;
    private final OnCheckForUpdatesListener mListener;

    public CheckForAppUpdates(@NotNull final BaseActivity activity, @NotNull final OnCheckForUpdatesListener listener) {
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

    private File getApkLocalFile() throws Exception {
        return new File(mActivity.getExternalFilesDir("downloads"), APK_NAME);
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

    public void run() {
        File apk_local_file = null;
        boolean there_is_an_app_update = false;
        try {
            final SharedPreferences preferences = Constants.getDefaultSharedPreferences(mActivity);
            final JSONObject object = getLatestReleaseData(true);
            final String last_available_app_version = object.optString(APP_VERSION_ENTRY_NAME), app_version = "1.4"; //mActivity.getString(R.string.app_version_value);
            String last_downloaded_version = preferences.getString(LAST_DOWNLOADED_APP_VERSION_KEY, null);
            apk_local_file = getApkLocalFile();
            if ((app_version.equals(last_downloaded_version)) || (! last_available_app_version.equals(last_downloaded_version))) {
                apk_local_file.delete();
                preferences.edit().remove(LAST_DOWNLOADED_APP_VERSION_KEY).apply();
                last_downloaded_version = null;
            }
            if ((! app_version.equals(last_available_app_version)) && (! object.optBoolean(DRAFT_ENTRY_NAME, true)) && (! object.optBoolean(PRE_RELEASE_ENTRY_NAME, true))) {
                there_is_an_app_update = last_available_app_version.equals(last_downloaded_version);
                if (! there_is_an_app_update) {
                    final JSONObject apk_asset = getLatestApkFileData(object);
                    if ((apk_asset != null) && (downloadLatestApkFileData(apk_asset, apk_local_file))) {
                        preferences.edit().putString(LAST_DOWNLOADED_APP_VERSION_KEY, last_available_app_version).apply();
                        there_is_an_app_update = true;
                    }
                }
            }
        }
        catch (Exception e) {
            Log.d(Constants.LOG_TAG_NAME, "Exception while checking for app updates", e);
        }
        finally {
            if (! mActivity.isFinishedOrFinishing()) {
                mActivity.runOnUiThread(new CheckForUpdatesFinishedNotifier(there_is_an_app_update ? apk_local_file : null));
            }
        }
    }
}
