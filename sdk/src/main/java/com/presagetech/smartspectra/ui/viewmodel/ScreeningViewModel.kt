package com.presagetech.smartspectra.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.presage.physiology.proto.MetricsProto.MetricsBuffer


internal class ScreeningViewModel private constructor() : ViewModel() {
    private lateinit var apiKey: String

    private val _denseMeshPoints = MutableLiveData<List<Pair<Int, Int>>>()
    val denseMeshPoints: LiveData<List<Pair<Int, Int>>> = _denseMeshPoints

    private val _metricsBuffer = MutableLiveData<MetricsBuffer>()
    val metricsBuffer: LiveData<MetricsBuffer> = _metricsBuffer

    internal fun getApiKey(): String {
        if (!::apiKey.isInitialized) {
            throw IllegalStateException("API key is not initialized. Use .setApiKey() method on SmartSpectraButton to set the key")
        }
        return apiKey
    }

    internal fun setApiKey(apiKey: String) {
        this.apiKey = apiKey
    }

    fun setDenseMeshPoints(points: ShortArray) {
        val unflattenedPoints = ArrayList<Pair<Int, Int>>(points.size / 2)
        for (i in points.indices step 2) {
            unflattenedPoints.add(Pair(points[i].toInt(), points[i + 1].toInt()))
        }
        _denseMeshPoints.postValue(unflattenedPoints)
    }

    fun observeDenseMeshPoints(observer: (List<Pair<Int, Int>>) -> Unit) {
        denseMeshPoints.observeForever(observer)
    }

    fun setMetricsBuffer(metricsBuffer: MetricsBuffer) {
        _metricsBuffer.postValue(metricsBuffer)
    }

    fun observeMetricsBuffer(observer: (MetricsBuffer) -> Unit) {
        metricsBuffer.observeForever(observer)
    }

    internal companion object {
        @Volatile
        private var INSTANCE: ScreeningViewModel? = null

        fun getInstance(): ScreeningViewModel {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ScreeningViewModel().also { INSTANCE = it }
            }
        }
    }
}
