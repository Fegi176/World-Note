package app.nodenote.worldbuilder

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PickerInstrumentedTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun actualSystemBackupPickerRestoreAndCancel() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val exportName = "NodeNote-picker-${System.currentTimeMillis()}.nnbackup"
        createSampleForTest(rule)
        rule.onNodeWithText("Menu").performClick()
        rule.onNodeWithText("Settings & backup").performClick()
        rule.onNodeWithText("Complete workspace backup").performScrollTo().performClick()
        rule.onNodeWithText("Choose destination").performClick()
        device.waitForIdle()
        val edit =
            device.wait(
                Until.findObject(By.res("android:id/title").clazz("android.widget.EditText")),
                30000,
            )
        if (edit == null)
            device.executeShellCommand("screencap -p /sdcard/Download/picker-save-failure.png")
        assertNotNull("The native DocumentsUI filename field must be visible", edit)
        edit.text = exportName
        val save =
            device.findObject(By.text("SAVE")) ?: device.findObject(By.res("android:id/button1"))
        assertNotNull(save)
        save.click()
        device.waitForIdle()
        if (device.hasObject(By.text("Replace"))) device.findObject(By.text("Replace")).click()
        rule.waitUntil(30000) {
            rule
                .onAllNodesWithText("Export finished.", substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        rule.onNodeWithText("OK").performClick()
        rule
            .onNodeWithText("Restore / import backup")
            .performScrollTo()
            .performClick()
        device.waitForIdle()
        val file = device.wait(Until.findObject(By.text(exportName)), 30000)
        assertNotNull("Exported file should appear in actual DocumentsUI", file)
        file.click()
        rule.waitUntil(30000) {
            rule.onAllNodesWithText("Import validated").fetchSemanticsNodes().isNotEmpty()
        }
        device.executeShellCommand("mkdir -p /sdcard/Download/NodeNote-test-screenshots")
        device.executeShellCommand(
            "screencap -p /sdcard/Download/NodeNote-test-screenshots/08-import-validation-emulator-api${android.os.Build.VERSION.SDK_INT}.png"
        )
        rule.onNodeWithText("Import as new worlds").performClick()
        rule.waitUntil(30000) {
            rule
                .onAllNodesWithText("Restored ", substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        rule.onNodeWithText("OK").performClick()
        rule
            .onNodeWithText("Restore / import backup")
            .performScrollTo()
            .performClick()
        device.wait(Until.hasObject(By.text(exportName)), 30000)
        device.pressBack()
        rule.waitForIdle()
        rule.onNodeWithText("Settings & backup").assertExists()
        // Leave a known root destination for the next UI walkthrough.
        device.pressBack()
        rule.waitForIdle()
    }
}
