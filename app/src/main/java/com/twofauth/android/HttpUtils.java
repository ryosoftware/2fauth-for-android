package com.twofauth.android;

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

public class HttpUtils {
    private static HttpURLConnection getConnection(@NotNull final URL url, @NotNull final String method, @Nullable final String authorization) throws Exception {
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
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

    public static HttpURLConnection get(@NotNull final URL url, @Nullable final String authorization) throws Exception {
        final HttpURLConnection connection = getConnection(url, "GET", authorization);
        connection.connect();
        return connection;
    }

    public static HttpURLConnection get(@NotNull final URL url) throws Exception {
        return get(url, null);
    }

    private static void sendConnectionData(@NotNull final HttpURLConnection connection, @NotNull final String data) throws Exception {
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);
        try (OutputStream out = connection.getOutputStream()) {
            final byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
            out.write(bytes, 0, bytes.length);
        }
    }

    public static HttpURLConnection put(@NotNull final URL url, @Nullable final String authorization, @Nullable final String data) throws Exception {
        final HttpURLConnection connection = getConnection(url, "PUT", authorization);
        if (data != null) {
            sendConnectionData(connection, data);
        }
        connection.connect();
        return connection;
    }

    public static HttpURLConnection put(@NotNull final URL url, @Nullable final String data) throws Exception {
        return put(url, null, data);
    }

    public static HttpURLConnection post(@NotNull final URL url, @Nullable final String authorization, @Nullable final String data) throws Exception {
        final HttpURLConnection connection = getConnection(url, "POST", authorization);
        if (data != null) {
            sendConnectionData(connection, data);
        }
        connection.connect();
        return connection;
    }

    public static HttpURLConnection post(@NotNull final URL url, @Nullable final String data) throws Exception {
        return post(url, null, data);
    }

    public static HttpURLConnection send(@NotNull final URL url, @NotNull final String operation, @Nullable final String authorization, @Nullable final String data) throws Exception {
        if (operation.equalsIgnoreCase("PUT")) {
            return put(url, authorization, data);
        }
        else {
            return post(url, authorization, data);
        }
    }

    public static HttpURLConnection send(@NotNull final URL url, @NotNull final String operation, @Nullable final String data) throws Exception {
        return send(url, operation, null, data);
    }

    public static String getContentString(@NotNull final HttpURLConnection connection) throws Exception {
        try (final BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            final StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    public static void saveContent(@NotNull final HttpURLConnection connection, @NotNull final File file) throws Exception {
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
