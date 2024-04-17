package com.presagetech.smartspectra

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt
import org.json.JSONObject

class SmartSpectraResultView(
    context: Context,
    attrs: AttributeSet?
) : LinearLayout(context, attrs), SmartSpectraResultListener {
    private var descriptionTextView: TextView
    var callback: SmartSpectraResultsCallback? = null

    init {
        orientation = VERTICAL
        background =
            ContextCompat.getDrawable(context, R.drawable.smart_spectra_result_view_background)

        LayoutInflater.from(context).inflate(R.layout.view_result, this, true)
        descriptionTextView = findViewById(R.id.tv_description)
    }

    override fun onResult(result: ScreeningResult) {
        when (result) {
            is ScreeningResult.Success -> success(result)
            is ScreeningResult.Failed -> failed()
        }
    }

    private fun success(result: ScreeningResult.Success) {
        val strictBreathingRate = result.rrAverage.roundToInt()
        val strictPulseRate = result.hrAverage.roundToInt()
        val stringMetrics = result.jsonMetrics
        descriptionTextView.text = context.getString(R.string.rr_hr_values,
            strictBreathingRate, strictPulseRate)
        val jsonMetrics = JSONObject(stringMetrics)
        callback?.onMetricsJsonReceive(jsonMetrics)
        callback?.onStrictPuleRateReceived(strictPulseRate)
        callback?.onStrictBreathingRateReceived(strictBreathingRate)
    }

    private fun failed() {
        descriptionTextView.setText(R.string.rr_hr_empty)
    }
    interface SmartSpectraResultsCallback {
        fun onMetricsJsonReceive(jsonMetrics: JSONObject)
        fun onStrictPuleRateReceived(strictPulseRate: Int)
        fun onStrictBreathingRateReceived(strictBreathingRate: Int)
    }

}

