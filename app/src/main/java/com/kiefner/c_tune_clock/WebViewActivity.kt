package com.kiefner.c_tune_clock

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class WebViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val webView = WebView(this)
        setContentView(webView)

        val url = intent.getStringExtra("url")
        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = true

        if (url != null) {
            webView.loadUrl(url)
        } else {
            webView.loadData("<html><body><h1>Error</h1><p>URL not provided.</p></body></html>", "text/html", "UTF-8")
        }
    }
}
