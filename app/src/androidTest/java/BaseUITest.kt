package com.opacitylabs.opacitycoreexample

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaseUITest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        androidx.test.espresso.intent.Intents.init()
    }

    @After
    fun tearDown() {
        androidx.test.espresso.intent.Intents.release()
    }

    @Test
    fun testButtonClick() {
        // Check if button is displayed
        composeTestRule.onNodeWithText("Uber Rider Profile").assertIsDisplayed()

        // Perform click
        composeTestRule.onNodeWithText("Uber Rider Profile").performClick()

        Thread.sleep(2000)

        // Check that the intent to open the browser was fired
        intended(
            allOf(
                hasComponent("com.opacitylabs.opacitycore.InAppBrowserActivity"),
//                hasExtra("url", "https://uber.opacitylabs.com/rider")
            )
        )
    }
}