package com.kiefner.c_tune_clock

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kiefner.c_tune_clock.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferences = getSharedPreferences("prefs", MODE_PRIVATE)

        // Set up the onboarding content
        binding.welcomeText.text = "Welcome to C-Tune Clock!"
        binding.ctuExplanation.text = "CTU (Calculated Time Uncoordinated) is a revolutionary timekeeping system aligned with the sun."
        binding.ctuBenefits.text = "\u2022 Anchored to solar noon\n\u2022 Eliminates DST harm\n\u2022 Aligns with circadian rhythms\n\u2022 Weekday/Week toggle: Tap to reveal full date"

        // Set up button listeners
        binding.learnMoreButton.setOnClickListener {
            // Open the full CTU README in a WebView or browser
            val intent = Intent(this, WebViewActivity::class.java)
            intent.putExtra("url", "file:///android_asset/docs/README.html")
            startActivity(intent)
        }

        binding.startClockButton.setOnClickListener {
            // Mark onboarding as complete and proceed to MainActivity
            preferences.edit().putBoolean("onboarding_complete", true).apply()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}