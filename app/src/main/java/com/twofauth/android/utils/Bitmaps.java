package com.twofauth.android.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.util.Base64;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;

public class Bitmaps {
    private static boolean hasAlphaChannel(@NotNull final Bitmap bitmap) {
        final Bitmap.Config config = bitmap.getConfig();
        return ((config == Bitmap.Config.ARGB_8888) || (config == Bitmap.Config.ARGB_4444));
    }

    private static @Nullable Bitmap cropToSquare(@NotNull final Bitmap bitmap) {
        final int width = bitmap.getWidth(), height = bitmap.getHeight(), new_size = Math.min(width, height);
        return Bitmap.createBitmap(bitmap, (width - new_size) / 2, (height - new_size) / 2, new_size, new_size).copy(Bitmap.Config.ARGB_8888, true);
    }

    public static @Nullable Bitmap cropToSquare(@NotNull final Context context, @NotNull final Uri uri, @Nullable final File file) throws Exception {
        final Bitmap bitmap = cropToSquare(get(context, uri));
        if (file != null) {
            save(bitmap, file);
        }
        return bitmap;
    }

    public static void save(@Nullable final Bitmap bitmap, @NotNull final File file) throws Exception {
        if (bitmap != null) {
            try (final FileOutputStream out = new FileOutputStream(file)) {
                bitmap.compress(hasAlphaChannel(bitmap) ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG, 100, out);
            }
        }
        else {
            file.delete();
        }
    }

    public static @Nullable byte[] bytes(@Nullable final Bitmap bitmap) throws Exception {
        if (bitmap != null) {
            try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                bitmap.compress(hasAlphaChannel(bitmap) ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG, 100, out);
                return out.toByteArray();
            }
        }
        return null;
    }

    public static @Nullable Bitmap get(@NotNull final Context context, @Nullable final Uri uri) throws Exception {
        return uri == null ? null : ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.getContentResolver(), uri));
    }

    public static @Nullable Bitmap get(@Nullable final File file) throws Exception {
        return file == null ? null : BitmapFactory.decodeFile(file.getPath());
    }

    public static @Nullable Bitmap get(@NotNull final HttpURLConnection connection) throws Exception {
        final String content_type = connection.getContentType();
        if (content_type.startsWith("image/")) {
            try (final InputStream in = connection.getInputStream()) {
                return BitmapFactory.decodeStream(in);
            }
        }
        return null;
    }

    public static @Nullable Bitmap get(@Nullable final byte[] bytes) throws Exception {
        return bytes == null ? null : BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}
