package com.presagetech.smartspectra

sealed class ScreeningResult {
    data class Success(
        val hrAverage: Double, // average heart rate
        val rrAverage: Double, // average respiratory rate
        val jsonMetrics: String,
    ) : ScreeningResult()

    data object Failed : ScreeningResult()
}
