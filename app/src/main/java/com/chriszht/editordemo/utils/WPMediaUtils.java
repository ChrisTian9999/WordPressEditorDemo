package com.chriszht.editordemo.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.view.ViewConfiguration;

import com.chriszht.editordemo.R;

import org.wordpress.android.util.DeviceUtils;
import org.wordpress.android.util.ImageUtils;
import org.wordpress.passcodelock.AppLockManager;

import java.io.File;
import java.io.IOException;

public class WPMediaUtils {

    public interface LaunchCameraCallback {
        void onMediaCapturePathReady(String mediaCapturePath);
    }

    // Max picture size will be 3000px wide. That's the maximum resolution you can set in the current picker.
    public static final int OPTIMIZE_IMAGE_MAX_SIZE = 3000;
    public static final int OPTIMIZE_IMAGE_ENCODER_QUALITY = 85;
    public static final int OPTIMIZE_VIDEO_MAX_WIDTH = 1280;
    public static final int OPTIMIZE_VIDEO_ENCODER_BITRATE_KB = 3000;


    public static void advertiseImageOptimization(final Activity activity,
                                                  final OnAdvertiseImageOptimizationListener listener) {

        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    if (AppPrefs.isImageOptimize()) {
                        // null or image optimization already ON. We should not be here though.
                    } else {
                        AppPrefs.setImageOptimize(true);
                    }
                }

                listener.done();
            }
        };

        DialogInterface.OnCancelListener onCancelListener = new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                listener.done();
            }
        };

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
        builder.setTitle(R.string.image_optimization_promo_title);
        builder.setMessage(R.string.image_optimization_promo_desc);
        builder.setPositiveButton(R.string.turn_on, onClickListener);
        builder.setNegativeButton(R.string.leave_off, onClickListener);
        builder.setOnCancelListener(onCancelListener);
        builder.show();
        // Do not ask again
        AppPrefs.setImageOptimizePromoRequired(false);
    }

    public static Uri fixOrientationIssue(Activity activity, String path, boolean isVideo) {
        if (isVideo) {
            return null;
        }

        String rotatedPath = ImageUtils.rotateImageIfNecessary(activity, path);
        if (rotatedPath != null) {
            return Uri.parse(rotatedPath);
        }

        return null;
    }

    public static boolean isVideoOptimizationAvailable() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
    }

    public static boolean isVideoOptimizationEnabled() {
        return isVideoOptimizationAvailable() && AppPrefs.isVideoOptimize();
    }

    /**
     *
     * Check if we should advertise image optimization feature for the current site.
     *
     * The following condition need to be all true:
     * 1) Image optimization is OFF on the site.
     * 2) Didn't already ask to enable the feature.
     * 3) The user has granted storage access to the app.
     * This is because we don't want to ask so much things to users the first time they try to add a picture to the app.
     *
     * @param act The host activity
     * @return true if we should advertise the feature, false otherwise.
     */
    public static boolean shouldAdvertiseImageOptimization(final Activity act) {
        boolean isPromoRequired = AppPrefs.isImageOptimizePromoRequired();
        if (!isPromoRequired) {
            return false;
        }

        // Check we can access storage before asking for optimizing image
        boolean hasStoreAccess = ContextCompat.checkSelfPermission(
                act, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        if (!hasStoreAccess) {
            return false;
        }

        // Check whether image optimization is already available for the site
        return !AppPrefs.isImageOptimize();
    }

    public interface OnAdvertiseImageOptimizationListener {
        void done();
    }


    private static void showSDCardRequiredDialog(Context context) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setTitle(context.getResources().getText(R.string.sdcard_title));
        dialogBuilder.setMessage(context.getResources().getText(R.string.sdcard_message));
        dialogBuilder.setPositiveButton(context.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        dialogBuilder.setCancelable(true);
        dialogBuilder.create().show();
    }

    public static void launchVideoLibrary(Activity activity) {
        AppLockManager.getInstance().setExtendedTimeout();
        activity.startActivityForResult(prepareVideoLibraryIntent(activity),
                RequestCodes.VIDEO_LIBRARY);
    }

    private static Intent prepareVideoLibraryIntent(Context context) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        return Intent.createChooser(intent, context.getString(R.string.pick_video));
    }

    public static void launchVideoCamera(Activity activity) {
        AppLockManager.getInstance().setExtendedTimeout();
        activity.startActivityForResult(prepareVideoCameraIntent(), RequestCodes.TAKE_VIDEO);
    }

    private static Intent prepareVideoCameraIntent() {
        return new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
    }

    public static void launchPictureLibrary(Activity activity) {
        AppLockManager.getInstance().setExtendedTimeout();
        activity.startActivityForResult(preparePictureLibraryIntent(activity.getString(R.string.pick_photo)),
                RequestCodes.PICTURE_LIBRARY);
    }

    private static Intent preparePictureLibraryIntent(String title) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        return Intent.createChooser(intent, title);
    }

    private static Intent prepareGalleryIntent(String title) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        return Intent.createChooser(intent, title);
    }

    public static void launchCamera(Activity activity, String applicationId, LaunchCameraCallback callback) {
        Intent intent = prepareLaunchCamera(activity, applicationId, callback);
        if (intent != null) {
            AppLockManager.getInstance().setExtendedTimeout();
            activity.startActivityForResult(intent, RequestCodes.TAKE_PHOTO);
        }
    }

    private static Intent prepareLaunchCamera(Context context, String applicationId, LaunchCameraCallback callback) {
        String state = Environment.getExternalStorageState();
        if (!state.equals(Environment.MEDIA_MOUNTED)) {
            showSDCardRequiredDialog(context);
            return null;
        } else {
            try {
                return getLaunchCameraIntent(context, applicationId, callback);
            } catch (IOException e) {
                // No need to write log here
                return null;
            }
        }
    }

    private static Intent getLaunchCameraIntent(Context context, String applicationId, LaunchCameraCallback callback)
            throws IOException {
        File externalStoragePublicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        String mediaCapturePath = externalStoragePublicDirectory + File.separator + "Camera" + File.separator + "wp-" + System
                .currentTimeMillis() + ".jpg";

        // make sure the directory we plan to store the recording in exists
        File directory = new File(mediaCapturePath).getParentFile();
        if (directory == null || (!directory.exists() && !directory.mkdirs())) {
            try {
                throw new IOException("Path to file could not be created: " + mediaCapturePath);
            } catch (IOException e) {
//                AppLog.e(T.MEDIA, e);
                throw e;
            }
        }

        Uri fileUri;
        try {
            fileUri = FileProvider.getUriForFile(context, applicationId + ".provider", new File(mediaCapturePath));
        } catch (IllegalArgumentException e) {
//            AppLog.e(T.MEDIA, "Cannot access the file planned to store the new media", e);
            throw new IOException("Cannot access the file planned to store the new media");
        } catch (NullPointerException e) {
//            AppLog.e(T.MEDIA, "Cannot access the file planned to store the new media - " +
//                    "FileProvider.getUriForFile cannot find a valid provider for the authority: " + applicationId + ".provider", e);
            throw new IOException("Cannot access the file planned to store the new media");
        }

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

        if (callback != null) {
            callback.onMediaCapturePathReady(mediaCapturePath);
        }
        return intent;
    }

    private static Intent makePickOrCaptureIntent(Context context, String applicationId, LaunchCameraCallback callback) {
        Intent pickPhotoIntent = prepareGalleryIntent(context.getString(R.string.capture_or_pick_photo));

        if (DeviceUtils.getInstance().hasCamera(context)) {
            try {
                Intent cameraIntent = getLaunchCameraIntent(context, applicationId, callback);
                pickPhotoIntent.putExtra(
                        Intent.EXTRA_INITIAL_INTENTS,
                        new Intent[]{ cameraIntent });
            } catch (IOException e) {
                // No need to write log here
            }
        }

        return pickPhotoIntent;
    }

    public static int getPlaceholder(String url) {
//        if (MediaUtils.isValidImage(url)) {
//            return R.drawable.ic_gridicons_image;
//        } else if (MediaUtils.isDocument(url)) {
//            return R.drawable.ic_gridicons_page;
//        } else if (MediaUtils.isPowerpoint(url)) {
//            return R.drawable.media_powerpoint;
//        } else if (MediaUtils.isSpreadsheet(url)) {
//            return R.drawable.media_spreadsheet;
//        } else if (MediaUtils.isVideo(url)) {
//            return R.drawable.ic_gridicons_video_camera;
//        } else if (MediaUtils.isAudio(url)) {
//            return R.drawable.ic_gridicons_audio;
//        } else {
//            return 0;
//        }
        return 0;
    }

    /**
     * Returns a poster (thumbnail) URL given a VideoPress video URL
     * @param videoUrl the remote URL to the VideoPress video
     */
    public static String getVideoPressVideoPosterFromURL(String videoUrl) {
        String posterUrl = "";

        if (videoUrl != null) {
            int fileTypeLocation = videoUrl.lastIndexOf(".");
            if (fileTypeLocation > 0) {
                posterUrl = videoUrl.substring(0, fileTypeLocation) + "_std.original.jpg";
            }
        }

        return posterUrl;
    }

    /*
     * passes a newly-created media file to the media scanner service so it's available to
     * the media content provider - use this after capturing or downloading media to ensure
     * that it appears in the stock Gallery app
     */
    public static void scanMediaFile(@NonNull Context context, @NonNull String localMediaPath) {
        MediaScannerConnection.scanFile(context,
                new String[]{localMediaPath}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
//                        AppLog.d(T.MEDIA, "Media scanner finished scanning " + path);
                    }
                });
    }

    /*
     * returns the minimum distance for a fling which determines whether to disable loading
     * thumbnails in the media grid or photo picker - used to conserve memory usage during
     * a reasonably-sized fling
     */
    public static int getFlingDistanceToDisableThumbLoading(@NonNull Context context) {
        return ViewConfiguration.get(context).getScaledMaximumFlingVelocity() / 2;
    }
}
