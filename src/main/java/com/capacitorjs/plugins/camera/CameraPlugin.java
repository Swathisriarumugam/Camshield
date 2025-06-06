package com.capacitorjs.plugins.camera;

import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import com.getcapacitor.JSObject;

@CapacitorPlugin(
    name = "Camera",
    permissions = {
        @Permission(strings = {Manifest.permission.CAMERA}, alias = CameraPlugin.CAMERA),
        @Permission(strings = {}, alias = CameraPlugin.PHOTOS),
        @Permission(strings = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, alias = CameraPlugin.SAVE_GALLERY)
    }
)
public class CameraPlugin extends Plugin {
    private String imageFileSavePath;
    private Uri imageFileUri;
    private CameraSettings settings = new CameraSettings();

    @PluginMethod
    public void getPhoto(PluginCall call) {
        settings = getSettings(call);
        if (!checkCameraPermissions(call)) return;
        if (settings.getSource() == CameraSource.CAMERA) openCamera(call);
        else openPhotos(call);
    }

    private boolean checkCameraPermissions(PluginCall call) {
        if (getPermissionState(CAMERA) != PermissionState.GRANTED) {
            requestPermissionForAlias(CAMERA, call, "cameraPermissionsCallback");
            return false;
        }
        return true;
    }

    @PermissionCallback
    private void cameraPermissionsCallback(PluginCall call) {
        if (getPermissionState(CAMERA) == PermissionState.GRANTED) doShow(call);
        else call.reject("Permission denied");
    }

    private void doShow(PluginCall call) {
        if (settings.getSource() == CameraSource.CAMERA) openCamera(call);
        else openPhotos(call);
    }

    private void openCamera(PluginCall call) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            File photoFile = CameraUtils.createImageFile(getActivity());
            imageFileSavePath = photoFile.getAbsolutePath();
            imageFileUri = FileProvider.getUriForFile(getActivity(), getAppId() + ".fileprovider", photoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageFileUri);
            startActivityForResult(call, intent, "processCameraImage");
        } catch (Exception e) {
            call.reject("Image file save error", e);
        }
    }

    private void openPhotos(PluginCall call) {
        ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerActivityResultLauncher(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) processPickedImage(uri, call);
                else call.reject("User cancelled");
            });
        pickMedia.launch(new PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build());
    }

    private void processPickedImage(Uri uri, PluginCall call) {
        try (InputStream stream = getContext().getContentResolver().openInputStream(uri)) {
            Bitmap bitmap = BitmapFactory.decodeStream(stream);
            if (bitmap == null) call.reject("Unable to process bitmap");
            else returnResult(call, bitmap, uri);
        } catch (IOException e) {
            call.reject("Image processing error", e);
        }
    }

    @ActivityCallback
    public void processCameraImage(PluginCall call, ActivityResult result) {
        if (imageFileSavePath == null) {
            call.reject("No file found");
            return;
        }
        Bitmap bitmap = BitmapFactory.decodeFile(imageFileSavePath);
        if (bitmap == null) call.reject("User cancelled");
        else returnResult(call, bitmap, Uri.fromFile(new File(imageFileSavePath)));
    }

    private CameraSettings getSettings(PluginCall call) {
        CameraSettings s = new CameraSettings();
        s.setResultType(CameraResultType.valueOf(call.getString("resultType", "BASE64").toUpperCase()));
        s.setSource(CameraSource.valueOf(call.getString("source", "PROMPT").toUpperCase()));
        s.setSaveToGallery(call.getBoolean("saveToGallery", true));
        return s;
    }

    private void returnResult(PluginCall call, Bitmap bitmap, Uri uri) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        String base64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
        JSObject ret = new JSObject();
        ret.put("base64String", base64);
        ret.put("format", "jpeg");
        call.resolve(ret);
    }
}