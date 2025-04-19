package com.twofauth.android.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.util.Log;

import com.twofauth.android.utils.Bitmaps;
import com.twofauth.android.Main;
import com.twofauth.android.utils.Strings;
import com.twofauth.android.utils.UI;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class TwoFactorIcon extends TableRow {
    public enum BitmapTheme { DARK, LIGHT }

    private static final String DARK_ICON_SUFFIX = "-dark";
    private static final String LIGHT_ICON_SUFFIX = "-light";

    public static final String SOURCE = "source";
    public static final String SOURCE_ID = "source_id";

    protected static final String[] PROJECTION = new String[] {
        ROW_ID,
        SOURCE,
        SOURCE_ID,
    };

    private static final int SOURCE_ORDER = ROW_ID_ORDER + 1;
    private static final int SOURCE_ID_ORDER = SOURCE_ORDER + 1;

    private String mSource;
    private String mSourceId;

    private Bitmap mStoredBitmapThemeDark = null;
    private Bitmap mStoreableBitmapThemeDark = null;
    private boolean mBitmapThemeDarkDirty = false;

    private Bitmap mStoredBitmapThemeLight = null;
    private Bitmap mStoreableBitmapThemeLight = null;
    private boolean mBitmapThemeLightDirty = false;

    private Bitmap mStoredBitmapThemeNone = null;
    private Bitmap mStoreableBitmapThemeNone = null;
    private boolean mBitmapThemeNoneDirty = false;

    public TwoFactorIcon(@NotNull final Cursor cursor) {
        super(TwoFactorIconsHelper.TABLE_NAME, cursor);
        mSource = cursor.getString(SOURCE_ORDER);
        mSourceId = cursor.getString(SOURCE_ID_ORDER);
    }

    public TwoFactorIcon() {
        super(TwoFactorIconsHelper.TABLE_NAME);
        mSource = null;
        mSourceId = null;
    }

    public String getSource() {
        return mSource;
    }

    private void setSource(@Nullable final String source) {
        if (! Strings.equals(mSource, source)) { setDirty(SOURCE, mSource = source); }
    }

    public String getSourceId() {
        return mSourceId;
    }

    private void setSourceId(@Nullable final String source_id) {
        if (! Strings.equals(mSourceId, source_id)) { setDirty(SOURCE_ID, mSourceId = source_id); }
    }

    public void setSourceData(@Nullable final String source, @Nullable final String source_id) {
        setSource(source);
        setSourceId(source_id);
    }

    public void setBitmap(@Nullable final Bitmap bitmap, @Nullable final BitmapTheme theme) {
        if (theme == BitmapTheme.DARK) {
            mStoreableBitmapThemeDark = bitmap;
            mBitmapThemeDarkDirty = true;
        }
        else if (theme == BitmapTheme.LIGHT) {
            mStoreableBitmapThemeLight = bitmap;
            mBitmapThemeLightDirty = true;
        }
        else {
            mStoreableBitmapThemeNone = bitmap;
            mBitmapThemeNoneDirty = true;
        }
        setDirty();
    }

    public void setBitmap(@Nullable final File file, @Nullable final BitmapTheme theme) throws Exception {
        setBitmap(Bitmaps.get(file), theme);
    }

    public void setBitmaps(@Nullable Bitmap not_themed_bitmap, @Nullable final Bitmap dark_themed_bitmap, @Nullable final Bitmap light_themed_bitmap) throws Exception {
        setBitmap(not_themed_bitmap, null);
        setBitmap(dark_themed_bitmap, BitmapTheme.DARK);
        setBitmap(light_themed_bitmap, BitmapTheme.LIGHT);
    }

    public void setBitmaps(@Nullable File not_themed_bitmap_file, @Nullable final File dark_themed_bitmap_file, @Nullable final File light_themed_bitmap_file) throws Exception {
        setBitmap(not_themed_bitmap_file, null);
        setBitmap(dark_themed_bitmap_file, BitmapTheme.DARK);
        setBitmap(light_themed_bitmap_file, BitmapTheme.LIGHT);
    }

    public void unsetThemedBitmaps() {
        setBitmap((Bitmap) null, BitmapTheme.DARK);
        setBitmap((Bitmap) null, BitmapTheme.LIGHT);
    }

    private @Nullable File getBitmapFile(@NotNull final Context context, @Nullable final BitmapTheme theme) {
        String filename = (_id < 0) ? null : String.valueOf(_id);
        if ((filename != null) && (theme != null)) {
            filename += (theme == BitmapTheme.DARK) ? DARK_ICON_SUFFIX : LIGHT_ICON_SUFFIX;
        }
        return (filename == null) ? null : new File(TwoFactorIconsHelper.getBaseFolder(context), filename);
    }

    private @Nullable Bitmap loadBitmap(@NotNull final Context context, @Nullable final BitmapTheme theme) {
        try {
            final File file = getBitmapFile(context, theme);
            if ((file != null) && file.exists()) { return Bitmaps.get(file); }
        }
        catch (Exception e) {
            Log.e(Main.LOG_TAG_NAME, "Exception while reading a bitmap from storage", e);
        }
        return null;
    }

    public @Nullable Bitmap getBitmap(@NotNull final Context context, @Nullable BitmapTheme theme) {
        if (theme == BitmapTheme.DARK) {
            if ((! mBitmapThemeDarkDirty) && (mStoredBitmapThemeDark == null)) { mStoredBitmapThemeDark = loadBitmap(context, theme); }
            return mBitmapThemeDarkDirty ? mStoreableBitmapThemeDark : mStoredBitmapThemeDark;
        }
        else if (theme == BitmapTheme.LIGHT) {
            if ((! mBitmapThemeLightDirty) && (mStoredBitmapThemeLight == null)) { mStoredBitmapThemeLight = loadBitmap(context, theme); }
            return mBitmapThemeLightDirty ? mStoreableBitmapThemeLight : mStoredBitmapThemeLight;
        }
        else {
            if ((! mBitmapThemeNoneDirty) && (mStoredBitmapThemeNone == null)) { mStoredBitmapThemeNone = loadBitmap(context, theme); }
            return mBitmapThemeNoneDirty ? mStoreableBitmapThemeNone : mStoredBitmapThemeNone;
        }
    }

    public @Nullable Bitmap getBitmap(@NotNull final Context context) {
        final Bitmap bitmap = getBitmap(context, UI.isDarkModeActive(context) ? BitmapTheme.DARK : BitmapTheme.LIGHT);
        return (bitmap == null) ? getBitmap(context, null) : bitmap;
    }

    @Override
    public boolean isDirty() {
        return super.isDirty() || mBitmapThemeNoneDirty || mBitmapThemeDarkDirty || mBitmapThemeLightDirty;
    }

    protected void setDatabaseValues(@NotNull final ContentValues values) {
        if (values.size() == 0) {
            values.put(SOURCE, mSource);
            values.put(SOURCE_ID, mSourceId);
        }
    }

    @Override
    protected void onDataSaved(@NotNull final Context context) throws Exception {
        if (mBitmapThemeDarkDirty) {
            Bitmaps.save(mStoreableBitmapThemeDark, getBitmapFile(context, BitmapTheme.DARK));
            mStoredBitmapThemeDark = mStoreableBitmapThemeDark;
            mBitmapThemeDarkDirty = false;
            mStoreableBitmapThemeDark = null;
        }
        if (mBitmapThemeLightDirty) {
            Bitmaps.save(mStoreableBitmapThemeLight, getBitmapFile(context, BitmapTheme.LIGHT));
            mStoredBitmapThemeLight = mStoreableBitmapThemeLight;
            mBitmapThemeLightDirty = false;
            mStoreableBitmapThemeLight = null;
        }
        if (mBitmapThemeNoneDirty) {
            Bitmaps.save(mStoreableBitmapThemeNone, getBitmapFile(context, null));
            mStoredBitmapThemeNone = mStoreableBitmapThemeNone;
            mBitmapThemeNoneDirty = false;
            mStoreableBitmapThemeNone = null;
        }
        super.onDataSaved(context);
    }

    @Override
    protected void onDataDeleted(@NotNull final SQLiteDatabase database, @NotNull final Context context) throws Exception {
        for (final BitmapTheme theme : new BitmapTheme[] { null, BitmapTheme.DARK, BitmapTheme.LIGHT }) {
            final File file = getBitmapFile(context, theme);
            if (file != null) { file.delete(); }
        }
        super.onDataDeleted(database, context);
    }
}
