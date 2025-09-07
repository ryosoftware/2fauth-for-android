package com.twofauth.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;

import com.twofauth.android.database.TwoFactorAccount;
import com.twofauth.android.database.TwoFactorGroup;
import com.twofauth.android.database.TwoFactorIcon;
import com.twofauth.android.database.TwoFactorIcon.BitmapTheme;
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

        private static final String DARK_THEMED_ICON = "-dark";
        private static final String LIGHT_THEMED_ICON = "-light";
        private static final String NO_THEMED_ICON = "";

        public static @NotNull String standardizeServiceName(@NotNull final String service) {
            return service.toLowerCase().replace(" ", "-");
        }

        private static @NotNull String standardizeServiceName(@NotNull final String service, @Nullable final String theme) {
            String standarized_service_name = standardizeServiceName(service);
            if (! Strings.isEmptyOrNull(theme)) { standarized_service_name += theme; }
            return standarized_service_name;
        }

        public static @Nullable Bitmap getIcon(@NotNull final String service, @Nullable final BitmapTheme theme) throws Exception {
            final HttpURLConnection connection = HTTP.get(new URL(BASE_LOCATION.replace("%SERVICE%", standardizeServiceName(service, theme == null ? NO_THEMED_ICON : theme == BitmapTheme.DARK ? DARK_THEMED_ICON : LIGHT_THEMED_ICON))), false);
            return (connection.getResponseCode() == HttpURLConnection.HTTP_OK) ? Bitmaps.get(connection) : null;
        }
    }

    private static boolean areInsecureCertificatesAllowed(@NotNull final Context context) {
        return Preferences.getDefaultSharedPreferences(context).getBoolean(Constants.ALLOW_INSECURE_CERTIFICATES_KEY, context.getResources().getBoolean(R.bool.allow_insecure_certificates));
    }

    private static void raiseNetworkErrorException(@NotNull final HttpURLConnection connection) throws Exception {
        if (connection.getResponseCode() == 422) {
            final String detailed_errors = HTTP.getErrorString(connection);
            if (! Strings.isEmptyOrNull(detailed_errors)) { throw new Exception(detailed_errors); }
        }
        throw new Exception(connection.getResponseMessage());
    }

    public static boolean refreshIdentityData(@NotNull final TwoFactorServerIdentity server_identity, @NotNull final Context context, final boolean raise_exception_on_error) throws Exception {
        final HttpURLConnection connection = HTTP.get(new URL(GET_USER_DATA_LOCATION.replace("%SERVER%", server_identity.getServer())), AUTH_TOKEN.replace("%TOKEN%", server_identity.getToken()), areInsecureCertificatesAllowed(context));
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            final JSONObject object = JSON.toJSONObject(HTTP.getContentString(connection));
            server_identity.setRemoteId(object.optInt(Constants.USER_DATA_ID_KEY, 0));
            server_identity.setName(object.optString(Constants.USER_DATA_NAME_KEY, ""));
            server_identity.setEmail(object.optString(Constants.USER_DATA_EMAIL_KEY, ""));
            server_identity.setIsAdmin(object.optBoolean(Constants.USER_DATA_IS_ADMIN_KEY, false));
            return true;
        }
        else if (raise_exception_on_error) {
            raiseNetworkErrorException(connection);
        }
        return false;
    }

    public static @Nullable List<JSONObject> getAccounts(@NotNull final TwoFactorServerIdentity server_identity, @NotNull final Context context, final boolean raise_exception_on_error) throws Exception {
        final HttpURLConnection connection = HTTP.get(new URL(LIST_ACCOUNTS_LOCATION.replace("%SERVER%", server_identity.getServer())), AUTH_TOKEN.replace("%TOKEN%", server_identity.getToken()), areInsecureCertificatesAllowed(context));
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) { return JSON.toListOfJSONObjects(HTTP.getContentString(connection)); }
        else if (raise_exception_on_error) { raiseNetworkErrorException(connection); }
        return null;
    }

    public static @Nullable List<TwoFactorGroup> getGroups(@NotNull final TwoFactorServerIdentity server_identity, @NotNull final Context context, final boolean raise_exception_on_network_error) throws Exception {
        final HttpURLConnection connection = HTTP.get(new URL(GROUPS_LOCATION.replace("%SERVER%", server_identity.getServer())), AUTH_TOKEN.replace("%TOKEN%", server_identity.getToken()), areInsecureCertificatesAllowed(context));
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            final List<JSONObject> groups_objects = JSON.toListOfJSONObjects(HTTP.getContentString(connection));
            if (groups_objects != null) {
                final List<TwoFactorGroup> groups = new ArrayList<TwoFactorGroup>();
                for (final JSONObject group_object : groups_objects) {
                    if (group_object.has(Constants.GROUP_DATA_ID_KEY) && (! group_object.isNull(Constants.GROUP_DATA_ID_KEY)) && (group_object.getInt(Constants.GROUP_DATA_ID_KEY) != 0)) {
                        final TwoFactorGroup group = new TwoFactorGroup();
                        group.setServerIdentity(server_identity);
                        group.setRemoteId(group_object.getInt(Constants.GROUP_DATA_ID_KEY));
                        group.setName(group_object.getString(Constants.GROUP_DATA_NAME_KEY));
                        group.setStatus(TwoFactorGroup.STATUS_DEFAULT);
                        groups.add(group);
                    }
                }
                return groups;
            }
        }
        else if (raise_exception_on_network_error) {
            raiseNetworkErrorException(connection);
        }
        return null;
    }

    private static @Nullable Bitmap getBitmapFromDatabase(@NotNull final SQLiteDatabase database, @NotNull final Context context, @NotNull final TwoFactorServerIdentity server_identity, @NotNull final String icon_id) throws Exception {
        final TwoFactorIcon icon = Main.getInstance().getDatabaseHelper().getTwoFactorIconsHelper().get(database, server_identity, ICON_SOURCE_DEFAULT, icon_id);
        return (icon == null) ? null : icon.getBitmap(context, null);
    }

    private static @Nullable Bitmap getBitmapFromDatabase(@NotNull final SQLiteDatabase database, @NotNull final Context context, @NotNull final String service, @Nullable final BitmapTheme theme) throws Exception {
        final TwoFactorIcon icon = Main.getInstance().getDatabaseHelper().getTwoFactorIconsHelper().get(database, null, ICON_SOURCE_DASHBOARD, service);
        return (icon == null) ? null : icon.getBitmap(context, theme);
    }

    private static @Nullable Bitmap getBitmapFromServer(@NotNull final TwoFactorServerIdentity server_identity, @NotNull final String icon_id, @NotNull final Context context, final boolean raise_exception_on_network_error) throws Exception {
        final HttpURLConnection connection = HTTP.get(new URL(GET_ICON_LOCATION.replace("%SERVER%", server_identity.getServer()).replace("%FILE%", icon_id)), AUTH_TOKEN.replace("%TOKEN%", server_identity.getToken()), areInsecureCertificatesAllowed(context));
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) { return Bitmaps.get(connection); }
        else if (raise_exception_on_network_error) { raiseNetworkErrorException(connection); }
        return null;
    }

    private static @Nullable Bitmap getBitmapFromExternalSource(@NotNull final String service, @Nullable final BitmapTheme theme) throws Exception { return DashBoardIconsUtils.getIcon(service, theme); }

    private static TwoFactorIcon getIcon(@NotNull final SQLiteDatabase database, @NotNull final Context context, @NotNull final TwoFactorServerIdentity server_identity, @Nullable final String server_icon_file, @Nullable final String service, @Nullable final Map<String, TwoFactorIcon> icons_map_by_icon_file, @Nullable final Map<String, TwoFactorIcon> icons_map_by_service, final boolean download_icons_from_external_sources, final boolean download_icons_from_external_sources_only_one_time, final boolean raise_exception_on_network_error) throws Exception {
        TwoFactorIcon icon = null;
        final boolean server_icon_supported = ((! Strings.isEmptyOrNull(server_icon_file)) && (! server_icon_file.toLowerCase().endsWith(".svg")));
        boolean not_themed_icon_exists = false;
        for (final BitmapTheme theme : new BitmapTheme[] { null, BitmapTheme.DARK, BitmapTheme.LIGHT }) {
            Bitmap bitmap = null;
            if (server_icon_supported && (theme == null)) {
                if ((icons_map_by_icon_file != null) && icons_map_by_icon_file.containsKey(server_icon_file)) { icon = icons_map_by_icon_file.get(server_icon_file); break; }
                bitmap = getBitmapFromDatabase(database, context, server_identity, server_icon_file);
                if (bitmap == null) { bitmap = getBitmapFromServer(server_identity, server_icon_file, context, raise_exception_on_network_error); }
            }
            else if ((! server_icon_supported) && (download_icons_from_external_sources || download_icons_from_external_sources_only_one_time) && (! Strings.isEmptyOrNull(service))) {
                if ((icons_map_by_service != null) && icons_map_by_service.containsKey(service)) { icon = icons_map_by_service.get(service); break; }
                bitmap = getBitmapFromDatabase(database, context, service, theme);
                if (download_icons_from_external_sources && ((bitmap == null) || (! download_icons_from_external_sources_only_one_time)) && ((theme == null) || (! download_icons_from_external_sources_only_one_time) || (! not_themed_icon_exists))) { final Bitmap downloaded_bitmap = getBitmapFromExternalSource(service, theme); bitmap = (downloaded_bitmap == null) ? bitmap : downloaded_bitmap; }
                else if ((theme == null) && (bitmap != null)) { not_themed_icon_exists = true; }
            }
            if (bitmap != null) {
                if (icon == null) { icon = new TwoFactorIcon(); icon.setSourceData(server_icon_supported ? ICON_SOURCE_DEFAULT : ICON_SOURCE_DASHBOARD, server_icon_supported ? server_icon_file : service); }
                icon.setBitmap(bitmap, theme);
            }
        }
        if (icon != null) {
            if ((icons_map_by_icon_file != null) && server_icon_supported) { icons_map_by_icon_file.put(server_icon_file, icon); }
            else if ((icons_map_by_service != null) && (! server_icon_supported)) { icons_map_by_service.put(service, icon); }
        }
        return icon;
    }

    private static TwoFactorIcon getIcon(@NotNull final SQLiteDatabase database, @NotNull final Context context, @NotNull final TwoFactorServerIdentity server_identity, @Nullable final String server_icon_file, @Nullable final String service, @Nullable final Map<String, TwoFactorIcon> icons_map_by_icon_file, @Nullable final Map<String, TwoFactorIcon> icons_map_by_service, final boolean raise_exception_on_network_error) throws Exception {
        final SharedPreferences preferences = Preferences.getDefaultSharedPreferences(context);
        final Resources resources = context.getResources();
        final boolean download_icons_from_external_sources = preferences.getBoolean(Constants.DOWNLOAD_ICONS_FROM_EXTERNAL_SOURCES_KEY, resources.getBoolean(R.bool.download_icons_from_external_sources)), download_icons_from_external_sources_only_one_time = preferences.getBoolean(Constants.DOWNLOAD_ICONS_FROM_EXTERNAL_SOURCES_ONLY_ONE_TIME_KEY, resources.getBoolean(R.bool.download_icons_from_external_sources_only_one_time));
        return getIcon(database, context, server_identity, server_icon_file, service, icons_map_by_icon_file, icons_map_by_service, download_icons_from_external_sources, download_icons_from_external_sources_only_one_time, raise_exception_on_network_error);
    }

    private static TwoFactorIcon getIcon(@NotNull final SQLiteDatabase database, @NotNull final Context context, @NotNull final TwoFactorServerIdentity server_identity, @Nullable final String server_icon_file, @Nullable final String service, final boolean raise_exception_on_network_error) throws Exception { return getIcon(database, context, server_identity, server_icon_file, service, null, null, raise_exception_on_network_error); }

    public static void getIcons(@NotNull final SQLiteDatabase database, @NotNull final Context context, @NotNull final TwoFactorServerIdentity server_identity, @Nullable final Collection<JSONObject> accounts_objects, final boolean raise_exception_on_network_error) throws Exception {
        if (accounts_objects != null) {
            final Map<String, TwoFactorIcon> icons_map_by_icon_file = new HashMap<String, TwoFactorIcon>(), icons_map_by_service = new HashMap<String, TwoFactorIcon>();
            final SharedPreferences preferences = Preferences.getDefaultSharedPreferences(context);
            final Resources resources = context.getResources();
            final boolean download_icons_from_external_sources = preferences.getBoolean(Constants.DOWNLOAD_ICONS_FROM_EXTERNAL_SOURCES_KEY, resources.getBoolean(R.bool.download_icons_from_external_sources)), download_icons_from_external_sources_only_one_time = preferences.getBoolean(Constants.DOWNLOAD_ICONS_FROM_EXTERNAL_SOURCES_ONLY_ONE_TIME_KEY, resources.getBoolean(R.bool.download_icons_from_external_sources_only_one_time));
            for (final JSONObject account_object : accounts_objects) {
                TwoFactorIcon icon = getIcon(database, context, server_identity, (account_object.has(Constants.ACCOUNT_DATA_ICON_KEY) && (! account_object.isNull(Constants.ACCOUNT_DATA_ICON_KEY))) ? account_object.getString(Constants.ACCOUNT_DATA_ICON_KEY) : null, (account_object.has(Constants.ACCOUNT_DATA_SERVICE_KEY) && (! account_object.isNull(Constants.ACCOUNT_DATA_SERVICE_KEY))) ? DashBoardIconsUtils.standardizeServiceName(account_object.getString(Constants.ACCOUNT_DATA_SERVICE_KEY)) : null, icons_map_by_icon_file, icons_map_by_service, download_icons_from_external_sources, download_icons_from_external_sources_only_one_time, raise_exception_on_network_error);
                if (icon == null) { account_object.remove(Constants.ACCOUNT_DATA_ICON_KEY); }
                else { account_object.put(Constants.ACCOUNT_DATA_ICON_KEY, icon); }
            }
        }
    }

    public static @Nullable String getQR(@NotNull final TwoFactorServerIdentity server_identity, final int account_id, @NotNull final Context context, final boolean raise_exception_on_network_error) throws Exception {
        final HttpURLConnection connection = HTTP.get(new URL(ACCOUNT_QR_LOCATION.replace("%SERVER%", server_identity.getServer()).replace("%ID%", String.valueOf(account_id))), AUTH_TOKEN.replace("%TOKEN%", server_identity.getToken()), areInsecureCertificatesAllowed(context));
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) { return JSON.toJSONObject(HTTP.getContentString(connection)).optString(QR_CODE_JSON_KEY); }
        else if (raise_exception_on_network_error) { raiseNetworkErrorException(connection); }
        return null;
    }

    public static @Nullable String getQR(@NotNull final TwoFactorAccount account, @NotNull final Context context, final boolean raise_exception_on_network_error) throws Exception {
        return getQR(account.getServerIdentity(), account.getRemoteId(), context, raise_exception_on_network_error);
    }

    private static void onAccountDataUpdated(@NotNull final SQLiteDatabase database, @NotNull final Context context, @NotNull final TwoFactorAccount account, @NotNull final JSONObject object, final boolean reload_icon, final boolean raise_exception_on_network_error) throws Exception {
        account.fromJSONObject(database, account.getServerIdentity(), object);
        if (reload_icon) {
            final String server_icon_file = (object.has(Constants.ACCOUNT_DATA_ICON_KEY) && (! object.isNull(Constants.ACCOUNT_DATA_ICON_KEY))) ? object.getString(Constants.ACCOUNT_DATA_ICON_KEY) : null, service = DashBoardIconsUtils.standardizeServiceName(account.getService());
            final TwoFactorIcon icon = getIcon(database, context, account.getServerIdentity(), server_icon_file, service, raise_exception_on_network_error);
            if (account.hasIcon()) { account.getIcon().setBitmaps(context, icon); }
            else { account.setIcon(icon); }
        }
        account.setStatus(TwoFactorAccount.STATUS_DEFAULT);
        account.save(database, context);
    }

    private static void onAccountDataRemoved(@NotNull final SQLiteDatabase database, @NotNull final Context context, @NotNull final TwoFactorAccount account, final boolean physical_remove, final boolean raise_exception_on_network_error) throws Exception {
        final TwoFactorIcon icon = account.getIcon();
        if (icon != null) {
            if (API.ICON_SOURCE_DEFAULT.equals(icon.getSource())) {
                final String source_id = icon.getSourceId();
                if (! Strings.isEmptyOrNull(source_id)) { deleteIcon(account.getServerIdentity(), source_id, context, raise_exception_on_network_error); }
                icon.setSourceData(null, null);
            }
            if (physical_remove) { icon.setBitmaps((Bitmap) null, (Bitmap) null, (Bitmap) null); }
        }
        account.setRemoteId(0);
        if (physical_remove) { account.delete(database, context); }
        else { account.setStatus(TwoFactorAccount.STATUS_DELETED); account.save(database, context); }
    }

    public static boolean synchronizeAccount(@NotNull final SQLiteDatabase database, @NotNull final Context context, @NotNull final TwoFactorAccount account, final boolean reload_data_after_synchronization, final boolean raise_exception_on_network_error) throws Exception {
        final URL url = new URL(account.isRemote() ? ACCOUNT_LOCATION.replace("%SERVER%", account.getServerIdentity().getServer()).replace("%ID%", String.valueOf(account.getRemoteId())) : ACCOUNTS_LOCATION.replace("%SERVER%", account.getServerIdentity().getServer()));
        final String authorization = AUTH_TOKEN.replace("%TOKEN%", account.getServerIdentity().getToken());
        if (account.isDeleted()) {
            if (account.isRemote()) {
                // Is a server account that was locally deleted we try to remote delete
                final HttpURLConnection connection = HTTP.delete(url, authorization, areInsecureCertificatesAllowed(context));
                if ((connection.getResponseCode() == HttpURLConnection.HTTP_OK) || (connection.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT) || (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND)) {
                    // Account is remote deleted then we delete locally (we try to delete the icon, if any)
                    onAccountDataRemoved(database, context, account, true, raise_exception_on_network_error);
                    return true;
                }
                else if (raise_exception_on_network_error) {
                    raiseNetworkErrorException(connection);
                }
            }
            else {
                // Is a local account and has been deleted
                onAccountDataRemoved(database, context, account, true, raise_exception_on_network_error);
                return true;
            }
            return false;
        }
        else if (! account.isSynced()) {
            // Account is out of sync
            // First, synchronize icon, if account has icon
            synchronizeIcon(account.getServerIdentity(), database, context, account.getIcon(), raise_exception_on_network_error);
            // After synchronize icon we try to synchronize account, but before we synchronize or create group data, if account belongs to a group
            if (! account.hasGroup() || synchronizeGroup(database, context, account.getGroup(), reload_data_after_synchronization, raise_exception_on_network_error)) {
                // We sent the account data (if is not a new account we use put, in other case we use post)
                final HttpURLConnection connection = account.isRemote() ? HTTP.put(url, authorization, JSON.toString(account.toJSONObject()), areInsecureCertificatesAllowed(context)) : HTTP.post(url, authorization, JSON.toString(account.toJSONObject()), areInsecureCertificatesAllowed(context));
                if ((connection.getResponseCode() == HttpURLConnection.HTTP_OK) || (connection.getResponseCode() == HttpURLConnection.HTTP_CREATED)) {
                    // Account has been updated or created. We update values to the ones sent by the server, reload icon (if param) then set status to default then save data
                    onAccountDataUpdated(database, context, account, JSON.toJSONObject(HTTP.getContentString(connection)), reload_data_after_synchronization, raise_exception_on_network_error);
                    return true;
                }
                else if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                    // Is an update but account is not found at server: we set has been deleted
                    onAccountDataRemoved(database, context, account, false, raise_exception_on_network_error);
                    return true;
                }
                else if (raise_exception_on_network_error) {
                    raiseNetworkErrorException(connection);
                }
                return false;
            }
        }
        else if (reload_data_after_synchronization && account.isRemote()) {
            // We try to download account data
            final HttpURLConnection connection = HTTP.get(url, authorization, areInsecureCertificatesAllowed(context));
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                // Account data exists and their data has been downloaded
                onAccountDataUpdated(database, context, account, JSON.toJSONObject(HTTP.getContentString(connection)), reload_data_after_synchronization, raise_exception_on_network_error);
                return true;
            }
            else if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                // Account is not found at server: we set has been deleted
                onAccountDataRemoved(database, context, account, false, raise_exception_on_network_error);
                return true;
            }
            else if (raise_exception_on_network_error) {
                raiseNetworkErrorException(connection);
            }
            return false;
        }
        return true;
    }

    public static boolean synchronizeAccount(@NotNull final SQLiteDatabase database, @NotNull final Context context, @NotNull final TwoFactorAccount account, final boolean raise_exception_on_network_error) throws Exception {
        return synchronizeAccount(database, context, account, false, raise_exception_on_network_error);
    }

    // This function looks similar to synchronizing accounts but we have to take into account the case where a group cannot be deleted locally because it still has references

    private static void onGroupDataUpdated(@NotNull final SQLiteDatabase database, @NotNull final Context context, @NotNull final TwoFactorGroup group, @NotNull final JSONObject object) throws Exception {
        group.fromJSONObject(object);
        group.setStatus(TwoFactorAccount.STATUS_DEFAULT);
        group.save(database, context);
    }

    private static void onGroupDataRemoved(@NotNull final SQLiteDatabase database, @NotNull final Context context, @NotNull final TwoFactorGroup group) throws Exception {
        group.setRemoteId(0);
        group.setStatus(TwoFactorGroup.STATUS_DELETED);
        group.save(database, context);
        if (! group.isReferenced(database)) { group.delete(database, context); }
    }

    public static boolean synchronizeGroup(@NotNull final SQLiteDatabase database, @NotNull final Context context, @NotNull final TwoFactorGroup group, final boolean reload_data_if_synchronized, final boolean raise_exception_on_network_error) throws Exception {
        final URL url = new URL(group.isRemote() ? GROUP_LOCATION.replace("%SERVER%", group.getServerIdentity().getServer()).replace("%ID%", String.valueOf(group.getRemoteId())) : GROUPS_LOCATION.replace("%SERVER%", group.getServerIdentity().getServer()));
        final String authorization = AUTH_TOKEN.replace("%TOKEN%", group.getServerIdentity().getToken());
        if (group.isDeleted()) {
            if (group.isRemote()) {
                // Is a server group that was locally deleted we try to remote delete
                final HttpURLConnection connection = HTTP.delete(url, authorization, areInsecureCertificatesAllowed(context));
                if ((connection.getResponseCode() == HttpURLConnection.HTTP_OK) || (connection.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT) || (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND)) {
                    // Group has been deleted
                    onGroupDataRemoved(database, context, group);
                    return true;
                }
                else if (raise_exception_on_network_error) {
                    raiseNetworkErrorException(connection);
                }
            }
            else {
                onGroupDataRemoved(database, context, group);
                return true;
            }
            return false;
        }
        else if (! group.isSynced()) {
            // Group is out of sync (we use a put if group already exists and post in other case)
            final HttpURLConnection connection = group.isRemote() ? HTTP.put(url, authorization, JSON.toString(group.toJSONObject()), areInsecureCertificatesAllowed(context)) : HTTP.post(url, authorization, JSON.toString(group.toJSONObject()), areInsecureCertificatesAllowed(context));
            if ((connection.getResponseCode() == HttpURLConnection.HTTP_OK) || (connection.getResponseCode() == HttpURLConnection.HTTP_CREATED)) {
                // Group has been updated or created
                onGroupDataUpdated(database, context, group, JSON.toJSONObject(HTTP.getContentString(connection)));
                return true;
            }
            else if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                // Group is not found at server we delete locally
                onGroupDataRemoved(database, context, group);
                return true;
            }
            else if (raise_exception_on_network_error) {
                raiseNetworkErrorException(connection);
            }
            return false;
        }
        else if (reload_data_if_synchronized && group.isRemote()) {
            // We try to download group data
            final HttpURLConnection connection = HTTP.get(url, authorization, areInsecureCertificatesAllowed(context));
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                // Group data exists and their data has been downloaded
                onGroupDataUpdated(database, context, group, JSON.toJSONObject(HTTP.getContentString(connection)));
                return true;
            }
            else if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                // Group is not found at server we delete locally
                onGroupDataRemoved(database, context, group);
                return true;
            }
            else if (raise_exception_on_network_error) {
                raiseNetworkErrorException(connection);
            }
            return false;
        }
        return true;
    }

    public static boolean synchronizeGroup(@NotNull final SQLiteDatabase database, @NotNull final Context context, @NotNull final TwoFactorGroup group, final boolean raise_exception_on_network_error) throws Exception {
        return synchronizeGroup(database, context, group, false, raise_exception_on_network_error);
    }

    // Synchronizes a icon source

    private static boolean deleteIcon(@NotNull final TwoFactorServerIdentity server_identity, @NotNull final String source_id, @NotNull final Context context, final boolean raise_exception_on_network_error) throws Exception {
        final HttpURLConnection connection = HTTP.delete(new URL(ICON_LOCATION.replace("%SERVER%", server_identity.getServer()).replace("%FILE%", source_id)), AUTH_TOKEN.replace("%TOKEN%", server_identity.getToken()), areInsecureCertificatesAllowed(context));
        if ((connection.getResponseCode() == HttpURLConnection.HTTP_OK) || (connection.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT) || (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND)) {
            return true;
        }
        else if (raise_exception_on_network_error) {
            raiseNetworkErrorException(connection);
        }
        return false;
    }

    public static boolean synchronizeIcon(@NotNull final TwoFactorServerIdentity server_identity, @NotNull final SQLiteDatabase database, @NotNull final Context context, @Nullable final TwoFactorIcon icon, final boolean raise_exception_on_network_error) throws Exception {
        boolean success = true;
        // If icon is not null but source is null is a custom icon
        if ((icon != null) && (icon.getSource() == null)) {
            final Bitmap bitmap = icon.getBitmap(context, null);
            final String authorization = AUTH_TOKEN.replace("%TOKEN%", server_identity.getToken()), source_id = icon.getSourceId();
            // If source-id has a value this is a 2FA icon that has been deleted/updated by the user at local and is still not synchronized
            if (! Strings.isEmptyOrNull(source_id)) {
                if (deleteIcon(server_identity, source_id, context, raise_exception_on_network_error)) {
                    icon.setSourceData(null, null);
                    // If icon has no bitmap and is not referenced by an account, we delete from database, in other case we save data
                    if ((bitmap == null) && (! icon.isReferenced(database))) { icon.delete(database, context); }
                    else { icon.save(database, context); }
                }
                else {
                    success = false;
                }
            }
            // If bitmap is not empty, we try to send it to the server
            if (bitmap != null) {
                final HttpURLConnection connection = HTTP.post(new URL(ICONS_LOCATION.replace("%SERVER%", server_identity.getServer())), authorization, Bitmaps.bytes(bitmap), ICON_DATA_NAME, HTTP.CONTENT_TYPE_MULTIPART_FORM_DATA, areInsecureCertificatesAllowed(context));
                if (connection.getResponseCode() == HttpURLConnection.HTTP_CREATED) {
                    final JSONObject object = JSON.toJSONObject(HTTP.getContentString(connection));
                    icon.setSourceData(ICON_SOURCE_DEFAULT, object.getString(Constants.ICON_DATA_ID_KEY));
                    icon.save(database, context);
                }
                else if (raise_exception_on_network_error) {
                    raiseNetworkErrorException(connection);
                }
                else {
                    success = false;
                }
            }
        }
        return success;
    }

    public static @Nullable JSONObject decodeQR(@NotNull final TwoFactorServerIdentity server_identity, @NotNull final Bitmap qr, @NotNull final Context context) throws Exception {
        final URL url = new URL(QR_DECODER_LOCATION.replace("%SERVER%", server_identity.getServer()));
        final String authorization = AUTH_TOKEN.replace("%TOKEN%", server_identity.getToken());
        final HttpURLConnection connection = HTTP.post(url, authorization, Bitmaps.bytes(qr), QR_DATA_NAME, HTTP.CONTENT_TYPE_MULTIPART_FORM_DATA, areInsecureCertificatesAllowed(context));
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            final Uri uri = Uri.parse(JSON.toJSONObject(HTTP.getContentString(connection)).getString("data"));
            if (Strings.equals(uri.getScheme(), "otpauth")) {
                final JSONObject object = new JSONObject();
                object.put(Constants.ACCOUNT_DATA_OTP_TYPE_KEY, uri.getHost());
                final String[] label_parts = uri.getPath().substring(1).split(":");
                object.put(Constants.ACCOUNT_DATA_SERVICE_KEY, label_parts[0]);
                object.put(Constants.ACCOUNT_DATA_USER_KEY, label_parts.length > 1 ? label_parts[1] : "");
                for (String param : uri.getQuery().split("&")) {
                    final String[] key_value = param.split("=");
                    if (key_value.length == 2) {
                        if (Constants.ACCOUNT_DATA_SECRET_KEY.equals(key_value[0])) { object.put(Constants.ACCOUNT_DATA_SECRET_KEY, key_value[1]); }
                        else if (Constants.ACCOUNT_DATA_ALGORITHM_KEY.equals(key_value[0])) { object.put(Constants.ACCOUNT_DATA_ALGORITHM_KEY, key_value[1]); }
                        else if (Constants.ACCOUNT_DATA_OTP_LENGTH_KEY.equals(key_value[0])) { object.put(Constants.ACCOUNT_DATA_OTP_LENGTH_KEY, Strings.parseInt(key_value[1], Constants.DEFAULT_OTP_LENGTH)); }
                        else if (Constants.ACCOUNT_DATA_PERIOD_KEY.equals(key_value[0])) { object.put(Constants.ACCOUNT_DATA_PERIOD_KEY, Strings.parseInt(key_value[1], Constants.DEFAULT_PERIOD)); }
                        else if (Constants.ACCOUNT_DATA_COUNTER_KEY.equals(key_value[0])) { object.put(Constants.ACCOUNT_DATA_COUNTER_KEY, Strings.parseInt(key_value[1], Constants.DEFAULT_COUNTER)); }
                        else if (Constants.QR_DATA_ISSUER_KEY.equals(key_value[0]) && Strings.isEmptyOrNull(object.getString(Constants.ACCOUNT_DATA_USER_KEY)) && (! Strings.isEmptyOrNull(key_value[1]))) { object.put(Constants.ACCOUNT_DATA_USER_KEY, key_value[1]); }
                    }
                }
                return object;
            }
        }
        return null;
    }
}

