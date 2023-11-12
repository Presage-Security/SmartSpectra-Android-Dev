package com.presagetech.physiology.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.presagetech.physiology.network.SDKApiService

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
