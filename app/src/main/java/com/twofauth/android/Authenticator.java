package com.twofauth.android;

import android.content.SharedPreferences;

import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.FragmentActivity;

import com.twofauth.android.authenticator.AuthenticateWithBiometrics;
import com.twofauth.android.authenticator.AuthenticateWithPin;
import com.twofauth.android.utils.Preferences;
import com.twofauth.android.utils.UI;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Authenticator implements AuthenticateWithBiometrics.OnBiometricAuthenticationFinishedListener, AuthenticateWithPin.OnPinAuthenticationFinishedListener {
    public interface OnAuthenticatorFinishListener {
        public abstract void onAuthenticationSuccess(Object object);
        public abstract void onAuthenticationError(Object object);
    }

    private final FragmentActivity mActivity;

    private OnAuthenticatorFinishListener mListener;
    private Object mObject;

    public Authenticator(@NotNull final FragmentActivity activity) {
        mActivity = activity;
    }

    private void onAuthenticationSuccess() { mListener.onAuthenticationSuccess(mObject); }

    private void onAuthenticationError() { mListener.onAuthenticationError(mObject); }

    @Override
    public void onBiometricAuthenticationSucceeded() { onAuthenticationSuccess(); }

    @Override
    public void onBiometricAuthenticationError(int error_code) {
        if (error_code == BiometricPrompt.ERROR_LOCKOUT) { Preferences.getDefaultSharedPreferences(mActivity).edit().remove(Constants.USE_FINGERPRINT_INSTEAD_OF_PIN_CODE_KEY).apply(); }
        onAuthenticationError();
    }

    @Override
    public void onPinAuthenticationSucceeded() { onAuthenticationSuccess(); }

    @Override
    public void onPinAuthenticationError(boolean cancelled) { onAuthenticationError(); }

    private void authenticate() {
        final SharedPreferences preferences = Preferences.getDefaultSharedPreferences(mActivity);
        if (preferences.contains(Constants.PIN_CODE_KEY)) {
            if (preferences.getBoolean(Constants.USE_FINGERPRINT_INSTEAD_OF_PIN_CODE_KEY, false)) {
                if (AuthenticateWithBiometrics.canUseBiometrics(mActivity) && AuthenticateWithBiometrics.areBiometricsLinked()) {
                    AuthenticateWithBiometrics.authenticate(mActivity, this);
                    return;
                }
                else {
                    UI.showToast(mActivity, R.string.fingerprint_validation_disabled_due_to_biometric_enrollment);
                    preferences.edit().remove(Constants.USE_FINGERPRINT_INSTEAD_OF_PIN_CODE_KEY).apply();
                }
            }
            AuthenticateWithPin.authenticate(mActivity, this, Preferences.getEncryptedString(mActivity, preferences, Constants.PIN_CODE_KEY, null));
            return;
        }
        mListener.onAuthenticationSuccess(mObject);
    }

    public void authenticate(@NotNull final OnAuthenticatorFinishListener listener, @Nullable final Object object) {
        mListener = listener;
        mObject = object;
        authenticate();
    }

    public void authenticate(@NotNull final OnAuthenticatorFinishListener listener) {
        authenticate(listener, null);
    }
}
