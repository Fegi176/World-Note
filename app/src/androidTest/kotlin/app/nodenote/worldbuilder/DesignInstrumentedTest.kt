package app.nodenote.worldbuilder

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.graphics.ColorUtils
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.nodenote.worldbuilder.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DesignInstrumentedTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun nativePinchPreservesLoreAndPlacementCoordinates() {
        val vm = createSampleForTest(rule)
        val original = vm.bundle.value!!
        val board = original.records.first { it.title == "The living world" }
        rule.onNodeWithText("Boards", useUnmergedTree = true).performClick()
        rule.onNodeWithText("The living world").performClick()
        fun toolbarPixels(): IntArray {
            val root = rule.onRoot()
            val canvas =
                rule.onNodeWithContentDescription(
                    "Board canvas. Use Outline for accessible entry and relationship controls."
                )
            val top =
                (canvas.fetchSemanticsNode().boundsInRoot.top -
                        root.fetchSemanticsNode().boundsInRoot.top)
                    .toInt()
            val bitmap = root.captureToImage().asAndroidBitmap()
            return IntArray(bitmap.width * top).also {
                bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, top)
            }
        }
        val toolbarBefore = toolbarPixels()
        rule
            .onNodeWithContentDescription(
                "Board canvas. Use Outline for accessible entry and relationship controls."
            )
            .performTouchInput {
                pinch(
                    center + androidx.compose.ui.geometry.Offset(-60f, 0f),
                    center + androidx.compose.ui.geometry.Offset(-150f, 0f),
                    center + androidx.compose.ui.geometry.Offset(60f, 0f),
                    center + androidx.compose.ui.geometry.Offset(150f, 0f),
                    500L,
                )
            }
        rule.waitUntil(15000) {
            (vm.bundle.value?.records?.find { it.id == board.id }?.n("scale") ?: 0.0) >
                board.n("scale", .65)
        }
        assertArrayEquals(
            "Zoomed artwork must remain clipped below the toolbar",
            toolbarBefore,
            toolbarPixels(),
        )
        val after = vm.bundle.value!!
        assertEquals(
            original.records.filter { it.kind == app.nodenote.core.Kind.PLACEMENT }.toSet(),
            after.records.filter { it.kind == app.nodenote.core.Kind.PLACEMENT }.toSet(),
        )
        assertEquals(
            original.records.filter { it.kind.lore }.toSet(),
            after.records.filter { it.kind.lore }.toSet(),
        )
        rule.activityRule.scenario.recreate()
        rule
            .onNodeWithContentDescription(
                "Board canvas. Use Outline for accessible entry and relationship controls."
            )
            .assertIsDisplayed()
    }

    @Test
    fun atlasContrastAndAccessibleNavigation() {
        for (scheme in listOf(AtlasDark, AtlasLight)) {
            listOf(
                    scheme.onSurface to scheme.surface,
                    scheme.onSurfaceVariant to scheme.surfaceContainer,
                    scheme.primary to scheme.surface,
                    scheme.onSecondaryContainer to scheme.secondaryContainer,
                    scheme.onPrimaryContainer to scheme.primaryContainer,
                )
                .forEach { (text, background) ->
                    assertTrue(
                        "Text contrast must reach WCAG AA: $text / $background",
                        ColorUtils.calculateContrast(text.toArgb(), background.toArgb()) >= 4.5,
                    )
                }
        }
        rule.enableAccessibilityChecks()
        val vm = createSampleForTest(rule)
        rule.onNodeWithText("Search", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Search title, alias, or full notes").performTextInput("Physician")
        rule.waitUntil(10000) { vm.searchResults.value.isNotEmpty() }
        rule.onNodeWithTag("library-results").performScrollToNode(hasText("The Ash Physician"))
        rule.onNodeWithText("The Ash Physician").performClick()
        rule.onNodeWithText("Done").performClick()
        rule.onNodeWithText("Boards", useUnmergedTree = true).performClick()
        rule.onNodeWithText("The living world").performClick()
        rule.onNodeWithText("Outline").performClick()
        rule.onNodeWithText("The Ash Physician").assertExists()
        rule.onNodeWithText("Boards").performClick()
        rule.onNodeWithText("Menu").performClick()
        rule.onNodeWithText("Storage & recovery").performClick()
        rule.onNodeWithText("Clear preview cache").performClick()
        rule
            .onNodeWithText("Original images are preserved. Previews are rebuilt when needed.")
            .assertExists()
    }
}
