package com.presagetech.smartspectra

sealed class ScreeningResult {
    data class Success(
        // Strict pulse rates are the average of only high confidence pulse rate values.
        val hrAverage: Double, // beats per minute

        // Heart rate trace is a list sorted by time in seconds.
        val hrTrace: List<TraceEntry>?,

        // Strict breathing rates are the average of only high confidence breathing rate values.
        val rrAverage: Double, // breaths per minute

        // Breathing rate trace is a list sorted by time in seconds.
        val rrTrace: List<TraceEntry>?,
    ) : ScreeningResult() {
        init {
            // Ensure that double values are finite and non-negative.
            // And that traces if present are sorted by time.

            require(hrAverage.isFinite() && hrAverage >= 0.0)
            require(hrTrace == null || hrTrace.isNotEmpty() && hrTrace.isSorted())
            hrTrace?.forEach { require(it.time.isFinite() && it.value.isFinite()) }

            require(rrAverage.isFinite() && rrAverage >= 0.0)
            require(rrTrace == null || rrTrace.isNotEmpty() && rrTrace.isSorted())
            rrTrace?.forEach { require(it.time.isFinite() && it.value.isFinite()) }
        }
    }

    data object Failed : ScreeningResult()
}

class TraceEntry(
    val time: Float, // time in seconds
    val value: Float, // heart rate value
)

private fun List<TraceEntry>.isSorted() = zipWithNext { a, b -> a.time <= b.time }.all { it }
