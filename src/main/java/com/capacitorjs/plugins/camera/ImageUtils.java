package com.capacitorjs.plugins.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import androidx.exifinterface.media.ExifInterface;
import com.getcapacitor.Logger;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtils {
    public static Bitmap resize(Bitmap bitmap, final int desiredMaxWidth, final int desiredMaxHeight) {
        return ImageUtils.resizePreservingAspectRatio(bitmap, desiredMaxWidth, desiredMaxHeight);
    }

    private static Bitmap resizePreservingAspectRatio(Bitmap bitmap, final int desiredMaxWidth, final int desiredMaxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int maxHeight = desiredMaxHeight == 0 ? height : desiredMaxHeight;
        int maxWidth = desiredMaxWidth == 0 ? width : desiredMaxWidth;
        float newWidth = Math.min(width, maxWidth);
        float newHeight = (height * newWidth) / width;
        if (newHeight > maxHeight) {
            newWidth = (width * maxHeight) / height;
            newHeight = maxHeight;
        }
        return Bitmap.createScaledBitmap(bitmap, Math.round(newWidth), Math.round(newHeight), false);
    }

    private static Bitmap transform(final Bitmap bitmap, final Matrix matrix) {
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static Bitmap correctOrientation(final Context c, final Bitmap bitmap, final Uri imageUri, ExifWrapper exif) throws IOException {
        final int orientation = getOrientation(c, imageUri);
        if (orientation != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(orientation);
            exif.resetOrientation();
            return transform(bitmap, matrix);
        } else {
            return bitmap;
        }
    }

    private static int getOrientation(final Context c, final Uri imageUri) throws IOException {
        int result = 0;
        try (InputStream iStream = c.getContentResolver().openInputStream(imageUri)) {
            final ExifInterface exifInterface = new ExifInterface(iStream);
            final int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                result = 90;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                result = 180;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                result = 270;
            }
        }
        return result;
    }

    public static ExifWrapper getExifData(final Context c, final Bitmap bitmap, final Uri imageUri) {
        InputStream stream = null;
        try {
            stream = c.getContentResolver().openInputStream(imageUri);
            final ExifInterface exifInterface = new ExifInterface(stream);
            return new ExifWrapper(exifInterface);
        } catch (IOException ex) {
            Logger.error("Error loading exif data from image", ex);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignored) {}
            }
        }
        return new ExifWrapper(null);
    }
}