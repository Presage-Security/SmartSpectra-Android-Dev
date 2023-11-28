package com.presagetech.smartspectra.utils

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.opengl.GLES20
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.*
import androidx.camera.core.SurfaceRequest.TransformationInfo
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mediapipe.glutil.EglManager
import timber.log.Timber
import java.util.*
import java.util.concurrent.*


@ExperimentalCamera2Interop
class MyCameraXPreviewHelper {
    fun interface OnCameraStartedListener {
        fun onCameraStarted(surfaceTexture: SurfaceTexture)
    }

    var onCameraStartedListener: OnCameraStartedListener? = null
    private val selectedLensFacing = CameraMetadata.LENS_FACING_FRONT

    private val renderExecutor = Executors.newSingleThreadExecutor()
    private var preview: Preview? = null
    private var camera: Camera? = null
    private var textures: IntArray? = null
    private var frameSize: Size? = null
    private var frameRotation = 0
    private var cameraCharacteristics: CameraCharacteristics? = null

    fun startCamera(
        context: Context,
        lifecycleOwner: LifecycleOwner,
    ) {
        val mainThreadExecutor = ContextCompat.getMainExecutor(context)
        cameraCharacteristics = getCameraCharacteristics(context, selectedLensFacing)

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                val cameraProvider = cameraProviderFuture.get()!!
                cameraProviderOpened(context, cameraProvider, lifecycleOwner)
            }, mainThreadExecutor
        )
    }

    private fun cameraProviderOpened(
        context: Context,
        cameraProvider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner,
    ) {
        val targetSize = TARGET_SIZE
        val rotatedSize = Size(targetSize.height, targetSize.width)

        val previewBuilder = Preview.Builder()
        preview = previewBuilder.setTargetResolution(rotatedSize).build()

        val cameraSelector = when (selectedLensFacing) {
            CameraMetadata.LENS_FACING_FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
            CameraMetadata.LENS_FACING_BACK -> CameraSelector.DEFAULT_BACK_CAMERA
            else -> throw IllegalStateException()
        }

        // Provide surface texture.
        preview!!.setSurfaceProvider(renderExecutor) { request: SurfaceRequest ->
            frameSize = request.resolution
            Timber.d("Received surface request for resolution $frameSize")
            val previewFrameTexture = createSurfaceTexture()
            previewFrameTexture.setDefaultBufferSize(
                frameSize!!.width, frameSize!!.height
            )
            request.setTransformationInfoListener(renderExecutor) { transformationInfo: TransformationInfo ->
                frameRotation = transformationInfo.rotationDegrees
                previewFrameTexture.detachFromGLContext()
                val listener = onCameraStartedListener
                if (listener != null) {
                    ContextCompat
                        .getMainExecutor(context)
                        .execute { listener.onCameraStarted(previewFrameTexture) }
                }
            }
            val surface = Surface(previewFrameTexture)
            Timber.d("Providing surface")
            request.provideSurface(surface, renderExecutor) { result: SurfaceRequest.Result ->
                Timber.d("Surface request result: $result")
                if (textures != null) {
                    GLES20.glDeleteTextures(1, textures, 0)
                }
                previewFrameTexture.release()
                surface.release()
            }
        }
        cameraProvider.unbindAll()
        camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
    }

    val isCameraRotated: Boolean get() = frameRotation % 180 == 90


    fun computeDisplaySizeFromViewSize(viewSize: Size?): Size {
        // Camera target size is computed already, so just return the capture frame size.
        return frameSize ?: throw IllegalStateException()
    }

    fun toggleCameraControl(locked: Boolean) {
        val options = CaptureRequestOptions.Builder()
            .setCaptureRequestOption(CaptureRequest.CONTROL_AWB_LOCK, locked)
            .setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, locked)
            .setCaptureRequestOption(CaptureRequest.BLACK_LEVEL_LOCK, locked)
            .build()
        Camera2CameraControl.from(camera?.cameraControl!!).captureRequestOptions = options
    }

    private fun createSurfaceTexture(): SurfaceTexture {
        val eglManager = EglManager(null)
        val tempEglSurface = eglManager.createOffscreenSurface(1, 1)
        eglManager.makeCurrent(tempEglSurface, tempEglSurface)
        textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        return SurfaceTexture(textures!![0])
    }

    companion object {
        // Target frame and view resolution size in landscape.
        private val TARGET_SIZE = Size(1280, 720)

        private fun getCameraCharacteristics(
            context: Context,
            lensFacing: Int
        ): CameraCharacteristics? {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            try {
                val cameraList = Arrays.asList(*cameraManager.cameraIdList)
                for (availableCameraId in cameraList) {
                    val availableCameraCharacteristics =
                        cameraManager.getCameraCharacteristics(availableCameraId)
                    val availableLensFacing =
                        availableCameraCharacteristics.get(CameraCharacteristics.LENS_FACING)
                            ?: continue
                    if (availableLensFacing == lensFacing) {
                        return availableCameraCharacteristics
                    }
                }
            } catch (e: CameraAccessException) {
                Timber.e("Accessing camera ID info got error: $e")
            }
            return null
        }
    }
}
