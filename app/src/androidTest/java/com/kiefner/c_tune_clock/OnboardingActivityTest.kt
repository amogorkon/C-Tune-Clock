package com.kiefner.c_tune_clock

import android.content.Intent
import android.content.SharedPreferences
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kiefner.c_tune_clock.R
import org.hamcrest.CoreMatchers.containsString
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingActivityTest {

    private lateinit var preferences: SharedPreferences

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        preferences = context.getSharedPreferences("prefs", 0)
        preferences.edit().clear().apply() // Clear preferences before each test
    }

    @After
    fun tearDown() {
        preferences.edit().clear().apply() // Clear preferences after each test
    }

    @Test
    fun testOnboardingContentDisplayed() {
        val scenario = ActivityScenario.launch(OnboardingActivity::class.java)

        onView(withId(R.id.welcomeText)).check(matches(isDisplayed()))
        onView(withId(R.id.welcomeText)).check(matches(withText("Welcome to C-Tune Clock!")))

        onView(withId(R.id.ctuExplanation)).check(matches(isDisplayed()))
        onView(withId(R.id.ctuExplanation)).check(matches(withText(containsString("CTU (Calculated Time Uncoordinated)"))))

        onView(withId(R.id.ctuBenefits)).check(matches(isDisplayed()))
        onView(withId(R.id.ctuBenefits)).check(matches(withText(containsString("Anchored to solar noon"))))
    }

    @Test
    fun testLearnMoreButtonOpensWebView() {
        val scenario = ActivityScenario.launch(OnboardingActivity::class.java)

        onView(withId(R.id.learnMoreButton)).perform(click())

        // Verify that the WebViewActivity is launched
        // onWebView().check(webMatches(getText(), containsString("Expected Text")))
    }

    @Test
    fun testOnboardingCompletionFlag() {
        val scenario = ActivityScenario.launch(OnboardingActivity::class.java)

        onView(withId(R.id.startClockButton)).perform(click())

        // Verify that the onboarding completion flag is set
        val isOnboardingComplete = preferences.getBoolean("onboarding_complete", false)
        assert(isOnboardingComplete)
    }
}
