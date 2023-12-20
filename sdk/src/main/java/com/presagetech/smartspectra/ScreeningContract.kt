package com.presagetech.smartspectra

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.presagetech.smartspectra.ui.SmartSpectraActivity

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
            require(intent.hasExtra(SmartSpectraActivity.RESULT_HR_KEY))
            require(intent.hasExtra(SmartSpectraActivity.RESULT_RR_KEY))
            ScreeningResult.Success(
                intent.getDoubleExtra(SmartSpectraActivity.RESULT_HR_KEY, 0.0),
                intent.getDoubleExtra(SmartSpectraActivity.RESULT_RR_KEY, 0.0)
            )
        } else {
            ScreeningResult.Failed
        }
    }
}
