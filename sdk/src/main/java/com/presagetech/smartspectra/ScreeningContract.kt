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
        return if (resultCode == Activity.RESULT_OK) {
            requireNotNull(intent)
            @Suppress("DEPRECATION")
            val data = intent.getParcelableExtra(SmartSpectraActivity.RESULT_DATA_KEY)
                    as? ScreeningViewModel.RetrievedData
                ?: throw IllegalArgumentException("Missing response")

            ScreeningResult.Success(
                data.hrAverage,
                data.hrTrace?.map { TraceEntry(it.first, it.second) },
                data.rrAverage,
                data.rrTrace?.map { TraceEntry(it.first, it.second) },
                data.hrVals?.map { TraceEntry(it.first, it.second) },
                data.hrConfidence?.map { TraceEntry(it.first, it.second) },
                data.rrVals?.map { TraceEntry(it.first, it.second) },
                data.rrConfidence?.map { TraceEntry(it.first, it.second) },
                data.amplitude?.map { TraceEntry(it.first, it.second) },
                data.apnea?.map { ApneaEntry(it.first, it.second) },
                data.baseline?.map { TraceEntry(it.first, it.second) },
                data.ie?.map { TraceEntry(it.first, it.second) },
                data.rrl?.map { TraceEntry(it.first, it.second) },
                data.phasic?.map { TraceEntry(it.first, it.second) },
                data.hrv?.map { TraceEntry(it.first, it.second) },
                data.version,
                data.upload_date
                )
        } else {
            ScreeningResult.Failed
        }
    }
}
