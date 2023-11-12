package com.presagetech.physiology.core.button

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.presagetech.physiology.R
import com.presagetech.physiology.core.SmartSpectra
import com.presagetech.physiology.core.result.SmartSpectraResultView
import com.presagetech.physiology.ui.WalkThroughActivity
import com.presagetech.physiology.ui.summary.UploadingFragment
import com.presagetech.physiology.utils.PreferencesUtils
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt


class SmartSpectraButton(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private var resultView: SmartSpectraResultView? = null
    private var checkupLinearLayout: LinearLayout? = null
    private var heartIcon: AppCompatImageView? = null
    private var titleTextView: AppCompatTextView? = null
    private var infoIconFrame: FrameLayout? = null
    private var infoIcon: AppCompatImageView? = null

    private val linksMap = mapOf(
        R.id.txt_terms_of_service to "https://api.physiology.presagetech.com/termsofservice",
        R.id.txt_privacy_policy to "https://api.physiology.presagetech.com/privacypolicy",
        R.id.txt_instruction_of_use to "https://api.physiology.presagetech.com/instructions ",
        R.id.txt_contact_us to "https://api.physiology.presagetech.com/contact",
        R.id.txt_contact_us to "https://api.physiology.presagetech.com/contact",
    )

    private val infoBottomSheetDialog: BottomSheetDialog by lazy {
        val dialog = BottomSheetDialog(context).also {
            it.setContentView(R.layout.info_bottom_sheet_layout)
        }
        linksMap.forEach { (id, url) ->
            dialog.findViewById<AppCompatTextView>(id)!!.setOnClickListener {
                openInWebView(url)
            }
        }
        dialog.findViewById<AppCompatTextView>(R.id.show_tutorial)!!.setOnClickListener {
            dialog.dismiss()
            infoIconFrame?.post {
                PreferencesUtils.saveBoolean(context, PreferencesUtils.Tutorial_Key, false)
                openWalkThrough(context)
            }
        }
        dialog
    }

    var cameraProcessResult: ActivityResultLauncher<Intent> =
        (context as AppCompatActivity).registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            val data = it.data
            if (it.resultCode == Activity.RESULT_OK && data != null) {
                require(data.hasExtra(UploadingFragment.RR_RESULT_KEY))
                val rr = data.getDoubleExtra(UploadingFragment.RR_RESULT_KEY, 0.0)
                require(data.hasExtra(UploadingFragment.HR_RESULT_KEY))
                val hr = data.getDoubleExtra(UploadingFragment.HR_RESULT_KEY, 0.0)
                resultView?.showData(rr.toInt(), hr.toInt())
            } else {
                resultView?.showEmptyData()
            }
        }

    init {
        gravity = Gravity.CENTER_VERTICAL
        background = ContextCompat.getDrawable(context, R.drawable.smart_spectra_button_background)

        checkupLinearLayout = LinearLayout(context).apply {

            gravity = Gravity.CENTER_VERTICAL
            heartIcon = AppCompatImageView(context).apply {
                setImageResource(R.drawable.ic_baseline_favorite_24)
            }
            addView(
                heartIcon,
                LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT
                ).apply {
                    setPadding(dpToPx(16), 0, dpToPx(16), 0)
                }
            )

            titleTextView = AppCompatTextView(context).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18f)
                text = context.getString(R.string.checkup)
                setTextColor(Color.WHITE)
            }
            addView(
                titleTextView,
                LayoutParams(
                    0,
                    LayoutParams.WRAP_CONTENT
                ).apply {
                    weight = 1f
                }
            )
        }
        addView(
            checkupLinearLayout,
            LayoutParams(
                0,
                LayoutParams.MATCH_PARENT
            ).apply {
                weight = 1f
            }
        )

        infoIconFrame = FrameLayout(context).apply {
            setPadding(dpToPx(16), 0, dpToPx(16), 0)
            background =
                ContextCompat.getDrawable(context, R.drawable.smart_spectra_info_button_background)
            infoIcon = AppCompatImageView(context).apply {
                setImageResource(R.drawable.ic_info_24)
            }
            addView(
                infoIcon,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ).apply {
                    gravity = Gravity.CENTER
                }
            )
        }
        addView(
            infoIconFrame,
            LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT
            )
        )

        setOnClickListener {
            SmartSpectra.createScreeningPage(context as AppCompatActivity, cameraProcessResult)
        }
        infoIconFrame?.doOnLayout {
            openWalkThrough(context)
        }
        infoIconFrame?.setOnClickListener {
            infoBottomSheetDialog.show()
        }
    }

    private fun openWalkThrough(context: Context) {
        if (!PreferencesUtils.getBoolean(context, PreferencesUtils.Tutorial_Key, false)) {
            val rootLocation = getViewLocation(this@SmartSpectraButton)
            val intent = Intent(context, WalkThroughActivity::class.java).apply {
                putExtra("data", generateViewsLocationJson().toString())
                putExtra("top", rootLocation.top)
                putExtra("bottom", rootLocation.bottom)
            }
            context.startActivity(intent)
        }

    }

    private fun generateViewsLocationJson(): JSONArray {
        return JSONArray().apply {
            put(
                JSONObject().apply {
                    val rect = getViewLocation(checkupLinearLayout!!)
                    put("left", rect.left)
                    put("top", rect.top)
                    put("right", rect.right)
                    put("bottom", rect.bottom)
                    put("description", resources.getString(R.string.checkup_description))
                }
            )
            put(
                JSONObject().apply {
                    val rect = getViewLocation(infoIconFrame!!)
                    put("left", rect.left)
                    put("top", rect.top)
                    put("right", rect.right)
                    put("bottom", rect.bottom)
                    put("description", resources.getString(R.string.info_description))
                }
            )
        }
    }

    private fun getViewLocation(view: View): Rect {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val x = location[0]
        val y = location[1]
        return Rect(x, y, x + view.width, y + view.height)
    }

    private fun dpToPx(dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).roundToInt()
    }

    fun setupResultView(resultView: SmartSpectraResultView) {
        this.resultView = resultView
    }

    private fun openInWebView(url: String) {
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = dpToPx(56)
        val heightSpec = MeasureSpec.makeMeasureSpec(desiredHeight, MeasureSpec.EXACTLY)
        super.onMeasure(widthMeasureSpec, heightSpec)
    }
}
