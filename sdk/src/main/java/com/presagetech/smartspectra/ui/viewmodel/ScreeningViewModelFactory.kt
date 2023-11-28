package com.presagetech.smartspectra.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.presagetech.smartspectra.network.SDKApiService

class ScreeningViewModelFactory(private val sdkApiService: SDKApiService) :
    ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(ScreeningViewModel::class.java) -> {
                ScreeningViewModel(sdkApiService) as T
            }
            else -> throw IllegalArgumentException()
        }
    }
}
