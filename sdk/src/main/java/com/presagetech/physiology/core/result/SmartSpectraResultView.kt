package com.presagetech.physiology.core.result

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.presagetech.physiology.R
import kotlin.math.roundToInt

class SmartSpectraResultView(
    context: Context,
    attrs: AttributeSet?
) : LinearLayout(context, attrs) {
    fun showData(rr: Int, hr: Int) {
        descriptionTextView?.text = context.getString(R.string.rr_hr_values, rr, hr)
    }
    fun showEmptyData() {
        descriptionTextView?.setText(R.string.rr_hr_empty)
    }

    private var titleTextView: TextView? = null
    private var descriptionTextView: TextView? = null

    init {
        orientation = VERTICAL
        background =
            ContextCompat.getDrawable(context, R.drawable.smart_spectra_result_view_background)

        titleTextView = TextView(context).apply {
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            text = context.getString(R.string.hr_rr)
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18f)
            gravity = Gravity.CENTER
        }
        addView(
            titleTextView,
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(
                    dpToPx(16),
                    dpToPx(16),
                    dpToPx(16),
                    dpToPx(16)
                )
            }
        )

        descriptionTextView = TextView(context).apply {
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT
            text = context.getString(R.string.rr_hr_hint)
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
            gravity = Gravity.CENTER
        }
        addView(
            descriptionTextView,
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(
                    dpToPx(16),
                    0,
                    dpToPx(16),
                    dpToPx(16)
                )
            }
        )
    }

    private fun dpToPx(dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).roundToInt()
    }
}
