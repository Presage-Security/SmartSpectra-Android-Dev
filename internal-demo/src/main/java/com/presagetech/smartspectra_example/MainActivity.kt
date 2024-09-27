package com.presagetech.smartspectra_example


import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

// Plotting imports
import androidx.core.view.isVisible
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.ScatterChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.ScatterData
import com.github.mikephil.charting.data.ScatterDataSet
import com.presage.physiology.proto.MetricsProto.MetricsBuffer

// SmartSpectra SDK Specific Imports
import com.presagetech.smartspectra.SmartSpectraButton
import com.presagetech.smartspectra.SmartSpectraResultView
import timber.log.Timber


class MainActivity : AppCompatActivity() {
    private lateinit var smartSpectraButton: SmartSpectraButton
    private lateinit var resultView: SmartSpectraResultView
    private lateinit var chartContainer: LinearLayout
    private lateinit var faceMeshContainer: ScatterChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setting up SmartSpectra Results/Views
        smartSpectraButton = findViewById(R.id.btn)
        resultView = findViewById(R.id.result_view)
        chartContainer = findViewById(R.id.chart_container)
        faceMeshContainer = findViewById(R.id.mesh_container)

        // Valid range for spot time is between 20.0 and 120.0
        smartSpectraButton.setSpotTime(30.0)
        smartSpectraButton.setShowFps(false)

        // Your api token from https://physiology.presagetech.com/
        smartSpectraButton.setApiKey("YOUR_API_KEY")

        // Optional: Only need to set it if you want to access face mesh points
        smartSpectraButton.setMeshPointsObserver{ meshPoints ->
            handleMeshPoints(meshPoints)
        }

        smartSpectraButton.setMetricsBufferObserver { metricsBuffer ->
            Timber.d("Printing metrics buffer from main activity")
            Timber.d(metricsBuffer.metadata.toString())
            handleMetricsBuffer(metricsBuffer)
        }
    }

    private fun handleMeshPoints(meshPoints: List<Pair<Int, Int>>) {
        Timber.d("Observed mesh points: ${meshPoints.size}")
        // TODO: Update UI or handle the points as needed

        // Reference the ScatterChart from the layout
        val chart = faceMeshContainer
        chart.isVisible = true


        // Scale the points and sort by x
        // Sorting is important here for scatter plot as unsorted points cause negative array size exception in scatter chart
        // See https://github.com/PhilJay/MPAndroidChart/issues/2074#issuecomment-239936758
        // --- Important --- we are subtracting the y points for plotting since (0,0) is top-left on the screen but bottom-left on the chart
        // --- Important --- we are subtracting the x points to mirror horizontally
        val scaledPoints = meshPoints.map { Entry(1f - it.first / 720f, 1f - it.second / 720f) }
            .sortedBy { it.x }

        // Create a dataset and add the scaled points
        val dataSet = ScatterDataSet(scaledPoints, "Mesh Points").apply {
            setDrawValues(false)
            scatterShapeSize = 15f
            setScatterShape(ScatterChart.ScatterShape.CIRCLE)
        }

        // Create ScatterData with the dataset
        val scatterData = ScatterData(dataSet)

        // Customize the chart
        chart.apply {
            data = scatterData
            axisLeft.isEnabled = false
            axisRight.isEnabled = false
            xAxis.isEnabled = false
            setTouchEnabled(false)
            description.isEnabled = false
            legend.isEnabled = false

            // Set visible range to make x and y axis have the same range

            setVisibleXRange(0f, 1f)
            setVisibleYRange(0f, 1f, YAxis.AxisDependency.LEFT)

            // Move view to the data
            moveViewTo(0f, 0f, YAxis.AxisDependency.LEFT)
        }

        // Refresh the chart
        chart.invalidate()
    }

    private fun handleMetricsBuffer(metrics: MetricsBuffer) {
        // Clear the chart container before plotting new results
        chartContainer.removeAllViews()

        // Plot the results
//        if (result is ScreeningResult.Success) {
//            result.pulsePleth?.let {
//                addChart( it.map { Entry(it.time, it.value) }, "Pulse Pleth", false)
//            }
//            result.breathingPleth?.let {
//                addChart( it.map { Entry(it.time, it.value) }, "Breathing Pleth", false)
//            }
//            result.pulseValues?.let {
//                addChart( it.map { Entry(it.time, it.value) }, "Pulse Rates", true)
//            }
//            result.pulseConfidence?.let {
//                addChart( it.map { Entry(it.time, it.value) }, "Pulse Rate Confidence", true)
//            }
//            result.hrv?.let {
//                addChart( it.map { Entry(it.time, it.value) }, "Pulse Rate Variability", true)
//            }
//
//            result.breathingValues?.let {
//                addChart( it.map { Entry(it.time, it.value) }, "Breathing Rates", true)
//            }
//            result.breathingConfidence?.let {
//                addChart( it.map { Entry(it.time, it.value) }, "Breathing Rate Confidence", true)
//            }
//            result.breathingAmplitude?.let {
//                addChart( it.map { Entry(it.time, it.value) }, "Breathing Amplitude", true)
//            }
//            result.apnea?.let {
//                addChart( it.map { Entry(it.time, if(it.value) 1f else 0f) }, "Apnea", true)
//            }
//            result.breathingBaseline?.let {
//                addChart( it.map { Entry(it.time, it.value) }, "Breathing Baseline", true)
//            }
//            result.phasic?.let {
//                addChart( it.map { Entry(it.time, it.value) }, "Phasic", true)
//            }
//            result.rrl?.let {
//                addChart( it.map { Entry(it.time, it.value) }, "RRL", true)
//            }
//            result.ie?.let {
//                addChart( it.map { Entry(it.time, it.value) }, "IE", true)
//            }
//        }
    }

    private fun addChart(entries: List<Entry>, title: String, showYTicks: Boolean) {
        val chart = LineChart(this)

        val density = resources.displayMetrics.density
        val heightInPx = (200 * density).toInt()

        chart.layoutParams = LinearLayout.LayoutParams (
            LinearLayout.LayoutParams.MATCH_PARENT,
            heightInPx
        )


        val titleView = TextView(this)
        titleView.text = title
        titleView.textSize = 18f
        titleView.gravity = Gravity.CENTER
        titleView.setTypeface(null, Typeface.BOLD)

        val xLabelView = TextView(this)
        xLabelView.setText(R.string.api_xLabel)
        xLabelView.gravity = Gravity.CENTER
        xLabelView.setPadding(0, 0, 0, 20)

        chartContainer.addView(titleView)
        chartContainer.addView(chart)
        chartContainer.addView(xLabelView)

        dataPlotting(chart, entries, showYTicks)
    }

    /**
     * Configures and displays a line chart with the provided data entries.
     * This function sets up the line chart to show a simplified and clean visualization,
     * removing unnecessary visual elements like grid lines, axis lines, labels, and legends.
     * It sets the line color to red and ensures that no markers or value texts are shown.
     *
     * @param chart The LineChart object to configure and display data on.
     * @param entries The list of Entry objects representing the data points to be plotted.
     * @param showYTicks Whether to show the Y axis ticks
     */
    private fun dataPlotting(chart: LineChart, entries: List<Entry>, showYTicks: Boolean) {
        val dataSet = LineDataSet(entries, "Data")

        // Clean up line
        dataSet.setDrawValues(false)
        dataSet.setDrawCircles(false)
        dataSet.color = Color.RED

        chart.data = LineData(dataSet)

        // x axis setup
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.setDrawAxisLine(true)
        chart.xAxis.granularity = 1.0f


        // y axis setup
        chart.axisLeft.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
        chart.axisLeft.setDrawZeroLine(false)
        chart.axisLeft.setDrawGridLines(false)
        chart.axisLeft.setDrawAxisLine(true)
        chart.axisLeft.setDrawLabels(showYTicks)

        // chart specific setup
        chart.axisRight.isEnabled = false
        chart.legend.isEnabled = false
        chart.description.isEnabled = false
        chart.onTouchListener = null
        chart.invalidate()

    }

}
