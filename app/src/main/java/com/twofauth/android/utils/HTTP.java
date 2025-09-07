package com.twofauth.android.utils;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.preference.CheckBoxPreference;

import com.twofauth.android.Constants;
import com.twofauth.android.R;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class HTTP {
    public static final String CONTENT_TYPE_APPLICATION_OCTET_STREAM = "application/octet-stream";
    public static final String CONTENT_TYPE_MULTIPART_FORM_DATA = "multipart/form-data";

    private static SSLContext mSSLContext = null;
    private static SSLContext initializeInsecureContext() {
        if (mSSLContext == null) {
            SSLContext ssl_context = null;
            try {
                @SuppressLint("CustomX509TrustManager")
                TrustManager[] trust_all_certs = new TrustManager[] {
                    new X509TrustManager() {
                        @SuppressLint("TrustAllX509TrustManager")
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                        @SuppressLint("TrustAllX509TrustManager")
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    }
                };
                ssl_context = SSLContext.getInstance("TLS");
                ssl_context.init(null, trust_all_certs, new SecureRandom());
            }
            catch (Exception e) {
                ssl_context = null;
            }
            finally {
                mSSLContext = ssl_context;
            }
        }
        return mSSLContext;
    }

    private static void initializeConnectionInsecureContextIfAllowed(@NotNull final HttpURLConnection connection) {
        if (connection instanceof HttpsURLConnection) {
            final SSLContext ssl_context = initializeInsecureContext();
            if (ssl_context != null) {
                ((HttpsURLConnection) connection).setSSLSocketFactory(ssl_context.getSocketFactory());
                ((HttpsURLConnection) connection).setHostnameVerifier((hostname, session) -> true);
            }
        }
    }

    private static @NotNull HttpURLConnection getConnection(@NotNull final URL url, @NotNull final String method, @Nullable final String authorization, final boolean allow_insecure_certificates) throws Exception {
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        if (allow_insecure_certificates) { initializeConnectionInsecureContextIfAllowed(connection); }
        connection.setRequestMethod(method);
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setDefaultUseCaches(false);
        connection.setUseCaches(false);
        connection.setInstanceFollowRedirects(true);
        if (authorization != null) {
            connection.setRequestProperty("Authorization", authorization);
        }
        return connection;
    }

    public static @NotNull HttpURLConnection get(@NotNull final URL url, @Nullable final String authorization, final boolean allow_insecure_certificates) throws Exception {
        final HttpURLConnection connection = getConnection(url, "GET", authorization, allow_insecure_certificates);
        connection.connect();
        return connection;
    }

    public static @NotNull HttpURLConnection get(@NotNull final URL url, final boolean allow_insecure_certificates) throws Exception {
        return get(url, null, allow_insecure_certificates);
    }

    private static @NotNull void sendConnectionData(@NotNull final HttpURLConnection connection, @NotNull final String data) throws Exception {
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);
        try (OutputStream out = connection.getOutputStream()) {
            final byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
            out.write(bytes, 0, bytes.length);
            out.flush();
        }
    }

    private static void sendConnectionData(@NotNull final HttpURLConnection connection, @NotNull final byte[] data, @NotNull final String name, @Nullable final String content_type) throws Exception {
        if (CONTENT_TYPE_APPLICATION_OCTET_STREAM.equals(content_type)) {
            connection.setRequestProperty("Content-Type", CONTENT_TYPE_APPLICATION_OCTET_STREAM);
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
            try (OutputStream out = connection.getOutputStream()) {
                out.write(data, 0, data.length);
                out.flush();
            }
        }
        else {
            final String boundary = "----boundary";
            connection.setRequestProperty("Content-Type", CONTENT_TYPE_MULTIPART_FORM_DATA + "; boundary=" + boundary);
            connection.setRequestProperty("Accept", "application/json");
            final StringBuilder body_builder = new StringBuilder();
            body_builder.append("--").append(boundary).append("\r\n");
            body_builder.append(String.format("Content-Disposition: form-data; name=\"%s\"; filename=\"%s.bin\"\r\n", name, name));
            body_builder.append("Content-Type: application/octet-stream\r\n\r\n");
            try (OutputStream out = connection.getOutputStream()) {
                out.write(body_builder.toString().getBytes());
                out.write(data);
                out.write(("\r\n--" + boundary + "--\r\n").getBytes());
                out.flush();
            }
        }
    }

    public static @NotNull HttpURLConnection put(@NotNull final URL url, @Nullable final String authorization, @Nullable final String data, final boolean allow_insecure_certificates) throws Exception {
        final HttpURLConnection connection = getConnection(url, "PUT", authorization, allow_insecure_certificates);
        if (data != null) {
            sendConnectionData(connection, data);
        }
        connection.connect();
        return connection;
    }

    public static @NotNull HttpURLConnection put(@NotNull final URL url, @Nullable final String data, final boolean allow_insecure_certificates) throws Exception {
        return put(url, (String) null, data, allow_insecure_certificates);
    }

    public static @NotNull HttpURLConnection put(@NotNull final URL url, @Nullable final String authorization, @Nullable final byte[] data, @NotNull final String name, @Nullable final String content_type, final boolean allow_insecure_certificates) throws Exception {
        final HttpURLConnection connection = getConnection(url, "PUT", authorization, allow_insecure_certificates);
        if (data != null) {
            sendConnectionData(connection, data, name, content_type);
        }
        connection.connect();
        return connection;
    }

    public static @NotNull HttpURLConnection put(@NotNull final URL url, @Nullable byte[] data, @NotNull final String name, @Nullable final String content_type, final boolean allow_insecure_certificates) throws Exception {
        return put(url, null, data, name, content_type, allow_insecure_certificates);
    }

    public static @NotNull HttpURLConnection post(@NotNull final URL url, @Nullable final String authorization, @Nullable final String data, final boolean allow_insecure_certificates) throws Exception {
        final HttpURLConnection connection = getConnection(url, "POST", authorization, allow_insecure_certificates);
        if (data != null) {
            sendConnectionData(connection, data);
        }
        connection.connect();
        return connection;
    }

    public static @NotNull HttpURLConnection post(@NotNull final URL url, @Nullable final String data, final boolean allow_insecure_certificates) throws Exception {
        return post(url, (String) null, data, allow_insecure_certificates);
    }

    public static @NotNull HttpURLConnection post(@NotNull final URL url, @Nullable final String authorization, @Nullable final byte[] data, @NotNull final String name, @Nullable final String content_type, final boolean allow_insecure_certificates) throws Exception {
        final HttpURLConnection connection = getConnection(url, "POST", authorization, allow_insecure_certificates);
        if (data != null) {
            sendConnectionData(connection, data, name, content_type);
        }
        connection.connect();
        return connection;
    }

    public static @NotNull HttpURLConnection post(@NotNull final URL url, @Nullable byte[] data, @NotNull final String name, @Nullable final String content_type, final boolean allow_insecure_certificates) throws Exception {
        return post(url, null, data, name, content_type, allow_insecure_certificates);
    }

    public static @NotNull HttpURLConnection delete(@NotNull final URL url, @Nullable final String authorization, final boolean allow_insecure_certificates) throws Exception {
        final HttpURLConnection connection = getConnection(url, "DELETE", authorization, allow_insecure_certificates);
        connection.connect();
        return connection;
    }

    public static HttpURLConnection delete(@NotNull final URL url, final boolean allow_insecure_certificates) throws Exception {
        return delete(url, null, allow_insecure_certificates);
    }

    private static @NotNull String getConnectionString(@NotNull final HttpURLConnection connection, final boolean is_error) throws Exception {
        try (final BufferedReader in = new BufferedReader(new InputStreamReader(is_error ? connection.getErrorStream() : connection.getInputStream()))) {
            final StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) { response.append(line); }
            return response.toString();
        }
    }

    public static @NotNull String getContentString(@NotNull final HttpURLConnection connection) throws Exception { return getConnectionString(connection, false); }

    public static @NotNull String getErrorString(@NotNull final HttpURLConnection connection) throws Exception { return getConnectionString(connection, true); }

    public static @NotNull void saveContent(@NotNull final HttpURLConnection connection, @NotNull final File file) throws Exception {
        int count;
        try (final InputStream in = connection.getInputStream()) {
            file.getParentFile().mkdirs();
            try (final OutputStream out = new FileOutputStream(file)) {
                final byte[] bytes = new byte[4096];
                while ((count = in.read(bytes)) >= 0) {
                    out.write(bytes, 0, count);
                }
            }
        }
    }
}
