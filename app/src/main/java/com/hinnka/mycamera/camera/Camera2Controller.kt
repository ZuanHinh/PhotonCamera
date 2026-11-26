package com.hinnka.mycamera.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ColorSpace
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.ColorSpaceTransform
import android.hardware.camera2.params.MeteringRectangle
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.DynamicRangeProfiles
import android.hardware.camera2.params.RggbChannelVector
import android.hardware.camera2.params.SessionConfiguration
import android.hardware.camera2.params.TonemapCurve
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import android.util.Range
import android.util.Rational
import android.util.Size
import android.view.Surface
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.hinnka.mycamera.raw.ColorSpace as RawColorSpace
import com.hinnka.mycamera.raw.DngSdkColorSpec
import com.hinnka.mycamera.utils.PLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import com.hinnka.mycamera.livephoto.LivePhotoRecorder
import com.hinnka.mycamera.lut.LutConfig
import com.hinnka.mycamera.lut.VideoColorEffectLayer
import com.hinnka.mycamera.model.SafeImage
import com.hinnka.mycamera.model.ColorRecipeParams
import com.hinnka.mycamera.stabilization.RealtimeStabilizationCoordinator
import com.hinnka.mycamera.stabilization.normalizeStabilizationLookahead
import com.hinnka.mycamera.stabilization.normalizeStabilizationStrength
import com.hinnka.mycamera.utils.DeviceUtil
import com.hinnka.mycamera.utils.OrientationObserver
import com.hinnka.mycamera.video.CaptureMode
import com.hinnka.mycamera.video.VideoAspectRatio
import com.hinnka.mycamera.video.VIDEO_AUDIO_INPUT_AUTO
import com.hinnka.mycamera.video.VideoBitratePreset
import com.hinnka.mycamera.video.VideoCapabilitiesResolver
import com.hinnka.mycamera.video.VideoFpsPreset
import com.hinnka.mycamera.video.VideoEncoderColorRequest
import com.hinnka.mycamera.video.VideoLogProfile
import com.hinnka.mycamera.video.VideoRecorder
import com.hinnka.mycamera.video.VideoRecordingPath
import com.hinnka.mycamera.video.VideoResolutionPreset
import com.hinnka.mycamera.video.VideoRecordingState
import com.hinnka.mycamera.video.VideoStabilizationMode
import com.hinnka.mycamera.video.resolveSurfaceTextureVideoOrientationDegrees
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class Camera2Controller(private val context: Context) {
    companion object {
        private const val TAG = "Camera2Controller"
        const val ERROR_CAMERA_DISCONNECTED = 1000
        const val ERROR_CAMERA_OPEN_FAILED = 1001
        const val ERROR_CAMERA_CHARACTERISTICS_UNAVAILABLE = 1002
        const val ERROR_CAMERA_SESSION_CONFIG_FAILED = 1003

        private const val SINGLE_CAPTURE_READER_MAX_IMAGES = 2
        private const val BURST_CAPTURE_BATCH_SIZE = 8
        private const val HDR_BRACKET_BASE_CAPTURE_COUNT = 3
        private const val HDR_BRACKET_SIDE_FRAME_COUNT = 2
        private const val STATE_PREVIEW = 0
        private const val STATE_WAITING_PRECAPTURE = 2
        private const val STATE_WAITING_NON_PRECAPTURE = 3
        private const val STATE_PICTURE_TAKEN = 4
        private const val PRECAPTURE_TIMEOUT_MS = 3_000L
        private const val MULTI_FRAME_TORCH_WARMUP_TIMEOUT_MS = 2_000L
    }

    private val cameraManager: CameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private val cameraDiscovery = CameraDiscovery(context)
    private val realtimeStabilizationCoordinator = RealtimeStabilizationCoordinator(context)

    private fun logicalMultiCameraActivePhysicalId(result: TotalCaptureResult): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        if (activeOutputPhysicalCameraId != null) return activeOutputPhysicalCameraId
        return runCatching {
            result.get(CaptureResult.LOGICAL_MULTI_CAMERA_ACTIVE_PHYSICAL_ID)
        }.getOrNull()
    }

    private var activeOutputPhysicalCameraId: String? = null

    private fun captureFrameMetadata(
        result: TotalCaptureResult,
        sensorTimestampNs: Long,
    ): CapturedFrameMetadata? {
        val exposureTimeNs = result.get(CaptureResult.SENSOR_EXPOSURE_TIME) ?: return null
        val sensitivityIso = result.get(CaptureResult.SENSOR_SENSITIVITY) ?: return null
        val exposureProduct = RawExposureMath.productOrNull(exposureTimeNs, sensitivityIso)
            ?: return null
        val desiredExposureProduct = RawExposureMath.productOrNull(
            result.request.get(CaptureRequest.SENSOR_EXPOSURE_TIME),
            result.request.get(CaptureRequest.SENSOR_SENSITIVITY),
        )
        val channelNoiseProfile = captureChannelNoiseProfile(result)
        val framePhysicalCameraId = logicalMultiCameraActivePhysicalId(result)
        val sensorCharacteristics = framePhysicalCameraId
            ?.let { cameraId ->
                getCameraCharacteristicsOrNull(cameraId, "RAW frame gain limits")
            }
            ?: getActiveOpenCameraCharacteristics()
        val sensitivityLimits = sensorSensitivityLimits(sensorCharacteristics)

        return CapturedFrameMetadata(
            sensorTimestampNs = sensorTimestampNs,
            frameNumber = result.frameNumber,
            exposureTimeNs = exposureTimeNs,
            sensitivityIso = sensitivityIso,
            minimumSensitivityIso = sensitivityLimits.minimumIso,
            maximumAnalogSensitivityIso = sensitivityLimits.maximumAnalogIso,
            exposureProduct = exposureProduct,
            focusDistanceDiopters = result.get(CaptureResult.LENS_FOCUS_DISTANCE) ?: Float.NaN,
            lensState = result.get(CaptureResult.LENS_STATE),
            rollingShutterSkewNs = result.get(CaptureResult.SENSOR_ROLLING_SHUTTER_SKEW),
            gyroWindow = burstGyroRecorder.exposureWindow(sensorTimestampNs, exposureTimeNs),
            channelNoiseProfile = channelNoiseProfile,
            multiFrameCaptureRole = result.request.tag as? MultiFrameCaptureRole,
            desiredExposureProduct = desiredExposureProduct,
            dynamicBlackLevelByCfaPosition = result
                .get(CaptureResult.SENSOR_DYNAMIC_BLACK_LEVEL)
                ?.takeIf { it.size >= 4 }
                ?.copyOf(4),
        )
    }
}
