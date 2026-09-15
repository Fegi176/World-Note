package app.nodenote.worldbuilder

import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import app.nodenote.core.*
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StressUiInstrumentedTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun onDiskStressWorldOpensAndPinchesTwoThousandPlacementBoard() {
        val vm = ViewModelProvider(rule.activity)[AppModel::class.java]
        rule.waitUntil(20000) { vm.prefsReady.value }
        lateinit var fixture: WorldBundle
        val insert = measureTimeMillis {
            runBlocking(Dispatchers.IO) {
                fixture = Remap.world(Fixtures.world(10000, 25000, 2000))
                vm.repo.insert(fixture)
            }
        }
        val board = fixture.records.first { it.kind == Kind.BOARD && it.title == "Stress board" }
        val opened = measureTimeMillis {
            rule.runOnIdle { vm.chooseWorld(fixture.world.id) }
            rule.waitUntil(30000) { vm.bundle.value?.world?.id == fixture.world.id }
            rule.runOnIdle { vm.boardRequest.value = board.id }
            rule
                .onNodeWithContentDescription(
                    "Board canvas. Use Outline for accessible entry and relationship controls."
                )
                .assertIsDisplayed()
        }
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        device.executeShellCommand("dumpsys gfxinfo ${context.packageName} reset")
        val canvas =
            rule.onNodeWithContentDescription(
                "Board canvas. Use Outline for accessible entry and relationship controls."
            )
        repeat(4) { step ->
            canvas.performTouchInput {
                val start = if (step % 2 == 0) 60f else 120f
                val end = if (step % 2 == 0) 120f else 60f
                pinch(
                    center + Offset(-start, 0f),
                    center + Offset(-end, 0f),
                    center + Offset(start, 0f),
                    center + Offset(end, 0f),
                    600,
                )
            }
            rule.waitForIdle()
        }
        device.executeShellCommand("mkdir -p /sdcard/Download/NodeNote-test-screenshots")
        device
            .executeShellCommand("dumpsys gfxinfo ${context.packageName}")
            .lineSequence()
            .takeWhile { !it.startsWith("Caches:") }
            .forEach { Log.i("NodeNoteFrames", it) }
        device
            .executeShellCommand("dumpsys meminfo ${context.packageName}")
            .lineSequence()
            .filter {
                it.contains("TOTAL") ||
                    it.contains("Java Heap:") ||
                    it.contains("Native Heap:") ||
                    it.contains("Graphics:")
            }
            .forEach { Log.i("NodeNoteMemory", it) }
        device.executeShellCommand(
            "screencap -p /sdcard/Download/NodeNote-test-screenshots/17-stress-board-api34.png"
        )
        Log.i(
            "NodeNotePerformance",
            "onDiskStress records=${fixture.records.size} generationAndInsertMs=$insert worldAndBoardOpenMs=$opened nativePinches=4",
        )
        rule.onNodeWithText("Outline").performClick()
        rule.onNodeWithText("Synthetic record 0").assertExists()
    }
}
