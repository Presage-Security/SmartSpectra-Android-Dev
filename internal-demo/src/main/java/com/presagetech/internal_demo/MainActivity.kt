package com.presagetech.internal_demo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import com.presagetech.smartspectra.core.button.SmartSpectraButton
import com.presagetech.smartspectra.core.result.SmartSpectraResultView

class MainActivity : AppCompatActivity() {
    private lateinit var tokenEditText: EditText
    private lateinit var smartSpectraButton: SmartSpectraButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        smartSpectraButton = findViewById(R.id.btn)
        val resultView = findViewById<SmartSpectraResultView>(R.id.result_view)
        smartSpectraButton.setResultListener(resultView)

        tokenEditText = findViewById(R.id.text_api_token)
        tokenEditText.setOnEditorActionListener { _, _, _ ->
            val token = tokenEditText.text.toString().trim()
            saveToken(token)
            smartSpectraButton.setApiKey(token)
            true
        }

        val storedToken = loadToken()
        smartSpectraButton.setApiKey(storedToken)
        tokenEditText.setText(storedToken)
    }

    private fun loadToken(): String {
        return getSharedPreferences(TOKEN_SHARED_PREFERENCES, MODE_PRIVATE)
            .getString(TOKEN_SHARED_PREFERENCES, null) ?: ""
    }

    private fun saveToken(token: String) {
        getSharedPreferences(TOKEN_SHARED_PREFERENCES, MODE_PRIVATE).edit().apply {
            putString(TOKEN_SHARED_PREFERENCES, token)
            apply()
        }
    }

    companion object {
        const val TOKEN_SHARED_PREFERENCES = "demo_api_key"
    }
}
