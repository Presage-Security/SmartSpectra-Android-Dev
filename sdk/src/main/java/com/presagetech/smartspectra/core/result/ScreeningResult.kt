package com.presagetech.smartspectra.core.result

sealed class ScreeningResult {
    data class Success(
        val hrAverage: Double, // average heart rate
        val rrAverage: Double, // average respiratory rate
    ) : ScreeningResult()

    data object Failed : ScreeningResult()
}
