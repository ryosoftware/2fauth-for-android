package com.twofauth.android.main_activity;

import android.content.Context;
import android.hardware.biometrics.BiometricManager;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.twofauth.android.R;
import com.twofauth.android.UiUtils;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;

public class AuthenticWithBiometrics {
    public interface OnBiometricAuthenticationFinished {
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

    public static void authenticate(@NotNull final FragmentActivity activity, @NotNull final OnBiometricAuthenticationFinished callback) {
        Executor executor = ContextCompat.getMainExecutor(activity);
        final BiometricPrompt biometric_prompt = new BiometricPrompt(activity, executor, new BiometricPrompt.AuthenticationCallback() {
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

