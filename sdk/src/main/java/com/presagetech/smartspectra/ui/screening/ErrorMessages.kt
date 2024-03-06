package com.presagetech.smartspectra.ui.screening

import androidx.annotation.StringRes
import com.presagetech.smartspectra.R

object ErrorMessages {
    val map = mapOf<Int, @receiver:StringRes Int>(
        0 to R.string.all_good,
        1 to R.string.no_face_found,
        2 to R.string.too_many_face,
        3 to R.string.face_distance,
        4 to R.string.face_is_not_centered,
        5 to R.string.image_too_dark,
        6 to R.string.image_too_bright,
        7 to R.string.not_enough_chest,
    )
}
