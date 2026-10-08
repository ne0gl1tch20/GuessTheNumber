package com.jarrlyyy.guessthenumber

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChangelogCrashBruteTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun repeatedlyOpenChangelogFromMoreWithoutCrashing() {
        enterPlayableState()

        repeat(10) {
            composeRule.onNodeWithText("More", useUnmergedTree = true).performClick()
            composeRule.waitUntil(timeoutMillis = 5_000) {
                composeRule.onAllNodesWithText("Changelog", useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithText("Changelog", useUnmergedTree = true).performClick()
            composeRule.waitUntil(timeoutMillis = 5_000) {
                composeRule.onAllNodesWithText("Changelog", useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithText("Changelog", useUnmergedTree = true).assertIsDisplayed()
            composeRule.activityRule.scenario.onActivity {
                it.onBackPressedDispatcher.onBackPressed()
            }
            composeRule.waitUntil(timeoutMillis = 5_000) {
                composeRule.onAllNodesWithText("More Hub", useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
        }
    }

    /**
     * The debug APK can start in Save Slots instead of the game. The brute test must
     * bootstrap a fresh slot and complete the guided tutorial before looking for More.
     */
    private fun enterPlayableState() {
        val saveSlotsVisible = composeRule.onAllNodesWithText(
            "Save Slots",
            useUnmergedTree = true
        ).fetchSemanticsNodes().isNotEmpty()

        if (saveSlotsVisible) {
            composeRule.onNodeWithText("Start New Game", useUnmergedTree = true).performClick()
            composeRule.waitUntil(timeoutMillis = 5_000) {
                composeRule.onAllNodesWithText("Create", useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithText("Create", useUnmergedTree = true).performClick()

            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule.onAllNodesWithText("Guess The Number", useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
        } else {
            composeRule.waitUntil(timeoutMillis = 15_000) {
                composeRule.onAllNodesWithText("More", useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty() ||
                    composeRule.onAllNodesWithText("Guess The Number", useUnmergedTree = true)
                        .fetchSemanticsNodes().isNotEmpty()
            }
        }

        completeGuidedTutorialIfPresent()

        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText("More", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun completeGuidedTutorialIfPresent() {
        val tutorialVisible = composeRule.onAllNodesWithText(
            "Step 1 of 4 • Action required",
            useUnmergedTree = true
        ).fetchSemanticsNodes().isNotEmpty()

        if (!tutorialVisible) return

        // Step 1: make the first real guess.
        composeRule.onNodeWithText("Enter your guess", useUnmergedTree = true).performTextInput("1")
        composeRule.onNodeWithText("GUESS NUMBER", useUnmergedTree = true).performClick()

        // Step 2: open Upgrades.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Upgrades", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Upgrades", useUnmergedTree = true).performClick()

        // Step 3: open More.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("More", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("More", useUnmergedTree = true).performClick()

        // Step 4: open Settings. This completes the tutorial.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Settings", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Settings", useUnmergedTree = true).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Settings", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
