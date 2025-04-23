// MainActivity.kt
package com.kiefner.c_tune_clock

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import com.chaquo.python.Python
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val handler = Handler(Looper.getMainLooper())
    private var longitude: Float = 0.0f
    private val updateInterval: Long = 1000L // Update every second

    private val updateRunnable = object : Runnable {
        override fun run() {
            updateTimeDisplay()
            handler.postDelayed(this, updateInterval)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.time_webview)

        initLocation()
        handler.post(updateRunnable)
    }

    private fun initLocation() {
        val timezoneOffsetMillis = TimeZone.getDefault().rawOffset
        val offsetHours = timezoneOffsetMillis / (1000 * 60 * 60)
        longitude = (15.0 * offsetHours).toFloat()
    }

    private fun updateTimeDisplay() {
        val utcNowMillis = System.currentTimeMillis()
        val utcFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val utcFormatted = utcFormatter.format(Date(utcNowMillis))

        val python = Python.getInstance()
        val module = python.getModule("ctu")
        val ctuFormatted: String = try {
            module.callAttr("now", longitude).toString()
        } catch (e: Exception) {
            "CTU Error"
        }

        val htmlContent = """
            <!DOCTYPE html>
            <html>
              <head>
                <meta charset="utf-8">
                <title>C-Tune Clock</title>
                <style>
                  body { font-family: sans-serif; text-align: center; margin-top: 50px; }
                  .time { font-size: 2em; margin: 20px 0; }
                </style>
              </head>
              <body>
                <div class="time">UTC: $utcFormatted</div>
                <div class="time">CTU: $ctuFormatted</div>
              </body>
            </html>
        """.trimIndent()

        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
    }

    override fun onDestroy() {
        handler.removeCallbacks(updateRunnable)
        super.onDestroy()
    }
}
