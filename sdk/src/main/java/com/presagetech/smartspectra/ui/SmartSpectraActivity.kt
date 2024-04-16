package com.presagetech.smartspectra.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
@SuppressLint("UnsafeOptInUsageError")
class SmartSpectraActivity : AppCompatActivity() {

    lateinit var viewModelFactory: ScreeningViewModelFactory

    private var currentFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val apiKey = intent.getStringExtra(EXTRA_API_KEY) ?: throw IllegalArgumentException("API key is missing")
        viewModelFactory = ScreeningViewModelFactory(SDKApiService(apiKey))

        setContentView(R.layout.activity_main_layout_nav)
        openPermissionsFragment()
    }

    override fun onResume() {
        super.onResume()
        val cameraPermissionGranted = PermissionHelper.cameraPermissionsGranted(this)
        if (cameraPermissionGranted) {
            if (currentFragment is PermissionsRequestFragment) {
                openCameraFragment()
            }
        } else {
            if (currentFragment !is PermissionsRequestFragment) {
                openPermissionsFragment()
            }
        }
    }

    fun openPermissionsFragment() {
        openFragment(PermissionsRequestFragment())
    }
    fun openCameraFragment() {
        openFragment(CameraProcessFragment())
    }
    fun openUploadFragment() {
        openFragment(UploadingFragment())
    }
    private fun openFragment(fragment: Fragment) {
        Timber.i("Opening fragment: ${fragment::class.java.simpleName}")
        supportFragmentManager.beginTransaction().replace(
            R.id.host_fragment, fragment
        ).commit()
        currentFragment = fragment
    }

    companion object {
        const val EXTRA_API_KEY = "apiKey"
        const val RESULT_HR_KEY = "hr"
        const val RESULT_RR_KEY = "rr"
        const val JSON_METRICS = "jsonMetrics"
    }
}
