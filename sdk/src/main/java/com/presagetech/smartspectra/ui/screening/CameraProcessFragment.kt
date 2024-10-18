package com.presagetech.smartspectra.ui.screening

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.Toolbar
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.view.PreviewView
import androidx.fragment.app.Fragment
import com.google.mediapipe.components.FrameProcessor
import com.google.mediapipe.components.PermissionHelper
import com.google.mediapipe.framework.AndroidAssetUtil
import com.google.mediapipe.framework.Packet
import com.google.mediapipe.framework.PacketGetter
import com.google.mediapipe.glutil.EglManager
import com.presage.physiology.Messages
import com.presage.physiology.proto.MetricsProto.MetricsBuffer
import com.presage.physiology.proto.StatusProto
import com.presagetech.smartspectra.R
import com.presagetech.smartspectra.SmartSpectraSDKConfig
import com.presagetech.smartspectra.ui.SmartSpectraActivity
import com.presagetech.smartspectra.ui.viewmodel.ScreeningViewModel
import com.presagetech.smartspectra.utils.MyCameraXPreviewHelper
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


@ExperimentalCamera2Interop
class CameraProcessFragment : Fragment() {
    private val BINARY_GRAPH_NAME = "preprocessing_cpu_spot_json.binarypb"

    // == input streams
    private val INPUT_VIDEO_STREAM_NAME = "input_video"
    private val SELECTED_INPUT_STREAM_NAME = "start_button_pre"
    // == output streams
    private val STATUS_CODE_STREAM_NAME = "status_code"
    private val TIME_LEFT_STREAM_NAME = "time_left_s"
    private val DENSE_MESH_POINTS_STREAM_NAME = "dense_facemesh_points"
    private val METRICS_BUFFER_STREAM_NAME = "metrics_buffer"
    // == input side packets
    private val SPOT_DURATION_SIDE_PACKET_NAME = "spot_duration_s"
    private val ENABLE_BP_SIDE_PACKET_NAME = "enable_phasic_bp"
    private val MODEL_DIRECTORY_SIDE_PACKET_NAME = "model_directory"
    private val API_KEY_SIDE_PACKET_NAME = "api_key"

    private var isRecording: Boolean = false
    private var cameraHelper: MyCameraXPreviewHelper? = null

    private lateinit var timerTextView: TextView
    private lateinit var hintText: TextView
    private lateinit var recordingButton: AppCompatTextView
    private lateinit var fpsTextView: TextView
    private lateinit var previewDisplayView: PreviewView  // frames processed by MediaPipe
    private lateinit var backgroundExecutor: ExecutorService

    private val fpsTimestamps: ArrayDeque<Long> = ArrayDeque()

    // Initializes the mediapipe graph and provides access to the input/output streams
    private var eglManager: EglManager? = null
    private var processor: FrameProcessor? = null

    private var timeLeft: Double = 0.0
    @Volatile var statusCode: StatusProto.StatusCode = StatusProto.StatusCode.PROCESSING_NOT_STARTED
    private var cameraLockTimeout: Long = 0L
    private var countdownTimer: CountDownTimer? = null

    private val viewModel: ScreeningViewModel by lazy {
        ScreeningViewModel.getInstance()
    }

    private var processingState: ProcessingStatus
        get() = _processingState
        set(value) {
            _processingState = value
            onProcessingStateChanged(value)
        }
    private var buttonState: ButtonState
        get() = _buttonState
        set(value) {
            _buttonState = value
            onButtonStateChanged(value)
        }

    private var _buttonState = ButtonState.READY


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_camera_process_layout, container, false).also {
            previewDisplayView = it.findViewById(R.id.preview_view)
            timerTextView = it.findViewById(R.id.text_timer)
            hintText = it.findViewById(R.id.text_hint)
            recordingButton = it.findViewById(R.id.button_recording)
            fpsTextView = it.findViewById(R.id.fps_text_view)
        }

        val infoButton = view.findViewById<ImageButton>(R.id.info_button)
        infoButton.setOnClickListener {
            showInfoDialog()
        }

        hintText.setText(R.string.loading_hint)
        recordingButton.setOnClickListener(::recordButtonClickListener)
        previewDisplayView.visibility = View.GONE
        if (SmartSpectraSDKConfig.SHOW_FPS) {
            fpsTextView.visibility = View.VISIBLE
        }
        backgroundExecutor = Executors.newSingleThreadExecutor()

        AndroidAssetUtil.initializeNativeAssetManager(requireContext())
        eglManager = EglManager(null)
        processor = FrameProcessor(
            requireContext(),
            eglManager!!.nativeContext,
            BINARY_GRAPH_NAME,
            null,
            null,
        ).also {
            it.setVideoInputStreamCpu(INPUT_VIDEO_STREAM_NAME)
            it.setInputSidePackets(mapOf(
                SPOT_DURATION_SIDE_PACKET_NAME to it.packetCreator.createFloat64(SmartSpectraSDKConfig.spotDuration),
                ENABLE_BP_SIDE_PACKET_NAME to it.packetCreator.createBool(SmartSpectraSDKConfig.ENABLE_BP),
                MODEL_DIRECTORY_SIDE_PACKET_NAME to it.packetCreator.createString(SmartSpectraSDKConfig.MODEL_DIRECTORY),
                API_KEY_SIDE_PACKET_NAME to it.packetCreator.createString(viewModel.getApiKey()),
            ))
            it.setOnWillAddFrameListener(::handleOnWillAddFrame)
            it.addPacketCallback(TIME_LEFT_STREAM_NAME, ::handleTimeLeftPacket)
            it.addPacketCallback(STATUS_CODE_STREAM_NAME, ::handleStatusCodePacket)
            it.addPacketCallback(DENSE_MESH_POINTS_STREAM_NAME, ::handleDenseMeshPacket)
            it.addPacketCallback(METRICS_BUFFER_STREAM_NAME, ::handleMetricsBufferPacket)
            it.preheat()
        }

        view.findViewById<Toolbar>(R.id.toolbar).also {
            (requireActivity() as AppCompatActivity).setSupportActionBar(it)
            it.setNavigationIcon(R.drawable.ic_arrow_back)
            it.setNavigationOnClickListener { _ ->
                requireActivity().onBackPressedDispatcher.onBackPressed()
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
        when (buttonState) {
            ButtonState.READY -> {
                startCountdown {
                    startRecording()
                    buttonState = ButtonState.RUNNING
                    processingState = ProcessingStatus.PREPROCESSING
                }
            }
            ButtonState.COUNTDOWN -> {
                buttonState = ButtonState.READY
            }
            ButtonState.RUNNING -> {
                stopRecording()
                processingState = ProcessingStatus.IDLE
                buttonState = ButtonState.READY
            }
            ButtonState.DISABLE -> {
                Timber.d("recordButton is disabled: status code: $statusCode")
            }
        }
    }

    private fun startCountdown(onCountdownFinish: () -> Unit) {
        buttonState = ButtonState.COUNTDOWN
        countdownTimer = object : CountDownTimer(SmartSpectraSDKConfig.recordingDelay * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (buttonState == ButtonState.COUNTDOWN) {
                    val secondsLeft = (millisUntilFinished / 1000)
                    recordingButton.text = secondsLeft.toString()
                } else {
                    countdownTimer?.cancel()
                }
            }

            override fun onFinish() {
                if (buttonState == ButtonState.COUNTDOWN) {
                    // call the closure if the button state is still countdown
                    onCountdownFinish()
                }
            }
        }.start()
    }

    private fun startRecording() {
        cameraHelper?.toggleCameraControl(locked = true)
        cameraLockTimeout = SystemClock.elapsedRealtime() + CAMERA_LOCKING_TIMEOUT
        isRecording = true
        buttonState = ButtonState.RUNNING
    }

    private fun stopRecording() {
        cameraHelper?.toggleCameraControl(locked = false)
        cameraLockTimeout = 0L
        isRecording = false
        buttonState = ButtonState.READY
    }

    private fun handleOnWillAddFrame(timestamp: Long) {
        val processor = processor ?: throw IllegalStateException()
        val value = (buttonState == ButtonState.RUNNING) && SystemClock.elapsedRealtime() > cameraLockTimeout
        processor
            .graph
            .addPacketToInputStream(
                SELECTED_INPUT_STREAM_NAME, processor.packetCreator.createBool(value), timestamp
            )
    }

    private fun handleTimeLeftPacket(packet: Packet?) {
        if (packet == null) return
        timeLeft = PacketGetter.getFloat64(packet)
        timerTextView.post {
            timerTextView.text = timeLeft.toInt().toString()
        }

        if (timeLeft == 0.0 && processingState == ProcessingStatus.PREPROCESSING) {
            processingState = ProcessingStatus.PREPROCESSED
        }
    }

    private fun handleDenseMeshPacket(packet: Packet?) {
        if (packet == null) return
        val denseMeshPoints = PacketGetter.getInt16Vector(packet)
        viewModel.setDenseMeshPoints(denseMeshPoints)
    }

    private fun handleMetricsBufferPacket(packet: Packet?) {
        if (packet == null) return
        val metricsBuffer = PacketGetter.getProto(packet, MetricsBuffer.parser())

        Timber.d("Received metrics protobuf")
        Timber.d(metricsBuffer.metadata.toString())
        viewModel.setMetricsBuffer(metricsBuffer)
        processingState = ProcessingStatus.DONE
    }

    private fun handleStatusCodePacket(packet: Packet?) {
        if (packet == null) return
        val newStatusCodeMessage: StatusProto.StatusValue = PacketGetter.getProto(packet, StatusProto.StatusValue.parser())
        val newStatusCode = newStatusCodeMessage.value

        // Calculate and set FPS based on statusCode packet
        if (SmartSpectraSDKConfig.SHOW_FPS) {
            calculateAndSetFPS(packet.timestamp)
        }

        if (newStatusCode != statusCode) {
            statusCode = newStatusCode
            hintText.post {
                hintText.text = Messages.getStatusHint(statusCode)
            }
            recordingButton.post {
                buttonState = if (canRecord()) {
                    if(isRecording) {
                        ButtonState.RUNNING
                    } else {
                        ButtonState.READY
                    }
                } else {
                    ButtonState.DISABLE
                }
            }
        }
    }

    private fun calculateAndSetFPS(timestamp: Long) {
        fpsTimestamps.addLast(timestamp)
        if (fpsTimestamps.size > 10) {
            fpsTimestamps.removeFirst()
        }

        if (fpsTimestamps.size > 1) {
            val duration = timestamp - fpsTimestamps.first()
            val fps = (1_000_000.0 * (fpsTimestamps.size - 1)) / duration  // Convert interval to FPS, since timestamp is in microseconds
            val roundedFps = kotlin.math.round(fps).toInt()
            fpsTextView.post {
                fpsTextView.text = context?.getString(R.string.fps_label, roundedFps)
            }
        }
    }

    private fun showInfoDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Tip")
        builder.setMessage("Please ensure the subject’s face, shoulders, and upper chest are in view and remove any clothing that may impede visibility. Please refer to Instructions For Use for more information.")
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    override fun onResume() {
        super.onResume()

        if (!PermissionHelper.cameraPermissionsGranted(requireActivity())) {
            throw RuntimeException("Handle camera permission in host activity")
        }

        startCamera()
    }

    override fun onPause() {
        super.onPause()
        stopRecording()
        // Hide preview display until we re-open the camera again.
        previewDisplayView.visibility = View.GONE
        //release the resources
        cameraHelper?.onCameraImageProxyListener = null
        cameraHelper?.stopCamera()

        backgroundExecutor.execute {
            processor?.waitUntilIdle()
            processor?.close()
            eglManager?.release()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("camera process fragment destroyed")

        backgroundExecutor.shutdown()
        backgroundExecutor.awaitTermination(
            Long.MAX_VALUE,
            TimeUnit.NANOSECONDS
        )
    }

    private fun startCamera() {
        cameraHelper = MyCameraXPreviewHelper()
            .also {
                it.onCameraImageProxyListener = processImageFrames
                it.startCamera(
                    requireActivity(),
                    viewLifecycleOwner,
                    previewDisplayView,
                    backgroundExecutor
                )
            }
        // show preview
        previewDisplayView.visibility = View.VISIBLE
    }

    private val processImageFrames = MyCameraXPreviewHelper.OnCameraImageProxyListener { imageProxy ->
        // passing timestamp as micro-second
        imageProxy.use {
            processor?.onNewFrame(imageProxy.toBitmap(), imageProxy.imageInfo.timestamp / 1000L)
        }
    }


    private fun onProcessingStateChanged(newState: ProcessingStatus) {
        when (newState) {
            ProcessingStatus.IDLE -> {
                Timber.d("Presage Processing idle")
            }

            ProcessingStatus.PREPROCESSING -> {
                Timber.d("Presage Processing")

            }

            ProcessingStatus.PREPROCESSED -> {
                Timber.d("Presage Processed")
                // TODO: Graph needs to move to a service for this to not hang until graph is done here
                (requireActivity() as SmartSpectraActivity).openUploadFragment()
            }

            ProcessingStatus.DONE -> {
                Timber.d("Got metrics buffer.")
                requireActivity().let {
                    it.setResult(Activity.RESULT_OK)
                    it.finish()
                }

            }

            ProcessingStatus.ERROR -> {
                Timber.e("Presage Processing error")
            }
        }
    }

    private fun onButtonStateChanged(newState: ButtonState) {
        when (newState) {
            ButtonState.DISABLE -> {
                recordingButton.text = getString(R.string.record)
                recordingButton.textSize = 20.0f
                recordingButton.setBackgroundResource(R.drawable.record_background_disabled)
            }
            ButtonState.READY -> {
                recordingButton.text = getString(R.string.record)
                recordingButton.textSize = 20.0f
                recordingButton.setBackgroundResource(R.drawable.record_background)
            }
            ButtonState.COUNTDOWN -> {
                recordingButton.text = SmartSpectraSDKConfig.recordingDelay.toString()
                recordingButton.setBackgroundResource(R.drawable.record_background)
                recordingButton.textSize = 40.0f
            }
            ButtonState.RUNNING -> {
                recordingButton.text = getString(R.string.stop)
                recordingButton.textSize = 20.0f
                recordingButton.setBackgroundResource(R.drawable.record_background)
            }
        }
    }

    internal companion object {
        enum class ProcessingStatus {
            IDLE,
            PREPROCESSING,
            PREPROCESSED,
            DONE,
            ERROR
        }

        enum class ButtonState {
            DISABLE,
            READY,
            COUNTDOWN,
            RUNNING
        }
        private const val CAMERA_LOCKING_TIMEOUT = 500L  // ms

        private var _processingState = ProcessingStatus.IDLE
    }
}
