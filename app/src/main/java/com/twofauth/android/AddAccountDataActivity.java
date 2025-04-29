package com.twofauth.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.Manifest.permission;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;

import androidx.activity.result.ActivityResult;
import androidx.core.content.FileProvider;

import com.twofauth.android.database.TwoFactorAccount;
import com.twofauth.android.api_tasks.DecodeQR;
import com.twofauth.android.api_tasks.DecodeQR.OnQRDecodedListener;
import com.twofauth.android.api_tasks.LoadServerIdentities;
import com.twofauth.android.api_tasks.LoadServerIdentities.OnServerIdentitiesLoadedListener;
import com.twofauth.android.database.TwoFactorServerIdentity;
import com.twofauth.android.utils.Bitmaps;
import com.twofauth.android.utils.Files;
import com.twofauth.android.utils.JSON;
import com.twofauth.android.utils.Lists;
import com.twofauth.android.utils.Threads;
import com.twofauth.android.utils.UI;
import com.twofauth.android.utils.Vibrator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AddAccountDataActivity extends BaseActivity implements OnServerIdentitiesLoadedListener, OnQRDecodedListener, OnClickListener {
    private static final int REQUEST_FOR_CAMERA_PERMISSION_THEN_OPEN_IMAGE_CHOOSER = 101;

    private static final String CAMERA_IMAGE_TEMP_FILE_PREFIX = "camera-";

    private static class BitmapUtils {
        private static @Nullable Bitmap getBitmapFromUri(@NotNull final Context context, @Nullable final Uri uri) {
            try {
                return uri == null ? null : Bitmaps.get(context, uri);
            }
            catch (Exception e) {
                Log.e(Main.LOG_TAG_NAME, "Exception while trying to load a bitmap from a Uri", e);
            }
            return null;
        }

        private static @Nullable Bitmap getBitmapFromFile(@NotNull final Context context, @Nullable final File file) {
            try {
                return file == null ? null : Bitmaps.get(file);
            }
            catch (Exception e) {
                Log.e(Main.LOG_TAG_NAME, "Exception while trying to load a bitmap from a File", e);
            }
            return null;
        }
    }

    private static class TwoFactorServerIdentitiesUtils {
        public static @Nullable List<String> getNames(@Nullable final List<TwoFactorServerIdentity> server_identities) {
            if ((server_identities != null) && (!server_identities.isEmpty())) {
                List<String> names = new ArrayList<String>();
                for (final TwoFactorServerIdentity server_identity : server_identities) {
                    names.add(server_identity.getTitle());
                }
                return names;
            }
            return null;
        }
    }

    private final List<TwoFactorServerIdentity> mServerIdentities = new ArrayList<TwoFactorServerIdentity>();

    private boolean mChoosingImage;

    private File mCameraTempFile;

    private Spinner mServerIdentitySpinner;

    private Thread mServerIdentitiesLoader = null;
    private Thread mDecoder = null;

    @Override
    protected void onCreate(@Nullable final Bundle saved_instance_state) {
        super.onCreate(saved_instance_state);
        mCameraTempFile = Files.createTempFile(this, CAMERA_IMAGE_TEMP_FILE_PREFIX);
        setContentView(R.layout.add_account_data_activity);
        mServerIdentitySpinner = (Spinner) findViewById(R.id.server_identity_selector);
        findViewById(R.id.use_qr).setOnClickListener(this);
        findViewById(R.id.use_form).setOnClickListener(this);
        setResult(Activity.RESULT_CANCELED);
        (mServerIdentitiesLoader = LoadServerIdentities.getBackgroundTask(this)).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Threads.interrupt(new Thread[] {mServerIdentitiesLoader, mDecoder });
        if (mCameraTempFile != null) { mCameraTempFile.delete(); }
    }

    @Override
    protected void processOnBackPressed() {
        finish();
    }

    // Function that is triggered when data needed to process this activity has been loaded
    // We need at least one server identity to add an account
    // We only display the server identity selector if more than one identity has been added

    @Override
    public void onServerIdentitiesLoaded(@Nullable final List<TwoFactorServerIdentity> server_identities) {
        mServerIdentitiesLoader = null;
        if (Lists.isEmptyOrNull(server_identities)) {
            UI.showToast(this, R.string.cannot_process_request_due_to_an_internal_error);
            finish();
        }
        else {
            Lists.setItems(mServerIdentities, server_identities);
            final ArrayAdapter<String> servers_adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, TwoFactorServerIdentitiesUtils.getNames(mServerIdentities));
            servers_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mServerIdentitySpinner.setAdapter(servers_adapter);
            mServerIdentitySpinner.setSelection(0);
            findViewById(R.id.server_identity_layout).setVisibility(mServerIdentities.size() > 1 ? View.VISIBLE : View.GONE);
        }
    }

    // Actions related with the QR decode

    private void decodeQR(@NotNull final Bitmap bitmap) {
        (mDecoder = DecodeQR.getBackgroundTask(mServerIdentities.get(mServerIdentitySpinner.getSelectedItemPosition()), bitmap, this)).start();
    }

    @Override
    public void onQRDecoded(@Nullable final TwoFactorAccount account) {
        if (! isFinishedOrFinishing()) {
            if (account == null) {
                UI.showToast(this, R.string.cannot_decode_selected_image);
                findViewById(R.id.qr_layout).setVisibility(View.GONE);
            }
            else {
                try {
                    final Bundle bundle = new Bundle();
                    account.setServerIdentity(mServerIdentities.get(mServerIdentitySpinner.getSelectedItemPosition()));
                    bundle.putString(EditAccountDataActivity.EXTRA_ACCOUNT_DATA, JSON.toString(account.toJSONObject()));
                    mChoosingImage = false;
                    addAccountFromQR(bundle);
                }
                catch (JSONException e) {
                    UI.showToast(this, R.string.cannot_process_request_due_to_an_internal_error);
                    Log.e(Main.LOG_TAG_NAME, "Exception while trying to convert an account JSON to String", e);
                }
            }
        }
    }

    private void addAccountFromQR(@NotNull final Bundle bundle) {
        startActivityForResult(EditAccountDataActivity.class, bundle);
    }

    // User interacion

    @Override
    public void onClick(@NotNull final View view) {
        Vibrator.vibrate(this, Constants.NORMAL_HAPTIC_FEEDBACK);
        mChoosingImage = (view.getId() == R.id.use_qr);
        if (mChoosingImage) { openImagePicker(true); }
        else { startActivityForResult(EditAccountDataActivity.class); }
    }

    private void openImagePicker(final boolean check_for_camera_permission) {
        try {
            final boolean has_camera_permission = hasPermission(permission.CAMERA);
            if (check_for_camera_permission && (! has_camera_permission) && (mCameraTempFile != null)) {
                requestPermission(permission.CAMERA, REQUEST_FOR_CAMERA_PERMISSION_THEN_OPEN_IMAGE_CHOOSER);
            }
            else {
                final Intent gallery_intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI), chooser = Intent.createChooser(gallery_intent, getString(R.string.select_a_image));
                if (has_camera_permission && (mCameraTempFile != null)) { chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { new Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(this, getPackageName() + ".provider", mCameraTempFile)) }); }
                startActivityFromIntent(chooser);
            }
        }
        catch (Exception e) {
            Log.e(Main.LOG_TAG_NAME, "Exception while opening an image chooser", e);
        }
    }

    // We show the open image picker dialog when the user allows (or not) for the camera permission

    @Override
    public void onRequestPermissionsResult(int request_code, @NotNull String[] permissions, @NotNull int[] grant_results) {
        super.onRequestPermissionsResult(request_code, permissions, grant_results);
        if (request_code == REQUEST_FOR_CAMERA_PERMISSION_THEN_OPEN_IMAGE_CHOOSER) { openImagePicker(false); }
    }

    // Function that is triggered when user selects a image
    // Also when the edit account activity has been finished

    @Override
    public void onActivityResult(@NotNull final ActivityResult result) {
        if (result.getResultCode() == RESULT_OK) {
            if (mChoosingImage) {
                if (result.getData() != null) {
                    final Uri selected_image_uri = result.getData().getData();
                    final Bitmap bitmap = (selected_image_uri == null) ? BitmapUtils.getBitmapFromFile(this, mCameraTempFile) : BitmapUtils.getBitmapFromUri(this, selected_image_uri);
                    if (bitmap == null) { UI.showToast(this, R.string.cannot_load_selected_image); return; }
                    ((ImageView) findViewById(R.id.qr)).setImageBitmap(bitmap);
                    findViewById(R.id.qr_layout).setVisibility(View.VISIBLE);
                    decodeQR(bitmap);
                }
            }
            else {
                setResult(Activity.RESULT_OK);
                finish();
            }
        }
        else if (!mChoosingImage) {
            finish();
        }
    }
}
