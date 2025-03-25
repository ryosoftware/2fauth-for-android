package com.twofauth.android.main_activity;

import android.content.Context;
import android.hardware.biometrics.BiometricManager;
import android.os.Build;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.twofauth.android.Constants;
import com.twofauth.android.KeyStoreUtils;
import com.twofauth.android.R;
import com.twofauth.android.UiUtils;

import org.jetbrains.annotations.NotNull;

import javax.crypto.IllegalBlockSizeException;

public class AuthenticWithBiometrics {
    public interface OnBiometricAuthenticationFinishedListener {
        public abstract void onBiometricAuthenticationSucceeded();
        public abstract void onBiometricAuthenticationError(int error_code);
    }

    public static boolean canUseBiometrics(@NotNull final Context context) {
        BiometricManager biometric_manager = (BiometricManager) context.getSystemService(Context.BIOMETRIC_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return biometric_manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS;
        }
        return biometric_manager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS;
    }

    private static boolean linkBiometrics(@NotNull final String key_alias) {
        try {
            KeyStoreUtils.deleteKeyAlias(key_alias);
            KeyStoreUtils.addKeyAlias(key_alias, true);
            return true;
        }
        catch (Exception e) {
            Log.e(Constants.LOG_TAG_NAME, "Exception while trying to regenerate biometric based keystore alias", e);
        }
        return false;
    }

    public static boolean linkBiometrics() {
        return linkBiometrics(Constants.FINGERPRINT_VALIDATED_KEYSTORE_ENTRY_NAME);
    }

    private static boolean areBiometricsLinked(@NotNull final String key_alias) {
        try {
            KeyStoreUtils.encrypt(key_alias, "test");
        }
        catch (KeyPermanentlyInvalidatedException e) {
            return false;
        }
        catch (Exception ignored) {
        }
        return true;
    }

    public static boolean areBiometricsLinked() {
        return areBiometricsLinked(Constants.FINGERPRINT_VALIDATED_KEYSTORE_ENTRY_NAME);
    }

    public static void authenticate(@NotNull final FragmentActivity activity, @NotNull final OnBiometricAuthenticationFinishedListener callback) {
        final BiometricPrompt biometric_prompt = new BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(final int error_code, @NonNull final CharSequence error_string) {
                super.onAuthenticationError(error_code, error_string);
                if (error_code != BiometricPrompt.ERROR_CANCELED) {
                    UiUtils.showToast(activity, R.string.fingerprint_is_not_valid);
                }
                callback.onBiometricAuthenticationError(error_code);
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull final BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                callback.onBiometricAuthenticationSucceeded();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
            }
        });
        biometric_prompt.authenticate(new BiometricPrompt.PromptInfo.Builder().setTitle(activity.getString(R.string.app_name)).setDescription(activity.getString(R.string.use_your_fingerprint_to_unlock_the_app)).setNegativeButtonText(activity.getString(R.string.cancel)).build());
    }
}

