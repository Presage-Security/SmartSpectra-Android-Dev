package com.presagetech.smartspectra

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import com.presage.physiology.proto.MetricsProto.MetricsBuffer
import com.presagetech.smartspectra.ui.viewmodel.ScreeningViewModel
import org.opencv.android.OpenCVLoader
import timber.log.Timber

class SmartSpectraView (
    context: Context,
    attrs: AttributeSet?
) : LinearLayout(context, attrs) {

    private var smartSpectraButton: SmartSpectraButton
    private var resultView: SmartSpectraResultView

    private val screeningViewModel: ScreeningViewModel by lazy {
        ScreeningViewModel.getInstance()
    }

    init {
        // Set LinearLayout orientation to vertical
        orientation = VERTICAL

        // Inflate the view layout
        LayoutInflater.from(context).inflate(R.layout.view_smart_spectra, this, true)

        // Find child views
        smartSpectraButton = findViewById(R.id.smart_spectra_button)
        resultView = findViewById(R.id.result_view)

        // In case of unsupported devices
        if (!isSupportedAbi()) {
            val checkupButton = smartSpectraButton.findViewById<Button>(R.id.button_checkup)
            checkupButton.isEnabled = false
            Toast.makeText(context, "Unsupported device (ABI)", Toast.LENGTH_LONG).show()
            Timber.d("Unsupported device (ABI)")
            Timber.d("This device ABIs: ${Build.SUPPORTED_ABIS.contentToString()}")
        } else {
            // Load necessary libraries
            System.loadLibrary("mediapipe_jni")
            if (OpenCVLoader.initLocal()) {
                Timber.i("OpenCV loaded successfully");
            } else {
                Timber.e("OpenCV initialization failed!");
            }

        }
    }

    // Setter methods to allow users to configure SmartSpectraButton and SmartSpectraView
    fun setSpotTime(spotDuration: Double) {
        SmartSpectraSDKConfig.spotDuration = spotDuration
    }

    fun setShowFps(showFps: Boolean) {
        SmartSpectraSDKConfig.SHOW_FPS = showFps
    }

    fun setApiKey(apiKey: String) {
        screeningViewModel.setApiKey(apiKey)
    }

    fun setMetricsBufferObserver(observer: (MetricsBuffer) -> Unit) {
        screeningViewModel.observeMetricsBuffer(observer)
    }

    fun setMeshPointsObserver(observer: (List<Pair<Int, Int>>) -> Unit) {
        screeningViewModel.observeDenseMeshPoints(observer)
    }
    private fun isSupportedAbi(): Boolean {
        Build.SUPPORTED_ABIS.forEach {
            if (it == "arm64-v8a" || it == "armeabi-v7a") {
                return true
            }
        }
        return false
    }

}