package com.twofauth.android.utils;

import android.content.Context;
import android.util.Log;

import com.twofauth.android.Main;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class Files {
    public static void delete(@Nullable final String pathname) {
        if (! Strings.isEmptyOrNull(pathname)) {
            final File file = new File(pathname);
            file.delete();
        }
    }

    public static void copy(@NotNull final File source, @NotNull final File destination) throws Exception {
        try (final FileInputStream in = new FileInputStream(source)) {
             try (final FileOutputStream out = new FileOutputStream(destination)) {
                 final byte[] buffer = new byte[4096];
                 int count;
                 while ((count = in.read(buffer)) != -1) {
                    out.write(buffer, 0, count);
                }
             }
        }
    }

    public static @Nullable File createTempFile(@NotNull final Context context, @NotNull final String prefix, @Nullable final String suffix, @Nullable final File folder) {
        try {
            return File.createTempFile(prefix, suffix, folder == null ? context.getExternalFilesDir("cache") : folder);
        }
        catch (Exception e) {
            Log.e(Main.LOG_TAG_NAME, "Exception while trying to create a temp file", e);
        }
        return null;
    }

    public static @Nullable File createTempFile(@NotNull final Context context, @NotNull final String prefix, @Nullable final String suffix) {
        return createTempFile(context, prefix, suffix, null);
    }

    public static @Nullable File createTempFile(@NotNull final Context context, @NotNull final String prefix) {
        return createTempFile(context, prefix, null);
    }
}
