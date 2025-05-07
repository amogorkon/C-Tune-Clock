package com.kiefner.c_tune_clock

import androidx.test.espresso.IdlingRegistry
import android.webkit.WebView
import com.kiefner.c_tune_clock.testutils.WebViewIdlingResource
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.web.assertion.WebViewAssertions.webMatches
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.getText
import androidx.test.espresso.web.webdriver.Locator
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.CoreMatchers.containsString
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.platform.app.InstrumentationRegistry

@RunWith(AndroidJUnit4::class)
class WebViewTimeDisplayTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setUp() {
        activityRule.scenario.onActivity { activity ->
            val webView = activity.findViewById<WebView>(R.id.time_webview)
            val idlingResource = WebViewIdlingResource(webView)
            IdlingRegistry.getInstance().register(idlingResource)
        }
    }

    private fun waitForCTUReady(webView: WebView) {
        val timeout = 10000L
        val interval = 100L
        val start = System.currentTimeMillis()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            webView.evaluateJavascript("window.CTU_READY", null)
        }
        while (true) {
            var ready = false
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                webView.evaluateJavascript("window.CTU_READY", {
                    ready = it == "true"
                })
            }
            if (ready) break
            if (System.currentTimeMillis() - start > timeout) throw AssertionError("CTU_READY not set in time")
            Thread.sleep(interval)
        }
    }

    @Test
    fun utcTimeIsDisplayed() {
        activityRule.scenario.onActivity { activity ->
            waitForCTUReady(activity.findViewById(R.id.time_webview))
        }
        onWebView()
            .withElement(findElement(Locator.ID, "utc-time"))
            .check(webMatches(getText(), containsString(":")))
    }
}
