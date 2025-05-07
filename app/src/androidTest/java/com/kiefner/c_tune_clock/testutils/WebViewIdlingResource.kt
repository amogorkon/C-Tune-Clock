package com.kiefner.c_tune_clock.testutils

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.espresso.IdlingResource

class WebViewIdlingResource(private val webView: WebView) : IdlingResource {
    @Volatile
    private var callback: IdlingResource.ResourceCallback? = null
    @Volatile
    private var idle = false

    private val client = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            idle = true
            callback?.onTransitionToIdle()
        }
    }

    init {
        webView.webViewClient = client
    }

    override fun getName(): String = "WebViewIdlingResource"

    override fun isIdleNow(): Boolean = idle

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        this.callback = callback
        if (idle) {
            callback?.onTransitionToIdle()
        }
    }
}
