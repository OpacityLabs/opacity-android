package com.opacitylabs.opacitycoreexample

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
class UITests {
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
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Uber Rider Profile").fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Perform click
        composeTestRule.onNodeWithText("Uber Rider Profile").performClick()

        Thread.sleep(2000)

        // Check that the intent to open the browser was fired
        intended(
            allOf(
                hasComponent("com.opacitylabs.opacitycore.InAppBrowserActivity"),
            )
        )
    }

    @Test
    fun testSuccessFlowButton() {
        // Check if button is displayed
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Test flow always succeeds").fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Perform click
        composeTestRule.onNodeWithText("Test flow always succeeds").performClick()

        // Wait for the async operation to complete
        Thread.sleep(3000)

        // Check that the success dialog is displayed
        composeTestRule.onNodeWithText("Success").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test flow completed successfully!").assertIsDisplayed()
        composeTestRule.onNodeWithText("OK").assertIsDisplayed()
    }
}