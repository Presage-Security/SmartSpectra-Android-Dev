package com.presagetech.internal_demo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.presagetech.physiology.core.button.SmartSpectraButton

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val smartSpectraButton = findViewById<SmartSpectraButton>(R.id.btn)
        smartSpectraButton.setupResultView(findViewById(R.id.result_view))
    }
}
