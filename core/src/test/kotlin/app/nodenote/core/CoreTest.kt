package app.nodenote.core

import java.io.File
import java.util.zip.*
import kotlin.random.Random
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test

class CoreTest {
    @Test
    fun markdownKeepsInternalLinksButDisablesScriptsAndRemoteImages() {
        val html =
            Markdown.html(
                "[Entry](nodenote://entry/abc)\n\n<script>alert(1)</script>\n\n![remote](https://example.com/image.png)\n\n[unsafe](javascript:alert)"
            )
        assertTrue(html.contains("nodenote://entry/abc"))
        assertFalse(html.contains("<script>"))
        assertFalse(html.contains("<img"))
        assertFalse(html.contains("href=\"javascript:"))
    }

    @Test
    fun completeDemoIsValid() {
        Integrity.requireValid(Demo.create())
    }

    private val w = World(name = "Fixture")

    private fun event(e: TimeExpr) =
        Record(world = w.id, kind = Kind.EVENT, title = "Event", times = mapOf("occurrence" to e))

    @Test
    fun exact64BitRoundTrip() {
        val e = TimeExpr(TimeKind.EXACT, year = "9223372036854775806")
        assertEquals(e, codec.decodeFromString<TimeExpr>(codec.encodeToString(e)))
        assertEquals(Long.MAX_VALUE - 1, Chronology(emptyList()).expression(e).low)
    }

    @Test
    fun approximateHasNoInventedBounds() {
        val p = Chronology(emptyList()).expression(TimeExpr(TimeKind.APPROXIMATE, year = "-100000"))
        assertNull(p.low)
        assertNull(p.high)
        assertEquals(-100000L, p.estimate)
    }

    @Test
    fun offsetPropagatesBounds() {
        val a = event(TimeExpr(TimeKind.RANGE, year = "-5000", upper = "-4900"))
        val b = event(TimeExpr(TimeKind.OFFSET, anchor = a.id, minimum = "200", maximum = "200"))
        val p = Chronology(listOf(a, b)).resolve(b.id)
        assertEquals(-4800L, p.low)
        assertEquals(-4700L, p.high)
        assertTrue(a.id in p.provenance)
    }

    @Test
    fun epochLocalYearAndUnknown() {
        val epoch =
            Record(
                world = w.id,
                kind = Kind.EPOCH,
                title = "Age",
                times = mapOf("start" to TimeExpr(TimeKind.EXACT, year = "-10000")),
            )
        val e = event(TimeExpr(TimeKind.LOCAL_YEAR, year = "412", anchor = epoch.id))
        assertEquals(-9589L, Chronology(listOf(epoch, e)).resolve(e.id).low)
        assertNull(Chronology(listOf(epoch.copy(times = emptyMap()), e)).resolve(e.id).low)
    }

    @Test
    fun withinIsHalfOpenWindowNotMidpoint() {
        val epoch =
            Record(
                world = w.id,
                kind = Kind.EPOCH,
                title = "Age",
                times =
                    mapOf(
                        "start" to TimeExpr(TimeKind.EXACT, year = "-10000"),
                        "end" to TimeExpr(TimeKind.EXACT, year = "-9000"),
                    ),
            )
        val p = Chronology(listOf(epoch)).expression(TimeExpr(TimeKind.WITHIN, anchor = epoch.id))
        assertEquals(-10000L, p.low)
        assertEquals(-9001L, p.high)
        assertNull(p.estimate)
    }

    @Test
    fun cyclesAndOverflowAreInvalid() {
        val a = event(TimeExpr())
        val b = event(TimeExpr(TimeKind.OFFSET, anchor = a.id))
        val aa = a.copy(times = mapOf("occurrence" to TimeExpr(TimeKind.OFFSET, anchor = b.id)))
        assertEquals(ResolutionStatus.INVALID, Chronology(listOf(aa, b)).resolve(a.id).status)
        assertEquals(
            ResolutionStatus.INVALID,
            Chronology(emptyList())
                .expression(
                    TimeExpr(
                        TimeKind.APPROXIMATE,
                        year = Long.MAX_VALUE.toString(),
                        tolerance = "20",
                    )
                )
                .status,
        )
    }

    @Test
    fun sameYearSequenceIsValid() {
        val a = event(TimeExpr(TimeKind.EXACT, year = "0"))
        val b = event(TimeExpr(TimeKind.EXACT, year = "0"))
        val order =
            Record(
                world = w.id,
                kind = Kind.ORDER,
                title = "Sequence",
                refs = mapOf("before" to listOf(a.id), "after" to listOf(b.id)),
            )
        assertTrue(Chronology(listOf(a, b, order)).diagnostics().isEmpty())
        assertEquals(
            listOf(a.id, b.id),
            Chronology(listOf(a, b, order)).ordered(listOf(b, a)).map { it.id },
        )
    }

    @Test
    fun orderCycleDetectedButGeneralGraphAllowed() {
        assertEquals(setOf("a", "b", "c"), cycleNodes(listOf("a" to "b", "b" to "c", "c" to "a")))
    }

    @Test
    fun transformInverseAndCentroidGenerated() {
        val random = Random(17)
        repeat(1000) {
            val v =
                Viewport(
                    random.nextDouble(.15, 4.0),
                    Point(random.nextDouble(-1e6, 1e6), random.nextDouble(-1e6, 1e6)),
                )
            val p = Point(random.nextDouble(-1e6, 1e6), random.nextDouble(-1e6, 1e6))
            val round = v.world(v.screen(p))
            assertEquals(p.x, round.x, 1e-7)
            assertEquals(p.y, round.y, 1e-7)
            val c = Point(150.0, 300.0)
            val z = v.zoom(c, 1.5)
            assertEquals(v.world(c).x, z.world(c).x, 1e-7)
        }
    }

    @Test
    fun largeTimelinePreservesNarrowDifferences() {
        assertEquals(2.0, timelineX(-999999999L, -1000000000L, 2.0), 0.0)
        assertEquals(2.0, timelineX(Long.MIN_VALUE + 1, Long.MIN_VALUE, 2.0), 0.0)
    }

    @Test
    fun overlapLanesAndSharedBoundary() {
        val l = lanes(listOf(LaneItem("a", -100, 0), LaneItem("b", -50, 50), LaneItem("c", 0, 5)))
        assertNotEquals(l["a"], l["b"])
        assertEquals(l["a"], l["c"])
    }

    @Test
    fun undoRedo() {
        val h = UndoHistory<Int>()
        h.push(1, 2)
        h.push(2, 3)
        assertEquals(2, h.undo())
        assertEquals(1, h.undo())
        assertEquals(2, h.redo())
        h.push(2, 4)
        assertFalse(h.canRedo)
    }

    @Test
    fun legacyScopedIdsAndUnicode() {
        val json =
            """{"format":"nodenote","version":1,"maps":[{"id":"m1","name":"Map","nodes":[{"id":"same","title":"Łódź 王朝 星","note":"a\n\n😀","x":0,"y":0}],"edges":[]},{"id":"m2","nodes":[{"id":"same","title":"Łódź 王朝 星","note":"different","x":1,"y":2}],"edges":[]}]}"""
        val b = Interchange.legacy(json).bundle
        val notes = b.records.filter { it.kind == Kind.NOTE }
        assertEquals(2, notes.size)
        assertNotEquals(notes[0].id, notes[1].id)
        assertEquals("a\n\n😀", notes[0].body)
    }

    @Test
    fun futureLegacyRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            Interchange.legacy("""{"format":"nodenote","version":2,"maps":[]}""")
        }
    }

    @Test
    fun canvasRoundTrip() {
        val b =
            Interchange.canvas(
                    """{"nodes":[{"id":"a","type":"text","text":"Full\n\nŁódź 😀","x":3,"y":8,"width":190,"height":112}],"edges":[]}"""
                )
                .bundle
        val c =
            Interchange.canvas(
                    Interchange.canvasExport(b, b.records.first { it.kind == Kind.BOARD }.id)
                )
                .bundle
        assertEquals(
            b.records.first { it.kind == Kind.NOTE }.body,
            c.records.first { it.kind == Kind.NOTE }.body,
        )
    }

    @Test
    fun remapAllReferencesAndRevision() {
        val a = event(TimeExpr(TimeKind.EXACT, year = "-9"))
        val b =
            event(TimeExpr(TimeKind.OFFSET, anchor = a.id))
                .copy(body = "[a](nodenote://entry/${a.id})")
        val rev =
            Record(
                world = w.id,
                kind = Kind.REVISION,
                title = "Revision",
                refs = mapOf("owner" to listOf(b.id)),
                fields = mapOf("snapshot" to codec.encodeToString(b)),
            )
        val source = WorldBundle(w, listOf(a, b, rev))
        val copy = Remap.world(source)
        Integrity.requireValid(copy)
        val encoded = codec.encodeToString(copy)
        assertFalse(encoded.contains(a.id))
        assertFalse(encoded.contains(b.id))
        assertFalse(encoded.contains(w.id))
        assertEquals(source.records.map { it.kind }, copy.records.map { it.kind })
    }

    @Test
    fun archiveRoundTripAndHash() {
        val folder = kotlin.io.path.createTempDirectory().toFile()
        try {
            val note = event(TimeExpr(TimeKind.UNKNOWN)).copy(body = "😀王朝".repeat(20000))
            val b = WorldBundle(w, listOf(note))
            val zip = File(folder, "backup.zip")
            zip.outputStream().use {
                Archives.write(Workspace(listOf(b)), it, { error("no assets") })
            }
            val p = Archives.inspect(zip, File(folder, "stage"))
            assertEquals(b, p.workspace.worlds.single())
        } finally {
            folder.deleteRecursively()
        }
    }

    @Test
    fun archiveTraversalAndCorruptionRejected() {
        val folder = kotlin.io.path.createTempDirectory().toFile()
        try {
            val zip = File(folder, "bad.zip")
            ZipOutputStream(zip.outputStream()).use {
                it.putNextEntry(ZipEntry("../escape"))
                it.write("bad".toByteArray())
                it.closeEntry()
            }
            assertThrows(IllegalArgumentException::class.java) {
                Archives.inspect(zip, File(folder, "stage"))
            }
            assertFalse(File(folder, "escape").exists())
            assertFalse(File(folder, "stage").exists())
        } finally {
            folder.deleteRecursively()
        }
    }

    @Test
    fun publicProjectionLeaksNoCanonicalToken() {
        val secret = "PRIVATE_SENTINEL_92481"
        val a = Record(world = w.id, kind = Kind.ACCOUNT, title = secret, body = secret)
        val e = event(TimeExpr()).copy(title = secret, body = secret)
        val assertion =
            Record(
                world = w.id,
                kind = Kind.ASSERTION,
                title = secret,
                body = secret,
                refs = mapOf("account" to listOf(a.id), "subject" to listOf(e.id)),
                fields =
                    mapOf(
                        "include" to "true",
                        "public title" to "Published age",
                        "public summary" to "A quiet age.",
                    ),
            )
        val public =
            Projection.markdown(Projection.account(WorldBundle(w, listOf(a, e, assertion)), a.id))
        assertFalse(public.contains(secret))
        assertFalse(public.contains(e.id))
        assertTrue(public.contains("A quiet age."))
    }
}
