package com.twofauth.android.main_service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.twofauth.android.Constants;
import com.twofauth.android.JsonUtils;
import com.twofauth.android.MainService;
import com.twofauth.android.StringUtils;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ServerDataLoader extends Thread
{
    private static final String GET_2FAUTH_CODES_LOCATION = "%SERVER%/api/v1/twofaccounts?withSecret=1";
    private static final String GET_2FAUTH_GROUPS_LOCATION = "%SERVER%/api/v1/groups";
    private static final String GET_2FAUTH_ICON_LOCATION = "%SERVER%/storage/icons/%FILE%";
    private static final String GET_TWO_FACTOR_AUTH_TOKEN = "Bearer %TOKEN%";

    private static class HttpUtils {
        private static HttpURLConnection get(@NotNull final URL url, @NotNull final String token) throws Exception {
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Cache-Control", "no-cache");
            connection.setDefaultUseCaches(false);
            connection.setUseCaches(false);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("Authorization", GET_TWO_FACTOR_AUTH_TOKEN.replace("%TOKEN%", token));
            connection.connect();
            return connection;
        }
        private static String getContentString(@NotNull final HttpURLConnection connection) throws Exception {
            try (final BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                final StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            }
        }

        private static void saveContent(@NotNull final HttpURLConnection connection, @NotNull final File file) throws Exception {
            int count;
            try (final InputStream in = connection.getInputStream()) {
                file.getParentFile().mkdirs();
                try (final OutputStream out = new FileOutputStream(file)) {
                    final byte[] bytes = new byte[4096];
                    while ((count = in.read(bytes)) >= 0) {
                        out.write(bytes, 0, count);
                    }
                }
            }
        }
    }
    private final MainService mMainService;
    private final String mServer;
    private final String mToken;
    public ServerDataLoader(@NotNull final MainService main_service) {
        mMainService = main_service;
        mServer = Constants.getDefaultSharedPreferences(main_service).getString(Constants.TWO_FACTOR_AUTH_SERVER_LOCATION_KEY, null);
        mToken = Constants.getDefaultSharedPreferences(main_service).getString(Constants.TWO_FACTOR_AUTH_TOKEN_KEY, null);
    }

    private void saveTwoFactorAuthIcon(@NotNull final JSONObject object, final boolean raise_exception_on_error) throws Exception {
        final String icon_file = object.optString(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_ICON_KEY, "");
        if (! icon_file.isEmpty()) {
            final HttpURLConnection connection = HttpUtils.get(new URL(GET_2FAUTH_ICON_LOCATION.replace("%SERVER%", mServer).replace("%FILE%", icon_file)), mToken);
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                HttpUtils.saveContent(connection, getTwoFactorAuthIconPath(mMainService, object));
            }
            else if (raise_exception_on_error) {
                throw new Exception(connection.getResponseMessage());
            }
        }
    }

    private void removeTwoFactorAuthIcon(@NotNull final JSONObject object) {
        try {
            getTwoFactorAuthIconPath(mMainService, object).delete();
        }
        catch (Exception e) {
            Log.e(Constants.LOG_TAG_NAME, "Exception while deleting icon file", e);
        }
    }

    private String getTwoFactorAuthGroups(final boolean raise_exception_on_error) throws Exception {
        final HttpURLConnection connection = HttpUtils.get(new URL(GET_2FAUTH_GROUPS_LOCATION.replace("%SERVER%", mServer)), mToken);
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            return HttpUtils.getContentString(connection);
        }
        else if (raise_exception_on_error) {
            throw new Exception(connection.getResponseMessage());
        }
        return null;
    }
    private String getTwoFactorAuthCodes(final boolean raise_exception_on_error) throws Exception {
        final HttpURLConnection connection = HttpUtils.get(new URL(GET_2FAUTH_CODES_LOCATION.replace("%SERVER%", mServer)), mToken);
        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) throw new Exception(connection.getResponseMessage());
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            return HttpUtils.getContentString(connection);
        }
        else if (raise_exception_on_error) {
            throw new Exception(connection.getResponseMessage());
        }
        return null;
    }

    @Override
    public void run() {
        if ((mServer != null) && (mToken != null)) {
            final SharedPreferences.Editor editor = Constants.getDefaultSharedPreferences(mMainService).edit();
            try {
                final String data = getTwoFactorAuthCodes(true);
                if (data != null) {
                    final Map<Integer, JSONObject> groups = JsonUtils.StringToJsonMap(getTwoFactorAuthGroups(true), Constants.TWO_FACTOR_AUTH_GROUP_ID_KEY), old_data = JsonUtils.StringToJsonMap(Constants.getDefaultSharedPreferences(mMainService).getString(Constants.TWO_FACTOR_AUTH_ACCOUNTS_DATA_KEY, null), Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_ID_KEY), new_data = JsonUtils.StringToJsonMap(data, Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_ID_KEY);
                    for (JSONObject object : new_data.values()) {
                        final int group_id = object.optInt(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_GROUP_KEY, -1);
                        object.put(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_GROUP_KEY, groups.containsKey(group_id) ? groups.get(group_id).optString(Constants.TWO_FACTOR_AUTH_GROUP_NAME_KEY, "") : "");
                        saveTwoFactorAuthIcon(object, true);
                    }
                    for (Integer id : old_data.keySet()) {
                        if (! new_data.containsKey(id)) {
                            removeTwoFactorAuthIcon(old_data.get(id));
                        }
                    }
                    editor.putString(Constants.TWO_FACTOR_AUTH_ACCOUNTS_DATA_KEY, JsonUtils.JSonObjectsToString(new_data.values())).putInt(Constants.TWO_FACTOR_AUTH_ACCOUNTS_DATA_SIZE_KEY, new_data.size()).putLong(Constants.TWO_FACTOR_AUTH_CODES_LAST_SYNC_TIME_KEY, System.currentTimeMillis()).remove(Constants.TWO_FACTOR_AUTH_CODES_LAST_SYNC_ERROR_KEY).remove(Constants.TWO_FACTOR_AUTH_CODES_LAST_SYNC_ERROR_TIME_KEY).apply();
                }
                mMainService.sendBroadcast(new Intent(MainService.ACTION_SERVICE_DATA_SYNCED));
            }
            catch (Exception e) {
                editor.putString(Constants.TWO_FACTOR_AUTH_CODES_LAST_SYNC_ERROR_KEY, e.getMessage()).putLong(Constants.TWO_FACTOR_AUTH_CODES_LAST_SYNC_ERROR_TIME_KEY, System.currentTimeMillis()).apply();
                Log.e(Constants.LOG_TAG_NAME, "Exception while processing downloaded 2FA codes", e);
            }
        }
        mMainService.stopSelf();
    }

    public static List<JSONObject> getTwoFactorAuthCodes(@NotNull final Context context) throws Exception {
        final List<JSONObject> list = new ArrayList<JSONObject>(JsonUtils.StringToJsonMap(Constants.getDefaultSharedPreferences(context).getString(Constants.TWO_FACTOR_AUTH_ACCOUNTS_DATA_KEY, null), Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_ID_KEY).values());
        list.sort(new Comparator<JSONObject>() {
            @Override
            public int compare(final JSONObject object1, final JSONObject object2) {
                int result = StringUtils.compare(object1.optString(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_SERVICE_KEY), object2.optString(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_SERVICE_KEY), true);
                return result == 0 ? StringUtils.compare(object1.optString(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_ACCOUNT_KEY), object2.optString(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_ACCOUNT_KEY), true) : result;
            }
        });
        return list;
    }

    public static long getTwoFactorAuthCodesLastLoadTime(@NotNull final Context context) {
        return Constants.getDefaultSharedPreferences(context).getLong(Constants.TWO_FACTOR_AUTH_CODES_LAST_SYNC_TIME_KEY, 0);
    }

    public static File getTwoFactorAuthIconPath(@NotNull final Context context, @NotNull final JSONObject object) throws Exception {
        return new File(context.getExternalFilesDir("icons"), String.valueOf(object.getInt(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_ID_KEY)));
    }
}
