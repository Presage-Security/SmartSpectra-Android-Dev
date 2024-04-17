package com.presagetech.internal_demo

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.presagetech.smartspectra.SmartSpectraButton
import com.presagetech.smartspectra.SmartSpectraResultView
import org.json.JSONObject
import timber.log.Timber

class MainActivity : AppCompatActivity(), SmartSpectraResultView.SmartSpectraResultsCallback {
    private lateinit var tokenEditText: EditText
    private lateinit var smartSpectraButton: SmartSpectraButton
    private lateinit var chart: LineChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        smartSpectraButton = findViewById(R.id.btn)
        val resultView = findViewById<SmartSpectraResultView>(R.id.result_view)
        resultView.callback = this
        smartSpectraButton.setResultListener(resultView)
        chart = findViewById(R.id.chart)

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
        plethPlotting(jsonMetrics)
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

    private fun plethPlotting(jsonMetrics: JSONObject) {
        val plethJson = jsonMetrics.getJSONObject("pulse").getJSONObject("hr_trace")
        val entries = mutableListOf<Entry>()

        val iterator = plethJson.keys()
        while (iterator.hasNext()) {
            val time = iterator.next() // This is the string key
            val value = plethJson.getJSONObject(time).getDouble("value")
            entries.add(Entry(time.toFloat(), value.toFloat()))
        }

        // Sort the entries based on the X value (time)
        entries.sortBy { it.x }


        val dataSet = LineDataSet(entries, "Pleth")

        // Clean up line
        dataSet.setDrawValues(false)
        dataSet.setDrawCircles(false)
        dataSet.color = Color.RED


        val lineData = LineData(dataSet)
        // clean up chart
        val xAxis = chart.xAxis
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(false)
        xAxis.setDrawLabels(false)
        val leftAxis = chart.axisLeft
        leftAxis.setDrawGridLines(false)
        leftAxis.setDrawAxisLine(false)
        leftAxis.setDrawLabels(false)
        val rightAxis = chart.axisRight
        rightAxis.setDrawGridLines(false)
        rightAxis.setDrawAxisLine(false)
        rightAxis.setDrawLabels(false)
        chart.legend.isEnabled = false
        chart.description.isEnabled = false

        chart.data = lineData
        chart.invalidate() // refresh the chart
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
