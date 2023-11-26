package com.presagetech.physiology.core.result

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.presagetech.physiology.R
import kotlin.math.roundToInt

class SmartSpectraResultView(
    context: Context,
    attrs: AttributeSet?
) : LinearLayout(context, attrs), SmartSpectraResultListener {
    private var descriptionTextView: TextView

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
        val rr = result.rrAverage.roundToInt()
        val hr = result.hrAverage.roundToInt()
        descriptionTextView.text = context.getString(R.string.rr_hr_values, rr, hr)
    }

    private fun failed() {
        descriptionTextView.setText(R.string.rr_hr_empty)
    }
}
