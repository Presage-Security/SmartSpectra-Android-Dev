package com.presagetech.smartspectra.ui.summary

sealed class UploadingState {
    data class Uploading(val progress: Float) : UploadingState()
    data object Processing : UploadingState()
    data object Failed : UploadingState()
}
