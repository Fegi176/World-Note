package app.nodenote.worldbuilder

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.nodenote.core.*
import app.nodenote.worldbuilder.ui.*
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BoardMediaInstrumentedTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun imageSource(): File {
        val file = File(rule.activity.cacheDir, "Board-image-${newId()}.png")
        val bitmap = Bitmap.createBitmap(96, 64, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.MAGENTA)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return file
    }

    @Test
    fun importedImageRendersAndCardContentStaysLocalAndBacksUp(): Unit = runBlocking {
        val vm = createSampleForTest(rule)
        val before = vm.bundle.value!!
        val board = before.records.first { it.title == "The living world" }
        val source = imageSource()
        val placement = vm.files.placeImage(Uri.fromFile(source), board, Point(20.0, 20.0))
        source.delete()
        val owner = vm.repo.record(placement.ref("entry")!!)!!
        val note =
            owner.copy(
                body = "Visible board note: Łódź 王朝",
                fields = owner.fields + ("tags" to "portrait, faction"),
            )
        vm.repo.save(note)
        val secondBoard = before.records.first { it.kind == Kind.BOARD && it.id != board.id }
        val elsewhere =
            placement.copy(
                id = newId(),
                refs = placement.refs + ("board" to listOf(secondBoard.id)),
            )
        vm.repo.batch(board.world, listOf(elsewhere))
        rule.waitUntil(15000) { vm.bundle.value?.records?.any { it.id == placement.id } == true }
        rule.onNodeWithText("Boards", useUnmergedTree = true).performClick()
        rule.onNodeWithText("The living world").performClick()
        val canvas =
            rule.onNodeWithContentDescription(
                "Board canvas. Use Outline for accessible entry and relationship controls."
            )
        fun magentaPixels(): Int {
            val bitmap = canvas.captureToImage().asAndroidBitmap()
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            return pixels.count {
                Color.red(it) > 180 && Color.blue(it) > 180 && Color.green(it) < 80
            }
        }
        rule.waitUntil(20000) { magentaPixels() > 500 }
        val density = rule.activity.resources.displayMetrics.density
        canvas.performTouchInput {
            click(Offset((12 + 60 * .65f) * density, (20 + 60 * .65f) * density))
        }
        rule.onNodeWithText("Card content").performClick()
        rule.onNodeWithText("Cover image").assertIsOn()
        rule.onNodeWithText("Note preview").performClick()
        rule.onNodeWithText("Tags and status").performClick()
        rule.onNodeWithText("Apply to selected cards").performClick()
        rule.waitUntil(15000) {
            vm.bundle.value?.records?.find { it.id == placement.id }?.f("show notes") == "true"
        }
        assertEquals(elsewhere, vm.repo.record(elsewhere.id))
        assertEquals(vm.repo.record(note.id)?.body, note.body)
        assertTrue(magentaPixels() > 500)
        val final = vm.repo.snapshot(board.world)
        val updated = final.records.first { it.id == placement.id }
        assertEquals("true", updated.f("show details"))
        assertTrue(updated.n("height") >= 290)
        val zip = File(rule.activity.cacheDir, "board-media-${newId()}.nnbackup")
        val stage = File(rule.activity.cacheDir, "board-media-stage-${newId()}")
        zip.outputStream().use { Archives.write(Workspace(listOf(final)), it, vm.repo::asset) }
        val inspected = Archives.inspect(zip, stage)
        assertEquals(final.records.toSet(), inspected.workspace.worlds.single().records.toSet())
        rule.activityRule.scenario.recreate()
        canvas.assertIsDisplayed()
        rule.waitUntil(20000) { magentaPixels() > 500 }
        val shot = File(rule.activity.getExternalFilesDir(null), "board-media-api34.png")
        shot.outputStream().use {
            canvas.captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test
    fun invalidBoardImageRollsBackRecordsAndCopiedAsset() = runBlocking {
        val vm = createSampleForTest(rule)
        val before = vm.repo.snapshot(vm.bundle.value!!.world.id)
        val invalidBoard = Record(world = before.world.id, kind = Kind.BOARD, title = "Absent")
        val source = imageSource()
        val filesBefore = vm.repo.assetRoot.list().orEmpty().toSet()
        try {
            assertTrue(
                runCatching {
                        vm.files.placeImage(Uri.fromFile(source), invalidBoard, Point(0.0, 0.0))
                    }
                    .isFailure
            )
            assertEquals(filesBefore, vm.repo.assetRoot.list().orEmpty().toSet())
            assertEquals(before.records.toSet(), vm.repo.snapshot(before.world.id).records.toSet())
        } finally {
            source.delete()
        }
    }
}
