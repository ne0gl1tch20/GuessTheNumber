package com.jarrlyyy.guessthenumber

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChangelogCrashBruteTest {
    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @Test
    fun repeatedlyOpenChangelogFromMoreWithoutCrashing() {
        enterPlayableState()

        repeat(10) {
            clickText("More")
            waitForText("Changelog")
            clickText("Changelog")
            waitForText("Changelog")
            check(device.findObject(By.text("Changelog")) != null) {
                "Changelog screen did not appear"
            }
            device.pressBack()
            waitForText("More Hub")
        }
    }

    private fun enterPlayableState() {
        waitForAnyText("Create your profile", "Save Slots", "More", "Guess The Number")

        if (hasText("Create your profile")) {
            setText("Username", "Archive Test")
            clickText("Create profile & continue")
            waitForText("Save Slots")
        }

        if (hasText("Save Slots")) {
            clickText("Start New Game")
            waitForText("Create")
            clickText("Create")
            waitForText("Guess The Number")
        } else {
            waitForAnyText("More", "Guess The Number")
        }

        completeGuidedTutorialIfPresent()
        waitForText("More")
    }

    private fun completeGuidedTutorialIfPresent() {
        if (!hasText("Step 1 of 4 • Action required")) return

        setText("Enter your guess", "1")
        clickText("GUESS NUMBER")

        waitForText("Upgrades")
        clickText("Upgrades")

        waitForText("More")
        clickText("More")

        waitForText("Settings")
        clickText("Settings")
        device.pressBack()
        waitForText("More")
    }

    private fun waitForText(text: String, timeout: Long = 10_000) {
        check(device.wait(Until.hasObject(By.text(text)), timeout)) {
            "Timed out waiting for '$text'"
        }
    }

    private fun waitForAnyText(vararg texts: String, timeout: Long = 20_000) {
        val selectors = texts.map { By.text(it) }
        check(device.wait(Until.hasObject(By.pkg(device.currentPackage).text(selectors.first().toString())), 1)) {
            // Keep the actual polling below; this branch only prevents a long Compose-idle wait.
        }
        val deadline = System.currentTimeMillis() + timeout
        while (System.currentTimeMillis() < deadline) {
            if (texts.any(::hasText)) return
            Thread.sleep(100)
        }
        error("Timed out waiting for any of: ${texts.joinToString()}")
    }

    private fun hasText(text: String): Boolean = device.findObject(By.text(text)) != null

    private fun clickText(text: String) {
        waitForText(text)
        device.findObject(By.text(text)).click()
    }

    private fun setText(label: String, value: String) {
        waitForText(label)
        val target = device.findObject(By.text(label))
        check(target != null) { "Could not find '$label'" }
        target.click()
        target.text = value
    }
}