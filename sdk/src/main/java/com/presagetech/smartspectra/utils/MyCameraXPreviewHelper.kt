package com.presagetech.smartspectra.utils

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.util.Size
import android.view.WindowManager
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import timber.log.Timber
import java.util.concurrent.ExecutorService


@ExperimentalCamera2Interop
class MyCameraXPreviewHelper {
    fun interface OnCameraImageProxyListener {
        fun onImageProxy(image: ImageProxy)
    }

    var onCameraImageProxyListener: OnCameraImageProxyListener? = null
    private val selectedLensFacing = CameraSelector.LENS_FACING_FRONT
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraCharacteristics: CameraCharacteristics? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var backgroundExecutor: ExecutorService

    fun startCamera(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        backgroundExecutor: ExecutorService
    ) {

        this.backgroundExecutor = backgroundExecutor
        cameraCharacteristics = getCameraCharacteristics(context, selectedLensFacing)

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                cameraProvider = cameraProviderFuture.get()
                // Build and bind camera uses cases
                cameraProviderOpened(context, cameraProvider!!, lifecycleOwner, previewView)
            }, ContextCompat.getMainExecutor(context)
        )
        Timber.d("Started camera in the camera preview helper")
    }

    private fun cameraProviderOpened(
        context: Context,
        cameraProvider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
    ) {

        // Get display rotation
        val displayRotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display!!.rotation
        } else {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.defaultDisplay.rotation
        }

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(selectedLensFacing)
            .build()

        // Preview. We are using 720p, so the aspect ratio is 16_9
        preview = Preview.Builder()
            .setTargetResolution(TARGET_SIZE)
            .setTargetRotation(displayRotation)
            .build()

        Timber.d("Display rotation: $displayRotation")

        imageAnalyzer =
                ImageAnalysis.Builder()
                    .setTargetResolution(TARGET_SIZE)
                    .setTargetRotation(displayRotation)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .setOutputImageRotationEnabled(true)
                    .build()
                    // assign to instance
                    .also {
                        it.setAnalyzer(
                            backgroundExecutor
                        ) { imageProxy ->
                            onCameraImageProxyListener?.onImageProxy(imageProxy)
                        }
                    }

        cameraProvider.unbindAll()
        try {
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer
            )

            //attach viewFinder's surface provide to preview use case
            preview?.setSurfaceProvider(previewView.surfaceProvider)

        } catch (e: Exception) {
            Timber.e("Camera use case binding failed $e")
        }
    }

    fun stopCamera() {
        cameraProvider?.unbindAll()
    }

    fun toggleCameraControl(locked: Boolean) {
        val cameraControl = camera?.cameraControl ?: return
        val options = CaptureRequestOptions.Builder()
            .setCaptureRequestOption(CaptureRequest.CONTROL_AWB_LOCK, locked)
            .setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, locked)
            .setCaptureRequestOption(CaptureRequest.BLACK_LEVEL_LOCK, locked)
            .build()
        Camera2CameraControl.from(cameraControl).captureRequestOptions = options
    }

    internal companion object {
        // Target frame and view resolution size in landscape.
        val TARGET_SIZE = Size(720,1280)

        private fun getCameraCharacteristics(
            context: Context,
            lensFacing: Int
        ): CameraCharacteristics? {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            try {
                val cameraList = listOf(*cameraManager.cameraIdList)
                for (availableCameraId in cameraList) {
                    val availableCameraCharacteristics =
                        cameraManager.getCameraCharacteristics(availableCameraId)
                    val availableLensFacing =
                        availableCameraCharacteristics.get(CameraCharacteristics.LENS_FACING)
                            ?: continue
                    if (availableLensFacing == lensFacing) {
                        // Check if the target resolution is supported
                        if (!isTargetResolutionSupported(availableCameraCharacteristics, TARGET_SIZE)) {
                            Timber.w("Target resolution ${TARGET_SIZE.width}x${TARGET_SIZE.height} not supported by camera $availableCameraId")
                        }
                        return availableCameraCharacteristics
                    }
                }
            } catch (e: CameraAccessException) {
                Timber.e("Accessing camera ID info got error: $e")
            }
            return null
        }

        private fun isTargetResolutionSupported(
            cameraCharacteristics: CameraCharacteristics,
            targetSize: Size
        ): Boolean {
            val streamConfigurationMap =
                cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    ?: return false
            val outputSizes = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG)
            val rotatedSize = Size(targetSize.height, targetSize.width)
            return outputSizes.contains(targetSize) || outputSizes.contains(rotatedSize)
        }
    }
}
