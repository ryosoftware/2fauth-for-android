package com.twofauth.android.main_service;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.twofauth.android.Constants;
import com.twofauth.android.Database;
import com.twofauth.android.JsonUtils;
import com.twofauth.android.HttpUtils;
import com.twofauth.android.Main;
import com.twofauth.android.Main.OnBackgroundTaskExecutionListener;
import com.twofauth.android.MainService;
import com.twofauth.android.MainService.SyncResultType;
import com.twofauth.android.ObjectUtils;
import com.twofauth.android.Database.TwoFactorAccount;
import com.twofauth.android.SharedPreferencesUtilities;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerDataSynchronizer
{
    private static final String GET_2FAUTH_CODES_LOCATION = "%SERVER%/api/v1/twofaccounts?withSecret=1";
    private static final String GET_2FAUTH_GROUPS_LOCATION = "%SERVER%/api/v1/groups";
    private static final String GET_2FAUTH_ICON_LOCATION = "%SERVER%/storage/icons/%FILE%";

    private static final String GET_2FAUTH_ACCOUNT_DATA_LOCATION = "%SERVER%/api/v1/twofaccounts/%ID%";
    private static final String GET_2FAUTH_ACCOUNT_NEW_DATA_LOCATION = "%SERVER%/api/v1/twofaccounts";

    private static final String GET_TWO_FACTOR_AUTH_TOKEN = "Bearer %TOKEN%";

    private static final List<String> TWO_FACTOR_AUTH_ACCOUNT_DATA_KEYS = Arrays.asList(new String[] {
        Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_ID_KEY,
        Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_SERVICE_KEY,
        Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_USER_KEY,
        Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_GROUP_KEY,
        Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_ICON_KEY,
        Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_OTP_TYPE_KEY,
        Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_SECRET_KEY,
        Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_PASSWORD_LENGTH_KEY,
        Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_ALGORITHM_KEY,
        Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_PERIOD_KEY,
        Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_COUNTER_KEY,
    });

    private static final List<String> TWO_FACTOR_AUTH_GROUP_DATA_KEYS = Arrays.asList(new String[] {
        Constants.TWO_FACTOR_AUTH_GROUP_ID_KEY,
        Constants.TWO_FACTOR_AUTH_GROUP_NAME_KEY,
    });

    private static class ServerDataSynchronizerImplementation implements OnBackgroundTaskExecutionListener {
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

        private final MainService mService;

        private final String mServer;
        private final String mToken;

        public ServerDataSynchronizerImplementation(@NotNull final MainService service, @NotNull final String server, @NotNull final String token) {
            mService = service;
            mServer = server;
            mToken = token;
        }

        private HttpURLConnection getIconFromServer(@NotNull final JSONObject object) throws Exception {
            return HttpUtils.get(new URL(GET_2FAUTH_ICON_LOCATION.replace("%SERVER%", mServer).replace("%FILE%", object.optString(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_ICON_KEY, ""))), GET_TWO_FACTOR_AUTH_TOKEN.replace("%TOKEN%", mToken));
        }

        @SuppressLint("DefaultLocale")
        private File getTwoFactorAuthIconPath(@NotNull final Context context, @NotNull final JSONObject object, @Nullable final DashBoardIconsUtils.Mode mode) throws Exception {
            final int id = object.getInt(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_ID_KEY);
            final String filename = (mode == null) ? String.valueOf(id) : String.format("%d-%s", id, mode == DashBoardIconsUtils.Mode.DARK_MODE ? DashBoardIconsUtils.DARK_MODE : DashBoardIconsUtils.LIGHT_MODE);
            return new File(context.getExternalFilesDir("icons"), filename);
        }

        private void saveTwoFactorAuthIcon(@NotNull final JSONObject object) throws Exception {
            final String server_icon_file = object.optString(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_ICON_KEY, "");
            final boolean is_server_icon_supported = ((! server_icon_file.isEmpty()) && (! server_icon_file.toLowerCase().endsWith(".svg")));
            final DashBoardIconsUtils.Mode[] modes = new DashBoardIconsUtils.Mode[] { DashBoardIconsUtils.Mode.DARK_MODE, DashBoardIconsUtils.Mode.LIGHT_MODE, null };
            for (DashBoardIconsUtils.Mode mode : modes) {
                final File file = getTwoFactorAuthIconPath(mService, object, mode);
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
                if (file.exists()) {
                    object.put(mode == null ? Database.TWO_FACTOR_ACCOUNT_GENERIC_ICON_PATHNAME : mode == DashBoardIconsUtils.Mode.DARK_MODE ? Database.TWO_FACTOR_ACCOUNT_DARK_ICON_PATHNAME : Database.TWO_FACTOR_ACCOUNT_LIGHT_ICON_PATHNAME, file.getPath());
                }
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

        private boolean syncPendingUpdates(final boolean raise_exception_on_error) throws Exception {
            final List<TwoFactorAccount> updated_accounts = Database.TwoFactorAccountOperations.get(true);
            if (updated_accounts != null) {
                for (TwoFactorAccount updated_account : updated_accounts) {
                    try {
                        synchronizeAccountData(mService, updated_account, mServer, mToken);
                    }
                    catch (Exception e) {
                        if (raise_exception_on_error) {
                            throw e;
                        }
                    }
                }
                return true;
            }
            return false;
        }

        private boolean groupEquals(@Nullable final JSONObject loaded_group, @Nullable final JSONObject stored_group) throws Exception {
            boolean equals = ((loaded_group == null) && (stored_group == null));
            if ((loaded_group != null) && (stored_group != null)) {
                equals = true;
                for (String key : TWO_FACTOR_AUTH_GROUP_DATA_KEYS) {
                    if (! ObjectUtils.equals(loaded_group.opt(key), stored_group.opt(key))) {
                        equals = false;
                        break;
                    }
                }
            }
            return equals;
        }

        private boolean accountEquals(@NotNull final JSONObject loaded_account, @Nullable final JSONObject stored_account) throws Exception {
            if (stored_account != null) {
                boolean equals = true;
                for (String key : TWO_FACTOR_AUTH_ACCOUNT_DATA_KEYS) {
                    if (! ObjectUtils.equals(loaded_account.opt(key), stored_account.opt(key))) {
                        equals = false;
                        break;
                    }
                }
                return equals;
            }
            return false;
        }

        private boolean equals(@Nullable final Map<Integer, JSONObject> loaded_accounts, @Nullable final Map<Integer, JSONObject> loaded_groups, @Nullable final Map<Integer, TwoFactorAccount> stored_accounts) throws Exception {
            final int number_of_accounts_loaded = loaded_accounts == null ? 0 : loaded_accounts.size(), number_of_accounts_stored = stored_accounts == null ? 0 : stored_accounts.size();
            if (number_of_accounts_loaded == number_of_accounts_stored) {
                boolean equals = true;
                if (number_of_accounts_loaded != 0) {
                    for (int key : loaded_accounts.keySet()) {
                        final JSONObject loaded_account = loaded_accounts.get(key);
                        final TwoFactorAccount stored_account = stored_accounts.get(key);
                        if ((stored_account == null) || (! accountEquals(loaded_account, stored_account.toJSONObject()))) {
                            equals = false;
                            break;
                        }
                        else {
                            final int loaded_group_id = ((loaded_account.has(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_GROUP_KEY)) && (! loaded_account.isNull(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_GROUP_KEY))) ? loaded_account.getInt(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_GROUP_KEY) : 0;
                            if (! groupEquals(loaded_groups == null ? null : loaded_groups.get(loaded_group_id), stored_account.getGroup() == null ? null : stored_account.getGroup().toJSONObject())) {
                                equals = false;
                                break;
                            }
                        }
                    }
                }
                return equals;
            }
            return false;
        }

        private Map<Integer, TwoFactorAccount> getStoredAccounts() {
            final List<TwoFactorAccount> accounts = Database.TwoFactorAccountOperations.get();
            if ((accounts != null) && (! accounts.isEmpty())) {
                final Map<Integer, TwoFactorAccount> map = new HashMap<Integer, TwoFactorAccount>();
                for (final TwoFactorAccount account : accounts) {
                    final int id = account.getId();
                    if (id > 0) {
                        map.put(id, account);
                    }
                }
                return map;
            }
            return null;
        }

        @Override
        public Object onBackgroundTaskStarted(@Nullable final Object data) {
            SyncResultType result_type = MainService.SyncResultType.ERROR;
            final SharedPreferences.Editor editor = SharedPreferencesUtilities.getDefaultSharedPreferences(mService).edit();
            try {
                final boolean pending_updates_synced = syncPendingUpdates(true);
                final Map<Integer, JSONObject> loaded_accounts = JsonUtils.StringToJsonMap(getTwoFactorAuthCodes(true), Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_ID_KEY), loaded_groups = JsonUtils.StringToJsonMap(getTwoFactorAuthGroups(true), Constants.TWO_FACTOR_AUTH_GROUP_ID_KEY);
                final Map<Integer, TwoFactorAccount> stored_accounts = getStoredAccounts();
                if (equals(loaded_accounts, loaded_groups, stored_accounts)) {
                    result_type = pending_updates_synced ? SyncResultType.UPDATED : SyncResultType.NO_CHANGES;
                }
                else {
                    for (JSONObject object : loaded_accounts.values()) {
                        saveTwoFactorAuthIcon(object);
                    }
                    if (stored_accounts != null) {
                        for (Integer id : stored_accounts.keySet()) {
                            if (! loaded_accounts.containsKey(id)) {
                                stored_accounts.get(id).delete();
                            }
                        }
                    }
                    if (! Database.TwoFactorAccountAtomicOperations.set(loaded_accounts.values(), loaded_groups.values())) {
                        throw new Exception("Unexpected error when saving data to internal app database");
                    }
                    result_type = SyncResultType.UPDATED;
                }
                editor.putLong(Constants.LAST_SYNC_TIME_KEY, System.currentTimeMillis()).remove(Constants.LAST_SYNC_ERROR_KEY).remove(Constants.LAST_SYNC_ERROR_TIME_KEY);
                mService.sendBroadcast(new Intent(MainService.ACTION_SERVICE_DATA_SYNCED).putExtra(MainService.EXTRA_RESULT_TYPE, result_type.name()));
            }
            catch (Exception e) {
                editor.putString(Constants.LAST_SYNC_ERROR_KEY, e.getMessage()).putLong(Constants.LAST_SYNC_ERROR_TIME_KEY, System.currentTimeMillis());
                Log.e(Constants.LOG_TAG_NAME, "Exception while processing downloaded 2FA codes", e);
            }
            editor.apply();
            return result_type;
        }

        @Override
        public void onBackgroundTaskFinished(@NotNull final Object data) {
            mService.stopSelf((SyncResultType) data);
        }
    }

    private static void synchronizeAccountData(@NotNull final Context context, @NotNull final TwoFactorAccount account, @NotNull final String server, @NotNull final String token) throws Exception {
        final int id = account.getId();
        final boolean is_new_account = (id == 0);
        final JSONObject account_data = account.toJSONObject();
        final URL url = new URL(is_new_account ? GET_2FAUTH_ACCOUNT_NEW_DATA_LOCATION.replace("%SERVER%", server) : GET_2FAUTH_ACCOUNT_DATA_LOCATION.replace("%SERVER%", server).replace("%ID%", String.valueOf(id)));
        JSONObject server_account_data = null;
        if (is_new_account) {
            server_account_data = new JSONObject();
        }
        else {
            final HttpURLConnection connection = HttpUtils.get(url, GET_TWO_FACTOR_AUTH_TOKEN.replace("%TOKEN%", token));
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                server_account_data = JsonUtils.StringToJsonObject(HttpUtils.getContentString(connection));
            }
            else if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                account.delete();
            }
            else {
                throw new Exception((connection.getResponseMessage()));
            }
        }
        if (server_account_data != null) {
            boolean there_are_changes = false;
            for (final String key : TWO_FACTOR_AUTH_ACCOUNT_DATA_KEYS) {
                if ((! server_account_data.has(key)) || ((Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_COUNTER_KEY.equals(key)) && (server_account_data.optInt(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_COUNTER_KEY, 0) < account_data.optInt(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_COUNTER_KEY, 0))) || ((! Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_COUNTER_KEY.equals(key)) && (! ObjectUtils.equals(server_account_data.opt(key), account_data.opt(key))))) {
                    server_account_data.put(key, account_data.opt(key));
                    there_are_changes = true;
                }
            }
            if (there_are_changes) {
                final HttpURLConnection connection = HttpUtils.send(url, is_new_account ? "POST" : "PUT", GET_TWO_FACTOR_AUTH_TOKEN.replace("%TOKEN%", token), JsonUtils.JSonObjectToString(server_account_data));
                if (connection.getResponseCode() != (is_new_account ? HttpURLConnection.HTTP_CREATED : HttpURLConnection.HTTP_OK)) {
                    throw new Exception(connection.getResponseMessage());
                }
            }
            account.setSynced();
        }
    }

    public static boolean synchronizeAccountData(@NotNull final Context context, @NotNull final TwoFactorAccount account) {
        final String server = SharedPreferencesUtilities.getEncryptedString(context, Constants.TWO_FACTOR_AUTH_SERVER_LOCATION_KEY, null), token = SharedPreferencesUtilities.getEncryptedString(context, Constants.TWO_FACTOR_AUTH_TOKEN_KEY, null);
        if ((server != null) && (token != null)) {
            try {
                synchronizeAccountData(context, account, server, token);
                return true;
            }
            catch (Exception e) {
                Log.e(Constants.LOG_TAG_NAME, "Exception while trying to sync account data", e);
            }
        }
        return false;
    }

    public static Thread getBackgroundTask(@NotNull final MainService service, @NotNull final String server, @NotNull final String token) {
        return Main.getInstance().getBackgroundTask(new ServerDataSynchronizerImplementation(service, server, token));
    }
}
