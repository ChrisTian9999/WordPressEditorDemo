package com.chriszht.editordemo;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.DragEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import com.chriszht.editordemo.photopicker.PhotoPickerFragment;
import com.chriszht.editordemo.utils.AniUtils;
import com.chriszht.editordemo.utils.WPMediaUtils;

import org.wordpress.android.editor.AztecEditorFragment;
import org.wordpress.android.editor.EditorFragmentAbstract;
import org.wordpress.android.editor.EditorFragmentAbstract.EditorDragAndDropListener;
import org.wordpress.android.editor.EditorFragmentAbstract.EditorFragmentListener;
import org.wordpress.android.editor.EditorFragmentAbstract.TrackableEvent;
import org.wordpress.android.editor.EditorMediaUploadListener;
import org.wordpress.android.editor.ImageSettingsDialogFragment;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.helpers.MediaFile;
import org.wordpress.passcodelock.AppLockManager;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditorExampleActivity extends AppCompatActivity
        implements EditorFragmentListener, EditorDragAndDropListener, PhotoPickerFragment.PhotoPickerListener {


    public static final String TITLE_PARAM = "TITLE_PARAM";
    public static final String CONTENT_PARAM = "CONTENT_PARAM";
    public static final String DRAFT_PARAM = "DRAFT_PARAM";
    public static final String TITLE_PLACEHOLDER_PARAM = "TITLE_PLACEHOLDER_PARAM";
    public static final String CONTENT_PLACEHOLDER_PARAM = "CONTENT_PLACEHOLDER_PARAM";

    public static final int ADD_MEDIA_ACTIVITY_REQUEST_CODE = 1111;
    public static final int ADD_MEDIA_FAIL_ACTIVITY_REQUEST_CODE = 1112;
    public static final int ADD_MEDIA_SLOW_NETWORK_REQUEST_CODE = 1113;

    public static final String MEDIA_REMOTE_ID_SAMPLE = "123";

    private static final int SELECT_IMAGE_MENU_POSITION = 0;
    private static final int SELECT_IMAGE_FAIL_MENU_POSITION = 1;
    private static final int SELECT_VIDEO_MENU_POSITION = 2;
    private static final int SELECT_VIDEO_FAIL_MENU_POSITION = 3;
    private static final int SELECT_IMAGE_SLOW_MENU_POSITION = 4;

    private EditorFragmentAbstract mEditorFragment;

    private Map<String, String> mFailedUploads;


    private static final String PHOTO_PICKER_TAG = "photo_picker";

    private View mPhotoPickerContainer;
    private PhotoPickerFragment mPhotoPickerFragment;
    private int mPhotoPickerOrientation = Configuration.ORIENTATION_UNDEFINED;

    private String mMediaCapturePath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_editor);

        mFailedUploads = new HashMap<>();
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if (fragment instanceof EditorFragmentAbstract) {
            mEditorFragment = (EditorFragmentAbstract) fragment;
        }
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = getFragmentManager()
                .findFragmentByTag(ImageSettingsDialogFragment.IMAGE_SETTINGS_DIALOG_TAG);
        if (fragment != null && fragment.isVisible()) {
            ((ImageSettingsDialogFragment) fragment).dismissFragment();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // resize the photo picker if the user rotated the device
        int orientation = newConfig.orientation;
        if (orientation != mPhotoPickerOrientation) {
            resizePhotoPicker();
        }

        // If we're showing the Async promo dialog, we need to notify it to take the new orientation into account
//        PromoDialog fragment = (PromoDialog) getSupportFragmentManager().findFragmentByTag(ASYNC_PROMO_DIALOG_TAG);
//        if (fragment != null) {
//            fragment.redrawForOrientationChange();
//        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        menu.add(0, SELECT_IMAGE_MENU_POSITION, 0, getString(R.string.select_image));
        menu.add(0, SELECT_IMAGE_FAIL_MENU_POSITION, 0, getString(R.string.select_image_fail));
        menu.add(0, SELECT_VIDEO_MENU_POSITION, 0, getString(R.string.select_video));
        menu.add(0, SELECT_VIDEO_FAIL_MENU_POSITION, 0, getString(R.string.select_video_fail));
        menu.add(0, SELECT_IMAGE_SLOW_MENU_POSITION, 0, getString(R.string.select_image_slow_network));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Intent intent = new Intent(Intent.ACTION_PICK);

        switch (item.getItemId()) {
            case SELECT_IMAGE_MENU_POSITION:
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent = Intent.createChooser(intent, getString(R.string.select_image));

                startActivityForResult(intent, ADD_MEDIA_ACTIVITY_REQUEST_CODE);
                return true;
            case SELECT_IMAGE_FAIL_MENU_POSITION:
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent = Intent.createChooser(intent, getString(R.string.select_image_fail));

                startActivityForResult(intent, ADD_MEDIA_FAIL_ACTIVITY_REQUEST_CODE);
                return true;
            case SELECT_VIDEO_MENU_POSITION:
                intent.setType("video/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent = Intent.createChooser(intent, getString(R.string.select_video));

                startActivityForResult(intent, ADD_MEDIA_ACTIVITY_REQUEST_CODE);
                return true;
            case SELECT_VIDEO_FAIL_MENU_POSITION:
                intent.setType("video/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent = Intent.createChooser(intent, getString(R.string.select_video_fail));

                startActivityForResult(intent, ADD_MEDIA_FAIL_ACTIVITY_REQUEST_CODE);
                return true;
            case SELECT_IMAGE_SLOW_MENU_POSITION:
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent = Intent.createChooser(intent, getString(R.string.select_image_slow_network));

                startActivityForResult(intent, ADD_MEDIA_SLOW_NETWORK_REQUEST_CODE);
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data == null) {
            return;
        }

        Uri mediaUri = data.getData();

        MediaFile mediaFile = new MediaFile();
        String mediaId = String.valueOf(System.currentTimeMillis());
        mediaFile.setMediaId(mediaId);
        mediaFile.setVideo(mediaUri.toString().contains("video"));

        switch (requestCode) {
            case ADD_MEDIA_ACTIVITY_REQUEST_CODE:
                mEditorFragment.appendMediaFile(mediaFile, mediaUri.toString(), null);

                if (mEditorFragment instanceof EditorMediaUploadListener) {
                    simulateFileUpload(mediaId, mediaUri.toString());
                }
                break;
            case ADD_MEDIA_FAIL_ACTIVITY_REQUEST_CODE:
                mEditorFragment.appendMediaFile(mediaFile, mediaUri.toString(), null);

                if (mEditorFragment instanceof EditorMediaUploadListener) {
                    simulateFileUploadFail(mediaId, mediaUri.toString());
                }
                break;
            case ADD_MEDIA_SLOW_NETWORK_REQUEST_CODE:
                mEditorFragment.appendMediaFile(mediaFile, mediaUri.toString(), null);

                if (mEditorFragment instanceof EditorMediaUploadListener) {
                    simulateSlowFileUpload(mediaId, mediaUri.toString());
                }
                break;
        }
    }

    @Override
    public void onSettingsClicked() {
        // TODO
        Log.e("TAG", "onSettingsClicked");
    }

    @Override
    public void onAddMediaClicked() {
        if (isPhotoPickerShowing()) {
            hidePhotoPicker();
        } else {
            showPhotoPicker();
        }
    }


    private boolean isPhotoPickerShowing() {
        return mPhotoPickerContainer != null
                && mPhotoPickerContainer.getVisibility() == View.VISIBLE;
    }

    @Override
    public boolean onMediaRetryClicked(String mediaId) {
        if (mFailedUploads.containsKey(mediaId)) {
            simulateFileUpload(mediaId, mFailedUploads.get(mediaId));
            return true;
        }
        return false;
    }

    @Override
    public void onMediaUploadCancelClicked(String mediaId) {

    }

    @Override
    public void onMediaDeleted(String mediaId) {

    }

    @Override
    public void onUndoMediaCheck(String undoedContent) {

    }

    @Override
    public void onFeaturedImageChanged(long mediaId) {

    }

    @Override
    public void onVideoPressInfoRequested(String videoId) {

    }

    @Override
    public String onAuthHeaderRequested(String url) {
        return "";
    }

    @Override
    public void onEditorFragmentInitialized() {
        // arbitrary setup
        mEditorFragment.setFeaturedImageSupported(true);
        mEditorFragment.setDebugModeEnabled(true);

        // get title and content and draft switch
        String title = getIntent().getStringExtra(TITLE_PARAM);
        String content = getIntent().getStringExtra(CONTENT_PARAM);
        boolean isLocalDraft = getIntent().getBooleanExtra(DRAFT_PARAM, true);
        mEditorFragment.setTitle(title);
        mEditorFragment.setContent(content);
        mEditorFragment.setTitlePlaceholder(getIntent().getStringExtra(TITLE_PLACEHOLDER_PARAM));
        mEditorFragment.setContentPlaceholder(getIntent().getStringExtra(CONTENT_PLACEHOLDER_PARAM));
        mEditorFragment.setLocalDraft(isLocalDraft);
    }

    @Override
    public void saveMediaFile(MediaFile mediaFile) {
        // TODO
    }

    @Override
    public void onTrackableEvent(TrackableEvent event) {
        AppLog.d(T.EDITOR, "Trackable event: " + event);
    }

    private void simulateFileUpload(final String mediaId, final String mediaUrl) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    float count = (float) 0.1;
                    while (count < 1.1) {
                        sleep(500);

                        ((EditorMediaUploadListener) mEditorFragment).onMediaUploadProgress(mediaId, count);

                        count += 0.1;
                    }

                    MediaFile mediaFile = new MediaFile();
                    mediaFile.setMediaId(MEDIA_REMOTE_ID_SAMPLE);
                    mediaFile.setFileURL(mediaUrl);

                    ((EditorMediaUploadListener) mEditorFragment).onMediaUploadSucceeded(mediaId, mediaFile);

                    if (mFailedUploads.containsKey(mediaId)) {
                        mFailedUploads.remove(mediaId);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        thread.start();
    }

    private void simulateFileUploadFail(final String mediaId, final String mediaUrl) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    float count = (float) 0.1;
                    while (count < 0.6) {
                        sleep(500);

                        ((EditorMediaUploadListener) mEditorFragment).onMediaUploadProgress(mediaId, count);

                        count += 0.1;
                    }

                    ((EditorMediaUploadListener) mEditorFragment).onMediaUploadFailed(mediaId, EditorFragmentAbstract.MediaType.IMAGE,
                            getString(R.string.tap_to_try_again));

                    mFailedUploads.put(mediaId, mediaUrl);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        thread.start();
    }

    private void simulateSlowFileUpload(final String mediaId, final String mediaUrl) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    sleep(5000);
                    float count = (float) 0.1;
                    while (count < 1.1) {
                        sleep(2000);

                        ((EditorMediaUploadListener) mEditorFragment).onMediaUploadProgress(mediaId, count);

                        count += 0.1;
                    }

                    MediaFile mediaFile = new MediaFile();
                    mediaFile.setMediaId(MEDIA_REMOTE_ID_SAMPLE);
                    mediaFile.setFileURL(mediaUrl);

                    ((EditorMediaUploadListener) mEditorFragment).onMediaUploadSucceeded(mediaId, mediaFile);

                    if (mFailedUploads.containsKey(mediaId)) {
                        mFailedUploads.remove(mediaId);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        thread.start();
    }

    @Override
    public void onMediaDropped(ArrayList<Uri> mediaUri) {
        // TODO
    }

    @Override
    public void onRequestDragAndDropPermissions(DragEvent dragEvent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requestTemporaryPermissions(dragEvent);
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private void requestTemporaryPermissions(DragEvent dragEvent) {
        requestDragAndDropPermissions(dragEvent);
    }


    @Override
    public void onPhotoPickerMediaChosen(@NonNull final List<Uri> uriList) {
        hidePhotoPicker();

        if (WPMediaUtils.shouldAdvertiseImageOptimization(this)) {
            boolean hasSelectedPicture = false;
            for (Uri uri : uriList) {
                if (!MediaUtils.isVideo(uri.toString())) {
                    hasSelectedPicture = true;
                    break;
                }
            }
            if (hasSelectedPicture) {
                WPMediaUtils.advertiseImageOptimization(this,
                        new WPMediaUtils.OnAdvertiseImageOptimizationListener() {
                            @Override
                            public void done() {
                                addMediaList(uriList, false);
                            }
                        });
                return;
            }
        }

        addMediaList(uriList, false);
    }

    @Override
    public void onPhotoPickerIconClicked(@NonNull PhotoPickerFragment.PhotoPickerIcon icon) {
        hidePhotoPicker();
        switch (icon) {
            case ANDROID_CAPTURE_PHOTO:
                launchCamera();
                break;
            case ANDROID_CAPTURE_VIDEO:
                launchVideoCamera();
                break;
            case ANDROID_CHOOSE_PHOTO:
                launchPictureLibrary();
                break;
            case ANDROID_CHOOSE_VIDEO:
                launchVideoLibrary();
                break;
//            case WP_MEDIA:
//                ActivityLauncher.viewMediaPickerForResult(this, mSite);
//                break;
        }
    }

    private void addMediaList(@NonNull List<Uri> uriList, boolean isNew) {
        // TODO: 2017/10/16
        // fetch any shared media first - must be done on the main thread
//        List<Uri> fetchedUriList = fetchMediaList(uriList);
//        mAddMediaListThread = new AddMediaListThread(fetchedUriList, isNew);
//        mAddMediaListThread.start();
    }


    private void launchCamera() {
        WPMediaUtils.launchCamera(this, BuildConfig.APPLICATION_ID,
                new WPMediaUtils.LaunchCameraCallback() {
                    @Override
                    public void onMediaCapturePathReady(String mediaCapturePath) {
                        mMediaCapturePath = mediaCapturePath;
                        AppLockManager.getInstance().setExtendedTimeout();
                    }
                });
    }

    private void launchPictureLibrary() {
        WPMediaUtils.launchPictureLibrary(this);
        AppLockManager.getInstance().setExtendedTimeout();
    }

    private void launchVideoLibrary() {
        WPMediaUtils.launchVideoLibrary(this);
        AppLockManager.getInstance().setExtendedTimeout();
    }

    private void launchVideoCamera() {
        WPMediaUtils.launchVideoCamera(this);
        AppLockManager.getInstance().setExtendedTimeout();
    }

    /*
     * resizes the photo picker based on device orientation - full height in landscape, half
     * height in portrait
     */
    private void resizePhotoPicker() {
        if (mPhotoPickerContainer == null) return;

        if (DisplayUtils.isLandscape(this)) {
            mPhotoPickerOrientation = Configuration.ORIENTATION_LANDSCAPE;
            mPhotoPickerContainer.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
        } else {
            mPhotoPickerOrientation = Configuration.ORIENTATION_PORTRAIT;
            int displayHeight = DisplayUtils.getDisplayPixelHeight(this);
            int containerHeight = (int) (displayHeight * 0.5f);
            mPhotoPickerContainer.getLayoutParams().height = containerHeight;
        }

        if (mPhotoPickerFragment != null) {
            mPhotoPickerFragment.reload();
        }
    }

    /*
     * loads the photo picker fragment, which is hidden until the user taps the media icon
     */
    private void initPhotoPicker() {
        mPhotoPickerContainer = findViewById(R.id.photo_fragment_container);

        // size the picker before creating the fragment to avoid having it load media now
        resizePhotoPicker();

        EnumSet<PhotoPickerFragment.PhotoPickerOption> options =
                EnumSet.of(PhotoPickerFragment.PhotoPickerOption.ALLOW_MULTI_SELECT);
        mPhotoPickerFragment = PhotoPickerFragment.newInstance(this, options);

        getFragmentManager()
                .beginTransaction()
                .add(R.id.photo_fragment_container, mPhotoPickerFragment, PHOTO_PICKER_TAG)
                .commit();
    }

    /*
     * user has requested to show the photo picker
     */
    private void showPhotoPicker() {
        boolean isAlreadyShowing = isPhotoPickerShowing();

        // make sure we initialized the photo picker
        if (mPhotoPickerFragment == null) {
            //SmartToast.reset();
            initPhotoPicker();
        }

        // hide soft keyboard
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        // slide in the photo picker
        if (!isAlreadyShowing) {
            AniUtils.animateBottomBar(mPhotoPickerContainer, true, AniUtils.Duration.MEDIUM);
            mPhotoPickerFragment.refresh();
            mPhotoPickerFragment.setPhotoPickerListener(this);
        }

        // animate in the editor overlay
        showOverlay(true);

        if (mEditorFragment instanceof AztecEditorFragment) {
            ((AztecEditorFragment) mEditorFragment).enableMediaMode(true);
        }

        // let the user know about long-press to multiselect, but only if the user has already granted
        // storage permission - otherwise the toast will appear above the "soft ask" view
        if (!isAlreadyShowing && ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
//            SmartToast.show(this, SmartToast.SmartToastType.MEDIA_LONG_PRESS);
        }
    }

    private void hidePhotoPicker() {
        if (isPhotoPickerShowing()) {
            mPhotoPickerFragment.finishActionMode();
            mPhotoPickerFragment.setPhotoPickerListener(null);
            AniUtils.animateBottomBar(mPhotoPickerContainer, false);
        }

        hideOverlay();

        if (mEditorFragment instanceof AztecEditorFragment) {
            ((AztecEditorFragment) mEditorFragment).enableMediaMode(false);
        }
    }

    /*
     * shows/hides the overlay which appears atop the editor, which effectively disables it
     */
    private void showOverlay(boolean animate) {
        View overlay = findViewById(R.id.view_overlay);
        if (animate) {
            AniUtils.fadeIn(overlay, AniUtils.Duration.MEDIUM);
        } else {
            overlay.setVisibility(View.VISIBLE);
        }
    }

    private void hideOverlay() {
        View overlay = findViewById(R.id.view_overlay);
        overlay.setVisibility(View.GONE);
    }

}
