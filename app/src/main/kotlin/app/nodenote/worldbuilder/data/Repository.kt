package app.nodenote.worldbuilder.data

import android.content.Context
import androidx.room.withTransaction
import app.nodenote.core.*
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

@OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class Repository(
    context: Context,
    val db: NoteDatabase = NoteDatabase.get(context),
) {
    private val dao = db.dao()
    private val writer = db.writeMutex
    val assetRoot = File(context.filesDir, "assets").also { it.mkdirs() }
    val worlds = dao.worlds().map { rows -> rows.map { it.domain() } }

    fun observe(world: String): Flow<WorldBundle?> = flow {
        var cached = emptyMap<String, Record>()
        var generations = emptyMap<String, Long>()
        db.invalidationTracker
            .createFlow("worlds", "records", "fields", "refs", "times")
            .debounce(60)
            .collect {
                val next = db.withTransaction {
                    val metadata =
                        dao.allWorlds().firstOrNull { it.id == world }
                            ?: return@withTransaction null
                    val ids = dao.recordIds(world)
                    val nextGenerations = ids.associateWith { db.recordVersions[it] ?: 0L }
                    val changed = ids.filter {
                        it !in cached || generations[it] != nextGenerations[it]
                    }
                    val loaded = mutableMapOf<String, Record>()
                    changed.chunked(500).forEach { chunk ->
                        val fields = dao.fieldsByIds(chunk).groupBy { it.owner }
                        val refs = dao.refsByIds(chunk).groupBy { it.owner }
                        val times = dao.timesByIds(chunk).groupBy { it.owner }
                        dao.recordsByIds(chunk).forEach { row ->
                            loaded[row.id] =
                                row.domain(
                                    fields[row.id].orEmpty(),
                                    refs[row.id].orEmpty(),
                                    times[row.id].orEmpty(),
                                )
                        }
                    }
                    val records = ids.mapNotNull { loaded[it] ?: cached[it] }
                    cached = records.associateBy { it.id }
                    generations = nextGenerations
                    WorldBundle(metadata.domain(), records)
                }
                if (next == null) {
                    cached = emptyMap()
                    generations = emptyMap()
                }
                emit(next)
            }
    }
        .flowOn(Dispatchers.IO)

    suspend fun snapshot(world: String): WorldBundle = db.withTransaction {
        val w = dao.allWorlds().first { it.id == world }.domain()
        val fields = dao.fields(world).groupBy { it.owner }
        val refs = dao.refs(world).groupBy { it.owner }
        val times = dao.times(world).groupBy { it.owner }
        WorldBundle(
            w,
            dao.records(world).map { row ->
                row.domain(
                    fields[row.id].orEmpty(),
                    refs[row.id].orEmpty(),
                    times[row.id].orEmpty(),
                )
            },
        )
    }

    suspend fun workspace(): Workspace = writer.withLock {
        db.withTransaction { Workspace(dao.allWorlds().map { snapshot(it.id) }) }
    }

    suspend fun world(world: World) = writer.withLock { dao.world(world.row()) }

    suspend fun record(id: String): Record? = db.withTransaction {
        dao.record(id)?.domain(dao.recordFields(id), dao.recordRefs(id), dao.recordTimes(id))
    }

    /** Viewport-only changes cannot affect the lore graph and need no full-world validation. */
    suspend fun boardView(id: String, viewport: Viewport, filter: String) = writer.withLock {
        require(
            viewport.scale.isFinite() &&
                viewport.scale > 0 &&
                viewport.translation.x.isFinite() &&
                viewport.translation.y.isFinite()
        )
        db.withTransaction {
            val current = record(id) ?: return@withTransaction
            require(current.kind == Kind.BOARD)
            put(
                listOf(
                    current.copy(
                        revision = current.revision + 1,
                        updated = System.currentTimeMillis(),
                        fields =
                            current.fields +
                                mapOf(
                                    "scale" to viewport.scale.toString(),
                                    "pan x" to viewport.translation.x.toString(),
                                    "pan y" to viewport.translation.y.toString(),
                                    "filter" to filter,
                                ),
                    )
                )
            )
        }
    }

    suspend fun attachmentIds(): Set<String> = dao.attachmentIds().toSet()

    private suspend fun checkIdentities(
        records: List<Record>,
        replaceWorlds: Set<String> = emptySet(),
    ) {
        require(records.map { it.id }.distinct().size == records.size) {
            "Duplicate record identities across imported worlds"
        }
        records
            .map { it.id }
            .chunked(500)
            .forEach { ids ->
                require(dao.identities(ids).all { it.world in replaceWorlds }) {
                    "Record identity already belongs to another world; import as new worlds"
                }
            }
    }

    suspend fun insert(bundle: WorldBundle) = writer.withLock {
        Integrity.requireValid(bundle)
        db.withTransaction {
            require(dao.allWorlds().none { it.id == bundle.world.id }) {
                "World already exists; use import as new world"
            }
            checkIdentities(bundle.records)
            dao.world(bundle.world.row())
            put(bundle.records)
        }
    }

    suspend fun insertMany(bundles: List<WorldBundle>) = writer.withLock {
        bundles.forEach(Integrity::requireValid)
        db.withTransaction {
            require(bundles.map { it.world.id }.distinct().size == bundles.size) {
                "Duplicate world identities"
            }
            checkIdentities(bundles.flatMap { it.records })
            bundles.forEach { bundle ->
                require(dao.allWorlds().none { it.id == bundle.world.id })
                dao.world(bundle.world.row())
            }
            bundles.forEach { put(it.records) }
        }
    }

    suspend fun replaceMany(bundles: List<WorldBundle>) = writer.withLock {
        bundles.forEach(Integrity::requireValid)
        db.withTransaction {
            require(bundles.map { it.world.id }.distinct().size == bundles.size) {
                "Duplicate world identities"
            }
            checkIdentities(bundles.flatMap { it.records }, bundles.map { it.world.id }.toSet())
            bundles.forEach { bundle ->
                dao.clearWorldRefs(bundle.world.id)
            }
            bundles.forEach { bundle ->
                dao.clearWorldIndex(bundle.world.id)
                dao.deleteWorldRecords(bundle.world.id)
                dao.world(bundle.world.row())
            }
            put(bundles.flatMap { it.records })
        }
    }

    suspend fun deleteWorld(id: String) = writer.withLock {
        db.withTransaction {
            dao.clearWorldRefs(id)
            dao.clearWorldIndex(id)
            dao.deleteWorldRecords(id)
            dao.deleteWorld(id)
        }
    }

    suspend fun eraseContent(r: Record) = writer.withLock {
        db.withTransaction {
            val current = snapshot(r.world)
            require(current.records.first { it.id == r.id }.trashed) { "Move to trash first" }
            val owned =
                current.records.filter {
                    it.ref("owner") == r.id || it.kind == Kind.STATE && it.ref("entry") == r.id
                }
            val revisions = owned.filter { it.kind in setOf(Kind.REVISION, Kind.DRAFT) }
            revisions.forEach {
                dao.clearIndex(it.id)
                dao.deleteRecord(it.id)
            }
            val tombstones =
                (owned - revisions.toSet() + r).map {
                    it.copy(
                        title = "[Permanently erased]",
                        summary = "",
                        body = "",
                        fields = if (it.kind == Kind.ATTACHMENT) it.fields else emptyMap(),
                        times = emptyMap(),
                        trashed = true,
                        locked = false,
                    )
                }
            put(tombstones)
        }
    }

    private suspend fun put(records: List<Record>) {
        dao.records(
            records.map { it.row() }
        ) // Register all identities before inserting references.
        records
            .map { it.id }
            .chunked(500)
            .forEach { ids ->
                dao.clearFieldsFor(ids)
                dao.clearRefsFor(ids)
                dao.clearTimesFor(ids)
                dao.clearIndexesFor(ids)
            }
        records.chunked(500).forEach { chunk ->
            dao.fields(chunk.flatMap { r -> r.fields.map { FieldRow(r.id, it.key, it.value) } })
            dao.refs(
                chunk.flatMap { r ->
                    r.refs.flatMap { (role, targets) ->
                        targets.mapIndexed { i, id -> RefRow(r.id, role, i, id) }
                    }
                }
            )
            dao.times(
                chunk.flatMap { r ->
                    r.times.map { TimeRow(r.id, it.key, codec.encodeToString(it.value)) }
                }
            )
            dao.indexes(
                chunk
                    .filter { it.kind !in setOf(Kind.REVISION, Kind.DRAFT, Kind.IMPORT) }
                    .map { r ->
                        SearchRow(
                            r.id,
                            r.world,
                            listOf(r.title, r.summary, r.body, r.fields.values.joinToString(" "))
                                .joinToString("\n"),
                        )
                    }
            )
        }
        val generation = db.changeVersion.incrementAndGet()
        records.forEach { db.recordVersions[it.id] = generation }
    }

    suspend fun save(record: Record, expected: Long? = null, checkpoint: Boolean = false): Record =
        writer.withLock {
            db.withTransaction {
                val current = snapshot(record.world)
                val old = current.records.find { it.id == record.id }
                if (old == null) checkIdentities(listOf(record))
                if (old?.locked == true && record.locked)
                    error("Entry is protected. Unlock explicitly before editing.")
                if (expected != null && old != null && old.revision != expected) {
                    // Draft is committed separately before save, retaining both sides even if this
                    // transaction fails.
                    error(
                        "Revision conflict. Your recoverable draft and the saved version are retained."
                    )
                }
                val saved =
                    record.copy(
                        revision = (old?.revision ?: 0) + 1,
                        updated = System.currentTimeMillis(),
                    )
                val candidate =
                    current.copy(records = current.records.filterNot { it.id == saved.id } + saved)
                Integrity.requireValid(candidate)
                if (checkpoint && old != null && old.kind !in setOf(Kind.REVISION, Kind.DRAFT))
                    put(
                        listOf(
                            Record(
                                world = old.world,
                                kind = Kind.REVISION,
                                title = "Revision ${old.revision} • ${old.title}",
                                refs = mapOf("owner" to listOf(old.id)),
                                fields =
                                    mapOf(
                                        "snapshot" to codec.encodeToString(old),
                                        "changed sections" to
                                            "Title, prose, fields, references or chronology",
                                    ),
                            )
                        )
                    )
                put(listOf(saved))
                if (checkpoint) {
                    val limit =
                        current.records
                            .firstOrNull { it.kind == Kind.SETTINGS }
                            ?.f("revision retention")
                            ?.toIntOrNull() ?: 0
                    if (limit > 0) {
                        val revisions =
                            snapshot(record.world)
                                .records
                                .filter { it.kind == Kind.REVISION && it.ref("owner") == record.id }
                                .sortedByDescending { it.created }
                        revisions.drop(limit).forEach {
                            dao.clearIndex(it.id)
                            dao.deleteRecord(it.id)
                        }
                    }
                }
                saved
            }
        }

    suspend fun draft(record: Record, sequence: Long) = writer.withLock {
        db.withTransaction {
            require(dao.record(record.id)?.world == record.world) {
                "Draft owner is missing or belongs to another world"
            }
            val existing = dao.owned(record.world, record.id, Kind.DRAFT.name).firstOrNull()
            put(
                listOf(
                    Record(
                        id = existing?.id ?: newId(),
                        world = record.world,
                        kind = Kind.DRAFT,
                        title = "Recoverable draft • ${record.title}",
                        refs = mapOf("owner" to listOf(record.id)),
                        fields =
                            mapOf(
                                "snapshot" to codec.encodeToString(record),
                                "sequence" to sequence.toString(),
                            ),
                        updated = System.currentTimeMillis(),
                    )
                )
            )
        }
    }

    suspend fun clearDraft(owner: String, world: String) = writer.withLock {
        db.withTransaction {
            dao.owned(world, owner, Kind.DRAFT.name).forEach {
                dao.clearIndex(it.id)
                dao.deleteRecord(it.id)
            }
        }
    }

    suspend fun batch(world: String, records: List<Record>, remove: Set<String> = emptySet()) =
        writer.withLock {
            db.withTransaction {
                val existing = snapshot(world)
                require(records.all { it.world == world })
                val existingIds = existing.records.map { it.id }.toSet()
                require(remove.all { it in existingIds }) {
                    "Cannot remove a record from another world"
                }
                checkIdentities(records.filter { it.id !in existingIds })
                val changedIds = records.map { it.id }.toSet()
                val candidate =
                    existing.copy(
                        records =
                            existing.records.filterNot { old ->
                                old.id in remove || old.id in changedIds
                            } + records
                    )
                Integrity.requireValid(candidate)
                remove.forEach { dao.clearRefs(it) }
                remove.forEach {
                    dao.clearIndex(it)
                    dao.deleteRecord(it)
                }
                put(records)
            }
        }

    suspend fun search(world: String, query: String): List<String> =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) return@withContext emptyList()
            val tokens =
                query
                    .trim()
                    .split(Regex("\\s+"))
                    .filter { it.isNotBlank() }
                    .joinToString(" AND ") { "\"${it.replace("\"","\"\"")}\"*" }
            val fts = runCatching { dao.search(world, tokens) }.getOrDefault(emptyList())
            val escaped = query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
            (fts + dao.substring(world, "%$escaped%")).distinct()
        }

    suspend fun duplicate(world: String): WorldBundle =
        withContext(Dispatchers.IO) {
            val source = snapshot(world)
            val copy = Remap.world(source)
            source.records
                .zip(copy.records)
                .filter { it.first.kind == Kind.ATTACHMENT }
                .forEach { (a, b) -> asset(a).copyTo(asset(b), overwrite = false) }
            insert(copy)
            copy
        }

    fun asset(r: Record) = File(assetRoot, r.id)
}

fun World.row() =
    WorldRow(id, name, description, archived, origin, present, epochLabel, eraLabel, created)

fun WorldRow.domain() =
    World(id, name, description, archived, origin, present, epochLabel, eraLabel, created)

fun Record.row() =
    RecordRow(
        id,
        world,
        kind.name,
        title,
        summary,
        body,
        canon,
        writing,
        favorite,
        inbox,
        locked,
        trashed,
        revision,
        created,
        updated,
    )

fun RecordRow.domain(fields: List<FieldRow>, refs: List<RefRow>, times: List<TimeRow>) =
    Record(
        id,
        world,
        Kind.valueOf(kind),
        title,
        summary,
        body,
        canon,
        writing,
        favorite,
        inbox,
        locked,
        trashed,
        revision,
        created,
        updated,
        fields.associate { it.key to it.value },
        refs
            .groupBy { it.role }
            .mapValues { it.value.sortedBy { r -> r.ordinal }.map { r -> r.target } },
        times.associate { it.slot to codec.decodeFromString<TimeExpr>(it.expression) },
    )
