package com.twofauth.android.main_service;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.util.Log;

import com.twofauth.android.Constants;
import com.twofauth.android.JsonUtils;
import com.twofauth.android.HttpUtils;
import com.twofauth.android.ListUtils;
import com.twofauth.android.MainService;
import com.twofauth.android.R;
import com.twofauth.android.StringUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerDataSynchronizer extends Thread
{
    private static final String GET_2FAUTH_CODES_LOCATION = "%SERVER%/api/v1/twofaccounts?withSecret=1";
    private static final String GET_2FAUTH_GROUPS_LOCATION = "%SERVER%/api/v1/groups";
    private static final String GET_2FAUTH_ICON_LOCATION = "%SERVER%/storage/icons/%FILE%";
    private static final String GET_TWO_FACTOR_AUTH_TOKEN = "Bearer %TOKEN%";

    public static class TwoAuthLoadedData {
        public final List<JSONObject> accounts;
        public final boolean alphaSorted;

        public TwoAuthLoadedData(@Nullable final List<JSONObject> _accounts, final boolean alpha_sorted) {
            accounts = _accounts;
            alphaSorted = alpha_sorted;
        }
    }
    private static class DashBoardIconsUtils {
        private static final String BASE_LOCATION = "https://cdn.jsdelivr.net/gh/homarr-labs/dashboard-icons/png/%SERVICE%.png";
        public static final String DARK_MODE = "dark";
        public static final String LIGHT_MODE = "light";

        public static enum Mode { DARK_MODE, LIGHT_MODE };

        private static String standarizeServiceName(@NotNull final String service, @Nullable final Mode mode) {
            String standarized_service_name = service.toLowerCase().replace(" ", "-");
            if (mode != null) {
                standarized_service_name += String.format("-%s", mode == Mode.DARK_MODE ? DARK_MODE : LIGHT_MODE);
            }
            return standarized_service_name;
        }
        public static HttpURLConnection getIcon(@NotNull final String service, @Nullable final Mode mode) throws Exception {
            return HttpUtils.get(new URL(BASE_LOCATION.replace("%SERVICE%", standarizeServiceName(service, mode))));
        }
    }

    private final MainService mMainService;
    private final String mServer;
    private final String mToken;
    public ServerDataSynchronizer(@NotNull final MainService main_service) {
        mMainService = main_service;
        mServer = Constants.getDefaultSharedPreferences(main_service).getString(Constants.TWO_FACTOR_AUTH_SERVER_LOCATION_KEY, null);
        mToken = Constants.getDefaultSharedPreferences(main_service).getString(Constants.TWO_FACTOR_AUTH_TOKEN_KEY, null);
    }

    private HttpURLConnection getIconFromServer(@NotNull final JSONObject object) throws Exception {
        return HttpUtils.get(new URL(GET_2FAUTH_ICON_LOCATION.replace("%SERVER%", mServer).replace("%FILE%", object.optString(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_ICON_KEY, ""))), GET_TWO_FACTOR_AUTH_TOKEN.replace("%TOKEN%", mToken));
    }

    private void saveTwoFactorAuthIcon(@NotNull final JSONObject object) throws Exception {
        final String server_icon_file = object.optString(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_ICON_KEY, "");
        final boolean is_server_icon_supported = ((! server_icon_file.isEmpty()) && (! server_icon_file.toLowerCase().endsWith(".svg")));
        final DashBoardIconsUtils.Mode[] modes = new DashBoardIconsUtils.Mode[] { DashBoardIconsUtils.Mode.DARK_MODE, DashBoardIconsUtils.Mode.LIGHT_MODE, null };
        for (DashBoardIconsUtils.Mode mode : modes) {
            final File file = getTwoFactorAuthIconPath(mMainService, object, mode);
            file.delete();
            if (! is_server_icon_supported) {
                final HttpURLConnection connection = DashBoardIconsUtils.getIcon(object.getString(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_SERVICE_KEY), mode);
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    HttpUtils.saveContent(connection, file);
                }
            }
            else if (mode == null) {
                final HttpURLConnection connection = getIconFromServer(object);
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    HttpUtils.saveContent(connection, file);
                }
            }
        }
    }

    private void removeTwoFactorAuthIcon(@NotNull final JSONObject object) {
        try {
            final DashBoardIconsUtils.Mode[] modes = new DashBoardIconsUtils.Mode[] { DashBoardIconsUtils.Mode.DARK_MODE, DashBoardIconsUtils.Mode.LIGHT_MODE, null };
            for (DashBoardIconsUtils.Mode mode : modes) {
                final File file = getTwoFactorAuthIconPath(mMainService, object, mode);
                if (file.exists()) {
                    file.delete();
                }
            }
        }
        catch (Exception e) {
            Log.e(Constants.LOG_TAG_NAME, "Exception while deleting icon file", e);
        }
    }

    private String getTwoFactorAuthGroups(final boolean raise_exception_on_error) throws Exception {
        final HttpURLConnection connection = HttpUtils.get(new URL(GET_2FAUTH_GROUPS_LOCATION.replace("%SERVER%", mServer)), GET_TWO_FACTOR_AUTH_TOKEN.replace("%TOKEN%", mToken));
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            return HttpUtils.getContentString(connection);
        }
        else if (raise_exception_on_error) {
            throw new Exception(connection.getResponseMessage());
        }
        return null;
    }
    private String getTwoFactorAuthCodes(final boolean raise_exception_on_error) throws Exception {
        final HttpURLConnection connection = HttpUtils.get(new URL(GET_2FAUTH_CODES_LOCATION.replace("%SERVER%", mServer)), GET_TWO_FACTOR_AUTH_TOKEN.replace("%TOKEN%", mToken));
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            return HttpUtils.getContentString(connection);
        }
        else if (raise_exception_on_error) {
            throw new Exception(connection.getResponseMessage());
        }
        return null;
    }

    private boolean equals(@NotNull final JSONObject object1, @Nullable final JSONObject object2) {
        if (object2 != null) {
            boolean equals = true;
            for (String key : Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_KEYS) {
                final Object object1_value = object1.opt(key), object2_value = object2.opt(key);
                if (((object1_value == null) && (object2_value != null)) || ((object1_value != null) && (! object1_value.equals(object2_value)))) {
                    equals = false;
                    break;
                }
            }
            return equals;
        }
        return false;
    }
    private boolean equals(@Nullable final Map<Integer, JSONObject> loaded_accounts, @Nullable final Map<Integer, JSONObject> stored_accounts) throws Exception {
        final int number_of_accounts_loaded = loaded_accounts == null ? 0 : loaded_accounts.size(), number_of_accounts_stored = stored_accounts == null ? 0 : stored_accounts.size();
        if (number_of_accounts_loaded == number_of_accounts_stored) {
            boolean equals = true;
            if (number_of_accounts_loaded != 0) {
                for (int key : loaded_accounts.keySet()) {
                    if (! equals(loaded_accounts.get(key), stored_accounts.get(key))) {
                        equals = false;
                        break;
                    }
                }
            }
            return equals;
        }
        return false;
    }

    @Override
    public void run() {
        boolean there_are_changes = false;
        if ((mServer != null) && (mToken != null)) {
            final SharedPreferences.Editor editor = Constants.getDefaultSharedPreferences(mMainService).edit();
            try {
                final Map<Integer, JSONObject> loaded_accounts = JsonUtils.StringToJsonMap(getTwoFactorAuthCodes(true), Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_ID_KEY), stored_accounts = JsonUtils.StringToJsonMap(Constants.getDefaultSharedPreferences(mMainService).getString(Constants.TWO_FACTOR_AUTH_ACCOUNTS_DATA_KEY, null), Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_ID_KEY), groups = JsonUtils.StringToJsonMap(getTwoFactorAuthGroups(true), Constants.TWO_FACTOR_AUTH_GROUP_ID_KEY);
                for (JSONObject object : loaded_accounts.values()) {
                    final int group_id = object.optInt(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_GROUP_KEY, -1);
                    object.put(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_GROUP_KEY, ((groups != null) && groups.containsKey(group_id)) ? groups.get(group_id).optString(Constants.TWO_FACTOR_AUTH_GROUP_NAME_KEY, "") : "");
                }
                if (! equals(loaded_accounts, stored_accounts)) {
                    for (JSONObject object : loaded_accounts.values()) {
                        saveTwoFactorAuthIcon(object);
                    }
                    for (Integer id : stored_accounts.keySet()) {
                        if (! loaded_accounts.containsKey(id)) {
                            final JSONObject object = stored_accounts.get(id);
                            editor.remove(Constants.getTwoFactorAccountLastUseKey(object));
                            removeTwoFactorAuthIcon(object);
                        }
                    }
                    editor.putString(Constants.TWO_FACTOR_AUTH_ACCOUNTS_DATA_KEY, JsonUtils.JSonObjectsToString(loaded_accounts.values())).putInt(Constants.TWO_FACTOR_AUTH_ACCOUNTS_DATA_LENGTH_KEY, loaded_accounts.size());
                    there_are_changes = true;
                }
                editor.putLong(Constants.TWO_FACTOR_AUTH_CODES_LAST_SYNC_TIME_KEY, System.currentTimeMillis()).remove(Constants.TWO_FACTOR_AUTH_CODES_LAST_SYNC_ERROR_KEY).remove(Constants.TWO_FACTOR_AUTH_CODES_LAST_SYNC_ERROR_TIME_KEY).apply();
                mMainService.sendBroadcast(new Intent(MainService.ACTION_SERVICE_DATA_SYNCED).putExtra(MainService.EXTRA_THERE_ARE_CHANGES, there_are_changes));
            }
            catch (Exception e) {
                editor.putString(Constants.TWO_FACTOR_AUTH_CODES_LAST_SYNC_ERROR_KEY, e.getMessage()).putLong(Constants.TWO_FACTOR_AUTH_CODES_LAST_SYNC_ERROR_TIME_KEY, System.currentTimeMillis()).apply();
                Log.e(Constants.LOG_TAG_NAME, "Exception while processing downloaded 2FA codes", e);
            }
        }
        mMainService.stopSelf(there_are_changes);
    }

    public static TwoAuthLoadedData getTwoFactorAuthCodes(@NotNull final Context context) throws Exception {
        final List<JSONObject> list = new ArrayList<JSONObject>();
        final Map<Integer, JSONObject> stored_accounts = JsonUtils.StringToJsonMap(Constants.getDefaultSharedPreferences(context).getString(Constants.TWO_FACTOR_AUTH_ACCOUNTS_DATA_KEY, null), Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_ID_KEY);
        if (stored_accounts != null) {
            ListUtils.setItems(list, stored_accounts.values());
        }
        final SharedPreferences preferences = Constants.getDefaultSharedPreferences(context);
        final boolean sort_using_last_use = preferences.getBoolean(Constants.SORT_ACCOUNTS_BY_LAST_USE_KEY, context.getResources().getBoolean(R.bool.sort_accounts_by_last_use_default));
        if (! list.isEmpty()) {
            final Map<Integer, Long> last_uses = sort_using_last_use ? new HashMap<Integer, Long>() : null;
            if (last_uses != null) {
                for (JSONObject object : list) {
                    last_uses.put(object.getInt(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_ID_KEY), preferences.getLong(Constants.getTwoFactorAccountLastUseKey(object), 0));
                }
            }
            list.sort(new Comparator<JSONObject>() {
                @Override
                public int compare(final JSONObject object1, final JSONObject object2) {
                    if (sort_using_last_use) {
                        final int result = Long.compare(last_uses.get(object1.optInt(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_ID_KEY)), last_uses.get(object2.optInt(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_ID_KEY)));
                        if (result != 0) {
                            return -result;
                        }
                    }
                    final int result = StringUtils.compare(object1.optString(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_SERVICE_KEY), object2.optString(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_SERVICE_KEY), true);
                    return result == 0 ? StringUtils.compare(object1.optString(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_ACCOUNT_KEY), object2.optString(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_ACCOUNT_KEY), true) : result;
                }
            });
        }
        return new TwoAuthLoadedData(list, ! sort_using_last_use);
    }

    public static long getTwoFactorAuthCodesLastLoadTime(@NotNull final Context context) {
        return Constants.getDefaultSharedPreferences(context).getLong(Constants.TWO_FACTOR_AUTH_CODES_LAST_SYNC_TIME_KEY, 0);
    }

    @SuppressLint("DefaultLocale")
    private static File getTwoFactorAuthIconPath(@NotNull final Context context, @NotNull final JSONObject object, @Nullable final DashBoardIconsUtils.Mode mode) throws Exception {
        final int id = object.getInt(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_ID_KEY);
        final String filename = (mode == null) ? String.valueOf(id) : String.format("%d-%s", id, mode == DashBoardIconsUtils.Mode.DARK_MODE ? DashBoardIconsUtils.DARK_MODE : DashBoardIconsUtils.LIGHT_MODE);
        return new File(context.getExternalFilesDir("icons"), filename);
    }

    public static File getTwoFactorAuthIconPath(@NotNull final Context context, @NotNull final JSONObject object) throws Exception {
        final boolean dark_mode_active = ((context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES);
        File file = getTwoFactorAuthIconPath(context, object, dark_mode_active ? DashBoardIconsUtils.Mode.DARK_MODE : DashBoardIconsUtils.Mode.LIGHT_MODE);
        if (! file.exists()) {
            file = getTwoFactorAuthIconPath(context, object, null);
        }
        return file.exists() ? file : null;
    }
}
