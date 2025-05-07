package com.kiefner.c_tune_clock

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
        setContentView(R.layout.activity_main)
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        handler = Handler(Looper.getMainLooper())

        locationUtils = LocationUtils(this)
        locationUtils.startLocationUpdates()
        webView = findViewById(R.id.time_webview)
        webView.settings.javaScriptEnabled = true

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
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

    private fun updateCTUTime() {
        val longitude = locationUtils.getCurrentLongitude()
        val latitude = locationUtils.getCurrentLatitude()
        val format = { t: Triple<Int, Int, Int> -> String.format("%02d:%02d:%02d", t.first, t.second, t.third) }

        val formattedUTC = java.text.SimpleDateFormat("HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

        // Get and format Local time and its time zone label (HH:mm:ss TimeZoneAbbreviation)
        val nowLocal = ZonedDateTime.now(ZoneId.systemDefault())
        val formattedLocalTime = nowLocal.format(DateTimeFormatter.ofPattern("HH:mm:ss")) // Just the time part
        val localTimeZoneLabel = nowLocal.format(DateTimeFormatter.ofPattern("z")) // 'z' gives the time zone abbreviation (e.g., CEST, CET, GMT+1)

        val ctuTime = PythonBridge.getCTUTime(longitude)
        val formattedCTU = ctuTime?.let { format(it) } ?: "??"
        val js = """
        updateTimes('$formattedUTC', '$localTimeZoneLabel', '$formattedLocalTime', '$formattedCTU');
    """.trimIndent()
        this.webView.evaluateJavascript(js, null)
    }

    override fun onDestroy() {
        // Clean up the handler callbacks
        handler.removeCallbacks(updateRunnable)
        super.onDestroy()
    }
}
