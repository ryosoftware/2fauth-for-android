package com.twofauth.android.authenticator;

import android.content.Context;
import android.hardware.biometrics.BiometricManager;
import android.os.Build;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.util.Log;

import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.twofauth.android.Constants;
import com.twofauth.android.utils.KeyStore;
import com.twofauth.android.Main;
import com.twofauth.android.R;
import com.twofauth.android.utils.UI;

import org.jetbrains.annotations.NotNull;

public class AuthenticateWithBiometrics {
    public interface OnBiometricAuthenticationFinishedListener {
        public abstract void onBiometricAuthenticationSucceeded();
        public abstract void onBiometricAuthenticationError(int error_code);
    }

    public static boolean canUseBiometrics(@NotNull final Context context) {
        final BiometricManager biometric_manager = (BiometricManager) context.getSystemService(Context.BIOMETRIC_SERVICE);
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ? (biometric_manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) : (biometric_manager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS);
    }

    private static boolean linkBiometrics(@NotNull final String key_alias) {
        try {
            KeyStore.deleteKeyAlias(key_alias);
            KeyStore.addKeyAlias(key_alias, true);
            return true;
        }
        catch (Exception e) {
            Log.e(Main.LOG_TAG_NAME, "Exception while trying to regenerate biometric based keystore alias", e);
        }
        return false;
    }

    public static boolean linkBiometrics() {
        return linkBiometrics(Constants.FINGERPRINT_VALIDATED_KEYSTORE_ENTRY_NAME);
    }

    private static boolean areBiometricsLinked(@NotNull final String key_alias) {
        try {
            KeyStore.encrypt(key_alias, "test");
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
            public void onAuthenticationError(final int error_code, @NotNull final CharSequence error_string) {
                super.onAuthenticationError(error_code, error_string);
                if (error_code != BiometricPrompt.ERROR_CANCELED) {
                    UI.showToast(activity, R.string.fingerprint_is_not_valid);
                }
                callback.onBiometricAuthenticationError(error_code);
            }

            @Override
            public void onAuthenticationSucceeded(@NotNull final BiometricPrompt.AuthenticationResult result) {
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

