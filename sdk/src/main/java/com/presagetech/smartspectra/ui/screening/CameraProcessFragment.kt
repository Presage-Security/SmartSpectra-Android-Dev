package com.presagetech.smartspectra.ui.screening

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.Toolbar
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.view.PreviewView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.mediapipe.components.FrameProcessor
import com.google.mediapipe.components.PermissionHelper
import com.google.mediapipe.framework.AndroidAssetUtil
import com.google.mediapipe.framework.Packet
import com.google.mediapipe.framework.PacketGetter
import com.google.mediapipe.glutil.EglManager
import com.presage.physiology.Messages
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
    private val INPUT_VIDEO_STREAM_NAME = "input_video"
    private val SELECTED_INPUT_STREAM_NAME = "start_button_pre"
    private val OUTPUT_DATA_STREAM_NAME = "json_data"
    private val STATUS_CODE_STREAM_NAME = "status_code"

    private val SPOT_DURATION_SIDE_PACKET_NAME = "spot_duration_s"
    private val ENABLE_BP_SIDE_PACKET_NAME = "enable_phasic_bp"

    private val TIME_LEFT_STREAM_NAME = "time_left_s"

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
            previewDisplayView = it.findViewById(R.id.preview_view)
            timerTextView = it.findViewById(R.id.text_timer)
            hintText = it.findViewById(R.id.text_hint)
            recordingButton = it.findViewById(R.id.button_recording)
            fpsTextView = it.findViewById(R.id.fps_text_view)
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
                ENABLE_BP_SIDE_PACKET_NAME to it.packetCreator.createBool(SmartSpectraSDKConfig.ENABLE_BP)
            ))
            it.setOnWillAddFrameListener(::handleOnWillAddFrame)
            it.addPacketCallback(TIME_LEFT_STREAM_NAME, ::handleTimeLeftPacket)
            it.addPacketCallback(OUTPUT_DATA_STREAM_NAME, ::handleJsonDataPacket)
            it.addPacketCallback(STATUS_CODE_STREAM_NAME, ::handleStatusCodePacket)
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
        cameraHelper?.toggleCameraControl(locked = true)
        cameraLockTimeout = SystemClock.elapsedRealtime() + CAMERA_LOCKING_TIMEOUT
        isRecording = true
        recordingButton.setText(R.string.stop)
    }

    private fun resetTimer() {
        cameraHelper?.toggleCameraControl(locked = false)
        cameraLockTimeout = 0L
        isRecording = false
        recordingButton.setText(R.string.record)
    }

    private fun handleOnWillAddFrame(timestamp: Long) {
        val processor = processor ?: throw IllegalStateException()
        val value = isRecording && SystemClock.elapsedRealtime() > cameraLockTimeout
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
        packet.release()
    }

    private fun handleJsonDataPacket(packet: Packet?) {
        if (packet == null) return
        val outputJson = PacketGetter.getJson(packet)
        viewModel.setJsonData(requireContext(), outputJson)
        (requireActivity() as SmartSpectraActivity).openUploadFragment()
        packet.release()
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
                if (canRecord()) {
                    recordingButton.setBackgroundResource(R.drawable.record_background)
                    if(isRecording) {
                        recordingButton.setText(R.string.stop)
                    }
                } else {
                    recordingButton.setBackgroundResource(R.drawable.record_background_disabled)
                    recordingButton.setText(R.string.record)
                }
            }
        }

        packet.release()
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

    override fun onResume() {
        super.onResume()

        if (!PermissionHelper.cameraPermissionsGranted(requireActivity())) {
            throw RuntimeException("Handle camera permission in host activity")
        }

        startCamera()
    }

    override fun onPause() {
        super.onPause()
        resetTimer()
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

    companion object {
        private const val CAMERA_LOCKING_TIMEOUT = 500L  // ms
    }
}
