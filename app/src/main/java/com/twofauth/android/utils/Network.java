package com.twofauth.android.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

import org.jetbrains.annotations.NotNull;

public class Network {
    public static boolean isWifiConnected(@NotNull final Context context) {
        final ConnectivityManager connectivity_manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity_manager != null) {
            final android.net.Network network = connectivity_manager.getActiveNetwork();
            if (network != null) {
                final NetworkCapabilities network_capabilities = connectivity_manager.getNetworkCapabilities(network);
                if (network_capabilities != null) { return network_capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI); }
            }
        }
        return false;
    }
}
