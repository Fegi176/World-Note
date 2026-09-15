package app.nodenote.worldbuilder

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import app.nodenote.core.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WritingInstrumentedTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun rapidUnicodeDraftDoneBackgroundReopenAndFontScale() {
        val vm = ViewModelProvider(rule.activity)[AppModel::class.java]
        rule.runOnIdle { vm.newWorld("Writing lifecycle fixture") }
        rule.waitUntil(20000) { vm.bundle.value?.world?.name == "Writing lifecycle fixture" }
        rule.runOnIdle { vm.create(Kind.CHARACTER, "Persistent writer") }
        rule.waitUntil(20000) { vm.editing.value?.title == "Persistent writer" }
        val body =
            "# A long history\n\nŁódź 王朝 日本語 😀 é\n".repeat(500) + "Latest acknowledged ending."
        rule.onNodeWithText("Full notes · Markdown").performScrollTo().performTextReplacement(body)
        rule.onNodeWithText("Done").performClick()
        rule.waitUntil(20000) { vm.editing.value == null }
        val record = runBlocking {
            vm.repo.snapshot(vm.selected.value!!).records.first { it.title == "Persistent writer" }
        }
        assertEquals(body, record.body)
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.pressHome()
        device.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.startActivity(
            context.packageManager
                .getLaunchIntentForPackage(context.packageName)!!
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        rule.waitForIdle()
        rule.runOnIdle { vm.open(record) }
        rule.waitForIdle()
        rule.onNodeWithText("Full notes · Markdown").performScrollTo().assertTextContains(body)
        // Never change the owner's system-wide font preference during isolated phone QA.
        val emulator =
            android.os.Build.HARDWARE in setOf("ranchu", "goldfish") ||
                android.os.Build.MODEL.startsWith("sdk_")
        if (!emulator) return
        val originalFont = device.executeShellCommand("settings get system font_scale").trim()
        try {
            device.executeShellCommand("settings put system font_scale 2.0")
            rule.activityRule.scenario.recreate()
            rule.waitForIdle()
            rule.onNodeWithText("Done").assertIsDisplayed()
            device.executeShellCommand("mkdir -p /sdcard/Download/NodeNote-test-screenshots")
            device.executeShellCommand(
                "screencap -p /sdcard/Download/NodeNote-test-screenshots/09-editor-font200-emulator-api${android.os.Build.VERSION.SDK_INT}.png"
            )
        } finally {
            device.executeShellCommand(
                if (originalFont == "null") "settings delete system font_scale"
                else "settings put system font_scale $originalFont"
            )
        }
    }
}
