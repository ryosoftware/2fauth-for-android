package com.twofauth.android;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import org.jetbrains.annotations.NotNull;

import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class KeyStoreUtils {
    private static final String PROVIDER_NAME = "AndroidKeyStore";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private static final int IV_LENGTH = 12;
    private static final int TLEN_LENGTH = 128;

    private static final String ENCRYPTED_STRING_PARTS_SEPARATOR = "~";

    public static void addKeyAlias(@NotNull final String key_alias, final boolean authentication_required) throws Exception {
        final KeyStore keystore = KeyStore.getInstance(PROVIDER_NAME);
        keystore.load(null);
        if (! keystore.containsAlias(key_alias)) {
            final KeyGenerator key_generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER_NAME);
            key_generator.init(new KeyGenParameterSpec.Builder(key_alias, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setUserAuthenticationRequired(authentication_required).setInvalidatedByBiometricEnrollment(authentication_required).build());
            key_generator.generateKey();
        }
    }

    public static void addKeyAlias(@NotNull final String key_alias) throws Exception {
        addKeyAlias(key_alias, false);
    }

    public static void deleteKeyAlias(@NotNull final String key_alias) throws Exception {
        final KeyStore keystore = KeyStore.getInstance(PROVIDER_NAME);
        keystore.load(null);
        if (keystore.containsAlias(key_alias)) {
            keystore.deleteEntry(key_alias);
        }
    }

    private static SecretKey getKeyAlias(@NotNull final String key_alias) throws Exception {
        final KeyStore keystore = KeyStore.getInstance(PROVIDER_NAME);
        keystore.load(null);
        return keystore.containsAlias(key_alias) ? (SecretKey) keystore.getKey(key_alias, null) : null;
    }

    public static boolean keyExists(@NotNull final String key_alias) throws Exception {
        return getKeyAlias(key_alias) != null;
    }

    public static String encrypt(@NotNull final String key_alias, @NotNull final String data) throws Exception {
        final SecretKey key = getKeyAlias(key_alias);
        if (key != null) {
            final Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return Base64.getEncoder().withoutPadding().encodeToString(cipher.getIV()) + ENCRYPTED_STRING_PARTS_SEPARATOR + Base64.getEncoder().withoutPadding().encodeToString(cipher.doFinal(data.getBytes()));
        }
        return null;
    }

    public static String decrypt(@NotNull final String key_alias, @NotNull final String data) throws Exception {
        final SecretKey key = getKeyAlias(key_alias);
        if (key != null) {
            final String[] data_parts = data.split(ENCRYPTED_STRING_PARTS_SEPARATOR);
            if (data_parts.length == 2) {
                final Cipher cipher = Cipher.getInstance(TRANSFORMATION);
                cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TLEN_LENGTH, Base64.getDecoder().decode(data_parts[0])));
                return new String(cipher.doFinal(Base64.getDecoder().decode(data_parts[1])));
            }
        }
        return null;
    }

    public static byte[] getRandom(int length) {
        final byte[] random_bytes = new byte[length];
        (new SecureRandom()).nextBytes(random_bytes);
        return random_bytes;
    }
}
