package app.nodenote.worldbuilder

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.nodenote.core.*
import app.nodenote.worldbuilder.data.*
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositoryInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: NoteDatabase
    private lateinit var repo: Repository

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(context, NoteDatabase::class.java).build()
        repo = Repository(context, db)
    }

    @After
    fun close() {
        db.close()
    }

    @Test
    fun configuredRevisionRetentionPrunesOnlySnapshots() = runBlocking {
        val world = World(name = "Retention")
        repo.insert(WorldBundle(world, emptyList()))
        repo.save(
            Record(
                world = world.id,
                kind = Kind.SETTINGS,
                title = "Settings",
                fields = mapOf("revision retention" to "2"),
            )
        )
        var entry =
            repo.save(Record(world = world.id, kind = Kind.NOTE, title = "Protected writing"))
        repeat(5) { entry = repo.save(entry.copy(body = "Revision $it"), entry.revision, true) }
        val result = repo.snapshot(world.id)
        assertEquals(2, result.records.count { it.kind == Kind.REVISION })
        assertEquals("Revision 4", result.records.first { it.id == entry.id }.body)
    }

    @Test
    fun demoIntegrityAndFullRoundTrip() = runBlocking {
        val sample = Demo.create()
        Integrity.requireValid(sample)
        repo.insert(sample)
        assertEquals(sample.records.toSet(), repo.snapshot(sample.world.id).records.toSet())
        val file = File(context.cacheDir, "test-${newId()}.nnbackup")
        val stage = File(context.cacheDir, "test-stage-${newId()}")
        try {
            file.outputStream().use { Archives.write(repo.workspace(), it, repo::asset) }
            val inspected = Archives.inspect(file, stage)
            val restored = Remap.world(inspected.workspace.worlds.single())
            repo.insert(restored)
            assertEquals(sample.records.size, repo.snapshot(restored.world.id).records.size)
            assertTrue(Integrity.validate(restored).none { it.severity == "ERROR" })
        } finally {
            file.delete()
            stage.deleteRecursively()
        }
    }

    @Test
    fun longUnicodeSaveSearchAndWorldIsolation() = runBlocking {
        val a = World(name = "A")
        val z = World(name = "Z")
        repo.insert(WorldBundle(a, emptyList()))
        repo.insert(WorldBundle(z, emptyList()))
        val prose = "Łódź 王朝 日本語 😀 é\n".repeat(10000)
        val r =
            repo.save(
                Record(
                    world = a.id,
                    kind = Kind.CHARACTER,
                    title = "Equal title",
                    body = prose,
                    fields = mapOf("aliases" to "UnusualAlias"),
                )
            )
        repo.save(Record(world = a.id, kind = Kind.CHARACTER, title = "Equal title"))
        assertEquals(prose, repo.snapshot(a.id).records.first { it.id == r.id }.body)
        assertTrue(r.id in repo.search(a.id, "UnusualAlias"))
        assertTrue(r.id in repo.search(a.id, "王朝"))
        assertTrue(repo.search(z.id, "王朝").isEmpty())
    }

    @Test
    fun placementChangesCannotOverwriteLoreOrOtherBoards() = runBlocking {
        val sample = Demo.create()
        repo.insert(sample)
        val person = sample.records.first { it.kind == Kind.CHARACTER }
        val placements =
            sample.records.filter { it.kind == Kind.PLACEMENT && it.ref("entry") == person.id }
        assertEquals(3, placements.size)
        val saved = repo.save(person.copy(body = "Fresh biography"), person.revision, true)
        val p = placements.first()
        repo.batch(person.world, listOf(p.withField("x", "900")))
        val state = repo.snapshot(person.world)
        assertEquals("Fresh biography", state.records.first { it.id == person.id }.body)
        assertEquals(placements[1], state.records.first { it.id == placements[1].id })
        val incident =
            state.records
                .filter { it.kind == Kind.EDGE && (it.ref("from") == p.id || it.ref("to") == p.id) }
                .map { it.id }
                .toSet()
        repo.batch(person.world, emptyList(), incident + p.id)
        assertEquals(saved, repo.snapshot(person.world).records.first { it.id == person.id })
        assertTrue(repo.snapshot(person.world).records.any { it.kind == Kind.RELATIONSHIP })
    }

    @Test
    fun rejectedEditIsAtomicAndDraftSurvives() = runBlocking {
        val sample = Demo.create()
        repo.insert(sample)
        val r = sample.records.first { it.kind == Kind.CHARACTER }
        repo.draft(r.copy(body = "Recover me"), 1)
        val stale = r.revision
        repo.save(r.copy(body = "Other writer"), stale, true)
        var rejected = false
        try {
            repo.save(r.copy(body = "Recover me"), stale)
        } catch (e: Exception) {
            rejected = true
        }
        assertTrue(rejected)
        val current = repo.snapshot(r.world)
        assertEquals("Other writer", current.records.first { it.id == r.id }.body)
        assertTrue(
            current.records.any { it.kind == Kind.DRAFT && it.f("snapshot").contains("Recover me") }
        )
        val bad =
            Record(
                world = r.world,
                kind = Kind.RELATIONSHIP,
                title = "Bad",
                refs = mapOf("source" to listOf(r.id), "target" to listOf(newId())),
            )
        try {
            repo.save(bad)
            fail("Expected rejection")
        } catch (_: IllegalArgumentException) {}
        assertFalse(repo.snapshot(r.world).records.any { it.id == bad.id })
    }

    @Test
    fun lockedEntryTrashRevisionAndDuplication() = runBlocking {
        val sample = Demo.create()
        repo.insert(sample)
        val r = sample.records.first { it.kind == Kind.CHARACTER }
        val locked =
            repo.save(
                r.copy(canon = "CANON", writing = "NEEDS_DEVELOPMENT", locked = true),
                r.revision,
                true,
            )
        try {
            repo.save(locked.copy(body = "Forbidden"))
            fail("Locked edit accepted")
        } catch (_: IllegalStateException) {}
        val unlocked = repo.save(locked.copy(locked = false), locked.revision, true)
        repo.save(unlocked.copy(trashed = true), unlocked.revision, true)
        val copy = Remap.world(repo.snapshot(r.world))
        repo.insert(copy)
        assertTrue(copy.records.any { it.trashed })
        assertTrue(copy.records.any { it.kind == Kind.REVISION })
        assertFalse(codec.encodeToString(copy).contains(r.id))
    }

    @Test
    fun archiveWithAssetsDraftsAndSecretsRoundTrips() = runBlocking {
        val w = World(name = "All content")
        val entry =
            Record(
                world = w.id,
                kind = Kind.NOTE,
                title = "Private",
                trashed = true,
                body = "Author secret",
            )
        val assetId = newId()
        val image = File(repo.assetRoot, assetId)
        image.writeBytes(byteArrayOf(1, 2, 3, 4, 5))
        val asset =
            Record(
                id = assetId,
                world = w.id,
                kind = Kind.ATTACHMENT,
                title = "fixture.bin",
                fields =
                    mapOf(
                        "path" to "assets/$assetId",
                        "bytes" to "5",
                        "sha256" to sha256(image),
                        "mime" to "application/octet-stream",
                    ),
            )
        val media =
            Record(
                world = w.id,
                kind = Kind.MEDIA,
                title = "Credit",
                refs = mapOf("owner" to listOf(entry.id), "attachment" to listOf(asset.id)),
            )
        val draft =
            Record(
                world = w.id,
                kind = Kind.DRAFT,
                title = "Draft",
                refs = mapOf("owner" to listOf(entry.id)),
                fields = mapOf("snapshot" to codec.encodeToString(entry)),
            )
        repo.insert(WorldBundle(w, listOf(entry, asset, media, draft)))
        val zip = File(context.cacheDir, "all-${newId()}.zip")
        val stage = File(context.cacheDir, "all-${newId()}")
        try {
            zip.outputStream().use { Archives.write(repo.workspace(), it, repo::asset) }
            val inspected = Archives.inspect(zip, stage)
            assertEquals(
                repo.snapshot(w.id).records.toSet(),
                inspected.workspace.worlds.single().records.toSet(),
            )
            assertEquals(
                sha256(image),
                sha256(inspected.files.getValue("worlds/${w.id}/assets/$assetId")),
            )
        } finally {
            image.delete()
            zip.delete()
            stage.deleteRecursively()
        }
    }
}
