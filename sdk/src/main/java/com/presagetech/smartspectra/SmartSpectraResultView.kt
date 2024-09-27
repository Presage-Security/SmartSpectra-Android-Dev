package com.presagetech.smartspectra

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.presage.physiology.proto.MetricsProto.MetricsBuffer
import com.presagetech.smartspectra.ui.viewmodel.ScreeningViewModel
import timber.log.Timber
import kotlin.math.roundToInt

class SmartSpectraResultView(
    context: Context,
    attrs: AttributeSet?
) : LinearLayout(context, attrs) {
    private var resultTextView: TextView
    private val viewModel: ScreeningViewModel by lazy {
        ScreeningViewModel.getInstance()
    }

    init {
        orientation = VERTICAL
        background =
            ContextCompat.getDrawable(context, R.drawable.smart_spectra_result_view_background)

        LayoutInflater.from(context).inflate(R.layout.view_result, this, true)
        resultTextView = findViewById(R.id.result_text)

        viewModel.observeMetricsBuffer { metricsBuffer ->
            updateResultText(metricsBuffer)
        }
    }

    private fun updateResultText(metricsBuffer: MetricsBuffer) {
        val strictPulseRate = metricsBuffer.pulse.strict.value.roundToInt()
        val strictBreathingRate = metricsBuffer.breathing.strict.value.roundToInt()
        if (strictPulseRate == 0 || strictBreathingRate == 0) {
            Timber.w("Measurement insufficent for strict. Strict Pulse rate: ${strictPulseRate}, Strict Breathing rate: ${strictBreathingRate}")
            resultTextView.text = context.getString(R.string.measurement_failed_hint)
        } else {
            resultTextView.text = context.getString(R.string.result_label,
                strictBreathingRate, strictPulseRate)
        }
    }
}

