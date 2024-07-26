package com.presagetech.smartspectra.ui

import android.os.Bundle
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.fragment.app.Fragment
import com.google.mediapipe.components.PermissionHelper
import com.presagetech.smartspectra.R
import com.presagetech.smartspectra.network.SDKApiService
import com.presagetech.smartspectra.ui.screening.CameraProcessFragment
import com.presagetech.smartspectra.ui.screening.PermissionsRequestFragment
import com.presagetech.smartspectra.ui.summary.UploadingFragment
import com.presagetech.smartspectra.ui.viewmodel.ScreeningViewModelFactory
import timber.log.Timber

/**
 * This is the MainActivity of SmartSpectra module the project structure is base on SingleActivity
 * structure so we used navigation component to handle navigation between module Fragments.
 *
 * */
class SmartSpectraActivity : AppCompatActivity() {

    lateinit var viewModelFactory: ScreeningViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val apiKey = intent.getStringExtra(EXTRA_API_KEY) ?: throw IllegalArgumentException("API key is missing")
        viewModelFactory = ScreeningViewModelFactory(SDKApiService(apiKey))

        setContentView(R.layout.activity_main_layout_nav)
        openPermissionsFragment()
    }

    override fun onResume() {
        super.onResume()
        Timber.w("Resumed Smart Spectra Activity")
        val cameraPermissionGranted = PermissionHelper.cameraPermissionsGranted(this)
        if (cameraPermissionGranted) {
            openCameraFragment()
        } else {
            openPermissionsFragment()
        }
    }

    private fun openPermissionsFragment() {
        openFragment(PermissionsRequestFragment())
    }
    @OptIn(ExperimentalCamera2Interop::class)
    private fun openCameraFragment() {
        openFragment(CameraProcessFragment())
    }
    fun openUploadFragment() {
        openFragment(UploadingFragment())
    }
    private fun openFragment(fragment: Fragment) {
        Timber.i("Opening fragment: ${fragment::class.java.simpleName}")
        supportFragmentManager.beginTransaction()
            .replace(R.id.host_fragment, fragment)
            .commit()
    }

    companion object {
        const val EXTRA_API_KEY = "apiKey"
    }
}
