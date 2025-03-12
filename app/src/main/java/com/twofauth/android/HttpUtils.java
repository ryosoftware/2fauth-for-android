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

public class HttpUtils {
    public static HttpURLConnection get(@NotNull final URL url, @Nullable final String authorization) throws Exception {
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setDefaultUseCaches(false);
        connection.setUseCaches(false);
        connection.setInstanceFollowRedirects(true);
        if (authorization != null) {
            connection.setRequestProperty("Authorization", authorization);
        }
        connection.connect();
        return connection;
    }

    public static HttpURLConnection get(@NotNull final URL url) throws Exception {
        return get(url, null);
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
