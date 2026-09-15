package app.nodenote.worldbuilder

import android.graphics.Bitmap
import android.net.Uri
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.nodenote.core.*
import app.nodenote.worldbuilder.data.*
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HardeningInstrumentedTest {
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
    fun incrementalObservationLoadsChangedChildrenAndReusesUnchangedProse() = runBlocking {
        val world = World(name = "Incremental observation")
        val a =
            Record(
                world = world.id,
                kind = Kind.NOTE,
                title = "Stable",
                body = "Long prose ".repeat(10000),
            )
        val b = a.copy(id = newId(), title = "Changing", body = "Before")
        repo.insert(WorldBundle(world, listOf(a, b)))
        val ready = CompletableDeferred<Unit>()
        var first: WorldBundle? = null
        val result =
            async(start = CoroutineStart.UNDISPATCHED) {
                withTimeout(10000) {
                    repo.observe(world.id).first { value ->
                        if (first == null && value != null) {
                            first = value
                            ready.complete(Unit)
                        }
                        value?.records?.find { it.id == b.id }?.f("custom") ==
                            "Changed without revision bump"
                    }
                }
            }
        ready.await()
        repo.batch(world.id, listOf(b.withField("custom", "Changed without revision bump")))
        val second = result.await()!!
        assertSame(
            first!!.records.single { it.id == a.id },
            second.records.single { it.id == a.id },
        )
        assertEquals(
            "Changed without revision bump",
            second.records.single { it.id == b.id }.f("custom"),
        )
    }

    @Test
    fun broadSearchDoesNotHideLaterMatchesBeforeUiFiltersAreApplied() = runBlocking {
        val world = World(name = "Search coverage")
        val records =
            (0 until 250).map {
                Record(world = world.id, kind = Kind.NOTE, title = "Shared marker $it")
            }
        val character =
            Record(world = world.id, kind = Kind.CHARACTER, title = "Shared marker character")
        repo.insert(WorldBundle(world, records + character))
        val hits = repo.search(world.id, "Shared marker").toSet()
        assertEquals(251, hits.size)
        assertTrue(character.id in hits)
    }

    @Test
    fun collidingImportsAndForeignRemovalsCannotRewriteAnotherWorld() = runBlocking {
        val original = World(name = "Original")
        val note =
            Record(
                world = original.id,
                kind = Kind.NOTE,
                title = "Keep me",
                body = "Exact original prose",
            )
        repo.insert(WorldBundle(original, listOf(note)))
        val alien = World(name = "Alien")
        val collision = WorldBundle(alien, listOf(note.copy(world = alien.id, body = "Collision")))
        assertTrue(runCatching { repo.insert(collision) }.isFailure)
        assertEquals(listOf(original.id), repo.workspace().worlds.map { it.world.id })
        repo.insert(WorldBundle(alien, emptyList()))
        assertTrue(runCatching { repo.save(note.copy(world = alien.id)) }.isFailure)
        assertTrue(runCatching { repo.batch(alien.id, emptyList(), setOf(note.id)) }.isFailure)
        assertTrue(runCatching { repo.replaceMany(listOf(collision)) }.isFailure)
        assertEquals(note, repo.record(note.id))
        assertTrue(repo.snapshot(alien.id).records.isEmpty())
    }

    @Test
    fun duplicatedIdsAcrossOneImportAreRejectedAtomically() = runBlocking {
        val a = World(name = "A")
        val b = World(name = "B")
        val r = Record(world = a.id, kind = Kind.NOTE, title = "Same identity")
        assertTrue(
            runCatching {
                repo.insertMany(
                    listOf(
                        WorldBundle(a, listOf(r)),
                        WorldBundle(b, listOf(r.copy(world = b.id))),
                    )
                )
            }
                .isFailure
        )
        assertTrue(repo.workspace().worlds.isEmpty())
    }

    @Test
    fun removingAnEntireBoardKeepsSharedLoreAndRelationshipNotes() = runBlocking {
        val sample = Demo.create()
        repo.insert(sample)
        val board = sample.records.first { it.kind == Kind.BOARD }
        val removed =
            sample.records
                .filter { it.id == board.id || it.ref("board") == board.id }
                .map { it.id }
                .toSet()
        val protected =
            sample.records.filter { it.kind.lore || it.kind == Kind.RELATIONSHIP }.toSet()
        repo.batch(sample.world.id, emptyList(), removed)
        val result = repo.snapshot(sample.world.id)
        assertTrue(result.records.containsAll(protected))
        assertTrue(result.records.none { it.id in removed })
        Integrity.requireValid(result)
    }

    @Test
    fun worldMetadataChangesReachTheObservedBundle() = runBlocking {
        val world = World(name = "Before")
        repo.insert(WorldBundle(world, emptyList()))
        val observed =
            async(start = CoroutineStart.UNDISPATCHED) {
                withTimeout(10000) { repo.observe(world.id).first { it?.world?.name == "After" } }
            }
        repo.world(world.copy(name = "After"))
        assertEquals("After", observed.await()!!.world.name)
    }

    @Test
    fun renamingEqualTitlesRetainsInlineLinksAndSeparateIdentities() = runBlocking {
        val w = World(name = "Ambiguous titles")
        val a = Record(world = w.id, kind = Kind.NOTE, title = "Same")
        val b = a.copy(id = newId())
        val link = a.copy(id = newId(), title = "Link", body = "[First](nodenote://entry/${a.id})")
        repo.insert(WorldBundle(w, listOf(a, b, link)))
        repo.save(a.copy(title = "Renamed"))
        assertEquals(link.body, repo.record(link.id)!!.body)
        assertEquals("Same", repo.record(b.id)!!.title)
        assertEquals("Renamed", repo.record(a.id)!!.title)
    }

    @Test
    fun storyReorderingAndRenamingCannotMoveHistoricalEventsOrLoseReveals() = runBlocking {
        val sample = Demo.create()
        repo.insert(sample)
        val story = sample.records.first { it.kind == Kind.STORY }
        val reveals = sample.records.filter { it.kind == Kind.REVEAL }
        val historical = sample.records.filter { it.kind == Kind.EVENT || it.kind.period }
        repo.save(
            story.copy(title = "Renamed chapter", fields = story.fields + ("order" to "-100"))
        )
        val after = repo.snapshot(sample.world.id)
        assertEquals(reveals.toSet(), after.records.filter { it.kind == Kind.REVEAL }.toSet())
        assertEquals(
            historical.toSet(),
            after.records.filter { it.kind == Kind.EVENT || it.kind.period }.toSet(),
        )
        Integrity.requireValid(after)
    }

    @Test
    fun canonicalClaimEditsDoNotInferCharacterKnowledgeOrRewritePublicAccounts() = runBlocking {
        val sample = Demo.create()
        repo.insert(sample)
        val claim = sample.records.first { it.kind == Kind.CLAIM }
        val knowledge = sample.records.filter { it.kind == Kind.KNOWLEDGE }
        val accounts =
            sample.records
                .filter { it.kind == Kind.ACCOUNT }
                .associate { it.id to Projection.account(sample, it.id) }
        repo.save(
            claim.copy(
                body = "Private revised explanation",
                fields = claim.fields + ("truth" to "UNDECIDED"),
            )
        )
        val after = repo.snapshot(sample.world.id)
        assertEquals(knowledge.toSet(), after.records.filter { it.kind == Kind.KNOWLEDGE }.toSet())
        accounts.forEach { (id, publicPages) ->
            assertEquals(publicPages, Projection.account(after, id))
        }
    }

    @Test
    fun parallelRelationshipsRetainIndependentNotesAndBoardReferences() = runBlocking {
        val sample = Demo.create()
        repo.insert(sample)
        val relationship = sample.records.first { it.kind == Kind.RELATIONSHIP }
        val parallel =
            relationship.copy(
                id = newId(),
                title = "A second reason",
                body = "Independent explanation",
            )
        repo.batch(sample.world.id, listOf(parallel))
        repo.save(relationship.copy(body = "Revised shared relationship notes"))
        val after = repo.snapshot(sample.world.id)
        assertEquals(parallel, after.records.single { it.id == parallel.id })
        assertEquals(
            sample.records.filter { it.kind == Kind.EDGE }.toSet(),
            after.records.filter { it.kind == Kind.EDGE }.toSet(),
        )
        assertEquals("Revised shared relationship notes", repo.record(relationship.id)!!.body)
    }

    @Test
    fun replacementRetainsAVerifiedSafetyArchiveAndDoesNotDuplicateTheWorld() = runBlocking {
        val files = ManagedFiles(context, repo)
        val original = Demo.create()
        repo.insert(original)
        val note = original.records.first { it.kind.lore }
        val modified =
            original.copy(
                records =
                    original.records.map {
                        if (it.id == note.id) it.copy(body = "Restored replacement prose") else it
                    }
            )
        val before = files.recoveryArchives().toSet()
        val stage = File(context.cacheDir, "verify-safety-${newId()}")
        try {
            RestoreEngine(repo, files).restore(listOf(modified), null, replace = true)
            assertEquals(1, repo.workspace().worlds.size)
            assertEquals("Restored replacement prose", repo.record(note.id)!!.body)
            val backup = (files.recoveryArchives().toSet() - before).single()
            assertEquals(original, Archives.inspect(backup, stage).workspace.worlds.single())
        } finally {
            (files.recoveryArchives().toSet() - before).forEach(files::removeRecoveryArchive)
            stage.deleteRecursively()
        }
    }

    @Test
    fun restoreFaultsRespectTheFileAndDatabaseCommitBoundary() = runBlocking {
        val files = ManagedFiles(context, repo)
        val original = files.sampleImages(Demo.create())
        repo.insert(original)
        val zip = File(context.cacheDir, "fault-${newId()}.nnbackup")
        val stage = File(context.cacheDir, "stage-${newId()}")
        try {
            zip.outputStream().use { Archives.write(Workspace(listOf(original)), it, repo::asset) }
            val archive = Archives.inspect(zip, stage)
            RestoreCheckpoint.entries.forEach { boundary ->
                val beforeWorlds = repo.workspace().worlds.map { it.world.id }.toSet()
                val beforeFiles = repo.assetRoot.list().orEmpty().toSet()
                val failure = runCatching {
                    RestoreEngine(repo, files)
                        .restore(
                            archive.workspace.worlds,
                            archive,
                            checkpoint = {
                                if (it == boundary) throw java.io.IOException("Injected $it")
                            },
                        )
                }
                assertTrue(failure.isFailure)
                RestoreEngine(repo, files).recoverJournals()
                val after = repo.workspace().worlds
                assertEquals(original, repo.snapshot(original.world.id))
                if (boundary == RestoreCheckpoint.AFTER_DATABASE) {
                    val restored = after.single { it.world.id !in beforeWorlds }
                    Integrity.requireValid(restored)
                    restored.records
                        .filter { it.kind == Kind.ATTACHMENT }
                        .forEach {
                            assertEquals(it.f("sha256"), sha256(repo.asset(it)))
                        }
                } else {
                    assertEquals(beforeWorlds, after.map { it.world.id }.toSet())
                    assertEquals(beforeFiles, repo.assetRoot.list().orEmpty().toSet())
                }
            }
        } finally {
            repo
                .workspace()
                .worlds
                .flatMap { it.records }
                .filter { it.kind == Kind.ATTACHMENT }
                .forEach {
                    repo.asset(it).delete()
                    files.removeThumbnail(it.id)
                }
            zip.delete()
            stage.deleteRecursively()
        }
    }

    @Test
    fun managedImageSurvivesDeletionOfThePickedSourceAndRejectsCorruption() = runBlocking {
        val w = World(name = "Images")
        val owner = Record(world = w.id, kind = Kind.NOTE, title = "Image owner")
        repo.insert(WorldBundle(w, listOf(owner)))
        val files = ManagedFiles(context, repo)
        val source = File(context.cacheDir, "source-${newId()}.png")
        val bad = File(context.cacheDir, "bad-${newId()}.png")
        var attachment: Record? = null
        try {
            val bitmap = Bitmap.createBitmap(24, 16, Bitmap.Config.ARGB_8888)
            source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            val pair = files.image(Uri.fromFile(source), owner)
            attachment = pair.first
            repo.batch(w.id, listOf(pair.first, pair.second))
            source.delete()
            assertEquals(pair.first.f("sha256"), sha256(repo.asset(pair.first)))
            assertTrue(files.thumbnail(pair.first).length() > 0)
            files.thumbnail(pair.first).writeBytes(byteArrayOf(1, 2, 3))
            assertTrue(
                "Damaged disposable preview must be regenerated",
                files.thumbnail(pair.first).length() > 3,
            )
            bad.writeBytes(byteArrayOf(1, 2, 3, 4))
            val before = repo.assetRoot.list().orEmpty().toSet()
            assertTrue(runCatching { files.image(Uri.fromFile(bad), owner) }.isFailure)
            assertEquals(before, repo.assetRoot.list().orEmpty().toSet())
        } finally {
            source.delete()
            bad.delete()
            attachment?.let {
                files.removeThumbnail(it.id)
                repo.asset(it).delete()
            }
        }
    }
}
