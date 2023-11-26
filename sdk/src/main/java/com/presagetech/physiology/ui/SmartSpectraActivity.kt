package com.presagetech.physiology.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.presagetech.physiology.R
import com.presagetech.physiology.network.SDKApiService
import com.presagetech.physiology.ui.screening.CameraProcessFragment
import com.presagetech.physiology.ui.summary.UploadingFragment
import com.presagetech.physiology.ui.viewmodel.ScreeningViewModelFactory

/**
 * This is the MainActivity of SmartSpectra module the project structure is base on SingleActivity
 * structure so we used navigation component to handle navigation between module Fragments.
 *
 * */
class SmartSpectraActivity : AppCompatActivity() {

    private lateinit var apiKey: String
    lateinit var viewModelFactory: ScreeningViewModelFactory

    private val cameraProcessFragment by lazy {
        CameraProcessFragment().apply {
            setOpenUploadFragment {
                supportFragmentManager.beginTransaction().replace(
                    R.id.host_fragment, uploadingFragment
                ).commit()
            }
        }
    }

    private val uploadingFragment by lazy {
        UploadingFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        apiKey = intent.getStringExtra(EXTRA_API_KEY) ?: throw IllegalArgumentException("API key is missing")
        viewModelFactory = ScreeningViewModelFactory(SDKApiService(apiKey))

        setContentView(R.layout.activity_main_layout_nav)
        supportFragmentManager.beginTransaction().replace(
            R.id.host_fragment, cameraProcessFragment
        ).commit()
    }

    companion object {
        const val EXTRA_API_KEY = "apiKey"
        const val RESULT_HR_KEY = "hr"
        const val RESULT_RR_KEY = "rr"
    }
}
