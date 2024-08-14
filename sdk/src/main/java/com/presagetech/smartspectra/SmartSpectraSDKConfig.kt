package com.presagetech.smartspectra

import timber.log.Timber

internal object SmartSpectraSDKConfig {
    var SAVE_JSON = false
    var SHOW_FPS = false

    // Spot duration
    private const val SPOT_DURATION_DEFAULT: Double = 30.0
    private const val SPOT_DURATION_MIN: Double = 20.0
    private const val SPOT_DURATION_MAX: Double = 120.0

    private var _spotDuration: Double = SPOT_DURATION_DEFAULT

    var spotDuration: Double
        get() = _spotDuration
        set(value) {
            if (value !in SPOT_DURATION_MIN..SPOT_DURATION_MAX) {
                Timber.w("Spot duration must be between $SPOT_DURATION_MIN and $SPOT_DURATION_MAX \nCurrent Spot duration is set to: $_spotDuration")
                return
            }
            _spotDuration = value
        }

    const val ENABLE_BP = false
}
