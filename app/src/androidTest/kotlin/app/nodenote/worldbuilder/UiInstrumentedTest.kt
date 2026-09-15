package app.nodenote.worldbuilder

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UiInstrumentedTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun shot(name: String) {
        rule.waitForIdle()
        val folder = File("/sdcard/Download/NodeNote-test-screenshots")
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.executeShellCommand("mkdir -p ${folder.path}")
        device.executeShellCommand(
            "screencap -p ${folder.path}/${name.replace("api34", "api${android.os.Build.VERSION.SDK_INT}")}.png"
        )
    }

    @Test
    fun nativeSampleNavigationAndChronology() {
        createSampleForTest(rule)
        shot("01-library-emulator-api34")
        rule.onNodeWithText("Boards", useUnmergedTree = true).performClick()
        rule.waitForIdle()
        rule.onNodeWithText("The living world").performClick()
        rule.waitForIdle()
        shot("02-native-canvas-emulator-api34")
        rule.onNodeWithText("Outline").performClick()
        rule.onNodeWithText("The Ash Physician").performClick()
        rule.waitForIdle()
        shot("03-editor-emulator-api34")
        rule.onNodeWithText("Done").performClick()
        rule.onNodeWithText("Boards").performClick()
        rule.onNodeWithText("Timeline", useUnmergedTree = true).performClick()
        rule.waitForIdle()
        shot("04-epoch-overview-emulator-api34")
        rule.onNodeWithText("The First Radiance").performScrollTo().performClick()
        rule.waitForIdle()
        shot("05-epoch-editor-emulator-api34")
        rule.onNodeWithText("Done").performClick()
        rule.onNodeWithText("Scaled").performClick()
        rule.waitForIdle()
        shot("06-scaled-timeline-emulator-api34")
        rule.onNodeWithText("Reveals").performClick()
        rule.waitForIdle()
        shot("07-reveals-emulator-api34")
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()
        rule.onNodeWithText("The Epoch Codex").assertExists()
        lateinit var model: AppModel
        rule.runOnIdle {
            model = androidx.lifecycle.ViewModelProvider(rule.activity)[AppModel::class.java]
            model.historyAccount.value =
                model.bundle.value!!.records.first { it.kind == app.nodenote.core.Kind.ACCOUNT }.id
        }
        rule.onNodeWithText("Search", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Search recorded history").performTextInput("Physician")
        rule.onNodeWithText("The Ash Physician").assertDoesNotExist()
        rule.onNodeWithText("Boards", useUnmergedTree = true).performClick()
        rule.onNodeWithText("The living world").assertDoesNotExist()
        rule.onNodeWithText("Return to author's history").performClick()
        rule.runOnIdle { model.preference("theme", "Light") }
        rule.waitUntil(10000) { model.prefs.value["theme"] == "Light" }
        shot("10-light-theme-emulator-api34")
        rule.runOnIdle { model.preference("theme", "Dark") }
    }
}
