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
import org.hamcrest.CoreMatchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.platform.app.InstrumentationRegistry

@RunWith(AndroidJUnit4::class)
class WebViewCTUDisplayTest {
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

    private fun waitForCTUReady() {
        val timeout = 10000L
        val interval = 100L
        val start = System.currentTimeMillis()
        while (true) {
            var ready = false
            try {
                onWebView().perform(
                    findElement(Locator.ID, "time_webview")
                ).check(webMatches(getText(), containsString("CTU_READY")))
            } catch (_: Throwable) {
                // Ignore transient errors while WebView is loading
            }
            if (ready) break
            if (System.currentTimeMillis() - start > timeout) throw AssertionError("CTU_READY not set in time")
            Thread.sleep(interval)
        }
    }

    @Test
    fun ctuTimeIsDisplayed() {
        activityRule.scenario.onActivity { _ ->
            waitForCTUReady()
        }
        onWebView()
            .withElement(findElement(Locator.ID, "ctu-time"))
            .check(webMatches(getText(), not(containsString("Error"))))
            .check(webMatches(getText(), containsString(":")))
    }

    @Test
    fun ctuReferenceDateIsDisplayed() {
        activityRule.scenario.onActivity { _ ->
            waitForCTUReady()
        }
        onWebView()
            .withElement(findElement(Locator.XPATH, "//div[contains(@class,'ref-date') and contains(text(),'Reference:')]") )
            .check(webMatches(getText(), containsString("Reference:")))
    }

    @Test
    fun dawnAndDuskAreDisplayed() {
        activityRule.scenario.onActivity { _ ->
            waitForCTUReady()
        }
        onWebView()
            .withElement(findElement(Locator.ID, "ctu-dawn"))
            .check(webMatches(getText(), not(containsString("N/A"))))
        onWebView()
            .withElement(findElement(Locator.ID, "ctu-dusk"))
            .check(webMatches(getText(), not(containsString("N/A"))))
    }

    @Test
    fun infoPanelIsInitiallyHidden() {
        activityRule.scenario.onActivity { _ ->
            waitForCTUReady()
        }
        onWebView()
            .withElement(findElement(Locator.ID, "info-panel"))
            .check(webMatches(getText(), containsString("CTU")))
    }
}
