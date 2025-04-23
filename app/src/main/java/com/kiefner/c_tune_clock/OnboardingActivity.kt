package com.kiefner.c_tune_clock

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView

class OnboardingActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        prefs = getSharedPreferences("c_tune_prefs", MODE_PRIVATE)

        val nextButton: Button = findViewById(R.id.next_button)
        val infoText: TextView = findViewById(R.id.info_text)

        // Set up your onboarding content:
        infoText.text = "Welcome to C-Tune Clock!\n\nCTU is Time as Nature Intended.\nSwipe or tap Next to learn more."

        nextButton.setOnClickListener {
            // For simplicity, flag onboarding complete and transition.
            // In a multi-step flow, you would advance pages instead.
            prefs.edit().putBoolean("isOnboardingComplete", true).apply()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
