package com.zen.clasp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun createText_addsDurableLibraryEntry() {
        val content = "Remember the Clasp launch date"

        composeRule.onNodeWithText("Text").performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput(content)
        composeRule.onNodeWithText("Save").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(content).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(content).assertIsDisplayed()
    }
}
