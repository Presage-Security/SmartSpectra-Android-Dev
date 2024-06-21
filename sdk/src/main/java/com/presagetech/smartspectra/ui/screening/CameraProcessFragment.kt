package com.presagetech.smartspectra.ui.screening

import android.content.Context
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.os.SystemClock
import android.util.Size
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.Toolbar
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.mediapipe.components.ExternalTextureConverter
import com.google.mediapipe.components.FrameProcessor
import com.google.mediapipe.components.PermissionHelper
import com.google.mediapipe.framework.AndroidAssetUtil
import com.google.mediapipe.framework.Packet
import com.google.mediapipe.framework.PacketGetter
import com.google.mediapipe.glutil.EglManager
import com.presagetech.smartspectra.Config
import com.presagetech.smartspectra.R
import com.presagetech.smartspectra.ui.SmartSpectraActivity
import com.presagetech.smartspectra.ui.viewmodel.ScreeningViewModel
import com.presagetech.smartspectra.utils.MyCameraXPreviewHelper
import com.presage.physiology.proto.StatusProto
import com.presage.physiology.Messages
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.io.IOException


@ExperimentalCamera2Interop
class CameraProcessFragment : Fragment() {
    private val BINARY_GRAPH_NAME = "preprocessing_gpu_spot_json.binarypb"
    private val INPUT_VIDEO_STREAM_NAME = "input_video"
    private val SELECTED_INPUT_STREAM_NAME = "start_button_pre"
    private val SPOT_DURATION_SIDE_PACKET_NAME = "spot_duration"
    private val SPOT_DURATION_DEFAULT_VALUE = 30.0

    private var isRecording: Boolean = false
    private var cameraHelper: MyCameraXPreviewHelper? = null

    private lateinit var timerTextView: TextView
    private lateinit var hintText: TextView
    private lateinit var recordingButton: AppCompatTextView
    private lateinit var previewDisplayView: SurfaceView  // frames processed by MediaPipe

    // {@link SurfaceTexture} where the camera-preview frames can be accessed.
    private var previewFrameTexture: SurfaceTexture? = null

    private var eglManager: EglManager? = null

    // Sends camera-preview frames into a MediaPipe graph for processing, and displays the processed
    // frames onto a {@link Surface}.
    private var processor: FrameProcessor? = null

    private val FLIP_FRAMES_VERTICALLY = true

    var timeLeft: Double = 100.0
    @Volatile var statusCode: StatusProto.StatusCode = StatusProto.StatusCode.PROCESSING_NOT_STARTED
    var cameraLockTimeout: Long = 0L  // based on

    // Converts the GL_TEXTURE_EXTERNAL_OES texture from Android camera into a regular texture to be
    // consumed by {@link FrameProcessor} and the underlying MediaPipe graph.
    private var textureConverter: ExternalTextureConverter? = null

    private val viewModel: ScreeningViewModel by lazy {
        ViewModelProvider(
            requireActivity(),
            (requireActivity() as SmartSpectraActivity).viewModelFactory
        )[ScreeningViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_camera_process_layout, container, false).also {
            timerTextView = it.findViewById(R.id.text_timer)
            hintText = it.findViewById(R.id.text_hint)
            recordingButton = it.findViewById(R.id.button_recording)
            previewDisplayView = it.findViewById(R.id.surface_preview)
        }
        hintText.setText(R.string.loading_hint)
        recordingButton.setOnClickListener(::recordButtonClickListener)
        previewDisplayView.holder.addCallback(previewSurfaceHolderCallback)
        previewDisplayView.isVisible = false

        AndroidAssetUtil.initializeNativeAssetManager(requireContext())
        eglManager = EglManager(null)

        val processor = FrameProcessor(
            requireContext(),
            eglManager!!.nativeContext,
            BINARY_GRAPH_NAME,
            INPUT_VIDEO_STREAM_NAME,
            "output_video"
        ).also {
            it.videoSurfaceOutput.setFlipY(FLIP_FRAMES_VERTICALLY)
            it.setInputSidePackets(mapOf(SPOT_DURATION_SIDE_PACKET_NAME to it.packetCreator.createFloat64(SPOT_DURATION_DEFAULT_VALUE)))
            it.setOnWillAddFrameListener(::handleOnWillAddFrame)
            it.addPacketCallback("time_left", ::handleTimeLeftPacket)
            it.addPacketCallback("json_data", ::handleJsonDataPacket)
            it.addPacketCallback("status_code", ::handleStatusCodePacket)
        }
        this.processor = processor

        view.findViewById<Toolbar>(R.id.toolbar).also {
            (requireActivity() as AppCompatActivity).setSupportActionBar(it)
            it.setNavigationIcon(R.drawable.ic_arrow_back)
            it.setNavigationOnClickListener { _ ->
                @Suppress("DEPRECATION")
                requireActivity().onBackPressed()
            }
        }

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        toggleBrightMode(true)
    }

    override fun onDetach() {
        super.onDetach()
        toggleBrightMode(false)
    }

    private fun toggleBrightMode(on: Boolean) {
        requireActivity().window.also {
            val brightness: Float
            if (on) {
                it.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                brightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
            } else {
                it.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                brightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
            val params: WindowManager.LayoutParams = it.attributes
            params.screenBrightness = brightness
            it.attributes = params
        }
    }

    private fun canRecord(): Boolean {
        return statusCode == StatusProto.StatusCode.OK
    }

    @Suppress("UNUSED_PARAMETER")
    private fun recordButtonClickListener(view: View) {
        Timber.i("recordButtonClick: isRecording=$isRecording, status_code: $statusCode")
        if (isRecording) {
            resetTimer()
        } else {
            if (canRecord()) {
                startTimer()
            } else {
                Timber.d("Can't start recording, status code: $statusCode")
            }
        }
    }

    private fun startTimer() {
        cameraHelper!!.toggleCameraControl(locked = true)
        cameraLockTimeout = SystemClock.elapsedRealtime() + CAMERA_LOCKING_TIMEOUT
        isRecording = true
        recordingButton.text = getString(R.string.stop)
    }

    private fun resetTimer() {
        cameraHelper!!.toggleCameraControl(locked = false)
        cameraLockTimeout = 0L
        isRecording = false
        recordingButton.text = getString(R.string.record)
    }

    private fun handleOnWillAddFrame(timestamp: Long) {
        val processor = processor ?: throw IllegalStateException()
        val value = isRecording && SystemClock.elapsedRealtime() > cameraLockTimeout
        val selectedButtonPacket = processor.packetCreator.createBool(value)
        processor
            .graph
            .addPacketToInputStream(
                SELECTED_INPUT_STREAM_NAME, selectedButtonPacket, timestamp
            )
    }

    private fun handleTimeLeftPacket(packet: Packet?) {
        if (packet == null) return
        timeLeft = PacketGetter.getFloat64(packet)
        timerTextView.post {
            timerTextView.text = timeLeft.toInt().toString()
        }
    }

    private fun handleJsonDataPacket(packet: Packet?) {
        if (packet == null) return
        val outputJson = PacketGetter.getJson(packet)
        viewModel.setJsonData(outputJson)
        if (Config.SAVE_JSONs) {
            saveJsonLocally(outputJson)
        }
        (requireActivity() as SmartSpectraActivity).openUploadFragment()
    }

    private fun handleStatusCodePacket(packet: Packet?) {
        if (packet == null) return
        val newStatusCodeMessage: StatusProto.StatusValue = PacketGetter.getProto(packet, StatusProto.StatusValue.parser())
        val newStatusCode = newStatusCodeMessage.value
        if (newStatusCode == statusCode) return
        statusCode = newStatusCode
        hintText.post {
            hintText.text = Messages.getStatusHint(statusCode)
        }
    }

    override fun onResume() {
        super.onResume()

        if (!PermissionHelper.cameraPermissionsGranted(requireActivity())) {
            throw RuntimeException("Handle camera permission in host activity")
        }

        textureConverter = ExternalTextureConverter(
            eglManager!!.context, 2
        ).also {
            it.setFlipY(FLIP_FRAMES_VERTICALLY)
            it.setConsumer(processor)
        }

        startCamera()
    }

    override fun onPause() {
        super.onPause()
        resetTimer()
        textureConverter!!.close()
        // Hide preview display until we re-open the camera again.
        previewDisplayView.isVisible = false
    }

    private val previewSurfaceHolderCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            processor?.videoSurfaceOutput?.setSurface(holder.surface)
        }

        override fun surfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
            val cameraHelper = cameraHelper ?: throw IllegalStateException()
            val textureConverter = textureConverter ?: throw IllegalStateException()
            // (Re-)Compute the ideal size of the camera-preview display (the area that the
            // camera-preview frames get rendered onto, potentially with scaling and rotation)
            // based on the size of the SurfaceView that contains the display.
            val displaySize = cameraHelper.computeDisplaySizeFromViewSize(Size(width, height))

            // Connect the converter to the camera-preview frames as its input (via
            // previewFrameTexture), and configure the output width and height as the computed
            // display size.
            val (rotatedWidth, rotatedHeight) = if (cameraHelper.isCameraRotated) {
                displaySize.height to displaySize.width
            } else {
                displaySize.width to displaySize.height
            }
            textureConverter.setSurfaceTextureAndAttachToGLContext(previewFrameTexture, rotatedWidth, rotatedHeight)
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            processor?.videoSurfaceOutput?.setSurface(null)
        }
    }

    private fun startCamera() {
        cameraHelper = MyCameraXPreviewHelper()
            .also {
                it.onCameraStartedListener = onCameraStartedListener
                it.startCamera(
                    requireActivity(),
                    requireActivity(),
                )
            }
    }

    private val onCameraStartedListener = MyCameraXPreviewHelper.OnCameraStartedListener {
        previewFrameTexture = it
        // Make the display view visible to start showing the preview. This triggers the
        // SurfaceHolder.Callback added to (the holder of) previewDisplayView.
        previewDisplayView.visibility = View.VISIBLE
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun saveJsonLocally(outputJson: String) = GlobalScope.launch(Dispatchers.IO) {
        val fileName = "output.json"
        val file = File(requireContext().filesDir, fileName)
        if (file.exists()) {
            file.delete()
        }
        try {
            FileWriter(file).use { fileWriter ->
                fileWriter.write(outputJson)
            }
            Timber.d("HR JSON written to $fileName")
        } catch (e: IOException) {
            Timber.e(e, "Error writing to file $fileName")
        }
    }

    companion object {
        private const val CAMERA_LOCKING_TIMEOUT = 500L  // ms

        init {
            System.loadLibrary("mediapipe_jni")
        }
    }
}
