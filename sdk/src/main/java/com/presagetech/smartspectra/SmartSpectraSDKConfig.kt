package com.presagetech.smartspectra

import android.util.Log

object SmartSpectraSDKConfig {
    const val SAVE_JSON = true

    // Spot duration
    private const val SPOT_DURATION_DEFAULT: Double = 30.0
    private const val SPOT_DURATION_MIN: Double = 20.0
    private const val SPOT_DURATION_MAX: Double = 120.0

    private var _spotDuration: Double = SPOT_DURATION_DEFAULT

    var spotDuration: Double
        get() = _spotDuration
        set(value) {
            if (value !in SPOT_DURATION_MIN..SPOT_DURATION_MAX) {
                Log.w("SDKConfig", "Spot duration must be between $SPOT_DURATION_MIN and $SPOT_DURATION_MAX")
                Log.w("SDKConfig", "Current Spot duration is set to: $_spotDuration")
                return
            }
            _spotDuration = value
        }

    const val ENABLE_BP = false
}
