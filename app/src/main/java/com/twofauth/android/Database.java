package com.twofauth.android;

import static com.twofauth.android.Constants.SORT_ACCOUNTS_BY_LAST_USE_KEY;
import static com.twofauth.android.Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_ALGORITHM_KEY;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.format.DateUtils;
import android.util.Log;

import com.bastiaanjansen.otp.HMACAlgorithm;
import com.bastiaanjansen.otp.HOTPGenerator;
import com.bastiaanjansen.otp.TOTPGenerator;

import net.zetetic.database.sqlcipher.SQLiteDatabase;
import net.zetetic.database.sqlcipher.SQLiteOpenHelper;

import java.io.File;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class Database {
    private static final String DATABASE_NAME = "database.db";
    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_PASSWORD_KEY = "database-password";
    private static final int PASSWORD_LENGTH = 128;

    private static final String ROW_ID = "_id";

    private static final String TWO_FACTOR_GROUPS_TABLE_NAME = "two_factor_groups";
    public static final String TWO_FACTOR_GROUP_SERVER_ID = "ID";
    public static final String TWO_FACTOR_GROUP_NAME = "name";

    private static final String[] GROUP_PROJECTION = new String[] {
        ROW_ID,
        TWO_FACTOR_GROUP_SERVER_ID,
        TWO_FACTOR_GROUP_NAME,
    };

    private static final int ROW_ID_ORDER = 0;

    private static final int TWO_FACTOR_GROUP_SERVER_ID_ORDER = ROW_ID_ORDER + 1;
    private static final int TWO_FACTOR_GROUP_NAME_ORDER = TWO_FACTOR_GROUP_SERVER_ID_ORDER + 1;

    private static final String TWO_FACTOR_ACCOUNTS_TABLE_NAME = "two_factor_accounts";

    public static final String TWO_FACTOR_ACCOUNT_SERVER_ID = "id";
    public static final String TWO_FACTOR_ACCOUNT_SERVICE = "service";
    public static final String TWO_FACTOR_ACCOUNT_USER = "account";
    public static final String TWO_FACTOR_ACCOUNT_GROUP = "group_id";
    public static final String TWO_FACTOR_ACCOUNT_ICON = "icon";
    public static final String TWO_FACTOR_ACCOUNT_OTP_TYPE = "otp_type";
    public static final String TWO_FACTOR_ACCOUNT_SECRET = "secret";
    public static final String TWO_FACTOR_ACCOUNT_PASSWORD_LENGTH = "digits";
    public static final String TWO_FACTOR_ACCOUNT_ALGORITHM = "algorithm";
    public static final String TWO_FACTOR_ACCOUNT_PERIOD = "period";
    public static final String TWO_FACTOR_ACCOUNT_COUNTER = "counter";
    public static final String TWO_FACTOR_ACCOUNT_NOT_SYNCED = "not_synced";
    public static final String TWO_FACTOR_ACCOUNT_LAST_USE = "last_use";
    public static final String TWO_FACTOR_ACCOUNT_GENERIC_ICON_PATHNAME = "generic_icon_pathname";
    public static final String TWO_FACTOR_ACCOUNT_DARK_ICON_PATHNAME = "dark_icon_pathname";
    public static final String TWO_FACTOR_ACCOUNT_LIGHT_ICON_PATHNAME = "light_icon_pathname";

    private static final String[] TWO_FACTOR_ACCOUNT_PROJECTION = new String[] {
        ROW_ID,
        TWO_FACTOR_ACCOUNT_SERVER_ID,
        TWO_FACTOR_ACCOUNT_SERVICE,
        TWO_FACTOR_ACCOUNT_USER,
        TWO_FACTOR_ACCOUNT_GROUP,
        TWO_FACTOR_ACCOUNT_ICON,
        TWO_FACTOR_ACCOUNT_OTP_TYPE,
        TWO_FACTOR_ACCOUNT_SECRET,
        TWO_FACTOR_ACCOUNT_PASSWORD_LENGTH,
        TWO_FACTOR_ACCOUNT_ALGORITHM,
        TWO_FACTOR_ACCOUNT_PERIOD,
        TWO_FACTOR_ACCOUNT_COUNTER,
        TWO_FACTOR_ACCOUNT_NOT_SYNCED,
        TWO_FACTOR_ACCOUNT_LAST_USE,
        TWO_FACTOR_ACCOUNT_GENERIC_ICON_PATHNAME,
        TWO_FACTOR_ACCOUNT_DARK_ICON_PATHNAME,
        TWO_FACTOR_ACCOUNT_LIGHT_ICON_PATHNAME,
    };

    private static final int TWO_FACTOR_ACCOUNT_SERVER_ID_ORDER = ROW_ID_ORDER + 1;
    private static final int TWO_FACTOR_ACCOUNT_SERVICE_ORDER = TWO_FACTOR_ACCOUNT_SERVER_ID_ORDER + 1;
    private static final int TWO_FACTOR_ACCOUNT_TWO_FACTOR_USER_ORDER = TWO_FACTOR_ACCOUNT_SERVICE_ORDER + 1;
    private static final int TWO_FACTOR_ACCOUNT_GROUP_ORDER = TWO_FACTOR_ACCOUNT_TWO_FACTOR_USER_ORDER + 1;
    private static final int TWO_FACTOR_ACCOUNT_ICON_ORDER = TWO_FACTOR_ACCOUNT_GROUP_ORDER + 1;
    private static final int TWO_FACTOR_ACCOUNT_OTP_TYPE_ORDER = TWO_FACTOR_ACCOUNT_ICON_ORDER + 1;
    private static final int TWO_FACTOR_ACCOUNT_SECRET_ORDER = TWO_FACTOR_ACCOUNT_OTP_TYPE_ORDER + 1;
    private static final int TWO_FACTOR_ACCOUNT_PASSWORD_LENGTH_ORDER = TWO_FACTOR_ACCOUNT_SECRET_ORDER + 1;
    private static final int TWO_FACTOR_ACCOUNT_ALGORITHM_ORDER = TWO_FACTOR_ACCOUNT_PASSWORD_LENGTH_ORDER + 1;
    private static final int TWO_FACTOR_ACCOUNT_PERIOD_ORDER = TWO_FACTOR_ACCOUNT_ALGORITHM_ORDER + 1;
    private static final int TWO_FACTOR_ACCOUNT_COUNTER_ORDER = TWO_FACTOR_ACCOUNT_PERIOD_ORDER + 1;
    private static final int TWO_FACTOR_ACCOUNT_NOT_SYNCED_ORDER = TWO_FACTOR_ACCOUNT_COUNTER_ORDER + 1;
    private static final int TWO_FACTOR_ACCOUNT_LAST_USE_ORDER = TWO_FACTOR_ACCOUNT_NOT_SYNCED_ORDER + 1;
    private static final int TWO_FACTOR_ACCOUNT_GENERIC_ICON_PATHNAME_ORDER = TWO_FACTOR_ACCOUNT_LAST_USE_ORDER + 1;
    private static final int TWO_FACTOR_ACCOUNT_DARK_ICON_PATHNAME_ORDER = TWO_FACTOR_ACCOUNT_GENERIC_ICON_PATHNAME_ORDER  + 1;
    private static final int TWO_FACTOR_ACCOUNT_LIGHT_ICON_PATHNAME_ORDER = TWO_FACTOR_ACCOUNT_DARK_ICON_PATHNAME_ORDER + 1;

    private static final boolean ALLOW_GENERIC_GROUP_ADITION = false;
    private static final int GENERIC_GROUP_SERVER_ID = 0;

    private static class FileUtils {
        public static File getFileIfExists(@Nullable final String pathname) {
            if (! StringUtils.isEmptyOrNull(pathname)) {
                final File file = new File(pathname);
                if (file.exists()) {
                    return file;
                }
            }
            return null;
        }
    }

    private static class Base32Utils {
        private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

        private static byte[] decode(@NotNull String encoded_string) {
            encoded_string = encoded_string.replace("=", "");
            HashMap<Character, Integer> map = new HashMap<>();
            for (int i = 0; i < ALPHABET.length(); i ++) {
                map.put(ALPHABET.charAt(i), i);
            }
            int byte_count = encoded_string.length() * 5 / 8;
            byte[] decoded_bytes = new byte[byte_count];
            int buffer = 0, bits_left = 0, byte_index = 0;
            for (final char character : encoded_string.toCharArray()) {
                if (! map.containsKey(character)) {
                    throw new IllegalArgumentException("Invelid Base32 character: " + character);
                }
                buffer = (buffer << 5) | map.get(character);
                bits_left += 5;
                if (bits_left >= 8) {
                    decoded_bytes[byte_index ++] = (byte) ((buffer >> (bits_left - 8)) & 0xFF);
                    bits_left -= 8;
                }
            }
            return decoded_bytes;
        }
    }

    private static class SteamOtpCodesGenerator {
        private static final String ALPHABET = "23456789BCDFGHJKMNPQRTVWXY";

        private static final String ALGORITHM = TwoFactorAccount.ALGORITHM_SHA1;

        private final byte[] mSecret;
        private final long mPeriod;

        SteamOtpCodesGenerator(@NotNull final String secret, final int period_in_seconds) {
            mSecret = Base32Utils.decode(secret);
            mPeriod = period_in_seconds * DateUtils.SECOND_IN_MILLIS;
        }

        public String getOtp(@NotNull final Date date) {
            try {
                final ByteBuffer time_buffer = ByteBuffer.allocate(8);
                time_buffer.putLong(date.getTime() / mPeriod);
                final Mac mac = Mac.getInstance("Hmac" + ALGORITHM);
                final SecretKeySpec key_spec = new SecretKeySpec(mSecret, "Hmac" + ALGORITHM);
                mac.init(key_spec);
                final byte[] time_hmac = mac.doFinal(time_buffer.array());
                final int begin = time_hmac[19] & 0xF;
                int f = ByteBuffer.wrap(time_hmac, begin, 4).getInt() & 0x7FFFFFFF;
                final StringBuilder otp = new StringBuilder();
                for (int i = 0; i < 5; i ++) {
                    int index = f % ALPHABET.length();
                    otp.append(ALPHABET.charAt(index));
                    f /= ALPHABET.length();
                }
                return otp.toString();
            }
            catch (Exception e)
            {
                Log.e(Constants.LOG_TAG_NAME, "Exception while generating a Steam OTP Code", e);
            }
            return null;
        }

        public String getOtp() {
            return getOtp(new Date());
        }

        public long getPeriodInMillis() { return mPeriod; }

        public long getPeriodUntilNextTimeWindowInMillis() {
            return mPeriod - System.currentTimeMillis() % mPeriod;
        }

        public static boolean isSupported(@NotNull final String algorithm, final int length) {
            return ((length == 5) && ALGORITHM.equals(algorithm));
        }
    }

    private static class HotpCodesGeneratorBasedOnMD5 {

        private final byte[] mSecret;
        private final int mDigits;

        HotpCodesGeneratorBasedOnMD5(@NotNull final String secret, final int digits) {
            mSecret = Base32Utils.decode(secret);
            mDigits = digits;
        }

        public String getOtp(final long counter) {
            try {
                final ByteBuffer buffer = ByteBuffer.allocate(8);
                buffer.putLong(counter);
                final Mac hmac = Mac.getInstance("HmacMD5");
                hmac.init(new SecretKeySpec(mSecret, "HmacMD5"));
                final byte[] hmac_bytes = hmac.doFinal(buffer.array());
                final int offset = hmac_bytes[hmac_bytes.length - 1] & 0x0F;
                final int binary_code = ((hmac_bytes[offset] & 0x7F) << 24) | ((hmac_bytes[offset + 1] & 0xFF) << 16) | ((hmac_bytes[offset + 2] & 0xFF) << 8) | (hmac_bytes[offset + 3] & 0xFF);
                return String.format("%0" + mDigits + "d", binary_code % (int) Math.pow(10, mDigits));
            }
            catch (Exception e) {
                Log.e(Constants.LOG_TAG_NAME, "Exception while generating a MD5 OTP Code", e);
            }
            return null;
        }
    }

    private static class TotpCodesGeneratorBasedOnMD5 extends HotpCodesGeneratorBasedOnMD5 {
        private final long mPeriod;

        TotpCodesGeneratorBasedOnMD5(@NotNull final String secret, final int digits, final int period_in_seconds) {
            super(secret, digits);
            mPeriod = period_in_seconds * DateUtils.SECOND_IN_MILLIS;
        }

        @Override
        public String getOtp(long time) {
            return super.getOtp(time / mPeriod);
        }

        public String getOtp(@NotNull final Date date) {
            return getOtp(date.getTime());
        }

        public String getOtp() {
            return getOtp(new Date());
        }

        public long getPeriodInMillis() { return mPeriod; }

        public long getPeriodUntilNextTimeWindowInMillis() {
            return mPeriod - System.currentTimeMillis() % mPeriod;
        }
    }

    public static class ApplicationDatabase extends SQLiteOpenHelper {
        ApplicationDatabase(@NotNull final Context context, @NotNull final String password) {
            super(context, DATABASE_NAME, password, null, DATABASE_VERSION, 1, null, null, false);
        }

        @Override
        public void onCreate(@NotNull final SQLiteDatabase database) {
            database.execSQL(String.format("CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s INTEGER DEFAULT 0, %s TEXT DEFAULT '')", TWO_FACTOR_GROUPS_TABLE_NAME, ROW_ID, TWO_FACTOR_GROUP_SERVER_ID, TWO_FACTOR_GROUP_NAME));
            database.execSQL(String.format("CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s INTEGER DEFAULT 0, %s TEXT, %s TEXT DEFAULT '', %s INTEGER REFERENCES %s(%s), %s TEXT DEFAULT '', %s TEXT, %s TEXT, %s INTEGER, %s TEXT, %s INTEGER DEFAULT 30, %s INTEGER DEFAULT 0, %s INTEGER DEFAULT 0, %s INTEGER DEFAULT 0, %s TEXT DEFAULT NULL, %s TEXT DEFAULT NULL, %s TEXT DEFAULT NULL)", TWO_FACTOR_ACCOUNTS_TABLE_NAME, ROW_ID, TWO_FACTOR_ACCOUNT_SERVER_ID, TWO_FACTOR_ACCOUNT_SERVICE, TWO_FACTOR_ACCOUNT_USER, TWO_FACTOR_ACCOUNT_GROUP, TWO_FACTOR_GROUPS_TABLE_NAME, ROW_ID, TWO_FACTOR_ACCOUNT_ICON, TWO_FACTOR_ACCOUNT_OTP_TYPE, TWO_FACTOR_ACCOUNT_SECRET, TWO_FACTOR_ACCOUNT_PASSWORD_LENGTH, TWO_FACTOR_ACCOUNT_ALGORITHM, TWO_FACTOR_ACCOUNT_PERIOD, TWO_FACTOR_ACCOUNT_COUNTER, TWO_FACTOR_ACCOUNT_NOT_SYNCED, TWO_FACTOR_ACCOUNT_LAST_USE, TWO_FACTOR_ACCOUNT_GENERIC_ICON_PATHNAME, TWO_FACTOR_ACCOUNT_DARK_ICON_PATHNAME, TWO_FACTOR_ACCOUNT_LIGHT_ICON_PATHNAME));
        }

        @Override
        public void onUpgrade(@NotNull final SQLiteDatabase database, final int old_version, final int new_version) {}
    }

    public static class TwoFactorGroup {
        private final long _id;

        public final int id;
        public final String name;

        TwoFactorGroup(@NotNull final Cursor cursor) {
            _id = cursor.getLong(ROW_ID_ORDER);
            id = cursor.getInt(TWO_FACTOR_GROUP_SERVER_ID_ORDER);
            name = cursor.getString(TWO_FACTOR_GROUP_NAME_ORDER);
        }

        public JSONObject toJSONObject() throws Exception {
            final JSONObject object = new JSONObject();
            object.put(Constants.TWO_FACTOR_AUTH_GROUP_ID_KEY, id);
            object.put(Constants.TWO_FACTOR_AUTH_GROUP_NAME_KEY, name);
            return object;
        }
    }

    public static class TwoFactorAccount {
        public static final String OTP_TYPE_TOTP_VALUE = "totp";
        public static final String OTP_TYPE_HOTP_VALUE = "hotp";
        public static final String OTP_TYPE_STEAM_VALUE = "steamtotp";

        private static final String ALGORITHM_SHA512 = "sha512";
        private static final String ALGORITHM_SHA384 = "sha384";
        private static final String ALGORITHM_SHA256 = "sha256";
        private static final String ALGORITHM_SHA224 = "sha224";
        private static final String ALGORITHM_SHA1 = "sha1";
        private static final String ALGORITHM_MD5 = "md5";

        private long _id;

        private int mId;
        private String mService;
        private String mUser;
        private TwoFactorGroup mGroup;
        private String mIcon;
        private String mOtpType;
        private String mSecret;
        private int mPasswordLength;
        private String mAlgorithm;
        private int mPeriod;
        private int mCounter;

        private boolean mNotSynced;
        private long mLastUse;
        private String mGenericIconPathname;
        private String mDarkIconPathname;
        private String mLightIconPathname;

        private Bitmap mDrawableIcon = null;
        private Object mPasswordGenerator = null;

        TwoFactorAccount(@NotNull final SQLiteDatabase database, @NotNull final Cursor cursor) {
            _id = cursor.getLong(ROW_ID_ORDER);
            mId = cursor.getInt(TWO_FACTOR_ACCOUNT_SERVER_ID_ORDER);
            mService = cursor.getString(TWO_FACTOR_ACCOUNT_SERVICE_ORDER);
            mUser = cursor.getString(TWO_FACTOR_ACCOUNT_TWO_FACTOR_USER_ORDER);
            mGroup = cursor.getLong(TWO_FACTOR_ACCOUNT_GROUP_ORDER) == 0 ? null : TwoFactorGroupOperations.getByRowId(database, cursor.getLong(TWO_FACTOR_ACCOUNT_GROUP_ORDER));
            mIcon = cursor.getString(TWO_FACTOR_ACCOUNT_ICON_ORDER);
            mOtpType = cursor.getString(TWO_FACTOR_ACCOUNT_OTP_TYPE_ORDER);
            mSecret = cursor.getString(TWO_FACTOR_ACCOUNT_SECRET_ORDER);
            mPasswordLength = cursor.getInt(TWO_FACTOR_ACCOUNT_PASSWORD_LENGTH_ORDER);
            mAlgorithm = cursor.getString(TWO_FACTOR_ACCOUNT_ALGORITHM_ORDER);
            mPeriod = cursor.getInt(TWO_FACTOR_ACCOUNT_PERIOD_ORDER);
            mCounter = cursor.getInt(TWO_FACTOR_ACCOUNT_COUNTER_ORDER);
            mNotSynced = cursor.getInt(TWO_FACTOR_ACCOUNT_NOT_SYNCED_ORDER) != 0;
            mLastUse = cursor.getLong(TWO_FACTOR_ACCOUNT_LAST_USE_ORDER);
            mGenericIconPathname = cursor.getString(TWO_FACTOR_ACCOUNT_GENERIC_ICON_PATHNAME_ORDER);
            mDarkIconPathname = cursor.getString(TWO_FACTOR_ACCOUNT_DARK_ICON_PATHNAME_ORDER);
            mLightIconPathname = cursor.getString(TWO_FACTOR_ACCOUNT_LIGHT_ICON_PATHNAME_ORDER);
        }

        public JSONObject toJSONObject() throws Exception {
            final JSONObject object = new JSONObject();
            object.put(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_ID_KEY, mId);
            object.put(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_SERVICE_KEY, mService);
            object.put(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_USER_KEY, mUser);
            if (mGroup != null) {
                object.put(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_GROUP_KEY, mGroup.id);
            }
            object.put(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_ICON_KEY, mIcon);
            object.put(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_OTP_TYPE_KEY, mOtpType);
            object.put(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_SECRET_KEY, mSecret);
            object.put(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_PASSWORD_LENGTH_KEY, mPasswordLength);
            object.put(TWO_FACTOR_AUTH_ACCOUNT_DATA_ALGORITHM_KEY, mAlgorithm);
            if (OTP_TYPE_TOTP_VALUE.equals(mOtpType) || OTP_TYPE_STEAM_VALUE.equals(mOtpType)) {
                object.put(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_PERIOD_KEY, mPeriod);
            }
            if (OTP_TYPE_HOTP_VALUE.equals(mOtpType)) {
                object.put(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_COUNTER_KEY, mCounter);
            }
            return object;
        }
        
        private Object getOtpGenerator() {
            if (mPasswordGenerator == null) {
                if (OTP_TYPE_TOTP_VALUE.equals(mOtpType)) {
                    if (ALGORITHM_MD5.equals(mAlgorithm)) {
                        mPasswordGenerator = new TotpCodesGeneratorBasedOnMD5(mSecret, mPasswordLength, mPeriod);
                    }
                    else {
                        mPasswordGenerator = new TOTPGenerator.Builder(mSecret).withHOTPGenerator(builder -> {
                            builder.withPasswordLength(mPasswordLength);
                            builder.withAlgorithm(ALGORITHM_SHA512.equals(mAlgorithm) ? HMACAlgorithm.SHA512 : ALGORITHM_SHA384.equals(mAlgorithm) ? HMACAlgorithm.SHA384 : ALGORITHM_SHA256.equals(mAlgorithm) ? HMACAlgorithm.SHA256 : ALGORITHM_SHA224.equals(mAlgorithm) ? HMACAlgorithm.SHA224 : HMACAlgorithm.SHA1);
                        }).withPeriod(Duration.ofSeconds(mPeriod)).build();
                    }
                }
                else if (OTP_TYPE_HOTP_VALUE.equals(mOtpType)) {
                    if (ALGORITHM_MD5.equals(mAlgorithm)) {
                        mPasswordGenerator = new HotpCodesGeneratorBasedOnMD5(mSecret, mPasswordLength);
                    }
                    else {
                        mPasswordGenerator = new HOTPGenerator.Builder(mSecret).withAlgorithm(ALGORITHM_SHA512.equals(mAlgorithm) ? HMACAlgorithm.SHA512 : ALGORITHM_SHA384.equals(mAlgorithm) ? HMACAlgorithm.SHA384 : ALGORITHM_SHA256.equals(mAlgorithm) ? HMACAlgorithm.SHA256 : ALGORITHM_SHA224.equals(mAlgorithm) ? HMACAlgorithm.SHA224 : HMACAlgorithm.SHA1).withPasswordLength(mPasswordLength).build();
                    }
                }
                else if (OTP_TYPE_STEAM_VALUE.equals(mOtpType)) {
                    mPasswordGenerator = new SteamOtpCodesGenerator(mSecret, mPeriod);
                }
            }
            return mPasswordGenerator;
        }

        public int getId() {
            return mId;
        }

        public void setId(final int id) {
            if (mId != id) {
                final ContentValues values = new ContentValues();
                values.put(TWO_FACTOR_ACCOUNT_SERVER_ID, id);
                values.put(TWO_FACTOR_ACCOUNT_NOT_SYNCED, 1);
                if (TwoFactorAccountOperations.update(_id, values)) {
                    mId = id;
                    mNotSynced = true;
                }
            }
        }

        public String getService() {
            return mService;
        }

        public void setService(@NotNull final String service) {
            if (! StringUtils.equals(mService, service)) {
                final ContentValues values = new ContentValues();
                values.put(TWO_FACTOR_ACCOUNT_SERVICE, service);
                values.put(TWO_FACTOR_ACCOUNT_NOT_SYNCED, 1);
                if (TwoFactorAccountOperations.update(_id, values)) {
                    mService = service;
                    mNotSynced = true;
                }
            }
        }

        public String getUser() {
            return mUser;
        }

        public void setUser(@NotNull final String user) {
            if (! StringUtils.equals(mUser, user)) {
                final ContentValues values = new ContentValues();
                values.put(TWO_FACTOR_ACCOUNT_USER, user);
                values.put(TWO_FACTOR_ACCOUNT_NOT_SYNCED, 1);
                if (TwoFactorAccountOperations.update(_id, values)) {
                    mUser = user;
                    mNotSynced = true;
                }
            }
        }

        public TwoFactorGroup getGroup() { return mGroup; }
        
        public String getIcon() {
            return mIcon;
        }

        public void setIcon(@NotNull final String icon) {
            if (! StringUtils.equals(mIcon, icon)) {
                final ContentValues values = new ContentValues();
                values.put(TWO_FACTOR_ACCOUNT_ICON, icon);
                values.put(TWO_FACTOR_ACCOUNT_NOT_SYNCED, 1);
                if (TwoFactorAccountOperations.update(_id, values)) {
                    mIcon = icon;
                    mNotSynced = true;
                }
            }
        }

        public String getOtpType() {
            return mOtpType;
        }

        public boolean isOtpTypeSupported() {
            if ((OTP_TYPE_TOTP_VALUE.equals(mOtpType)) || (OTP_TYPE_HOTP_VALUE.equals(mOtpType))) {
                for (String supported_algorithm : new String[] { ALGORITHM_SHA512, ALGORITHM_SHA384, ALGORITHM_SHA256, ALGORITHM_SHA224, ALGORITHM_SHA1, ALGORITHM_MD5 }) {
                    if (supported_algorithm.equals(mAlgorithm)) {
                        return true;
                    }
                }
            }
            if (OTP_TYPE_STEAM_VALUE.equals(mOtpType)) {
                return SteamOtpCodesGenerator.isSupported(mAlgorithm, mPasswordLength);
            }
            return false;
        }

        public void setOtpType(@NotNull final String otp_type) {
            if (! StringUtils.equals(mOtpType, otp_type)) {
                final ContentValues values = new ContentValues();
                values.put(TWO_FACTOR_ACCOUNT_OTP_TYPE, otp_type);
                values.put(TWO_FACTOR_ACCOUNT_NOT_SYNCED, 1);
                if (TwoFactorAccountOperations.update(_id, values)) {
                    mOtpType = otp_type;
                    mNotSynced = true;
                    mPasswordGenerator = null;
                }
            }
        }

        public String getSecret() {
            return mSecret;
        }

        public void setSecret(@NotNull final String secret) {
            if (! StringUtils.equals(mSecret, secret)) {
                final ContentValues values = new ContentValues();
                values.put(TWO_FACTOR_ACCOUNT_SECRET, secret);
                values.put(TWO_FACTOR_ACCOUNT_NOT_SYNCED, 1);
                if (TwoFactorAccountOperations.update(_id, values)) {
                    mSecret = secret;
                    mNotSynced = true;
                    mPasswordGenerator = null;
                }
            }
        }

        public int getPasswordLength() {
            return mPasswordLength;
        }

        public void setPasswordLength(final int password_length) {
            if (mPasswordLength != password_length) {
                final ContentValues values = new ContentValues();
                values.put(TWO_FACTOR_ACCOUNT_PASSWORD_LENGTH, password_length);
                values.put(TWO_FACTOR_ACCOUNT_NOT_SYNCED, 1);
                if (TwoFactorAccountOperations.update(_id, values)) {
                    mPasswordLength = password_length;
                    mNotSynced = true;
                    mPasswordGenerator = null;
                }
            }
        }

        public String getAlgorithm() {
            return mAlgorithm;
        }

        public void setAlgorithm(@NotNull final String algorithm) {
            if (! StringUtils.equals(mAlgorithm, algorithm)) {
                final ContentValues values = new ContentValues();
                values.put(TWO_FACTOR_ACCOUNT_ALGORITHM, algorithm);
                values.put(TWO_FACTOR_ACCOUNT_NOT_SYNCED, 1);
                if (TwoFactorAccountOperations.update(_id, values)) {
                    mAlgorithm = algorithm;
                    mNotSynced = true;
                    mPasswordGenerator = null;
                }
            }
        }

        public int getPeriod() {
            return mPeriod;
        }

        public long getPeriodInMillis() {
            return mPeriod * DateUtils.SECOND_IN_MILLIS;
        }

        public void setPeriod(final int period) {
            if (mPeriod != period) {
                final ContentValues values = new ContentValues();
                values.put(TWO_FACTOR_ACCOUNT_PERIOD, period);
                values.put(TWO_FACTOR_ACCOUNT_NOT_SYNCED, 1);
                if (TwoFactorAccountOperations.update(_id, values)) {
                    mPeriod = period;
                    mNotSynced = true;
                    mPasswordGenerator = null;
                }
            }
        }

        public int getCounter() {
            return mCounter;
        }

        public void setCounter(final int counter) {
            if (mCounter != counter) {
                final ContentValues values = new ContentValues();
                values.put(TWO_FACTOR_ACCOUNT_COUNTER, counter);
                values.put(TWO_FACTOR_ACCOUNT_NOT_SYNCED, 1);
                if (TwoFactorAccountOperations.update(_id, values)) {
                    mCounter = counter;
                    mNotSynced = true;
                }
            }
        }

        public long getOtpMillis() {
            Object generator = getOtpGenerator();
            if (generator != null) {
                if (OTP_TYPE_TOTP_VALUE.equals(mOtpType)) {
                    if (ALGORITHM_MD5.equals(mAlgorithm)) {
                        return ((TotpCodesGeneratorBasedOnMD5) generator).getPeriodInMillis();
                    }
                    return ((TOTPGenerator) generator).getPeriod().toMillis();
                }
                else if (OTP_TYPE_HOTP_VALUE.equals(mOtpType)) {
                    return Long.MAX_VALUE;
                }
                else if (OTP_TYPE_STEAM_VALUE.equals(mOtpType)) {
                    return ((SteamOtpCodesGenerator) generator).getPeriodInMillis();
                }
            }
            return -1;
        }

        public long getMillisUntilNextOtp() {
            Object generator = getOtpGenerator();
            if (generator != null) {
                if (OTP_TYPE_TOTP_VALUE.equals(mOtpType)) {
                    if (ALGORITHM_MD5.equals(mAlgorithm)) {
                        return ((TotpCodesGeneratorBasedOnMD5) generator).getPeriodUntilNextTimeWindowInMillis();
                    }
                    return ((TOTPGenerator) generator).durationUntilNextTimeWindow().toMillis();
                }
                else if (OTP_TYPE_HOTP_VALUE.equals(mOtpType)) {
                    return Long.MAX_VALUE;
                }
                else if (OTP_TYPE_STEAM_VALUE.equals(mOtpType)) {
                    return ((SteamOtpCodesGenerator) generator).getPeriodUntilNextTimeWindowInMillis();
                }
            }
            return -1;
        }

        public long getMillisUntilNextOtpCompleteCycle() {
            Object generator = getOtpGenerator();
            if (generator != null) {
                if (OTP_TYPE_TOTP_VALUE.equals(mOtpType)) {
                    if (ALGORITHM_MD5.equals(mAlgorithm)) {
                        return ((TotpCodesGeneratorBasedOnMD5) generator).getPeriodUntilNextTimeWindowInMillis() + ((TotpCodesGeneratorBasedOnMD5) generator).getPeriodInMillis();
                    }
                    return ((TOTPGenerator) generator).durationUntilNextTimeWindow().toMillis() + ((TOTPGenerator) generator).getPeriod().toMillis();
                }
                else if (OTP_TYPE_HOTP_VALUE.equals(mOtpType)) {
                    return Long.MAX_VALUE;
                }
                else if (OTP_TYPE_STEAM_VALUE.equals(mOtpType)) {
                    return ((SteamOtpCodesGenerator) generator).getPeriodUntilNextTimeWindowInMillis() + ((SteamOtpCodesGenerator) generator).getPeriodInMillis();
                }
            }
            return -1;
        }

        public String getOtp() {
            Object generator = getOtpGenerator();
            if (generator != null) {
                if (OTP_TYPE_TOTP_VALUE.equals(mOtpType)) {
                    if (ALGORITHM_MD5.equals(mAlgorithm)) {
                        return ((TotpCodesGeneratorBasedOnMD5) generator).getOtp();
                    }
                    return ((TOTPGenerator) generator).now();
                }
                else if (OTP_TYPE_HOTP_VALUE.equals(mOtpType)) {
                    if (ALGORITHM_MD5.equals(mAlgorithm)) {
                        return ((HotpCodesGeneratorBasedOnMD5) generator).getOtp(mCounter - 1);
                    }
                    return ((HOTPGenerator) generator).generate(mCounter - 1);
                }
                else if (OTP_TYPE_STEAM_VALUE.equals(mOtpType)) {
                    return ((SteamOtpCodesGenerator) generator).getOtp();
                }
            }
            return null;
        }

        public String getOtp(final Date date) {
            Object generator = getOtpGenerator();
            if (generator != null) {
                if (OTP_TYPE_TOTP_VALUE.equals(mOtpType)) {
                    if (ALGORITHM_MD5.equals(mAlgorithm)) {
                        return ((TotpCodesGeneratorBasedOnMD5) generator).getOtp(date);
                    }
                    return ((TOTPGenerator) generator).at(date);
                }
                else if (OTP_TYPE_STEAM_VALUE.equals(mOtpType)) {
                    return ((SteamOtpCodesGenerator) generator).getOtp(date);
                }
            }
            return null;
        }

        public boolean isNotSynced() {
            return mNotSynced;
        }

        public void setSynced() {
            if (mNotSynced) {
                final ContentValues values = new ContentValues();
                values.put(TWO_FACTOR_ACCOUNT_NOT_SYNCED, 0);
                if (TwoFactorAccountOperations.update(_id, values)) {
                    mNotSynced = false;
                }
            }
        }

        public long getLastUse() {
            return mLastUse;
        }

        public void setLastUse(final long last_use) {
            if (mLastUse != last_use) {
                final ContentValues values = new ContentValues();
                values.put(TWO_FACTOR_ACCOUNT_LAST_USE, last_use);
                if (TwoFactorAccountOperations.update(_id, values)) {
                    mLastUse = last_use;
                }
            }
        }

        public Bitmap getIconBitmap(@NotNull final Context context) {
            if (mDrawableIcon == null) {
                try {
                    File file = FileUtils.getFileIfExists(UiUtils.isDarkModeActive(context) ? mDarkIconPathname : mLightIconPathname);
                    file = (file == null) ? FileUtils.getFileIfExists(mGenericIconPathname) : file;
                    if (file != null) {
                        mDrawableIcon = BitmapFactory.decodeFile(file.getPath());
                    }
                }
                catch (Exception e) {
                    Log.e(Constants.LOG_TAG_NAME, "Exception while trying to read an icon", e);
                }
            }
            return mDrawableIcon;
        }

        public void setGenericIconPathname(@Nullable final String pathname) {
            if (! StringUtils.equals(mGenericIconPathname, pathname)) {
                final ContentValues values = new ContentValues();
                values.put(TWO_FACTOR_ACCOUNT_GENERIC_ICON_PATHNAME, pathname);
                if (TwoFactorAccountOperations.update(_id, values)) {
                    mGenericIconPathname = pathname;
                }
            }
        }

        public void setGenericIconPathname(@Nullable final File file) {
            setGenericIconPathname(file == null ? (String) null : file.getPath());
        }

        public void setDarkIconPathname(@Nullable final String pathname) {
            if (! StringUtils.equals(mDarkIconPathname, pathname)) {
                final ContentValues values = new ContentValues();
                values.put(TWO_FACTOR_ACCOUNT_DARK_ICON_PATHNAME, pathname);
                if (TwoFactorAccountOperations.update(_id, values)) {
                    mDarkIconPathname = pathname;
                }
            }
        }

        public void setDarkIconPathname(@Nullable final File file) {
            setDarkIconPathname(file == null ? (String) null : file.getPath());
        }

        public void setLightIconPathname(@Nullable final String pathname) {
            if (! StringUtils.equals(mLightIconPathname, pathname)) {
                final ContentValues values = new ContentValues();
                values.put(TWO_FACTOR_ACCOUNT_LIGHT_ICON_PATHNAME, pathname);
                if (TwoFactorAccountOperations.update(_id, values)) {
                    mLightIconPathname = pathname;
                }
            }
        }

        public void setLightIconPathname(@Nullable final File file) {
            setLightIconPathname(file == null ? (String) null : file.getPath());
        }

        public boolean delete() {
            if (TwoFactorAccountOperations.delete(_id)) {
                final String[] pathnames = new String[] { mDarkIconPathname, mLightIconPathname, mGenericIconPathname };
                for (final String pathname : pathnames) {
                    if (pathname != null) {
                        final File file = new File(pathname);
                        file.delete();
                    }
                }
                _id = -1;
                return true;
            }
            return false;
        }

        public boolean isDeleted() {
            return (_id < 0);
        }
    }

    private static final Object mSynchronizationObject = new Object();

    private static Context mContext = null;

    private static ApplicationDatabase mDatabase = null;

    private static String getDatabasePassword(@NotNull final Context context) {
        final SharedPreferences preferences = SharedPreferencesUtilities.getDefaultSharedPreferences(context);
        if (! preferences.contains(DATABASE_PASSWORD_KEY)) {
            final SharedPreferences.Editor editor = preferences.edit();
            SharedPreferencesUtilities.putEncryptedString(mContext, editor, DATABASE_PASSWORD_KEY, Base64.getEncoder().withoutPadding().encodeToString(KeyStoreUtils.getRandom(PASSWORD_LENGTH)));
            editor.apply();
        }
        return SharedPreferencesUtilities.getEncryptedString(mContext, preferences, DATABASE_PASSWORD_KEY, null);
    }

    public static void initialize(@NotNull final Context context) {
        synchronized (mSynchronizationObject) {
            if (mContext == null) {
                mContext = context.getApplicationContext();
                System.loadLibrary("sqlcipher");
                mDatabase = new ApplicationDatabase(context, getDatabasePassword(context));
                context.getDatabasePath(DATABASE_NAME).getParentFile().mkdirs();
            }
        }
    }

    private static class DatabaseOperations {
        private static SQLiteDatabase open(final boolean can_write) {
            return can_write ? mDatabase.getWritableDatabase() : mDatabase.getReadableDatabase();
        }

        private static void close(@Nullable final SQLiteDatabase database) {
            if (database != null) {
                database.close();
            }
        }

        private static void beginTransaction(@NotNull final SQLiteDatabase database) {
            database.beginTransaction();
        }

        private static void endTransaction(@NotNull final SQLiteDatabase database, final boolean commit_transaction) {
            if (commit_transaction) {
                database.setTransactionSuccessful();
            }
            database.endTransaction();
        }
    }

    public static class TwoFactorGroupOperations {
        private static TwoFactorGroup getByRowId(@NotNull final SQLiteDatabase database, final long id) {
            try (final Cursor cursor = database.query(true, TWO_FACTOR_GROUPS_TABLE_NAME, GROUP_PROJECTION, String.format("%s=?", ROW_ID), new String[] { String.valueOf(id) }, null, null, null, null)) {
                cursor.moveToFirst();
                if (cursor.getCount() == 1) {
                    return new TwoFactorGroup(cursor);
                }
            }
            return null;
        }

        private static TwoFactorGroup getByRowId(final long id) {
            synchronized (mSynchronizationObject) {
                if (mDatabase != null) {
                    try {
                        final SQLiteDatabase database = DatabaseOperations.open(false);
                        if (database != null) {
                            try {
                                return getByRowId(database, id);
                            }
                            finally {
                                DatabaseOperations.close(database);
                            }
                        }
                    }
                    catch (Exception e) {
                        Log.e(Constants.LOG_TAG_NAME, "Exception while trying to get a group by row-id", e);
                    }
                }
            }
            return null;
        }

        private static TwoFactorGroup getByServerId(@NotNull final SQLiteDatabase database, final long id) {
            try (final Cursor cursor = database.query(true, TWO_FACTOR_GROUPS_TABLE_NAME, GROUP_PROJECTION, String.format("%s=?", TWO_FACTOR_GROUP_SERVER_ID), new String[] { String.valueOf(id) }, null, null, null, null)) {
                if (cursor.getCount() == 1) {
                    cursor.moveToFirst();
                    return new TwoFactorGroup(cursor);
                }
            }
            return null;
        }

        private static TwoFactorGroup getByServerId(final long id) {
            synchronized (mSynchronizationObject) {
                if (mDatabase != null) {
                    try {
                        final SQLiteDatabase database = DatabaseOperations.open(false);
                        if (database != null) {
                            try {
                                return getByServerId(database, id);
                            }
                            finally {
                                DatabaseOperations.close(database);
                            }
                        }
                    }
                    catch (Exception e) {
                        Log.e(Constants.LOG_TAG_NAME, "Exception while trying to get a group by server-id", e);
                    }
                }
            }
            return null;
        }

        private static long add(@NotNull final SQLiteDatabase database, @NotNull final JSONObject object) throws Exception {
            final ContentValues values = new ContentValues();
            values.put(TWO_FACTOR_GROUP_SERVER_ID, object.getInt(Constants.TWO_FACTOR_AUTH_GROUP_ID_KEY));
            values.put(TWO_FACTOR_GROUP_NAME, object.getString(Constants.TWO_FACTOR_AUTH_GROUP_NAME_KEY));
            return database.insert(TWO_FACTOR_GROUPS_TABLE_NAME, null, values);
        }

        private static boolean add(@NotNull final SQLiteDatabase database, @Nullable final Collection<JSONObject> objects) throws Exception {
            if (objects != null) {
                for (final JSONObject object : objects) {
                    if ((ALLOW_GENERIC_GROUP_ADITION || (object.optInt(Constants.TWO_FACTOR_AUTH_GROUP_ID_KEY, GENERIC_GROUP_SERVER_ID) != GENERIC_GROUP_SERVER_ID)) && (add(database, object) < 0)) {
                        return false;
                    }
                }
            }
            return true;
        }

        public static boolean add(@Nullable final Collection<JSONObject> objects) {
            synchronized (mSynchronizationObject) {
                if (mDatabase != null) {
                    try {
                        final SQLiteDatabase database = DatabaseOperations.open(true);
                        if (database != null) {
                            try {
                                boolean commit_transaction = false;
                                DatabaseOperations.beginTransaction(database);
                                try {
                                    commit_transaction = add(database, objects);
                                    return commit_transaction;
                                }
                                finally {
                                    DatabaseOperations.endTransaction(database, commit_transaction);
                                }
                            }
                            finally {
                                DatabaseOperations.close(database);
                            }
                        }
                    }
                    catch(Exception e){
                        Log.e(Constants.LOG_TAG_NAME, "Exception while trying to add groups", e);
                    }
                }
            }
            return false;
        }

        private static int count(@NotNull final SQLiteDatabase database) {
            try (final Cursor cursor = database.query(true, TWO_FACTOR_GROUPS_TABLE_NAME, null, null, null, null, null, null, null)) {
                return cursor.getCount();
            }
        }

        private static boolean delete(@NotNull final SQLiteDatabase database) {
            database.delete(TWO_FACTOR_GROUPS_TABLE_NAME, null, null);
            return count(database) == 0;
        }

        public static boolean delete() {
            synchronized (mSynchronizationObject) {
                if (mDatabase != null) {
                    try {
                        final SQLiteDatabase database = DatabaseOperations.open(true);
                        if (database != null) {
                            try {
                                return delete(database);
                            }
                            finally {
                                DatabaseOperations.close(database);
                            }
                        }
                    }
                    catch (Exception e) {
                        Log.e(Constants.LOG_TAG_NAME, "Exception while trying to delete all groups", e);
                    }
                }
            }
            return false;
        }
    }

    public static class TwoFactorAccountOperations {
        public enum SortMode { SORT_BY_LAST_USE, SORT_BY_SERVICE};

        private static List<TwoFactorAccount> get(@NotNull final SQLiteDatabase database, @Nullable final SortMode sort_mode, final boolean only_not_synced_accounts) {
            try (final Cursor cursor = database.query(true, TWO_FACTOR_ACCOUNTS_TABLE_NAME, TWO_FACTOR_ACCOUNT_PROJECTION, only_not_synced_accounts ? String.format("%s=?", TWO_FACTOR_ACCOUNT_NOT_SYNCED) : null, only_not_synced_accounts ? new String[] { "1" } : null, null, null, sort_mode == SortMode.SORT_BY_LAST_USE ? String.format("%S DESC", TWO_FACTOR_ACCOUNT_LAST_USE) : sort_mode == SortMode.SORT_BY_SERVICE ? String.format("%s COLLATE NOCASE ASC, %s COLLATE NOCASE ASC", TWO_FACTOR_ACCOUNT_SERVICE, TWO_FACTOR_ACCOUNT_USER) : null, null)) {
                if (cursor.getCount() > 0) {
                    final List<TwoFactorAccount> accounts = new ArrayList<TwoFactorAccount>();
                    cursor.moveToFirst();
                    while (! cursor.isAfterLast()) {
                        accounts.add(new TwoFactorAccount(database, cursor));
                        cursor.moveToNext();
                    }
                    return accounts;
                }
            }
            return null;
        }

        public static List<TwoFactorAccount> get(@Nullable final SortMode sort_mode, final boolean only_not_synced_accounts) {
            synchronized (mSynchronizationObject) {
                if (mDatabase != null) {
                    try {
                        final SQLiteDatabase database = DatabaseOperations.open(false);
                        if (database != null) {
                            try {
                                return get(database, sort_mode, only_not_synced_accounts);
                            }
                            finally {
                                DatabaseOperations.close(database);
                            }
                        }
                    }
                    catch (Exception e) {
                        Log.e(Constants.LOG_TAG_NAME, "Exception while trying to get accounts", e);
                    }
                }
            }
            return null;
        }

        public static List<TwoFactorAccount> get(@Nullable final SortMode sort_mode) {
            return get(sort_mode, false);
        }

        public static List<TwoFactorAccount> get(final boolean only_not_synced_accounts) {
            return get(null, only_not_synced_accounts);
        }

        public static List<TwoFactorAccount> get() {
            return get(null, false);
        }

        private static TwoFactorAccount get(@NotNull final SQLiteDatabase database, final long id) {
            try (final Cursor cursor = database.query(true, TWO_FACTOR_ACCOUNTS_TABLE_NAME, TWO_FACTOR_ACCOUNT_PROJECTION, String.format("%s = ?", ROW_ID), new String[] { String.valueOf(id) }, null, null, null, null)) {
                if (cursor.getCount() == 1) {
                    return new TwoFactorAccount(database, cursor);
                }
            }
            return null;
        }

        private static TwoFactorAccount get(final long id) {
            synchronized (mSynchronizationObject) {
                if (mDatabase != null) {
                    try {
                        final SQLiteDatabase database = DatabaseOperations.open(false);
                        if (database != null) {
                            try {
                                return get(database, id);
                            }
                            finally {
                                DatabaseOperations.close(database);
                            }
                        }
                    }
                    catch (Exception e) {
                        Log.e(Constants.LOG_TAG_NAME, "Exception while trying to get an account", e);
                    }
                }
            }
            return null;
        }

        private static long add(@NotNull final SQLiteDatabase database, @NotNull final ContentValues values) {
            return database.insert(TWO_FACTOR_ACCOUNTS_TABLE_NAME, null, values);
        }

        public static TwoFactorAccount add(@NotNull final ContentValues values) throws Exception {
            synchronized (mSynchronizationObject) {
                if (mDatabase != null) {
                    try {
                        final SQLiteDatabase database = DatabaseOperations.open(true);
                        if (database != null) {
                            try {
                                final long id = add(database, values);
                                if (id >= 0) {
                                    return get(database, id);
                                }
                            }
                            finally {
                                DatabaseOperations.close(database);
                            }
                        }
                    }
                    catch (Exception e) {
                        Log.e(Constants.LOG_TAG_NAME, "Exception while trying to add an account", e);
                    }
                }
            }
            return null;
        }

        private static long add(@NotNull final SQLiteDatabase database, @NotNull final JSONObject object) throws Exception {
            final ContentValues values = new ContentValues();
            values.put(TWO_FACTOR_ACCOUNT_SERVER_ID, object.getInt(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_ID_KEY));
            values.put(TWO_FACTOR_ACCOUNT_SERVICE, object.getString(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_SERVICE_KEY));
            if (object.has(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_USER_KEY)) {
                values.put(TWO_FACTOR_ACCOUNT_USER, object.getString(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_USER_KEY));
            }
            if ((object.has(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_GROUP_KEY)) && (! object.isNull(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_GROUP_KEY)) && (ALLOW_GENERIC_GROUP_ADITION || (object.optInt(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_GROUP_KEY, GENERIC_GROUP_SERVER_ID) != GENERIC_GROUP_SERVER_ID))) {
                values.put(TWO_FACTOR_ACCOUNT_GROUP, TwoFactorGroupOperations.getByServerId(database, object.getInt(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_GROUP_KEY))._id);
            }
            if (object.has(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_ICON_KEY)) {
                values.put(TWO_FACTOR_ACCOUNT_ICON, object.getString(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_ICON_KEY));
            }
            values.put(TWO_FACTOR_ACCOUNT_OTP_TYPE, object.getString(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_OTP_TYPE_KEY));
            values.put(TWO_FACTOR_ACCOUNT_SECRET, object.getString(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_SECRET_KEY));
            values.put(TWO_FACTOR_ACCOUNT_PASSWORD_LENGTH, object.getInt(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_PASSWORD_LENGTH_KEY));
            values.put(TWO_FACTOR_ACCOUNT_ALGORITHM, object.getString(TWO_FACTOR_AUTH_ACCOUNT_DATA_ALGORITHM_KEY));
            if ((object.has(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_PERIOD_KEY)) && (! object.isNull(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_PERIOD_KEY))) {
                values.put(TWO_FACTOR_ACCOUNT_PERIOD, object.getInt(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_PERIOD_KEY));
            }
            if ((object.has(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_COUNTER_KEY)) && (! object.isNull(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_COUNTER_KEY))) {
                values.put(TWO_FACTOR_ACCOUNT_COUNTER, object.getInt(Constants.TWO_FACTOR_AUTH_ACCOUNT_DATA_COUNTER_KEY));
            }
            if (object.has(TWO_FACTOR_ACCOUNT_NOT_SYNCED)) {
                final Object value = object.get(TWO_FACTOR_ACCOUNT_NOT_SYNCED);
                values.put(TWO_FACTOR_ACCOUNT_NOT_SYNCED, value instanceof Boolean ? ((boolean) value) ? 1 : 0 : (int) value);
            }
            if (object.has(TWO_FACTOR_ACCOUNT_LAST_USE)) {
                values.put(SORT_ACCOUNTS_BY_LAST_USE_KEY, object.getLong(SORT_ACCOUNTS_BY_LAST_USE_KEY));
            }
            for (final String icon_pathname : new String[] { TWO_FACTOR_ACCOUNT_GENERIC_ICON_PATHNAME, TWO_FACTOR_ACCOUNT_DARK_ICON_PATHNAME, TWO_FACTOR_ACCOUNT_LIGHT_ICON_PATHNAME }) {
                if ((object.has(icon_pathname)) && (! object.isNull(icon_pathname))) {
                    final Object value = object.get(icon_pathname);
                    values.put(icon_pathname, value instanceof File ? ((File) value).getPath() : (String) value);
                }
            }
            return database.insert(TWO_FACTOR_ACCOUNTS_TABLE_NAME, null, values);
        }

        private static boolean add(@NotNull final SQLiteDatabase database, @Nullable Collection<JSONObject> objects) throws Exception {
            if (objects != null) {
                for (final JSONObject object : objects) {
                    if (add(database, object) < 0) {
                        return false;
                    }
                }
            }
            return true;
        }

        public static boolean add(@Nullable Collection<JSONObject> objects) {
            synchronized (mSynchronizationObject) {
                if (mDatabase != null) {
                    try {
                        final SQLiteDatabase database = DatabaseOperations.open(true);
                        if (database != null) {
                            try {
                                boolean commit_transaction = false;
                                DatabaseOperations.beginTransaction(database);
                                try {
                                    commit_transaction = add(database, objects);
                                    return commit_transaction;
                                }
                                finally {
                                    DatabaseOperations.endTransaction(database, commit_transaction);
                                }
                            }
                            finally {
                                DatabaseOperations.close(database);
                            }
                        }
                    }
                    catch (Exception e) {
                        Log.e(Constants.LOG_TAG_NAME, "Exception while trying to add accounts", e);
                    }
                }
            }
            return false;
        }

        private static boolean update(@NotNull final SQLiteDatabase database, final long id, @NotNull final ContentValues values) {
            return ((id >= 0) && (database.update(TWO_FACTOR_ACCOUNTS_TABLE_NAME, values, String.format("%s=?", ROW_ID), new String[] { String.valueOf(id) }) == 1));
        }

        private static boolean update(final long id, @NotNull final ContentValues values) {
            synchronized (mSynchronizationObject) {
                if (mDatabase != null) {
                    try {
                        final SQLiteDatabase database = DatabaseOperations.open(true);
                        if (database != null) {
                            try {
                                return update(database, id, values);
                            }
                            finally {
                                DatabaseOperations.close(database);
                            }
                        }
                    }
                    catch (Exception e) {
                        Log.e(Constants.LOG_TAG_NAME, "Exception while trying to update an account", e);
                    }
                }
            }
            return false;
        }

        private static boolean delete(@NotNull final SQLiteDatabase database, final long id) {
            return ((id >= 0) && (database.delete(TWO_FACTOR_ACCOUNTS_TABLE_NAME, String.format("%s=?", ROW_ID), new String[]{String.valueOf(id)}) == 1));
        }

        private static boolean delete(final long id) {
            synchronized (mSynchronizationObject) {
                if (mDatabase != null) {
                    try {
                        final SQLiteDatabase database = DatabaseOperations.open(true);
                        if (database != null) {
                            try {
                                return delete(database, id);
                            }
                            finally {
                                DatabaseOperations.close(database);
                            }
                        }
                    }
                    catch (Exception e) {
                        Log.e(Constants.LOG_TAG_NAME, "Exception while trying to delete an account", e);
                    }
                }
            }
            return false;
        }

        private static boolean delete(@NotNull final SQLiteDatabase database) {
            database.delete(TWO_FACTOR_ACCOUNTS_TABLE_NAME, null, null);
            return count(database) == 0;
        }

        public static boolean delete() {
            synchronized (mSynchronizationObject) {
                if (mDatabase != null) {
                    try {
                        final SQLiteDatabase database = DatabaseOperations.open(true);
                        if (database != null) {
                            try {
                                return delete(database);
                            }
                            finally {
                                DatabaseOperations.close(database);
                            }
                        }
                    }
                    catch (Exception e) {
                        Log.e(Constants.LOG_TAG_NAME, "Exception while trying to delete all accounts", e);
                    }
                }
            }
            return false;
        }

        private static int count(@NotNull final SQLiteDatabase database, final boolean only_not_synced_accounts) {
            try (final Cursor cursor = database.query(true, TWO_FACTOR_ACCOUNTS_TABLE_NAME, null, only_not_synced_accounts ? String.format("%s=?", TWO_FACTOR_ACCOUNT_NOT_SYNCED) : null, only_not_synced_accounts ? new String[] { "1" } : null, null, null, null, null)) {
                return cursor.getCount();
            }
        }

        private static int count(@NotNull final SQLiteDatabase database) {
            return count(database, false);
        }

        public static int count(final boolean only_not_synced_accounts) {
            synchronized (mSynchronizationObject) {
                if (mDatabase != null) {
                    try {
                        final SQLiteDatabase database = DatabaseOperations.open(false);
                        if (database != null) {
                            try {
                                return count(database, only_not_synced_accounts);
                            }
                            finally {
                                DatabaseOperations.close(database);
                            }
                        }
                    }
                    catch (Exception e) {
                        Log.e(Constants.LOG_TAG_NAME, "Exception while trying to count accounts", e);
                    }
                }
            }
            return 0;
        }

        public static int count() {
            return count(false);
        }

        public static boolean exists(final boolean only_not_synced_accounts) {
            return count(only_not_synced_accounts) != 0;
        }

        public static boolean exists() {
            return exists(false);
        }
    }
    public static class TwoFactorAccountAtomicOperations {
        public static boolean resetLastUses() {
            synchronized (mSynchronizationObject) {
                if (mDatabase != null) {
                    try {
                        final SQLiteDatabase database = DatabaseOperations.open(true);
                        if (database != null) {
                            try {
                                final ContentValues values = new ContentValues();
                                values.put(TWO_FACTOR_ACCOUNT_LAST_USE, 0);
                                database.update(TWO_FACTOR_ACCOUNTS_TABLE_NAME, values, null, null);
                                return true;
                            }
                            finally {
                                DatabaseOperations.close(database);
                            }
                        }
                    }
                    catch (Exception e) {
                        Log.e(Constants.LOG_TAG_NAME, "Exception while trying to update an account", e);
                    }
                }
            }
            return false;
        }

        private static boolean deleteAccountsAndGroups(@NotNull final SQLiteDatabase database) {
            return ((TwoFactorAccountOperations.delete(database)) && (TwoFactorGroupOperations.delete(database)));
        }

        public static boolean deleteAccountsAndGroups() {
            synchronized (mSynchronizationObject) {
                if (mDatabase != null) {
                    try {
                        final SQLiteDatabase database = DatabaseOperations.open(true);
                        if (database != null) {
                            try {
                                boolean commit_transaction = false;
                                DatabaseOperations.beginTransaction(database);
                                try {
                                    commit_transaction = deleteAccountsAndGroups(database);
                                    return commit_transaction;
                                }
                                finally {
                                    DatabaseOperations.endTransaction(database, commit_transaction);
                                }
                            }
                            finally {
                                DatabaseOperations.close(database);
                            }
                        }
                    }
                    catch (Exception e) {
                        Log.e(Constants.LOG_TAG_NAME, "Exception while trying to set accounts and groups", e);
                    }
                }
            }
            return false;
        }

        public static boolean set(@Nullable final Collection<JSONObject> accounts, @Nullable final Collection<JSONObject> groups) {
            synchronized (mSynchronizationObject) {
                if (mDatabase != null) {
                    try {
                        final SQLiteDatabase database = DatabaseOperations.open(true);
                        if (database != null) {
                            try {
                                boolean commit_transaction = false;
                                DatabaseOperations.beginTransaction(database);
                                try {
                                    if (deleteAccountsAndGroups(database)) {
                                        if (TwoFactorGroupOperations.add(database, groups)) {
                                            commit_transaction = TwoFactorAccountOperations.add(database, accounts);
                                            return commit_transaction;
                                        }
                                    }
                                }
                                finally {
                                    DatabaseOperations.endTransaction(database, commit_transaction);
                                }
                            }
                            finally {
                                DatabaseOperations.close(database);
                            }
                        }
                    }
                    catch (Exception e) {
                        Log.e(Constants.LOG_TAG_NAME, "Exception while trying to set accounts and groups", e);
                    }
                }
            }
            return false;
        }
    }
}

