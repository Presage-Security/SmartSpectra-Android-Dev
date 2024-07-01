package com.presagetech.smartspectra

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.presagetech.smartspectra.ui.SmartSpectraActivity
import com.presagetech.smartspectra.ui.viewmodel.ScreeningViewModel

class ScreeningContractInput(
    val apiKey: String // Physiology SDK API key, find more https://physiology.presagetech.com/
)

class ScreeningContract : ActivityResultContract<ScreeningContractInput, ScreeningResult>() {
    override fun createIntent(context: Context, input: ScreeningContractInput): Intent {
        return Intent(context, SmartSpectraActivity::class.java).apply {
            putExtra(SmartSpectraActivity.EXTRA_API_KEY, input.apiKey)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): ScreeningResult {
         if (resultCode != Activity.RESULT_OK) {
             return ScreeningResult.Failed
        }
         val data = ScreeningViewModel.screeningResultHolder
             ?: throw IllegalArgumentException("Missing response")
        ScreeningViewModel.screeningResultHolder = null

        return ScreeningResult.Success(
            data.strictPulseRate,
            data.pulsePleth?.map { TraceEntry(it.first, it.second) },
            data.strictBreathingRate,
            data.breathingPleth?.map { TraceEntry(it.first, it.second) },
            data.pulseValues?.map { TraceEntry(it.first, it.second) },
            data.pulseConfidence?.map { TraceEntry(it.first, it.second) },
            data.breathingValues?.map { TraceEntry(it.first, it.second) },
            data.breathingConfidence?.map { TraceEntry(it.first, it.second) },
            data.breathingAmplitude?.map { TraceEntry(it.first, it.second) },
            data.apnea?.map { ApneaEntry(it.first, it.second) },
            data.breathingBaseline?.map { TraceEntry(it.first, it.second) },
            data.ie?.map { TraceEntry(it.first, it.second) },
            data.rrl?.map { TraceEntry(it.first, it.second) },
            data.phasic?.map { TraceEntry(it.first, it.second) },
            data.hrv?.map { TraceEntry(it.first, it.second) },
            data.version,
            data.upload_date
        )
    }
}
