package com.kiefner.c_tune_clock

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.kiefner.c_tune_clock.bridge.PythonBridge
import com.kiefner.c_tune_clock.utils.LocationUtils
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.ZoneId



class MainActivity : AppCompatActivity() {
    // Show a toast for user-visible error feedback
    private fun showErrorToast(message: String) {
        runOnUiThread {
            android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
        }
    }
    // Preference helpers for clock mode
    private fun saveClockMode(mode: Int) {
        getSharedPreferences("prefs", MODE_PRIVATE)
            .edit()
            .putInt("clock_mode", mode)
            .apply()
    }

    private fun loadClockMode(): Int {
        return getSharedPreferences("prefs", MODE_PRIVATE)
            .getInt("clock_mode", 0)
    }

    // JS bridge for saving mode from JS
    inner class JSBridge {
        @android.webkit.JavascriptInterface
        fun saveClockMode(mode: Int) {
            this@MainActivity.saveClockMode(mode)
        }
    }

    private lateinit var locationUtils: LocationUtils
    private lateinit var webView: WebView
    private lateinit var handler: Handler
    private val updateInterval: Long = 1000L
    private val updateRunnable: Runnable = object : Runnable {
        override fun run() {
            updateCTUTime()
            handler.postDelayed(this, updateInterval)
        }
    }

    private val locationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Once permission is granted, trigger an update.
                updateCTUTime()
            }
        }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // Check if onboarding is complete
        val preferences = getSharedPreferences("prefs", MODE_PRIVATE)
        val onboardingComplete = preferences.getBoolean("onboarding_complete", false)

        if (!onboardingComplete) {
            // Launch OnboardingActivity
            val intent = Intent(this, OnboardingActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // Proceed directly to the main content
        setContentView(R.layout.activity_main)
        hideSystemUI()
        setContentView(R.layout.activity_main)
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        handler = Handler(Looper.getMainLooper())

        locationUtils = LocationUtils(this)
        // Set up error callback: only show toast if user can act (permission issue)
        locationUtils.onError = { e ->
            if (e is SecurityException) {
                showErrorToast("Location permission required for solar time features.")
            } // else: log only, not shown to user
        }
        locationUtils.startLocationUpdates()
        webView = findViewById(R.id.time_webview)
        webView.settings.javaScriptEnabled = true

        // Add JS bridge for saving mode
        webView.addJavascriptInterface(JSBridge(), "AndroidBridge")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                // Set initial mode from preferences
                val initialMode = loadClockMode()
                webView.evaluateJavascript("setDisplayState(" + initialMode + ");", null)
                // Once the initial page is loaded, proceed to update CTU continuously.
                if (ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    updateCTUTime()
                    handler.post(updateRunnable)
                }
            }
        }
        webView.loadUrl("file:///android_asset/time_display.html")

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
                // Enables immersive sticky mode.
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        // Makes the content appear under the system bars.
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        // Hides the navigation bar and status bar.
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_FULLSCREEN
                )
    }

    private fun updateCTUTime() {
        val longitude = locationUtils.getCurrentLongitude()
        val latitude = locationUtils.getCurrentLatitude()
        val format = { t: Triple<Int, Int, Int> -> String.format("%02d:%02d:%02d", t.first, t.second, t.third) }

        val formattedUTC = java.text.SimpleDateFormat("HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

        val nowLocal = ZonedDateTime.now(ZoneId.systemDefault())
        val formattedLocalTime = nowLocal.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        val localTimeZoneLabel = nowLocal.format(DateTimeFormatter.ofPattern("z")) // 'z' gives the time zone abbreviation (e.g., CEST, CET, GMT+1)


        val ctuTime = PythonBridge.getCTUTime(longitude)
        val formattedCTU = ctuTime?.let { format(it) } ?: "??"
        val dawnDusk = PythonBridge.dawn_dusk(latitude, longitude)
        val dawnStr: String
        val duskStr: String
        if (dawnDusk != null) {
            val (dawn, dusk) = dawnDusk
            dawnStr = String.format("%02d:%02d", dawn.first, dawn.second)
            duskStr = String.format("%02d:%02d", dusk.first, dusk.second)
        } else {
            dawnStr = "--:--"
            duskStr = "--:--"
        }

        val js = """
        updateTimes('$formattedUTC', '$localTimeZoneLabel', '$formattedLocalTime', '$formattedCTU', '$dawnStr', '$duskStr');
    """.trimIndent()
        this.webView.evaluateJavascript(js, null)
    }

    override fun onDestroy() {
        // Clean up the handler callbacks if initialized
        if (::handler.isInitialized) {
            handler.removeCallbacks(updateRunnable)
        }
        super.onDestroy()
    }

    override fun onBackPressed() {
        // Set onboarding_complete to false
        val preferences = getSharedPreferences("prefs", MODE_PRIVATE)
        preferences.edit().putBoolean("onboarding_complete", false).apply()

        // Navigate back to OnboardingActivity
        val intent = Intent(this, OnboardingActivity::class.java)
        startActivity(intent)
        finish()
    }
}
