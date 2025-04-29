package com.twofauth.android.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.text.format.DateUtils;
import android.util.Log;

import com.bastiaanjansen.otp.HMACAlgorithm;
import com.bastiaanjansen.otp.HOTPGenerator;
import com.bastiaanjansen.otp.TOTPGenerator;

import com.twofauth.android.Constants;
import com.twofauth.android.Main;
import com.twofauth.android.utils.Strings;
import com.twofauth.android.API;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class TwoFactorAccount extends SynceableTableRow {
    public static final String SERVER_IDENTITY = "server_identity";
    public static final String SERVICE = "service";
    public static final String ACCOUNT = "account";
    public static final String GROUP = "group_id";
    public static final String ICON = "icon";
    public static final String OTP_TYPE = "otp_type";
    public static final String SECRET = "secret";
    public static final String OTP_LENGTH = "digits";
    public static final String ALGORITHM = "algorithm";
    public static final String PERIOD = "period";
    public static final String COUNTER = "counter";
    public static final String LAST_USE = "last_use";

    protected static final String[] PROJECTION = new String[] {
        ROW_ID,
        SERVER_IDENTITY,
        REMOTE_ID,
        SERVICE,
        ACCOUNT,
        GROUP,
        ICON,
        OTP_TYPE,
        SECRET,
        OTP_LENGTH,
        ALGORITHM,
        PERIOD,
        COUNTER,
        LAST_USE,
        STATUS,
    };

    private static final int SERVER_IDENTITY_ORDER = ROW_ID_ORDER + 1;
    private static final int REMOTE_ID_ORDER = SERVER_IDENTITY_ORDER + 1;
    private static final int SERVICE_ORDER = REMOTE_ID_ORDER + 1;
    private static final int ACCOUNT_ORDER = SERVICE_ORDER + 1;
    private static final int GROUP_ORDER = ACCOUNT_ORDER + 1;
    private static final int ICON_ORDER = GROUP_ORDER + 1;
    private static final int OTP_TYPE_ORDER = ICON_ORDER + 1;
    private static final int SECRET_ORDER = OTP_TYPE_ORDER + 1;
    private static final int OTP_LENGTH_ORDER = SECRET_ORDER + 1;
    private static final int ALGORITHM_ORDER = OTP_LENGTH_ORDER + 1;
    private static final int PERIOD_ORDER = ALGORITHM_ORDER + 1;
    private static final int COUNTER_ORDER = PERIOD_ORDER + 1;
    private static final int LAST_USE_ORDER = COUNTER_ORDER + 1;
    private static final int STATUS_ORDER = LAST_USE_ORDER + 1;

    private static class Base32Utils {
        private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

        private static boolean isValid(@NotNull final String string) {
            for (final char character : string.trim().replace("=", "").toCharArray()) {
                if (ALPHABET.indexOf(character) < 0) { return false; }
            }
            return true;
        }

        private static @NotNull byte[] decode(@NotNull String encoded_string) {
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
                    throw new IllegalArgumentException("Invalid Base32 character: " + character);
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
        private static final String ALGORITHM = "HmacSHA1";

        private final byte[] mSecret;
        private final long mPeriod;

        SteamOtpCodesGenerator(@NotNull final String secret, final int period_in_seconds) {
            mSecret = Base32Utils.decode(secret);
            mPeriod = period_in_seconds * DateUtils.SECOND_IN_MILLIS;
        }

        public @Nullable String getOtp(@NotNull final Date date) {
            try {
                final ByteBuffer time_buffer = ByteBuffer.allocate(8);
                time_buffer.putLong(date.getTime() / mPeriod);
                final Mac mac = Mac.getInstance(ALGORITHM);
                final SecretKeySpec key_spec = new SecretKeySpec(mSecret, ALGORITHM);
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
                Log.e(Main.LOG_TAG_NAME, "Exception while generating a Steam OTP Code", e);
            }
            return null;
        }

        public @Nullable String getOtp() {
            return getOtp(new Date());
        }

        public long getPeriodInMillis() { return mPeriod; }

        public long getPeriodUntilNextTimeWindowInMillis() {
            return mPeriod - System.currentTimeMillis() % mPeriod;
        }

        public static boolean isSupported(@NotNull final String algorithm, final int length) {
            return ((length == 5) && Constants.ALGORITHM_SHA1.equals(algorithm));
        }
    }

    private static class HotpCodesGeneratorBasedOnMD5 {
        private static final String ALGORITHM = "HmacMD5";

        private final byte[] mSecret;
        private final int mDigits;

        HotpCodesGeneratorBasedOnMD5(@NotNull final String secret, final int digits) {
            mSecret = Base32Utils.decode(secret);
            mDigits = digits;
        }

        public @Nullable String getOtp(final long counter) {
            try {
                final ByteBuffer buffer = ByteBuffer.allocate(8);
                buffer.putLong(counter);
                final Mac hmac = Mac.getInstance(ALGORITHM);
                hmac.init(new SecretKeySpec(mSecret, ALGORITHM));
                final byte[] hmac_bytes = hmac.doFinal(buffer.array());
                final int offset = hmac_bytes[hmac_bytes.length - 1] & 0x0F;
                final int binary_code = ((hmac_bytes[offset] & 0x7F) << 24) | ((hmac_bytes[offset + 1] & 0xFF) << 16) | ((hmac_bytes[offset + 2] & 0xFF) << 8) | (hmac_bytes[offset + 3] & 0xFF);
                return String.format("%0" + mDigits + "d", binary_code % (int) Math.pow(10, mDigits));
            }
            catch (Exception e) {
                Log.e(Main.LOG_TAG_NAME, "Exception while generating a MD5 OTP Code", e);
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
        public @Nullable String getOtp(long time) {
            return super.getOtp(time / mPeriod);
        }

        public @Nullable String getOtp(@NotNull final Date date) {
            return getOtp(date.getTime());
        }

        public @Nullable String getOtp() {
            return getOtp(new Date());
        }

        public long getPeriodInMillis() { return mPeriod; }

        public long getPeriodUntilNextTimeWindowInMillis() {
            return mPeriod - System.currentTimeMillis() % mPeriod;
        }
    }

    private TwoFactorServerIdentity mServerIdentity;
    private String mService;
    private String mAccount;
    private TwoFactorGroup mGroup;
    private TwoFactorIcon mIcon;
    private String mOtpType;
    private String mSecret;
    private int mOtpLength;
    private String mAlgorithm;
    private int mPeriod;
    private int mCounter;
    private long mLastUse;

    private Object mPasswordGenerator = null;

    public TwoFactorAccount(@NotNull final SQLiteDatabase database, @NotNull final Cursor cursor) throws Exception {
        super(TwoFactorAccountsHelper.TABLE_NAME, cursor, REMOTE_ID_ORDER, STATUS_ORDER);
        mServerIdentity = Main.getInstance().getDatabaseHelper().getTwoFactorServerIdentitiesHelper().instance(database, cursor.getLong(SERVER_IDENTITY_ORDER));
        mService = cursor.getString(SERVICE_ORDER);
        mAccount = cursor.getString(ACCOUNT_ORDER);
        mGroup = cursor.isNull(GROUP_ORDER) ? null : Main.getInstance().getDatabaseHelper().getTwoFactorGroupsHelper().instance(database, cursor.getLong(GROUP_ORDER));
        mIcon = cursor.isNull(ICON_ORDER) ? null : Main.getInstance().getDatabaseHelper().getTwoFactorIconsHelper().instance(database, cursor.getLong(ICON_ORDER));
        mOtpType = cursor.getString(OTP_TYPE_ORDER);
        mSecret = cursor.getString(SECRET_ORDER);
        mOtpLength = cursor.getInt(OTP_LENGTH_ORDER);
        mAlgorithm = cursor.getString(ALGORITHM_ORDER);
        mPeriod = cursor.getInt(PERIOD_ORDER);
        mCounter = cursor.getInt(COUNTER_ORDER);
        mLastUse = cursor.getLong(LAST_USE_ORDER);
    }

    public TwoFactorAccount() {
        super(TwoFactorAccountsHelper.TABLE_NAME);
        mServerIdentity = null;
        mService = "";
        mAccount = "";
        mGroup = null;
        mIcon = null;
        mOtpType = Constants.DEFAULT_OTP_TYPE;
        mSecret = null;
        mOtpLength = Constants.DEFAULT_OTP_LENGTH;
        mAlgorithm = Constants.DEFAULT_ALGORITHM;
        mPeriod = Constants.DEFAULT_PERIOD;
        mCounter = Constants.DEFAULT_COUNTER;
        mLastUse = 0;
    }

    public TwoFactorAccount(@NotNull final TwoFactorAccount account) {
        this();
        setRowId(account.getRowId());
        setServerIdentity(account.getServerIdentity());
        setRemoteId(account.getRemoteId());
        setService(account.getService());
        setAccount(account.getAccount());
        setGroup(account.getGroup());
        setIcon(account.getIcon());
        setOtpType(account.getOtpType());
        setSecret(account.getSecret());
        setOtpLength(account.getOtpLength());
        setAlgorithm(account.getAlgorithm());
        setPeriod(account.getPeriod());
        setCounter(account.getCounter());
        setLastUse(account.getLastUse());
        setStatus(account.getStatus());
    }

    public TwoFactorAccount(@NotNull final SQLiteDatabase database, @NotNull final JSONObject object) throws Exception {
        this();
        setServerIdentity(object.optInt(SERVER_IDENTITY, -1) == -1 ? null : Main.getInstance().getDatabaseHelper().getTwoFactorServerIdentitiesHelper().get(database, object.getInt(SERVER_IDENTITY)));
        fromJSONObject(database, object);
    }

    private @NotNull Object getOtpGenerator() {
        if (mPasswordGenerator == null) {
            if (isSteam()) {
                mPasswordGenerator = new SteamOtpCodesGenerator(mSecret, mPeriod);
            }
            else if (isTotp()) {
                if (Constants.ALGORITHM_MD5.equals(mAlgorithm)) {
                    mPasswordGenerator = new TotpCodesGeneratorBasedOnMD5(mSecret, mOtpLength, mPeriod);
                }
                else {
                    mPasswordGenerator = new TOTPGenerator.Builder(mSecret).withHOTPGenerator(builder -> {
                        builder.withPasswordLength(mOtpLength);
                        builder.withAlgorithm(Constants.ALGORITHM_SHA512.equals(mAlgorithm) ? HMACAlgorithm.SHA512 : Constants.ALGORITHM_SHA384.equals(mAlgorithm) ? HMACAlgorithm.SHA384 : Constants.ALGORITHM_SHA256.equals(mAlgorithm) ? HMACAlgorithm.SHA256 : Constants.ALGORITHM_SHA224.equals(mAlgorithm) ? HMACAlgorithm.SHA224 : HMACAlgorithm.SHA1);
                    }).withPeriod(Duration.ofSeconds(mPeriod)).build();
                }
            }
            else if (isHotp()) {
                if (Constants.ALGORITHM_MD5.equals(mAlgorithm)) {
                    mPasswordGenerator = new HotpCodesGeneratorBasedOnMD5(mSecret, mOtpLength);
                }
                else {
                    mPasswordGenerator = new HOTPGenerator.Builder(mSecret).withAlgorithm(Constants.ALGORITHM_SHA512.equals(mAlgorithm) ? HMACAlgorithm.SHA512 : Constants.ALGORITHM_SHA384.equals(mAlgorithm) ? HMACAlgorithm.SHA384 : Constants.ALGORITHM_SHA256.equals(mAlgorithm) ? HMACAlgorithm.SHA256 : Constants.ALGORITHM_SHA224.equals(mAlgorithm) ? HMACAlgorithm.SHA224 : HMACAlgorithm.SHA1).withPasswordLength(mOtpLength).build();
                }
            }
        }
        return mPasswordGenerator;
    }

    public void fromJSONObject(@NotNull final SQLiteDatabase database, @NotNull final JSONObject object) throws Exception {
        setRemoteId(object.optInt(Constants.ACCOUNT_DATA_ID_KEY, mRemoteId));
        setService(object.optString(Constants.ACCOUNT_DATA_SERVICE_KEY, mService));
        setAccount(object.optString(Constants.ACCOUNT_DATA_USER_KEY, mService));
        setGroup(object.optInt(Constants.ACCOUNT_DATA_GROUP_KEY, 0) == 0 ? null : Main.getInstance().getDatabaseHelper().getTwoFactorGroupsHelper().instance(database, object.getInt(Constants.ACCOUNT_DATA_GROUP_KEY)));
        setIcon(object.optInt(Constants.ACCOUNT_DATA_ICON_KEY, 0) == 0  ? null : Main.getInstance().getDatabaseHelper().getTwoFactorIconsHelper().instance(database, object.getInt(Constants.ACCOUNT_DATA_ICON_KEY)));
        setOtpType(object.optString(Constants.ACCOUNT_DATA_OTP_TYPE_KEY, mOtpType));
        setSecret(object.optString(Constants.ACCOUNT_DATA_SECRET_KEY, mSecret));
        setOtpLength(object.optInt(Constants.ACCOUNT_DATA_OTP_LENGTH_KEY, mOtpLength));
        setAlgorithm(object.optString(Constants.ACCOUNT_DATA_ALGORITHM_KEY, mAlgorithm));
        setPeriod(object.optInt(Constants.ACCOUNT_DATA_PERIOD_KEY, mPeriod));
        setCounter(object.optInt(Constants.ACCOUNT_DATA_COUNTER_KEY, mCounter));
    }

    public @NotNull JSONObject toJSONObject() {
        try {
            final JSONObject object = new JSONObject();
            object.put(SERVER_IDENTITY, hasServerIdentity() ? getServerIdentity().getRowId() : -1);
            if (getRemoteId() > 0) { object.put(Constants.ACCOUNT_DATA_ID_KEY, getRemoteId()); }
            object.put(Constants.ACCOUNT_DATA_SERVICE_KEY, getService());
            object.put(Constants.ACCOUNT_DATA_USER_KEY, getAccount());
            object.put(Constants.ACCOUNT_DATA_GROUP_KEY, hasGroup() ? getGroup().getRemoteId() : JSONObject.NULL);
            object.put(Constants.ACCOUNT_DATA_ICON_KEY, hasIcon() && API.ICON_SOURCE_DEFAULT.equals(getIcon().getSource()) ? getIcon().getSourceId() : JSONObject.NULL);
            object.put(Constants.ACCOUNT_DATA_OTP_TYPE_KEY, getOtpType());
            object.put(Constants.ACCOUNT_DATA_SECRET_KEY, getValidatedSecret());
            object.put(Constants.ACCOUNT_DATA_OTP_LENGTH_KEY, getOtpLength());
            object.put(Constants.ACCOUNT_DATA_ALGORITHM_KEY, getAlgorithm());
            if (isTotp()) { object.put(Constants.ACCOUNT_DATA_PERIOD_KEY, getPeriod()); }
            if (isHotp()) { object.put(Constants.ACCOUNT_DATA_COUNTER_KEY, getCounter()); }
            return object;
        }
        catch (JSONException e) {
            Log.e(Main.LOG_TAG_NAME, "Exception while trying to convert an account to JSON", e);
            throw new RuntimeException(e);
        }
    }

    public boolean hasServerIdentity() {
        return ((mServerIdentity != null) && (mServerIdentity.getRemoteId() != 0));
    }

    public @Nullable TwoFactorServerIdentity getServerIdentity() { return mServerIdentity; }

    public void setServerIdentity(@Nullable final TwoFactorServerIdentity server_identity) {
        if (! isDeleted()) {
            final long current_server_identity = (mServerIdentity == null) ? -1 : mServerIdentity.getRowId(), new_server_identity = (server_identity == null) ? -1 : server_identity.getRowId();
            if (current_server_identity != new_server_identity) {
                mServerIdentity = server_identity;
                setDirty(SERVER_IDENTITY, true);
                setDirty(STATUS, mStatus = STATUS_NOT_SYNCED);
            }
        }
    }

    public boolean hasService() {
        return ! Strings.isEmptyOrNull(getService());
    }

    public @Nullable String getService() {
        return mService;
    }

    public void setService(@NotNull final String service) {
        if ((! isDeleted()) && (! Strings.equals(mService, service))) {
            setDirty(SERVICE, mService = service);
            setDirty(STATUS, mStatus = STATUS_NOT_SYNCED);
        }
    }

    public boolean hasAccount() {
        return ! Strings.isEmptyOrNull(getAccount());
    }

    public @Nullable String getAccount() {
        return mAccount;
    }

    public void setAccount(@NotNull final String account) {
        if ((! isDeleted()) && (! Strings.equals(mAccount, account))) {
            setDirty(ACCOUNT, mAccount = account);
            setDirty(STATUS, mStatus = STATUS_NOT_SYNCED);
        }
    }

    public boolean hasGroup() {
        return mGroup != null;
    }

    public @Nullable TwoFactorGroup getGroup() { return mGroup; }

    public void setGroup(@Nullable final TwoFactorGroup group) {
        if (! isDeleted()) {
            mGroup = group;
            setDirty(GROUP, true);
        }
    }

    public boolean hasIcon() {
        return (mIcon != null);
    }

    public @Nullable TwoFactorIcon getIcon() {
        return mIcon;
    }

    public void setIcon(@Nullable final TwoFactorIcon icon) {
        if (! isDeleted()) {
            mIcon = icon;
            setDirty(ICON, true);
        }
    }

    public boolean isOtpTypeSupported() {
        if ((Constants.OTP_TYPE_TOTP.equals(mOtpType)) || (Constants.OTP_TYPE_HOTP.equals(mOtpType))) {
            for (String supported_algorithm : new String[] { Constants.ALGORITHM_SHA512, Constants.ALGORITHM_SHA384, Constants.ALGORITHM_SHA256, Constants.ALGORITHM_SHA224, Constants.ALGORITHM_SHA1, Constants.ALGORITHM_MD5 }) {
                if (supported_algorithm.equals(mAlgorithm)) { return true; }
            }
        }
        if (Constants.OTP_TYPE_STEAM.equals(mOtpType)) {
            return SteamOtpCodesGenerator.isSupported(mAlgorithm, mOtpLength);
        }
        return false;
    }

    public @Nullable String getOtpType() {
        return mOtpType;
    }

    public boolean isHotp() {
        return isHotp(getOtpType());
    }

    public static boolean isHotp(@Nullable final String otp_type) {
        return Constants.OTP_TYPE_HOTP.equals(otp_type);
    }

    public boolean isSteam() {
        return isSteam(getOtpType());
    }

    public static boolean isSteam(@Nullable final String otp_type) {
        return Constants.OTP_TYPE_STEAM.equals(otp_type);
    }

    public boolean isTotp() {
        return isTotp(getOtpType());
    }

    public static boolean isTotp(@Nullable final String otp_type) {
        return (Constants.OTP_TYPE_TOTP.equals(otp_type) || isSteam(otp_type));
    }

    public void setOtpType(@NotNull final String otp_type) {
        if ((! isDeleted()) && (! Strings.equals(mOtpType, otp_type))) {
            setDirty(OTP_TYPE, mOtpType = otp_type);
            if (isSteam()) { setAlgorithm(Constants.ALGORITHM_SHA1); setOtpLength(5); }
            setDirty(STATUS, mStatus = STATUS_NOT_SYNCED);
            mPasswordGenerator = null;
        }
    }

    public @Nullable String getSecret() {
        return mSecret;
    }

    public @Nullable String getValidatedSecret() {
        String standarized_secret = mSecret;
        if (standarized_secret != null) { standarized_secret = standarized_secret.replace(" ", "").toUpperCase(); }
        return (Strings.isEmptyOrNull(standarized_secret) || (! Base32Utils.isValid(standarized_secret))) ? null : standarized_secret;
    }

    public void setSecret(@NotNull final String secret) {
        if ((! isDeleted()) && (! Strings.equals(mSecret, secret))) {
            setDirty(SECRET, mSecret = secret);
            setDirty(STATUS, mStatus = STATUS_NOT_SYNCED);
            mPasswordGenerator = null;
        }
    }

    public int getOtpLength() {
        return mOtpLength;
    }

    public void setOtpLength(final int otp_length) {
        if ((! isDeleted()) && (mOtpLength != otp_length)) {
            setDirty(OTP_LENGTH, mOtpLength = otp_length);
            setDirty(STATUS, mStatus = STATUS_NOT_SYNCED);
            mPasswordGenerator = null;
        }
    }

    public @Nullable String getAlgorithm() {
        return mAlgorithm;
    }

    public void setAlgorithm(@NotNull final String algorithm) {
        if ((! isDeleted()) && (! Strings.equals(mAlgorithm, algorithm))) {
            setDirty(ALGORITHM, mAlgorithm = algorithm);
            setDirty(STATUS, mStatus = STATUS_NOT_SYNCED);
            mPasswordGenerator = null;
        }
    }

    public int getPeriod() {
        return mPeriod;
    }

    public long getPeriodInMillis() {
        return mPeriod * DateUtils.SECOND_IN_MILLIS;
    }

    public void setPeriod(final int period) {
        if ((! isDeleted()) && (mPeriod != period)) {
            setDirty(PERIOD, mPeriod = period);
            setDirty(STATUS, mStatus = STATUS_NOT_SYNCED);
            mPasswordGenerator = null;
        }
    }

    public int getCounter() {
        return mCounter;
    }

    public void setCounter(final int counter) {
        if ((! isDeleted()) && (mCounter != counter)) {
            setDirty(COUNTER, mCounter = counter);
            setDirty(STATUS, mStatus = STATUS_NOT_SYNCED);
        }
    }

    public long getOtpMillis() {
        Object generator = getOtpGenerator();
        if (generator != null) {
            if (isSteam()) {
                return ((SteamOtpCodesGenerator) generator).getPeriodInMillis();
            }
            else if (isTotp()) {
                if (Constants.ALGORITHM_MD5.equals(mAlgorithm)) {
                    return ((TotpCodesGeneratorBasedOnMD5) generator).getPeriodInMillis();
                }
                return ((TOTPGenerator) generator).getPeriod().toMillis();
            }
            else if (isHotp()) {
                return Long.MAX_VALUE;
            }
        }
        return -1;
    }

    public long getMillisUntilNextOtp() {
        Object generator = getOtpGenerator();
        if (generator != null) {
            if (isSteam()) {
                return ((SteamOtpCodesGenerator) generator).getPeriodUntilNextTimeWindowInMillis();
            }
            else if (isTotp()) {
                if (Constants.ALGORITHM_MD5.equals(mAlgorithm)) {
                    return ((TotpCodesGeneratorBasedOnMD5) generator).getPeriodUntilNextTimeWindowInMillis();
                }
                return ((TOTPGenerator) generator).durationUntilNextTimeWindow().toMillis();
            }
            else if (isHotp()) {
                return Long.MAX_VALUE;
            }
        }
        return -1;
    }

    public long getMillisUntilNextOtpCompleteCycle() {
        Object generator = getOtpGenerator();
        if (generator != null) {
            if (isSteam()) {
                return ((SteamOtpCodesGenerator) generator).getPeriodUntilNextTimeWindowInMillis() + ((SteamOtpCodesGenerator) generator).getPeriodInMillis();
            }
            else if (isTotp()) {
                if (Constants.ALGORITHM_MD5.equals(mAlgorithm)) {
                    return ((TotpCodesGeneratorBasedOnMD5) generator).getPeriodUntilNextTimeWindowInMillis() + ((TotpCodesGeneratorBasedOnMD5) generator).getPeriodInMillis();
                }
                return ((TOTPGenerator) generator).durationUntilNextTimeWindow().toMillis() + ((TOTPGenerator) generator).getPeriod().toMillis();
            }
            else if (isHotp()) {
                return Long.MAX_VALUE;
            }
        }
        return -1;
    }

    public @Nullable String getOtp() {
        Object generator = getOtpGenerator();
        if (generator != null) {
            if (isSteam()) {
                return ((SteamOtpCodesGenerator) generator).getOtp();
            }
            else if (isTotp()) {
                if (Constants.ALGORITHM_MD5.equals(mAlgorithm)) {
                    return ((TotpCodesGeneratorBasedOnMD5) generator).getOtp();
                }
                return ((TOTPGenerator) generator).now();
            }
            else if (isHotp()) {
                if (Constants.ALGORITHM_MD5.equals(mAlgorithm)) {
                    return ((HotpCodesGeneratorBasedOnMD5) generator).getOtp(mCounter - 1);
                }
                return ((HOTPGenerator) generator).generate(mCounter - 1);
            }
        }
        return null;
    }

    public @Nullable String getOtp(final Date date) {
        Object generator = getOtpGenerator();
        if (generator != null) {
            if (isSteam()) {
                return ((SteamOtpCodesGenerator) generator).getOtp(date);
            }
            else if (isTotp()) {
                if (Constants.ALGORITHM_MD5.equals(mAlgorithm)) {
                    return ((TotpCodesGeneratorBasedOnMD5) generator).getOtp(date);
                }
                return ((TOTPGenerator) generator).at(date);
            }
        }
        return null;
    }

    public long getLastUse() {
        return mLastUse;
    }

    public void setLastUse(final long last_use) {
        if (mLastUse != last_use) {
            setDirty(LAST_USE, mLastUse = last_use);
        }
    }

    @Override
    public boolean isDirty() {
        return ((hasIcon() && getIcon().isDirty()) || (hasGroup() && getGroup().isDirty()) || super.isDirty());
    }

    @Override
    protected boolean onSavingData(@NotNull final SQLiteDatabase database, @NotNull final Context context, @NotNull final ContentValues values) throws Exception {
        if (mGroup != null) { mGroup.save(database, context); }
        if (mIcon != null) { mIcon.save(database, context); }
        return super.onSavingData(database, context, values);
    }

    protected void setDatabaseValues(@NotNull final ContentValues values) {
        if (values.size() == 0) {
            if (mServerIdentity == null) { throw new SQLException("Server Identity cannot be NULL"); }
            values.put(SERVER_IDENTITY, mServerIdentity.getRowId());
            super.setDatabaseValues(values);
            values.put(SERVICE, mService);
            values.put(ACCOUNT, mAccount);
            values.put(GROUP, mGroup == null ? null : mGroup.getRowId());
            values.put(ICON, mIcon == null ? null : mIcon.getRowId());
            values.put(OTP_TYPE, mOtpType);
            values.put(SECRET, mSecret);
            values.put(OTP_LENGTH, mOtpLength);
            values.put(ALGORITHM, mAlgorithm);
            values.put(PERIOD, mPeriod);
            values.put(COUNTER, mCounter);
            values.put(LAST_USE, mLastUse);
        }
        else {
            if (values.containsKey(SERVER_IDENTITY)) {
                if (mServerIdentity == null) { throw new SQLException("Server Identity cannot be NULL"); }
                values.put(SERVER_IDENTITY, mServerIdentity.getRowId());
            }
            if (values.containsKey(GROUP)) {
                if ((mGroup != null) && (mGroup.getServerIdentity().getRowId() != mServerIdentity.getRowId())) { throw new SQLException("Server Identity and Group Server Identity will be equals"); }
                values.put(GROUP, mGroup == null ? null : mGroup.getRowId());
            }
            if (values.containsKey(ICON)) {
                values.put(ICON, mIcon == null ? null : mIcon.getRowId());
            }
        }
    }

    protected void onDataDeleted(@NotNull final SQLiteDatabase database, @NotNull final Context context) {
        try {
            if (mIcon != null) {
                try {
                    mIcon.delete(database, context);
                }
                catch (SQLiteConstraintException ignored) {
                }
                catch (Exception e) {
                    Log.e(Main.LOG_TAG_NAME, "Exception while trying to delete an icon", e);
                }
            }
        }
        finally {
            mServerIdentity = null;
            mGroup = null;
            mIcon = null;
        }
    }
}
