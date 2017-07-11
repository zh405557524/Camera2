/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ex.camera2.portability;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.params.MeteringRectangle;
import android.location.Location;
import android.util.Range;

import com.android.ex.camera2.portability.CameraCapabilities.FlashMode;
import com.android.ex.camera2.portability.debug.Log;
import com.android.ex.camera2.utils.Camera2RequestSettingsSet;

import java.util.List;
import java.util.Objects;

import static android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON;
import static android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH;
import static android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH;
import static android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE;
import static android.hardware.camera2.CameraMetadata.CONTROL_AF_MODE_AUTO;
import static android.hardware.camera2.CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE;
import static android.hardware.camera2.CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO;
import static android.hardware.camera2.CameraMetadata.CONTROL_AF_MODE_EDOF;
import static android.hardware.camera2.CameraMetadata.CONTROL_AF_MODE_MACRO;
import static android.hardware.camera2.CameraMetadata.CONTROL_AF_MODE_OFF;
import static android.hardware.camera2.CameraMetadata.CONTROL_AWB_MODE_AUTO;
import static android.hardware.camera2.CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT;
import static android.hardware.camera2.CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT;
import static android.hardware.camera2.CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT;
import static android.hardware.camera2.CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT;
import static android.hardware.camera2.CameraMetadata.CONTROL_AWB_MODE_SHADE;
import static android.hardware.camera2.CameraMetadata.CONTROL_AWB_MODE_TWILIGHT;
import static android.hardware.camera2.CameraMetadata.CONTROL_AWB_MODE_WARM_FLUORESCENT;
import static android.hardware.camera2.CameraMetadata.CONTROL_SCENE_MODE_ACTION;
import static android.hardware.camera2.CameraMetadata.CONTROL_SCENE_MODE_BARCODE;
import static android.hardware.camera2.CameraMetadata.CONTROL_SCENE_MODE_BEACH;
import static android.hardware.camera2.CameraMetadata.CONTROL_SCENE_MODE_CANDLELIGHT;
import static android.hardware.camera2.CameraMetadata.CONTROL_SCENE_MODE_DISABLED;
import static android.hardware.camera2.CameraMetadata.CONTROL_SCENE_MODE_FIREWORKS;
import static android.hardware.camera2.CameraMetadata.CONTROL_SCENE_MODE_HDR;
import static android.hardware.camera2.CameraMetadata.CONTROL_SCENE_MODE_LANDSCAPE;
import static android.hardware.camera2.CameraMetadata.CONTROL_SCENE_MODE_NIGHT;
import static android.hardware.camera2.CameraMetadata.CONTROL_SCENE_MODE_PARTY;
import static android.hardware.camera2.CameraMetadata.CONTROL_SCENE_MODE_PORTRAIT;
import static android.hardware.camera2.CameraMetadata.CONTROL_SCENE_MODE_SNOW;
import static android.hardware.camera2.CameraMetadata.CONTROL_SCENE_MODE_SPORTS;
import static android.hardware.camera2.CameraMetadata.CONTROL_SCENE_MODE_STEADYPHOTO;
import static android.hardware.camera2.CameraMetadata.CONTROL_SCENE_MODE_SUNSET;
import static android.hardware.camera2.CameraMetadata.CONTROL_SCENE_MODE_THEATRE;
import static android.hardware.camera2.CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF;
import static android.hardware.camera2.CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON;
import static android.hardware.camera2.CameraMetadata.FLASH_MODE_OFF;
import static android.hardware.camera2.CameraMetadata.FLASH_MODE_SINGLE;
import static android.hardware.camera2.CameraMetadata.FLASH_MODE_TORCH;
import static android.hardware.camera2.CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF;
import static android.hardware.camera2.CaptureRequest.Builder;
import static android.hardware.camera2.CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION;
import static android.hardware.camera2.CaptureRequest.CONTROL_AE_LOCK;
import static android.hardware.camera2.CaptureRequest.CONTROL_AE_MODE;
import static android.hardware.camera2.CaptureRequest.CONTROL_AE_REGIONS;
import static android.hardware.camera2.CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE;
import static android.hardware.camera2.CaptureRequest.CONTROL_AF_MODE;
import static android.hardware.camera2.CaptureRequest.CONTROL_AF_REGIONS;
import static android.hardware.camera2.CaptureRequest.CONTROL_AWB_LOCK;
import static android.hardware.camera2.CaptureRequest.CONTROL_AWB_MODE;
import static android.hardware.camera2.CaptureRequest.CONTROL_SCENE_MODE;
import static android.hardware.camera2.CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE;
import static android.hardware.camera2.CaptureRequest.FLASH_MODE;
import static android.hardware.camera2.CaptureRequest.JPEG_GPS_LOCATION;
import static android.hardware.camera2.CaptureRequest.JPEG_QUALITY;
import static android.hardware.camera2.CaptureRequest.JPEG_THUMBNAIL_SIZE;
import static android.hardware.camera2.CaptureRequest.Key;
import static android.hardware.camera2.CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE;
import static android.hardware.camera2.CaptureRequest.SCALER_CROP_REGION;

/**
 * The subclass of {@link CameraSettings} for Android Camera 2 API.
 */
public class AndroidCamera2Settings extends CameraSettings {
    private static final Log.Tag TAG = new Log.Tag("AndCam2Set");

    private final Builder mTemplateSettings;
    private final Camera2RequestSettingsSet mRequestSettings;
    /** Sensor's active array bounds. */
    private final Rect mActiveArray;
    /** Crop rectangle for digital zoom (measured WRT the active array). */
    private final Rect mCropRectangle;
    /** Bounds of visible preview portion (measured WRT the active array). */
    private Rect mVisiblePreviewRectangle;

    /**
     * Create a settings representation that answers queries of unspecified
     * options in the same way as the provided template would.
     *
     * <p>The default settings provided by the given template are only ever used
     * for reporting back to the client app (i.e. when it queries an option
     * it didn't explicitly set first). {@link Camera2RequestSettingsSet}s
     * generated by an instance of this class will have any settings not
     * modified using one of that instance's mutators forced to default, so that
     * their effective values when submitting a capture request will be those of
     * the template that is provided to the camera framework at that time.</p>
     *
     * @param camera Device from which to draw default settings
     *               (non-{@code null}).
     * @param template Specific template to use for the defaults.
     * @param activeArray Boundary coordinates of the sensor's active array
     *                    (non-{@code null}).
     * @param preview Dimensions of preview streams.
     * @param photo Dimensions of captured images.
     *
     * @throws IllegalArgumentException If {@code camera} or {@code activeArray}
     *                                  is {@code null}.
     * @throws CameraAccessException Upon internal framework/driver failure.
     */
    public AndroidCamera2Settings(CameraDevice camera, int template, Rect activeArray,
                                  Size preview, Size photo) throws CameraAccessException {
        if (camera == null) {
            throw new NullPointerException("camera must not be null");
        }
        if (activeArray == null) {
            throw new NullPointerException("activeArray must not be null");
        }

        mTemplateSettings = camera.createCaptureRequest(template);
        mRequestSettings = new Camera2RequestSettingsSet();
        mActiveArray = activeArray;
        mCropRectangle = new Rect(0, 0, activeArray.width(), activeArray.height());

        mSizesLocked = false;

        Range<Integer> previewFpsRange = mTemplateSettings.get(CONTROL_AE_TARGET_FPS_RANGE);
        if (previewFpsRange != null) {
            setPreviewFpsRange(previewFpsRange.getLower(), previewFpsRange.getUpper());
        }
        setPreviewSize(preview);
        // TODO: mCurrentPreviewFormat
        setPhotoSize(photo);
        mJpegCompressQuality = queryTemplateDefaultOrMakeOneUp(JPEG_QUALITY, (byte) 0);
        // TODO: mCurrentPhotoFormat
        // NB: We're assuming that templates won't be zoomed in by default.
        mCurrentZoomRatio = CameraCapabilities.ZOOM_RATIO_UNZOOMED;
        // TODO: mCurrentZoomIndex
        mExposureCompensationIndex =
                queryTemplateDefaultOrMakeOneUp(CONTROL_AE_EXPOSURE_COMPENSATION, 0);

        mCurrentFlashMode = flashModeFromRequest();
        Integer currentFocusMode = mTemplateSettings.get(CONTROL_AF_MODE);
        if (currentFocusMode != null) {
            mCurrentFocusMode = AndroidCamera2Capabilities.focusModeFromInt(currentFocusMode);
        }
        Integer currentSceneMode = mTemplateSettings.get(CONTROL_SCENE_MODE);
        if (currentSceneMode != null) {
            mCurrentSceneMode = AndroidCamera2Capabilities.sceneModeFromInt(currentSceneMode);
        }
        Integer whiteBalance = mTemplateSettings.get(CONTROL_AWB_MODE);
        if (whiteBalance != null) {
            mWhiteBalance = AndroidCamera2Capabilities.whiteBalanceFromInt(whiteBalance);
        }

        mVideoStabilizationEnabled = queryTemplateDefaultOrMakeOneUp(
                        CONTROL_VIDEO_STABILIZATION_MODE, CONTROL_VIDEO_STABILIZATION_MODE_OFF) ==
                CONTROL_VIDEO_STABILIZATION_MODE_ON;
        mAutoExposureLocked = queryTemplateDefaultOrMakeOneUp(CONTROL_AE_LOCK, false);
        mAutoWhiteBalanceLocked = queryTemplateDefaultOrMakeOneUp(CONTROL_AWB_LOCK, false);
        // TODO: mRecordingHintEnabled
        // TODO: mGpsData
        android.util.Size exifThumbnailSize = mTemplateSettings.get(JPEG_THUMBNAIL_SIZE);
        if (exifThumbnailSize != null) {
            mExifThumbnailSize =
                    new Size(exifThumbnailSize.getWidth(), exifThumbnailSize.getHeight());
        }
    }

    public AndroidCamera2Settings(AndroidCamera2Settings other) {
        super(other);
        mTemplateSettings = other.mTemplateSettings;
        mRequestSettings = new Camera2RequestSettingsSet(other.mRequestSettings);
        mActiveArray = other.mActiveArray;
        mCropRectangle = new Rect(other.mCropRectangle);
    }

    @Override
    public CameraSettings copy() {
        return new AndroidCamera2Settings(this);
    }

    private <T> T queryTemplateDefaultOrMakeOneUp(Key<T> key, T defaultDefault) {
        T val = mTemplateSettings.get(key);
        if (val != null) {
            return val;
        } else {
            // Spoof the default so matchesTemplateDefault excludes this key from generated sets.
            // This approach beats a simple sentinel because it provides basic boolean support.
            mTemplateSettings.set(key, defaultDefault);
            return defaultDefault;
        }
    }

    private FlashMode flashModeFromRequest() {
        Integer autoExposure = mTemplateSettings.get(CONTROL_AE_MODE);
        if (autoExposure != null) {
            switch (autoExposure) {
                case CONTROL_AE_MODE_ON:
                    return FlashMode.OFF;
                case CONTROL_AE_MODE_ON_AUTO_FLASH:
                    return FlashMode.AUTO;
                case CONTROL_AE_MODE_ON_ALWAYS_FLASH: {
                    if (mTemplateSettings.get(FLASH_MODE) == FLASH_MODE_TORCH) {
                        return FlashMode.TORCH;
                    } else {
                        return FlashMode.ON;
                    }
                }
                case CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE:
                    return FlashMode.RED_EYE;
            }
        }
        return null;
    }

    @Override
    public void setZoomRatio(float ratio) {
        super.setZoomRatio(ratio);

        // Compute the crop rectangle to be passed to the framework
        mCropRectangle.set(0, 0,
                toIntConstrained(
                        mActiveArray.width() / mCurrentZoomRatio, 0, mActiveArray.width()),
                toIntConstrained(
                        mActiveArray.height() / mCurrentZoomRatio, 0, mActiveArray.height()));
        mCropRectangle.offsetTo((mActiveArray.width() - mCropRectangle.width()) / 2,
                (mActiveArray.height() - mCropRectangle.height()) / 2);

        // Compute the effective crop rectangle to be used for computing focus/metering coordinates
        mVisiblePreviewRectangle =
                effectiveCropRectFromRequested(mCropRectangle, mCurrentPreviewSize);
    }

    private boolean matchesTemplateDefault(Key<?> setting) {
        if (setting == CONTROL_AE_REGIONS) {
            return mMeteringAreas.size() == 0;
        } else if (setting == CONTROL_AF_REGIONS) {
            return mFocusAreas.size() == 0;
        } else if (setting == CONTROL_AE_TARGET_FPS_RANGE) {
            Range<Integer> defaultFpsRange = mTemplateSettings.get(CONTROL_AE_TARGET_FPS_RANGE);
            return (mPreviewFpsRangeMin == 0 && mPreviewFpsRangeMax == 0) ||
                    (defaultFpsRange != null && mPreviewFpsRangeMin == defaultFpsRange.getLower() &&
                            mPreviewFpsRangeMax == defaultFpsRange.getUpper());
        } else if (setting == JPEG_QUALITY) {
            return Objects.equals(mJpegCompressQuality,
                    mTemplateSettings.get(JPEG_QUALITY));
        } else if (setting == CONTROL_AE_EXPOSURE_COMPENSATION) {
            return Objects.equals(mExposureCompensationIndex,
                    mTemplateSettings.get(CONTROL_AE_EXPOSURE_COMPENSATION));
        } else if (setting == CONTROL_VIDEO_STABILIZATION_MODE) {
            Integer videoStabilization = mTemplateSettings.get(CONTROL_VIDEO_STABILIZATION_MODE);
            return (videoStabilization != null &&
                    (mVideoStabilizationEnabled && videoStabilization ==
                            CONTROL_VIDEO_STABILIZATION_MODE_ON) ||
                    (!mVideoStabilizationEnabled && videoStabilization ==
                            CONTROL_VIDEO_STABILIZATION_MODE_OFF));
        } else if (setting == CONTROL_AE_LOCK) {
            return Objects.equals(mAutoExposureLocked, mTemplateSettings.get(CONTROL_AE_LOCK));
        } else if (setting == CONTROL_AWB_LOCK) {
            return Objects.equals(mAutoWhiteBalanceLocked, mTemplateSettings.get(CONTROL_AWB_LOCK));
        } else if (setting == JPEG_THUMBNAIL_SIZE) {
            if (mExifThumbnailSize == null) {
                // It doesn't matter if this is true or false since setting this
                // to null in the request settings will use the default anyway.
                return false;
            }
            android.util.Size defaultThumbnailSize = mTemplateSettings.get(JPEG_THUMBNAIL_SIZE);
            return (mExifThumbnailSize.width() == 0 && mExifThumbnailSize.height() == 0) ||
                    (defaultThumbnailSize != null &&
                            mExifThumbnailSize.width() == defaultThumbnailSize.getWidth() &&
                            mExifThumbnailSize.height() == defaultThumbnailSize.getHeight());
        }
        Log.w(TAG, "Settings implementation checked default of unhandled option key");
        // Since this class isn't equipped to handle it, claim it matches the default to prevent
        // updateRequestSettingOrForceToDefault from going with the user-provided preference
        return true;
    }

    private <T> void updateRequestSettingOrForceToDefault(Key<T> setting, T possibleChoice) {
        mRequestSettings.set(setting, matchesTemplateDefault(setting) ? null : possibleChoice);
    }

    public Camera2RequestSettingsSet getRequestSettings() {


        updateRequestSettingOrForceToDefault(CONTROL_AE_REGIONS,
                legacyAreasToMeteringRectangles(mMeteringAreas));

        updateRequestSettingOrForceToDefault(CONTROL_AF_REGIONS,
                legacyAreasToMeteringRectangles(mFocusAreas));

        updateRequestSettingOrForceToDefault(CONTROL_AE_TARGET_FPS_RANGE,
                new Range(mPreviewFpsRangeMin, mPreviewFpsRangeMax));


        // TODO: mCurrentPreviewFormat
        updateRequestSettingOrForceToDefault(JPEG_QUALITY, mJpegCompressQuality);
        // TODO: mCurrentPhotoFormat
        mRequestSettings.set(SCALER_CROP_REGION, mCropRectangle);
        // TODO: mCurrentZoomIndex
        updateRequestSettingOrForceToDefault(CONTROL_AE_EXPOSURE_COMPENSATION,
                mExposureCompensationIndex);
        updateRequestFlashMode();
        updateRequestFocusMode();
        updateRequestSceneMode();
        updateRequestWhiteBalance();
        updateRequestSettingOrForceToDefault(CONTROL_VIDEO_STABILIZATION_MODE,
                mVideoStabilizationEnabled ?
                        CONTROL_VIDEO_STABILIZATION_MODE_ON : CONTROL_VIDEO_STABILIZATION_MODE_OFF);
        // OIS shouldn't be on if software video stabilization is.
        mRequestSettings.set(LENS_OPTICAL_STABILIZATION_MODE,
                mVideoStabilizationEnabled ? LENS_OPTICAL_STABILIZATION_MODE_OFF :
                        null);
        updateRequestSettingOrForceToDefault(CONTROL_AE_LOCK, mAutoExposureLocked);
        updateRequestSettingOrForceToDefault(CONTROL_AWB_LOCK, mAutoWhiteBalanceLocked);
        // TODO: mRecordingHintEnabled
        updateRequestGpsData();
        if (mExifThumbnailSize != null) {
            updateRequestSettingOrForceToDefault(JPEG_THUMBNAIL_SIZE,
                    new android.util.Size(
                            mExifThumbnailSize.width(), mExifThumbnailSize.height()));
        } else {
            updateRequestSettingOrForceToDefault(JPEG_THUMBNAIL_SIZE, null);
        }

        return mRequestSettings;
    }

    private MeteringRectangle[] legacyAreasToMeteringRectangles(
            List<Camera.Area> reference) {
        MeteringRectangle[] transformed = null;
        if (reference.size() > 0) {
            transformed = new MeteringRectangle[reference.size()];
            for (int index = 0; index < reference.size(); ++index) {
                android.hardware.Camera.Area source = reference.get(index);
                Rect rectangle = source.rect;

                // Old API coordinates were [-1000,1000]; new ones are [0,ACTIVE_ARRAY_SIZE).
                // We're also going from preview image--relative to sensor active array--relative.
                double oldLeft = (rectangle.left + 1000) / 2000.0;
                double oldTop = (rectangle.top + 1000) / 2000.0;
                double oldRight = (rectangle.right + 1000) / 2000.0;
                double oldBottom = (rectangle.bottom + 1000) / 2000.0;
                int left = mCropRectangle.left + toIntConstrained(
                        mCropRectangle.width() * oldLeft, 0, mCropRectangle.width() - 1);
                int top = mCropRectangle.top + toIntConstrained(
                        mCropRectangle.height() * oldTop, 0, mCropRectangle.height() - 1);
                int right = mCropRectangle.left + toIntConstrained(
                        mCropRectangle.width() * oldRight, 0, mCropRectangle.width() - 1);
                int bottom = mCropRectangle.top + toIntConstrained(
                        mCropRectangle.height() * oldBottom, 0, mCropRectangle.height() - 1);
                transformed[index] = new MeteringRectangle(left, top, right - left, bottom - top,
                        source.weight);
            }
        }
        return transformed;
    }

    private int toIntConstrained(double original, int min, int max) {
        original = Math.max(original, min);
        original = Math.min(original, max);
        return (int) original;
    }

    private void updateRequestFlashMode() {
        Integer aeMode = null;
        Integer flashMode = null;
        if (mCurrentFlashMode != null) {
            switch (mCurrentFlashMode) {
                case AUTO: {
                    aeMode = CONTROL_AE_MODE_ON_AUTO_FLASH;
                    break;
                }
                case OFF: {
                    aeMode = CONTROL_AE_MODE_ON;
                    flashMode = FLASH_MODE_OFF;
                    break;
                }
                case ON: {
                    aeMode = CONTROL_AE_MODE_ON_ALWAYS_FLASH;
                    flashMode = FLASH_MODE_SINGLE;
                    break;
                }
                case TORCH: {
                    flashMode = FLASH_MODE_TORCH;
                    break;
                }
                case RED_EYE: {
                    aeMode = CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE;
                    break;
                }
                default: {
                    Log.w(TAG, "Unable to convert to API 2 flash mode: " + mCurrentFlashMode);
                    break;
                }
            }
        }
        mRequestSettings.set(CONTROL_AE_MODE, aeMode);
        mRequestSettings.set(FLASH_MODE, flashMode);
    }

    private void updateRequestFocusMode() {
        Integer mode = null;
        if (mCurrentFocusMode != null) {
            switch (mCurrentFocusMode) {
                case AUTO: {
                    mode = CONTROL_AF_MODE_AUTO;
                    break;
                }
                case CONTINUOUS_PICTURE: {
                    mode = CONTROL_AF_MODE_CONTINUOUS_PICTURE;
                    break;
                }
                case CONTINUOUS_VIDEO: {
                    mode = CONTROL_AF_MODE_CONTINUOUS_VIDEO;
                    break;
                }
                case EXTENDED_DOF: {
                    mode = CONTROL_AF_MODE_EDOF;
                    break;
                }
                case FIXED: {
                    mode = CONTROL_AF_MODE_OFF;
                    break;
                }
                // TODO: We cannot support INFINITY
                case MACRO: {
                    mode = CONTROL_AF_MODE_MACRO;
                    break;
                }
                default: {
                    Log.w(TAG, "Unable to convert to API 2 focus mode: " + mCurrentFocusMode);
                    break;
                }
            }
        }
        mRequestSettings.set(CONTROL_AF_MODE, mode);
    }

    private void updateRequestSceneMode() {
        Integer mode = null;
        if (mCurrentSceneMode != null) {
            switch (mCurrentSceneMode) {
                case AUTO: {
                    mode = CONTROL_SCENE_MODE_DISABLED;
                    break;
                }
                case ACTION: {
                    mode = CONTROL_SCENE_MODE_ACTION;
                    break;
                }
                case BARCODE: {
                    mode = CONTROL_SCENE_MODE_BARCODE;
                    break;
                }
                case BEACH: {
                    mode = CONTROL_SCENE_MODE_BEACH;
                    break;
                }
                case CANDLELIGHT: {
                    mode = CONTROL_SCENE_MODE_CANDLELIGHT;
                    break;
                }
                case FIREWORKS: {
                    mode = CONTROL_SCENE_MODE_FIREWORKS;
                    break;
                }
                case HDR: {
                    mode = CONTROL_SCENE_MODE_HDR;
                    break;
                }
                case LANDSCAPE: {
                    mode = CONTROL_SCENE_MODE_LANDSCAPE;
                    break;
                }
                case NIGHT: {
                    mode = CONTROL_SCENE_MODE_NIGHT;
                    break;
                }
                // TODO: We cannot support NIGHT_PORTRAIT
                case PARTY: {
                    mode = CONTROL_SCENE_MODE_PARTY;
                    break;
                }
                case PORTRAIT: {
                    mode = CONTROL_SCENE_MODE_PORTRAIT;
                    break;
                }
                case SNOW: {
                    mode = CONTROL_SCENE_MODE_SNOW;
                    break;
                }
                case SPORTS: {
                    mode = CONTROL_SCENE_MODE_SPORTS;
                    break;
                }
                case STEADYPHOTO: {
                    mode = CONTROL_SCENE_MODE_STEADYPHOTO;
                    break;
                }
                case SUNSET: {
                    mode = CONTROL_SCENE_MODE_SUNSET;
                    break;
                }
                case THEATRE: {
                    mode = CONTROL_SCENE_MODE_THEATRE;
                    break;
                }
                default: {
                    Log.w(TAG, "Unable to convert to API 2 scene mode: " + mCurrentSceneMode);
                    break;
                }
            }
        }
        mRequestSettings.set(CONTROL_SCENE_MODE, mode);
    }

    private void updateRequestWhiteBalance() {
        Integer mode = null;
        if (mWhiteBalance != null) {
            switch (mWhiteBalance) {
                case AUTO: {
                    mode = CONTROL_AWB_MODE_AUTO;
                    break;
                }
                case CLOUDY_DAYLIGHT: {
                    mode = CONTROL_AWB_MODE_CLOUDY_DAYLIGHT;
                    break;
                }
                case DAYLIGHT: {
                    mode = CONTROL_AWB_MODE_DAYLIGHT;
                    break;
                }
                case FLUORESCENT: {
                    mode = CONTROL_AWB_MODE_FLUORESCENT;
                    break;
                }
                case INCANDESCENT: {
                    mode = CONTROL_AWB_MODE_INCANDESCENT;
                    break;
                }
                case SHADE: {
                    mode = CONTROL_AWB_MODE_SHADE;
                    break;
                }
                case TWILIGHT: {
                    mode = CONTROL_AWB_MODE_TWILIGHT;
                    break;
                }
                case WARM_FLUORESCENT: {
                    mode = CONTROL_AWB_MODE_WARM_FLUORESCENT;
                    break;
                }
                default: {
                    Log.w(TAG, "Unable to convert to API 2 white balance: " + mWhiteBalance);
                    break;
                }
            }
        }
        mRequestSettings.set(CONTROL_AWB_MODE, mode);
    }

    private void updateRequestGpsData() {
        if (mGpsData == null || mGpsData.processingMethod == null) {
            // It's a hack since we always use GPS time stamp but does
            // not use other fields sometimes. Setting processing
            // method to null means the other fields should not be used.
            mRequestSettings.set(JPEG_GPS_LOCATION, null);
        } else {
            Location location = new Location(mGpsData.processingMethod);
            location.setTime(mGpsData.timeStamp);
            location.setAltitude(mGpsData.altitude);
            location.setLatitude(mGpsData.latitude);
            location.setLongitude(mGpsData.longitude);
            mRequestSettings.set(JPEG_GPS_LOCATION, location);
        }
    }

    /**
     * Calculate the effective crop rectangle for this preview viewport;
     * assumes the preview is centered to the sensor and scaled to fit across one of the dimensions
     * without skewing.
     *
     * <p>Assumes the zoom level of the provided desired crop rectangle.</p>
     *
     * @param requestedCrop Desired crop rectangle, in active array space.
     * @param previewSize Size of the preview buffer render target, in pixels (not in sensor space).
     * @return A rectangle that serves as the preview stream's effective crop region (unzoomed), in
     *          sensor space.
     *
     * @throws NullPointerException
     *          If any of the args were {@code null}.
     */
    private static Rect effectiveCropRectFromRequested(Rect requestedCrop, Size previewSize) {
        float aspectRatioArray = requestedCrop.width() * 1.0f / requestedCrop.height();
        float aspectRatioPreview = previewSize.width() * 1.0f / previewSize.height();

        float cropHeight, cropWidth;
        if (aspectRatioPreview < aspectRatioArray) {
            // The new width must be smaller than the height, so scale the width by AR
            cropHeight = requestedCrop.height();
            cropWidth = cropHeight * aspectRatioPreview;
        } else {
            // The new height must be smaller (or equal) than the width, so scale the height by AR
            cropWidth = requestedCrop.width();
            cropHeight = cropWidth / aspectRatioPreview;
        }

        Matrix translateMatrix = new Matrix();
        RectF cropRect = new RectF(/*left*/0, /*top*/0, cropWidth, cropHeight);

        // Now center the crop rectangle so its center is in the center of the active array
        translateMatrix.setTranslate(requestedCrop.exactCenterX(), requestedCrop.exactCenterY());
        translateMatrix.postTranslate(-cropRect.centerX(), -cropRect.centerY());

        translateMatrix.mapRect(/*inout*/cropRect);

        // Round the rect corners towards the nearest integer values
        Rect result = new Rect();
        cropRect.roundOut(result);
        return result;
    }
}