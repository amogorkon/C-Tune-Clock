package com.kiefner.c_tune_clock

import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class WebViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.useWideViewPort = true
        webView.setInitialScale(1)
        webView.webViewClient = WebViewClient()
        webView.loadUrl("file:///android_asset/README.html")

        val scrollView = android.widget.ScrollView(this)
        scrollView.addView(webView)

        setContentView(scrollView)
    }

    override fun onBackPressed() {
        // Navigate back to OnboardingActivity
        val intent = Intent(this, OnboardingActivity::class.java)
        startActivity(intent)
        finish()
    }
}
