package com.twofauth.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import com.twofauth.android.database.TwoFactorAccount;
import com.twofauth.android.database.TwoFactorGroup;
import com.twofauth.android.database.TwoFactorIcon;
import com.twofauth.android.database.TwoFactorServerIdentity;
import com.twofauth.android.utils.Bitmaps;
import com.twofauth.android.utils.HTTP;
import com.twofauth.android.utils.JSON;
import com.twofauth.android.utils.Preferences;
import com.twofauth.android.utils.Strings;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class API {
    public static final String ICON_SOURCE_DEFAULT = "bubka";
    public static final String ICON_SOURCE_DASHBOARD = "dashboard";

    private static final String SERVER_LOCATION = "%SERVER%";
    private static final String SERVER_API_LOCATION = SERVER_LOCATION + "/api/v1";

    private static final String ACCOUNTS_LOCATION = SERVER_API_LOCATION + "/twofaccounts";
    private static final String LIST_ACCOUNTS_LOCATION = ACCOUNTS_LOCATION + "?withSecret=1";
    private static final String ACCOUNT_LOCATION = ACCOUNTS_LOCATION + "/%ID%";

    private static final String GROUPS_LOCATION = SERVER_API_LOCATION + "/groups";
    private static final String GROUP_LOCATION = GROUPS_LOCATION + "/%ID%";

    private static final String ICONS_LOCATION = SERVER_API_LOCATION + "/icons";
    private static final String ICON_LOCATION = ICONS_LOCATION + "/%FILE%";
    private static final String GET_ICON_LOCATION = SERVER_LOCATION + "/storage/icons/%FILE%";
    private static final String ICON_DATA_NAME = "icon";

    private static final String ACCOUNT_QR_LOCATION = ACCOUNT_LOCATION + "/qrcode";
    private static final String QR_CODE_JSON_KEY = "qrcode";

    private static final String QR_DECODER_LOCATION = SERVER_API_LOCATION + "/qrcode/decode";
    private static final String QR_DATA_NAME = "qrcode";

    private static final String GET_USER_DATA_LOCATION = SERVER_API_LOCATION + "/user";

    private static final String AUTH_TOKEN = "Bearer %TOKEN%";

    private static class DashBoardIconsUtils {
        private static final String BASE_LOCATION = "https://cdn.jsdelivr.net/gh/homarr-labs/dashboard-icons/png/%SERVICE%.png";

        public static final String DARK_THEMED_ICON = "-dark";
        public static final String LIGHT_THEMED_ICON = "-light";
        public static final String NO_THEMED_ICON = "";

        private static @NotNull String standarizeServiceName(@NotNull final String service, @Nullable final String theme) {
            String standarized_service_name = service.toLowerCase().replace(" ", "-");
            if ((theme != null) && (! NO_THEMED_ICON.equals(theme))) { standarized_service_name += theme; }
            return standarized_service_name;
        }

        public static @NotNull Bitmap getIcon(@NotNull final String service, @Nullable final String theme) throws Exception {
            final HttpURLConnection connection = HTTP.get(new URL(BASE_LOCATION.replace("%SERVICE%", standarizeServiceName(service, theme))));
            return (connection.getResponseCode() == HttpURLConnection.HTTP_OK) ? Bitmaps.get(connection) : null;
        }
    }

    public static boolean refreshIdentityData(@NotNull final TwoFactorServerIdentity server_identity, final boolean raise_exception_on_error) throws Exception {
        final HttpURLConnection connection = HTTP.get(new URL(GET_USER_DATA_LOCATION.replace("%SERVER%", server_identity.getServer())), AUTH_TOKEN.replace("%TOKEN%", server_identity.getToken()));
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            final JSONObject object = JSON.toJSONObject(HTTP.getContentString(connection));
            server_identity.setRemoteId(object.optInt(Constants.USER_DATA_ID_KEY, 0));
            server_identity.setName(object.optString(Constants.USER_DATA_NAME_KEY, ""));
            server_identity.setEmail(object.optString(Constants.USER_DATA_EMAIL_KEY, ""));
            server_identity.setIsAdmin(object.optBoolean(Constants.USER_DATA_IS_ADMIN_KEY, false));
            return true;
        }
        else if (raise_exception_on_error) {
            throw new Exception(connection.getResponseMessage());
        }
        return false;
    }

    public static @Nullable List<JSONObject> getAccounts(@NotNull final TwoFactorServerIdentity server_identity, final boolean raise_exception_on_error) throws Exception {
        final HttpURLConnection connection = HTTP.get(new URL(LIST_ACCOUNTS_LOCATION.replace("%SERVER%", server_identity.getServer())), AUTH_TOKEN.replace("%TOKEN%", server_identity.getToken()));
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) { return JSON.toListOfJSONObjects(HTTP.getContentString(connection)); }
        else if (raise_exception_on_error) { throw new Exception(connection.getResponseMessage()); }
        return null;
    }

    public static @Nullable List<TwoFactorGroup> getGroups(@NotNull final TwoFactorServerIdentity server_identity, @Nullable final Collection<JSONObject> accounts_objects, final boolean raise_exception_on_network_error) throws Exception {
        final HttpURLConnection connection = HTTP.get(new URL(GROUPS_LOCATION.replace("%SERVER%", server_identity.getServer())), AUTH_TOKEN.replace("%TOKEN%", server_identity.getToken()));
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            final List<JSONObject> groups_objects = JSON.toListOfJSONObjects(HTTP.getContentString(connection));
            if (groups_objects != null) {
                final Map<Integer, TwoFactorGroup> groups = new HashMap<Integer, TwoFactorGroup>();
                for (final JSONObject group_object : groups_objects) {
                    if (group_object.has(Constants.GROUP_DATA_ID_KEY) && (! group_object.isNull(Constants.GROUP_DATA_ID_KEY)) && (group_object.getInt(Constants.GROUP_DATA_ID_KEY) != 0)) {
                        final TwoFactorGroup group = new TwoFactorGroup();
                        group.setServerIdentity(server_identity);
                        group.setRemoteId(group_object.getInt(Constants.GROUP_DATA_ID_KEY));
                        group.setName(group_object.getString(Constants.GROUP_DATA_NAME_KEY));
                        groups.put(group.getRemoteId(), group);
                    }
                }
                if (accounts_objects != null) {
                    for (final JSONObject account_object : accounts_objects) {
                        if (account_object.has(Constants.ACCOUNT_DATA_GROUP_KEY) && (! account_object.isNull(Constants.ACCOUNT_DATA_GROUP_KEY))) {
                            account_object.put(Constants.ACCOUNT_DATA_GROUP_KEY, groups.get(account_object.getInt(Constants.ACCOUNT_DATA_GROUP_KEY)));
                        }
                    }
                }
                return new ArrayList<TwoFactorGroup>(groups.values());
            }
        }
        else if (raise_exception_on_network_error) {
            throw new Exception(connection.getResponseMessage());
        }
        return null;
    }

    public static void getIcons(@NotNull final TwoFactorServerIdentity server_identity, @NotNull final Context context, @Nullable final Collection<JSONObject> accounts_objects, final boolean raise_exception_on_network_error) throws Exception {
        if (accounts_objects != null) {
            final Map<String, TwoFactorIcon> icons_map_by_icon_file = new HashMap<String, TwoFactorIcon>(), icons_map_by_service = new HashMap<String, TwoFactorIcon>();
            final boolean download_icons_from_external_sources = Preferences.getDefaultSharedPreferences(context).getBoolean(Constants.DOWNLOAD_ICONS_FROM_EXTERNAL_SOURCES_KEY, context.getResources().getBoolean(R.bool.download_icons_from_external_sources));
            for (final JSONObject account_object : accounts_objects) {
                TwoFactorIcon icon = null;
                final String server_icon_file = (account_object.has(Constants.ACCOUNT_DATA_ICON_KEY) && (! account_object.isNull(Constants.ACCOUNT_DATA_ICON_KEY))) ? account_object.getString(Constants.ACCOUNT_DATA_ICON_KEY) : null, service = (account_object.has(Constants.ACCOUNT_DATA_SERVICE_KEY) && (! account_object.isNull(Constants.ACCOUNT_DATA_SERVICE_KEY))) ? account_object.getString(Constants.ACCOUNT_DATA_SERVICE_KEY) : null;
                final boolean server_icon_supported = ((! Strings.isEmptyOrNull(server_icon_file)) && (! server_icon_file.toLowerCase().endsWith(".svg")));
                account_object.remove(Constants.ACCOUNT_DATA_ICON_KEY);
                for (final String theme : new String[] { null, DashBoardIconsUtils.DARK_THEMED_ICON, DashBoardIconsUtils.LIGHT_THEMED_ICON }) {
                    Bitmap bitmap = null;
                    if (server_icon_supported && (theme == null)) {
                        if (icons_map_by_icon_file.containsKey(server_icon_file)) {
                            icon = icons_map_by_icon_file.get(server_icon_file);
                            break;
                        }
                        bitmap = getIcon(server_identity, server_icon_file, raise_exception_on_network_error);
                    }
                    else if ((! server_icon_supported) && (download_icons_from_external_sources) && (! Strings.isEmptyOrNull(service))) {
                        if (icons_map_by_service.containsKey(service)) {
                            icon = icons_map_by_service.get(service);
                            break;
                        }
                        bitmap = DashBoardIconsUtils.getIcon(service, theme == null ? DashBoardIconsUtils.NO_THEMED_ICON : theme);
                    }
                    if (bitmap != null) {
                        if (icon == null) {
                            icon = new TwoFactorIcon();
                            icon.setSourceData(server_icon_supported ? ICON_SOURCE_DEFAULT : ICON_SOURCE_DASHBOARD, server_icon_supported ? server_icon_file : service);
                        }
                        icon.setBitmap(bitmap, DashBoardIconsUtils.DARK_THEMED_ICON.equals(theme) ? TwoFactorIcon.BitmapTheme.DARK.DARK : DashBoardIconsUtils.LIGHT_THEMED_ICON.equals(theme) ? TwoFactorIcon.BitmapTheme.LIGHT : null);
                    }
                }
                if (icon != null) {
                    if (server_icon_supported) {
                        icons_map_by_icon_file.put(server_icon_file, icon);
                    }
                    else {
                        icons_map_by_service.put(service, icon);
                    }
                    account_object.put(Constants.ACCOUNT_DATA_ICON_KEY, icon);
                }
            }
        }
    }

    public static @Nullable Bitmap getIcon(@NotNull final TwoFactorServerIdentity server_identity, @NotNull final String icon_id, final boolean raise_exception_on_network_error) throws Exception {
        final HttpURLConnection connection = HTTP.get(new URL(GET_ICON_LOCATION.replace("%SERVER%", server_identity.getServer()).replace("%FILE%", icon_id)), AUTH_TOKEN.replace("%TOKEN%", server_identity.getToken()));
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) { return Bitmaps.get(connection); }
        else if (raise_exception_on_network_error) { throw new Exception(connection.getResponseMessage()); }
        return null;
    }

    public static @Nullable String getQR(@NotNull final TwoFactorServerIdentity server_identity, final int account_id, final boolean raise_exception_on_network_error) throws Exception {
        final HttpURLConnection connection = HTTP.get(new URL(ACCOUNT_QR_LOCATION.replace("%SERVER%", server_identity.getServer()).replace("%ID%", String.valueOf(account_id))), AUTH_TOKEN.replace("%TOKEN%", server_identity.getToken()));
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) { return JSON.toJSONObject(HTTP.getContentString(connection)).optString(QR_CODE_JSON_KEY); }
        else if (raise_exception_on_network_error) { throw new Exception(connection.getResponseMessage()); }
        return null;
    }

    public static @Nullable String getQR(@NotNull final TwoFactorAccount account, final boolean raise_exception_on_network_error) throws Exception {
        return getQR(account.getServerIdentity(), account.getRemoteId(), raise_exception_on_network_error);
    }

    public static boolean syncAccount(@NotNull final SQLiteDatabase database, @NotNull final Context context, @NotNull final TwoFactorAccount account, boolean raise_exception_on_network_error) throws Exception {
        final URL url = new URL(account.isRemote() ? ACCOUNT_LOCATION.replace("%SERVER%", account.getServerIdentity().getServer()).replace("%ID%", String.valueOf(account.getRemoteId())) : ACCOUNTS_LOCATION.replace("%SERVER%", account.getServerIdentity().getServer()));
        final String authorization = AUTH_TOKEN.replace("%TOKEN%", account.getServerIdentity().getToken());
        if (account.isDeleted()) {
            if (account.isRemote()) {
                // Is a server account that was locally deleted we try to remote delete
                final HttpURLConnection connection = HTTP.delete(url, authorization);
                if ((connection.getResponseCode() == HttpURLConnection.HTTP_OK) || (connection.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT) || (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND)) {
                    // Account is remote deleted then we delete locally
                    account.delete(database, context);
                    return true;
                }
                else if (raise_exception_on_network_error) {
                    throw new Exception(connection.getResponseMessage());
                }
            }
            else {
                // Is a local account and has been deleted
                account.delete(database, context);
                return true;
            }
            return false;
        }
        else if (! account.isSynced()) {
            // Account is out of sync
            // First, synchronize icon, if any, but do not stop the process if there's a related error (except a network error)
            putIcon(account.getServerIdentity(), database, context, account.getIcon(), raise_exception_on_network_error);
            // After synchronize icon we try to synchronize account, but before we synchronize or create group data, if any
            if (! account.hasGroup() || syncGroup(database, context, account.getGroup(), raise_exception_on_network_error)) {
                if (account.isRemote()) {
                    // Is an account that already exists at server, we try to update
                    final HttpURLConnection connection = HTTP.put(url, authorization, JSON.toString(account.toJSONObject()));
                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        // Account has been updated
                        account.setStatus(TwoFactorAccount.STATUS_DEFAULT);
                        account.save(database, context);
                        return true;
                    }
                    else if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                        // Account is not found at server we delete locally
                        account.delete(database, context);
                        return true;
                    }
                    else if (raise_exception_on_network_error) {
                        throw new Exception(connection.getResponseMessage());
                    }
                }
                else {
                    // Account doesn't exists at remote, we try to add
                    final HttpURLConnection connection = HTTP.post(url, authorization, JSON.toString(account.toJSONObject()));
                    if (connection.getResponseCode() == HttpURLConnection.HTTP_CREATED) {
                        // Account has been added
                        final JSONObject object = JSON.toJSONObject(HTTP.getContentString(connection));
                        account.setRemoteId(object.getInt(Constants.ACCOUNT_DATA_ID_KEY));
                        account.setStatus(TwoFactorAccount.STATUS_DEFAULT);
                        account.save(database, context);
                        return true;
                    }
                    else if (raise_exception_on_network_error) {
                        throw new Exception(connection.getResponseMessage());
                    }
                }
            }
            return false;
        }
        return true;
    }

    // This function looks similar to synchronizing accounts but we have to take into account the case where a group cannot be deleted locally because it still has references

    public static boolean syncGroup(@NotNull final SQLiteDatabase database, @NotNull final Context context, @NotNull final TwoFactorGroup group, boolean raise_exception_on_network_error) throws Exception {
        final URL url = new URL(group.isRemote() ? GROUP_LOCATION.replace("%SERVER%", group.getServerIdentity().getServer()).replace("%ID%", String.valueOf(group.getRemoteId())) : GROUPS_LOCATION.replace("%SERVER%", group.getServerIdentity().getServer()));
        final String authorization = AUTH_TOKEN.replace("%TOKEN%", group.getServerIdentity().getToken());
        if (group.isDeleted()) {
            if (group.isRemote()) {
                // Is a server group that was locally deleted we try to remote delete
                final HttpURLConnection connection = HTTP.delete(url, authorization);
                if ((connection.getResponseCode() == HttpURLConnection.HTTP_OK) || (connection.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT) || (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND)) {
                    // First we update the group instead of delete it, to prevent a recurrent deletion if group cannot be locally deleted (due to it's FK restrictions)
                    group.setRemoteId(0);
                    group.setStatus(TwoFactorAccount.STATUS_DEFAULT);
                    group.save(database, context);
                    // Now we try to delete the group
                    if (! group.isReferenced(database)) { group.delete(database, context); }
                    return true;
                }
                else if (raise_exception_on_network_error) {
                    throw new Exception(connection.getResponseMessage());
                }
            }
            else {
                // We only can delete a group if it's not referenced (by any account)
                if (! group.isReferenced(database)) { group.delete(database, context); }
                return true;
            }
            return false;
        }
        else if (! group.isSynced()) {
            // Group is out of sync
            if (group.isRemote()) {
                // Is an group that already exists at server, we try to update
                final HttpURLConnection connection = HTTP.put(url, authorization, JSON.toString(group.toJSONObject()));
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    // Group has been updated
                    group.setStatus(TwoFactorAccount.STATUS_DEFAULT);
                    group.save(database, context);
                    return true;
                }
                else if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                    // Group is not found at server we delete locally
                    group.delete(database, context);
                    return true;
                }
                else if (raise_exception_on_network_error) {
                    throw new Exception(connection.getResponseMessage());
                }
            }
            else {
                // Group doesn't exists at remote, we try to add
                final HttpURLConnection connection = HTTP.post(url, authorization, JSON.toString(group.toJSONObject()));
                if (connection.getResponseCode() == HttpURLConnection.HTTP_CREATED) {
                    // Group has been added
                    final JSONObject object = JSON.toJSONObject(HTTP.getContentString(connection));
                    group.setRemoteId(object.getInt(Constants.GROUP_DATA_ID_KEY));
                    group.setStatus(TwoFactorAccount.STATUS_DEFAULT);
                    group.save(database, context);
                    return true;
                }
                else if (raise_exception_on_network_error) {
                    throw new Exception(connection.getResponseMessage());
                }
            }
            return false;
        }
        return true;
    }

    public static boolean putIcon(@NotNull final TwoFactorServerIdentity server_identity, @NotNull final SQLiteDatabase database, @NotNull final Context context, @Nullable final TwoFactorIcon icon, final boolean raise_exception_on_network_error) throws Exception {
        if ((icon != null) && (icon.getSource() == null)) {
            final HttpURLConnection connection = HTTP.post(new URL(ICONS_LOCATION.replace("%SERVER%", server_identity.getServer())), AUTH_TOKEN.replace("%TOKEN%", server_identity.getToken()), Bitmaps.bytes(icon.getBitmap(context, null)), ICON_DATA_NAME, HTTP.CONTENT_TYPE_MULTIPART_FORM_DATA);
            if (connection.getResponseCode() == HttpURLConnection.HTTP_CREATED) {
                final JSONObject object = JSON.toJSONObject(HTTP.getContentString(connection));
                icon.setSourceData(ICON_SOURCE_DEFAULT, object.getString(Constants.ICON_DATA_ID_KEY));
                icon.unsetThemedBitmaps();
                icon.save(database, context);
                return true;
            }
            else if (raise_exception_on_network_error) {
                throw new Exception(connection.getResponseMessage());
            }
        }
        return false;
    }

    public static @Nullable TwoFactorAccount decodeQR(@NotNull final TwoFactorServerIdentity server_identity, @NotNull final Bitmap qr) throws Exception {
        final URL url = new URL(QR_DECODER_LOCATION.replace("%SERVER%", server_identity.getServer()));
        final String authorization = AUTH_TOKEN.replace("%TOKEN%", server_identity.getToken());
        final HttpURLConnection connection = HTTP.post(url, authorization, Bitmaps.bytes(qr), QR_DATA_NAME, HTTP.CONTENT_TYPE_MULTIPART_FORM_DATA);
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            final Uri uri = Uri.parse(JSON.toJSONObject(HTTP.getContentString(connection)).getString("data"));
            if (Strings.equals(uri.getScheme(), "otpauth")) {
                final TwoFactorAccount account = new TwoFactorAccount();
                account.setOtpType(uri.getHost());
                final String[] label_parts = uri.getPath().substring(1).split(":");
                account.setService(label_parts[0]);
                account.setAccount(label_parts.length > 1 ? label_parts[1] : "");
                for (String param : uri.getQuery().split("&")) {
                    final String[] key_value = param.split("=");
                    if (key_value.length == 2) {
                        if (Constants.ACCOUNT_DATA_SECRET_KEY.equals(key_value[0])) { account.setSecret(key_value[1]); }
                        else if (Constants.ACCOUNT_DATA_ALGORITHM_KEY.equals(key_value[0])) { account.setAlgorithm(key_value[1]); }
                        else if (Constants.ACCOUNT_DATA_OTP_LENGTH_KEY.equals(key_value[0])) { account.setOtpLength(Strings.parseInt(key_value[1], Constants.DEFAULT_OTP_LENGTH)); }
                        else if (Constants.ACCOUNT_DATA_PERIOD_KEY.equals(key_value[0])) { account.setPeriod(Strings.parseInt(key_value[1], Constants.DEFAULT_PERIOD)); }
                        else if (Constants.ACCOUNT_DATA_COUNTER_KEY.equals(key_value[0])) { account.setPeriod(Strings.parseInt(key_value[1], Constants.DEFAULT_COUNTER)); }
                        else if (Constants.QR_DATA_ISSUER_KEY.equals(key_value[0]) && Strings.isEmptyOrNull(account.getAccount()) && (! Strings.isEmptyOrNull(key_value[1]))) { account.setAccount(key_value[1]); }
                    }
                }
                return account;
            }
        }
        return null;
    }
}
