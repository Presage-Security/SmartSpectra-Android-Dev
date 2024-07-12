package com.presagetech.smartspectra.ui

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.presagetech.smartspectra.R
import com.presagetech.smartspectra.utils.PreferencesUtils

class OnboardingTutorialActivity : AppCompatActivity() {

    private var tutorialImageView: ImageView? = null
    private var tutorialDescriptionTextView: TextView? = null
    private var counter: Int = 0
    private lateinit var tutorialImages: List<Int>
    private lateinit var descriptions: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding_tutorial)

        tutorialImageView = findViewById(R.id.tutorial_image)
        tutorialDescriptionTextView = findViewById(R.id.tutorial_description)

        // Load the tutorial images and descriptions
        tutorialImages = listOf (
            R.drawable.tutorial_image1,
            R.drawable.tutorial_image2,
            R.drawable.tutorial_image3,
            R.drawable.tutorial_image4,
            R.drawable.tutorial_image5,
            R.drawable.tutorial_image6,
            R.drawable.tutorial_image7,
        )
        descriptions = resources.getStringArray(R.array.tutorial_descriptions)

        // Initialize the first tutorial step
        updateTutorialStep()

        // Set up click listener to go to the next tutorial step
        tutorialImageView?.setOnClickListener {
            counter++
            if (counter < tutorialImages.size) {
                updateTutorialStep()
            } else {
                // Finish the tutorial and close the activity
                PreferencesUtils.saveBoolean(this, PreferencesUtils.ONBOARDING_TUTORIAL_KEY, true)
                finish()
            }
        }
    }

    private fun updateTutorialStep() {
        tutorialImageView?.setImageResource(tutorialImages[counter])
        tutorialDescriptionTextView?.text = descriptions[counter]
        tutorialImageView?.contentDescription = descriptions[counter]
    }
}
