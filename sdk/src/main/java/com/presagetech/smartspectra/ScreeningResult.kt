package com.presagetech.smartspectra

import com.presagetech.smartspectra.ui.viewmodel.ScreeningViewModel
import org.json.JSONObject
import java.time.ZonedDateTime

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

        val hrVals: List<TraceEntry>?,

        val hrConfidence: List<TraceEntry>?,

        val rrVals: List<TraceEntry>?,

        val rrConfidence: List<TraceEntry>?,

        val amplitude: List<TraceEntry>?,

        val apnea: List<ApneaEntry>?,

        val baseline: List<TraceEntry>?,

        val ie: List<TraceEntry>?,

        val rrl: List<TraceEntry>?,

        val phasic: List<TraceEntry>?,

        val hrv: List<TraceEntry>?,

        val version: String,

        val upload_date: ZonedDateTime
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

            require(baseline == null || baseline.isNotEmpty() && baseline.isSorted())
            baseline?.forEach { require(it.time.isFinite() && it.value.isFinite()) }

            require(rrVals == null || rrVals.isNotEmpty() && rrVals.isSorted())
            rrVals?.forEach { require(it.time.isFinite() && it.value.isFinite()) }

            require(rrConfidence == null || rrConfidence.isNotEmpty() && rrConfidence.isSorted())
            rrConfidence?.forEach { require(it.time.isFinite() && it.value.isFinite()) }

            require(hrVals == null || hrVals.isNotEmpty() && hrVals.isSorted())
            hrVals?.forEach { require(it.time.isFinite() && it.value.isFinite()) }

            require(hrConfidence == null || hrConfidence.isNotEmpty() && hrConfidence.isSorted())
            hrConfidence?.forEach { require(it.time.isFinite() && it.value.isFinite()) }

            require(amplitude == null || amplitude.isNotEmpty() && amplitude.isSorted())
            amplitude?.forEach { require(it.time.isFinite() && it.value.isFinite()) }

            require(ie == null || ie.isNotEmpty() && ie.isSorted())
            ie?.forEach { require(it.time.isFinite() && it.value.isFinite()) }

            require(rrl == null || rrl.isNotEmpty() && rrl.isSorted())
            rrl?.forEach { require(it.time.isFinite() && it.value.isFinite()) }

            require(phasic == null || phasic.isNotEmpty() && phasic.isSorted())
            phasic?.forEach { require(it.time.isFinite() && it.value.isFinite()) }

            require(hrv == null || hrv.isNotEmpty() && hrv.isSorted())
            hrv?.forEach { require(it.time.isFinite() && it.value.isFinite()) }

            require(apnea == null || apnea.isNotEmpty() && apnea.sorted())
            apnea?.forEach { require(it.time.isFinite()) }
        }
    }

    data object Failed : ScreeningResult()
}

class TraceEntry(
    val time: Float, // time in seconds
    val value: Float, // heart rate value
)

class ApneaEntry(
    val time: Float, // time in seconds
    val value: Boolean, // heart rate value
)

private fun List<TraceEntry>.isSorted() = zipWithNext { a, b -> a.time <= b.time }.all { it }
private fun List<ApneaEntry>.sorted() = zipWithNext { a, b -> a.time <= b.time }.all { it }