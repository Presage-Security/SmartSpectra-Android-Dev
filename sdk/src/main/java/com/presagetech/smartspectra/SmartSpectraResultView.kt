package com.presagetech.smartspectra

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt

class SmartSpectraResultView(
    context: Context,
    attrs: AttributeSet?
) : LinearLayout(context, attrs), SmartSpectraResultListener {
    private var resultTextView: TextView

    init {
        orientation = VERTICAL
        background =
            ContextCompat.getDrawable(context, R.drawable.smart_spectra_result_view_background)

        LayoutInflater.from(context).inflate(R.layout.view_result, this, true)
        resultTextView = findViewById(R.id.result_text)
    }

    override fun onResult(result: ScreeningResult) {
        when (result) {
            is ScreeningResult.Success -> success(result)
            is ScreeningResult.Failed -> failed()
        }
    }

    private fun success(result: ScreeningResult.Success) {
        val strictBreathingRate = result.strictBreathingRate.roundToInt()
        val strictPulseRate = result.strictPulseRate.roundToInt()

        if (strictBreathingRate == 0 || strictPulseRate == 0) {
            failed()
            return
        }
        resultTextView.text = context.getString(R.string.result_label,
            strictBreathingRate, strictPulseRate)
    }

    private fun failed() {
        resultTextView.text = context.getString(R.string.measurement_failed_hint)
    }
}

