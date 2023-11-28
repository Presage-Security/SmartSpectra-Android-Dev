package com.presagetech.smartspectra.core.button

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.net.Uri
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.presagetech.smartspectra.R
import com.presagetech.smartspectra.core.ScreeningContract
import com.presagetech.smartspectra.core.ScreeningContractInput
import com.presagetech.smartspectra.core.result.SmartSpectraResultListener
import com.presagetech.smartspectra.ui.WalkThroughActivity
import com.presagetech.smartspectra.utils.PreferencesUtils
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt


class SmartSpectraButton(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val BASE_URL = "https://api.physiology.presagetech.com"
    private val linksMap = mapOf(
        R.id.txt_terms_of_service to "$BASE_URL/termsofservice",
        R.id.txt_privacy_policy to "$BASE_URL/privacypolicy",
        R.id.txt_instruction_of_use to "$BASE_URL/instructions ",
        R.id.txt_contact_us to "$BASE_URL/contact",
        R.id.txt_contact_us to "$BASE_URL/contact",
    )

    private var checkupButton: View
    private var infoButton: View

    private var tutorialHasBeenShown: Boolean
    private var apiKey: String? = null
    private var resultListener: SmartSpectraResultListener? = null

    init {
        tutorialHasBeenShown =
            PreferencesUtils.getBoolean(context, PreferencesUtils.Tutorial_Key, false)
        val appInfo = context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.GET_META_DATA
        )
        apiKey = appInfo.metaData?.getString(MANIFEST_KEY)

        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = ContextCompat.getDrawable(context, R.drawable.smart_spectra_button_background)
        LayoutInflater.from(context).inflate(R.layout.view_start_button, this, true)

        checkupButton = findViewById(R.id.button_checkup)
        setOnClickListener(this::onStartClicked)

        infoButton = findViewById(R.id.button_info)
        infoButton.doOnLayout { showTutorialIfNecessary() }
        infoButton.setOnClickListener { infoBottomSheetDialog.show() }
    }

    fun setApiKey(apiKey: String) {
        this.apiKey = apiKey
    }

    fun setResultListener(listener: SmartSpectraResultListener) {
        this.resultListener = listener
    }

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
            openWalkThrough(context)
        }
        dialog
    }

    private fun showTutorialIfNecessary() {
        if (!tutorialHasBeenShown) {
            openWalkThrough(context)
        }
    }

    private var screeningActivityLauncher: ActivityResultLauncher<ScreeningContractInput> =
        (context as AppCompatActivity).registerForActivityResult(ScreeningContract()) {
            val listener = resultListener ?: throw IllegalStateException("resultListener is null")
            listener.onResult(it)
        }

    private fun onStartClicked(view: View) {
        require(resultListener != null) { "Have you forgotten to set the result listener?" }
        val key = apiKey ?: throw IllegalStateException(
            "SDK API key is missing. " +
                    "It was not found in AndroidManifest.xml meta-data $MANIFEST_KEY, " +
                    "nor was it set via the .setApiKey() method. " +
                    "Please refer to the documentation for more details."
        )
        screeningActivityLauncher.launch(ScreeningContractInput(key))
    }

    private fun openWalkThrough(context: Context) {
        val rootLocation = getViewLocation(this@SmartSpectraButton)
        val intent = Intent(context, WalkThroughActivity::class.java).apply {
            putExtra("data", generateViewsLocationJson().toString())
            putExtra("top", rootLocation.top)
            putExtra("bottom", rootLocation.bottom)
        }
        context.startActivity(intent)
        tutorialHasBeenShown = true
    }

    private fun generateViewsLocationJson(): JSONArray {
        return JSONArray().apply {
            put(
                JSONObject().apply {
                    val rect = getViewLocation(checkupButton)
                    put("left", rect.left)
                    put("top", rect.top)
                    put("right", rect.right)
                    put("bottom", rect.bottom)
                    put("description", resources.getString(R.string.checkup_description))
                }
            )
            put(
                JSONObject().apply {
                    val rect = getViewLocation(infoButton)
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

    private fun dpToPx(dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).roundToInt()
    }

    companion object {
        const val MANIFEST_KEY = "com.presagetech.smartspectra.api_key"
    }
}
