package com.presagetech.smartspectra.ui

import android.content.res.Resources
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import com.presagetech.smartspectra.R
import com.presagetech.smartspectra.utils.PreferencesUtils
import org.json.JSONArray


class WalkThroughActivity : AppCompatActivity() {

    private var walkThroughLayout: WalkThroughLayout? = null
    private var textContainerFrameLayout: FrameLayout? = null
    private var textView: AppCompatTextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_walkthrough)

        walkThroughLayout = findViewById(R.id.walk_through)
        textContainerFrameLayout = findViewById(R.id.overlay_frame)
        textView = AppCompatTextView(this).apply {
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        }

        val statusBarHeight = calculateStatusBarHeight()
        val params = intent.getParcelableExtra<WalkActivityParams>(EXTRA_PARAMS)!!
        val maskedViews = listOf(
            getString(R.string.checkup_description) to Rect(
                params.checkupPosition.left,
                params.checkupPosition.top - statusBarHeight,
                params.checkupPosition.right,
                params.checkupPosition.bottom - statusBarHeight
            ),
            getString(R.string.info_description) to Rect(
                params.infoPosition.left,
                params.infoPosition.top - statusBarHeight,
                params.infoPosition.right,
                params.infoPosition.bottom - statusBarHeight
            ),
        )
        calculateTextViewPosition(params.rootPosition.top, params.rootPosition.bottom)
        startMasking(maskedViews)
    }

    private fun calculateStatusBarHeight(): Int {
        var statusBarHeight = 0
        val resources: Resources = Resources.getSystem()
        val resourceId: Int = resources.getIdentifier("status_bar_height", "dimen", "android")

        statusBarHeight = if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else {
            // Fallback method for getting status bar height
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 24f, resources.displayMetrics
            ).toInt()
        }
        return statusBarHeight
    }

    private fun convertInputJsonToMaskedViewArray(
        jsonString: String?, statusBarHeight: Int
    ): ArrayList<Pair<String, Rect>> {
        val maskedViews = ArrayList<Pair<String, Rect>>()

        val jsonArray = JSONArray(jsonString)
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(
                i
            )
            maskedViews.add(
                Pair(
                    jsonObject.getString("description"), Rect(
                        jsonObject.getInt("left"),
                        jsonObject.getInt("top") - statusBarHeight,
                        jsonObject.getInt("right"),
                        jsonObject.getInt("bottom") - statusBarHeight
                    )
                )
            )
        }
        return maskedViews
    }

    private fun calculateTextViewPosition(topOfView: Int, bottomOfView: Int) {
        val screenHeight = getScreenHeight()
        if (topOfView > screenHeight - bottomOfView) {
            textContainerFrameLayout?.addView(textView, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                setMargins(
                    0, 0, 0, screenHeight - topOfView + (bottomOfView - topOfView)
                )
            })
        } else {
            textContainerFrameLayout?.addView(textView, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                setMargins(
                    0, bottomOfView, 0, 0
                )
            })
        }
    }

    private fun startMasking(maskedViews: List<Pair<String, Rect>>) {
        var counter = 0
        setupMask(maskedViews[0])
        walkThroughLayout?.setOnClickListener {
            counter++
            if (counter < maskedViews.size) {
                setupMask(maskedViews[counter])
            } else {
                PreferencesUtils.saveBoolean(this, PreferencesUtils.Tutorial_Key, true)
                finish()
            }
        }
    }

    private fun setupMask(pair: Pair<String, Rect>) {
        walkThroughLayout?.maskRect(pair.second)
        textView?.text = pair.first
    }

    private fun getScreenHeight(): Int {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }

    companion object {
        const val EXTRA_PARAMS = "params"
    }
}
