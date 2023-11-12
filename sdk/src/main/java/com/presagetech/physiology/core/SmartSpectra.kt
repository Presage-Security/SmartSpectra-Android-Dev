package com.presagetech.physiology.core

import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import com.presagetech.physiology.ui.SmartSpectraActivity


/*
* This class is SDK presenter which has function that launch SDK main page
* */
internal object SmartSpectra {

    private const val apiKey = "smart_spectra_token"

    /**
     * Use this function to open the SDK main page
     * @param context Context for stating activity
     * @param launcher ActivityResultLauncher to get the results
     * @see ActivityResultLauncher [https://developer.android.com/training/basics/intents/result]
     * */
    fun createScreeningPage(context: AppCompatActivity, launcher: ActivityResultLauncher<Intent>) {
        val appInfo = context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.GET_META_DATA
        )
        val metaData = appInfo.metaData ?: throw Exception("Failed to get meta-data from AndroidManifest.xml. Have you added the SDK key?")
        val sdkStringValue = metaData.getString(apiKey) ?: throw Exception("Failed to get SDK key from AndroidManifest.xml. Have you added the SDK key?")
        // Now you have the SDK-related string from the app's manifest
        val intent = Intent(context, SmartSpectraActivity::class.java).apply {
            putExtra("apiKey", sdkStringValue)
        }
        launcher.launch(intent)
    }
}
