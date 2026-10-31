package com.oathly.app

import android.graphics.Bitmap
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Walks through the whole app like a new user (US onboarding, mock interview, results,
 * study cards, answers that change, progress, then the Australian test) and saves a
 * screenshot of each screen for the Play Store listing. A crash anywhere fails the run.
 */
@RunWith(AndroidJUnit4::class)
class StoreScreenshotsTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app get() = instrumentation.targetContext.applicationContext as OathlyApp

    @Before
    fun hideAds() {
        // Store screenshots should show the app, not test ads.
        instrumentation.runOnMainSync { app.graph.ads.setPro(true) }
    }

    private fun shot(name: String) {
        compose.waitForIdle()
        Thread.sleep(900)
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
        val dir = File(instrumentation.targetContext.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** Text on screen, ignoring what's typed into text fields. */
    private fun shown(text: String, substring: Boolean) = hasText(text, substring = substring) and !hasSetTextAction()

    private fun waitFor(text: String, substring: Boolean = false, timeoutMs: Long = 20_000) {
        compose.waitUntil(timeoutMs) {
            compose.onAllNodes(shown(text, substring)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun tap(text: String, substring: Boolean = false) {
        waitFor(text, substring)
        val node = compose.onAllNodes(shown(text, substring))[0]
        runCatching { node.performScrollTo() } // only works inside scrolling screens
        node.performClick()
        compose.waitForIdle()
    }

    private fun tapIcon(description: String) {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithContentDescription(description)[0].performClick()
        compose.waitForIdle()
    }

    @Test
    fun walkThroughTheAppAndCaptureStoreScreens() {
        ActivityScenario.launch(MainActivity::class.java)

        // Onboarding for the U.S. test
        tap("Get started")
        tap("U.S. citizenship test")
        tap("On or after October 20, 2025")
        tap("No")
        compose.waitUntil(20_000) { compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("California")
        tap("California")
        tap("District", substring = true)
        tap("No reminders")

        // Home
        waitFor("Start mock interview")
        compose.waitUntil(20_000) { compose.onAllNodesWithText("%", substring = true).fetchSemanticsNodes().isNotEmpty() }
        shot("01_home_us")

        // Mock interview, answered by self-check until the officer stops
        tap("Start mock interview")
        waitFor("The officer asks")
        shot("02_interview_question")
        tap("I don't know, show me")
        tap("Yes, I knew it")
        shot("03_interview_answer")
        var guard = 0
        while (compose.onAllNodesWithText("See your result").fetchSemanticsNodes().isEmpty() && guard++ < 40) {
            tap("Next question")
            tap("I don't know, show me")
            tap("Yes, I knew it")
        }
        tap("See your result")
        waitFor("You passed!")
        shot("04_interview_result")
        tap("Done")

        // Answers that change (state-specific)
        tap("Answers that change")
        waitFor("These answers change after elections", substring = true)
        shot("05_answers_that_change")
        tapIcon("Back")

        // Study cards
        tap("Study cards")
        tap("All questions")
        tap("Show answer")
        shot("06_study_card")
        tapIcon("Close")
        tapIcon("Back")

        // Progress
        tap("Your progress")
        waitFor("Learned by topic")
        shot("07_progress")
        tapIcon("Back")

        // Switch to the Australian test
        tapIcon("Settings")
        tap("Test")
        tap("Australian citizenship test")
        tapIcon("Back")
        waitFor("Take a practice test")
        shot("08_home_au")
        tap("Take a practice test")
        waitFor("Question 1 of 20")
        shot("09_au_question")
        for (i in 0 until 20) {
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("option_correct").fetchSemanticsNodes().isNotEmpty() }
            compose.onAllNodesWithTag("option_correct")[0].performClick()
            compose.waitForIdle()
            if (i == 0) shot("10_au_feedback")
            tap(if (i == 19) "See your result" else "Next question")
        }
        waitFor("of 20 correct", substring = true)
        shot("11_au_result")
    }
}
