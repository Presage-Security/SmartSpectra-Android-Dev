package com.presagetech.internal_demo

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import com.presagetech.smartspectra.SmartSpectraButton
import com.presagetech.smartspectra.SmartSpectraResultView
import org.json.JSONObject
import timber.log.Timber

class MainActivity : AppCompatActivity(), SmartSpectraResultView.SmartSpectraResultsCallback {
    private lateinit var tokenEditText: EditText
    private lateinit var smartSpectraButton: SmartSpectraButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        smartSpectraButton = findViewById(R.id.btn)
        val resultView = findViewById<SmartSpectraResultView>(R.id.result_view)
        resultView.callback = this
        smartSpectraButton.setResultListener(resultView)

        tokenEditText = findViewById(R.id.text_api_token)
        tokenEditText.setOnEditorActionListener { _, _, _ ->
            val token = tokenEditText.text.toString().trim()
            saveToken(token)
            smartSpectraButton.setApiKey(token)
            true
        }
        
        val storedToken = loadToken()
        smartSpectraButton.setApiKey(storedToken)
        tokenEditText.setText(storedToken)

        if (!isSupportedAbi()) {
            smartSpectraButton.isEnabled = false
            tokenEditText.isEnabled = false
            Toast.makeText(this, "Unsupported device (ABI)", Toast.LENGTH_LONG).show()
            Timber.d("Unsupported device (ABI)")
            Timber.d("This device ABIs: ${Build.SUPPORTED_ABIS.contentToString()}")
        }
    }
    override fun onMetricsJsonReceive(jsonMetrics: JSONObject) {
        // Here you can handle the received Metrics JSON
        Timber.d("Received JSON data: $jsonMetrics")
    }
    override fun onStrictPuleRateReceived(strictPulseRate: Int) {
        // Here you can handle the received strict Pulse Rate in beats per minute
        Timber.d("Received JSON data: $strictPulseRate")
    }
    override fun onStrictBreathingRateReceived(strictBreathingRate: Int) {
        // Here you can handle the received strict Breathing Rate in beats per minute
        Timber.d("Received JSON data: $strictBreathingRate")
    }

    private fun isSupportedAbi(): Boolean {
        Build.SUPPORTED_ABIS.forEach {
            if (it == "arm64-v8a" || it == "armeabi-v7a") {
                return true
            }
        }
        return false
    }

    private fun loadToken(): String {
        return getSharedPreferences(TOKEN_SHARED_PREFERENCES, MODE_PRIVATE)
            .getString(TOKEN_SHARED_PREFERENCES, null) ?: ""
    }

    private fun saveToken(token: String) {
        getSharedPreferences(TOKEN_SHARED_PREFERENCES, MODE_PRIVATE).edit().apply {
            putString(TOKEN_SHARED_PREFERENCES, token)
            apply()
        }
    }

    companion object {
        const val TOKEN_SHARED_PREFERENCES = "demo_api_key"
    }
}
