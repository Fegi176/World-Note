package app.nodenote.worldbuilder

import android.app.Application
import android.net.Uri
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.nodenote.core.*
import app.nodenote.worldbuilder.data.*
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString

val android.content.Context.preferences by preferencesDataStore("ui-preferences")

data class PendingImport(
    val bundles: List<WorldBundle>,
    val warnings: List<String>,
    val archive: InspectedArchive? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class AppModel(application: Application) : AndroidViewModel(application) {
    val repo = Repository(application)
    val files = ManagedFiles(application, repo)
    val worlds = repo.worlds.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val prefs = MutableStateFlow<Map<String, String>>(emptyMap())
    val prefsReady = MutableStateFlow(false)
    val storageReady = MutableStateFlow(false)
    val selected = MutableStateFlow<String?>(null)
    val historyAccount = MutableStateFlow<String?>(null)
    val bundle =
        selected
            .flatMapLatest { if (it == null) flowOf(null) else repo.observe(it) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val editing = MutableStateFlow<Record?>(null)
    val saveState = MutableStateFlow("Saved")
    val message = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow<String?>(null)
    val pending = MutableStateFlow<PendingImport?>(null)
    val replaceRestore = MutableStateFlow(false)
    val busyCancellable = MutableStateFlow(true)
    private var operationCommitted = false
    val boardRequest = MutableStateFlow<String?>(null)
    val searchResults = MutableStateFlow<List<String>>(emptyList())
    private var autoSave: Job? = null
    private var task: Job? = null
    private var searchJob: Job? = null
    private var editSequence = 0L
    private var expectedRevision = 0L
    private var checkpoint = true
    private val editWriter = Mutex()
    private val backStack = ArrayDeque<String>()

    init {
        viewModelScope.launch {
            application.preferences.data.collect { p ->
                prefs.value = p.asMap().mapKeys { it.key.name }.mapValues { it.value.toString() }
                prefsReady.value = true
            }
        }
        viewModelScope.launch {
            combine(worlds, prefs, prefsReady) { ws, p, ready -> Triple(ws, p, ready) }
                .collect { (ws, p, ready) ->
                    if (ready && selected.value == null) {
                        val id =
                            ws.find { it.id == p["world"] }?.id
                                ?: ws.firstOrNull { !it.archived }?.id
                        historyAccount.value = p["nav.$id.account"]?.takeIf { it.isNotEmpty() }
                        selected.value = id
                    }
                }
        }
    }

    init {
        viewModelScope.launch {
            val loaded = bundle.filterNotNull().first()
            val id = prefs.value["open entry"]
            val validIds = loaded.records.map { it.id }.toSet()
            backStack.addAll(
                prefs.value["nav.${loaded.world.id}.backstack"].orEmpty().split(',').filter {
                    it in validIds && it != id
                }
            )
            loaded.records
                .find { it.id == id }
                ?.let { saved ->
                    if (editing.value == null) {
                        val recovery =
                            loaded.records.firstOrNull {
                                it.kind == Kind.DRAFT && it.ref("owner") == saved.id
                            }
                        editing.value =
                            recovery?.let {
                                runCatching { codec.decodeFromString<Record>(it.f("snapshot")) }
                                    .getOrNull()
                            } ?: saved
                        expectedRevision = saved.revision
                        saveState.value =
                            if (recovery != null) "Recovered draft — review and Save"
                            else if (saved.locked) "Protected" else "Saved"
                    }
                }
        }
        action {
            try {
                repo.db.startupMutex.withLock {
                    if (!repo.db.startupRecovered) {
                        RestoreEngine(repo, files).recoverJournals()
                        repo.db.startupRecovered = true
                    }
                }
            } finally {
                storageReady.value = true
            }
        }
    }

    fun preference(key: String, value: String) {
        viewModelScope.launch {
            getApplication<Application>().preferences.edit { it[stringPreferencesKey(key)] = value }
        }
    }

    fun chooseWorld(id: String) {
        navigate {
            historyAccount.value = prefs.value["nav.$id.account"]?.takeIf { it.isNotEmpty() }
            backStack.clear()
            selected.value = id
            preference("world", id)
            preference("open entry", "")
            preference("nav.$id.backstack", "")
            editing.value = null
        }
    }

    fun setHistoryAccount(id: String?) {
        historyAccount.value = id
        selected.value?.let { preference("nav.$it.account", id.orEmpty()) }
    }

    fun action(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message.value = e.message ?: "Operation failed"
            }
        }
    }

    fun newWorld(name: String, sample: Boolean = false) = action {
        val b =
            if (sample) withContext(Dispatchers.IO) { files.sampleImages(Demo.create()) }
            else
                World(name = name.trim().ifBlank { "My world" }).let {
                    WorldBundle(it, Demo.templates(it.id))
                }
        repo.insert(b)
        historyAccount.value = null
        editing.value = null
        backStack.clear()
        selected.value = b.world.id
        preference("world", b.world.id)
        preference("open entry", "")
    }

    fun open(record: Record) {
        navigate {
            editing.value?.id?.takeIf { it != record.id }?.let { backStack.addLast(it) }
            editing.value = record
            expectedRevision = record.revision
            checkpoint = true
            editSequence = 0
            saveState.value = if (record.locked) "Protected" else "Saved"
            preference("open entry", record.id)
            preference("nav.${record.world}.backstack", backStack.joinToString(","))
        }
    }

    fun edit(record: Record) {
        editing.value = record
        editSequence++
        saveState.value = "Saving…"
        autoSave?.cancel()
        autoSave = viewModelScope.launch {
            delay(450)
            withContext(NonCancellable) { commit() }
        }
    }

    private suspend fun commit(): Boolean = editWriter.withLock {
        val record = editing.value ?: return@withLock true
        if (saveState.value in listOf("Saved", "Protected")) return@withLock true
        val sequence = editSequence
        try {
            withContext(Dispatchers.IO) { repo.draft(record, sequence) }
            val saved =
                withContext(Dispatchers.IO) { repo.save(record, expectedRevision, checkpoint) }
            expectedRevision = saved.revision
            checkpoint = false
            if (sequence == editSequence && editing.value?.id == record.id) {
                editing.value = saved
                saveState.value = "Saved"
                repo.clearDraft(record.id, record.world)
            }
            true
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            saveState.value = "Save failed — ${e.message}. Draft retained; retry or export text."
            false
        }
    }

    fun retry() {
        action { commit() }
    }

    fun flush() {
        autoSave?.cancel()
        action { commit() }
    }

    fun navigate(block: () -> Unit) {
        autoSave?.cancel()
        action {
            if (commit()) block()
            else
                message.value =
                    "Navigation paused to protect the draft. Correct the fields, retry, or export the text."
        }
    }

    fun close() = navigate {
        val previous =
            backStack.removeLastOrNull()?.let { id -> bundle.value?.records?.find { it.id == id } }
        editing.value = previous
        expectedRevision = previous?.revision ?: 0
        checkpoint = true
        editSequence = 0
        saveState.value = if (previous?.locked == true) "Protected" else "Saved"
        preference("open entry", previous?.id.orEmpty())
        selected.value?.let { preference("nav.$it.backstack", backStack.joinToString(",")) }
    }

    fun create(
        kind: Kind,
        title: String,
        refs: Map<String, List<String>> = emptyMap(),
        fields: Map<String, String> = emptyMap(),
        open: Boolean = true,
    ) = action {
        val w = selected.value ?: return@action
        val r =
            Record(
                world = w,
                kind = kind,
                title = title.trim().ifBlank { "Untitled ${kind.label}" },
                refs = refs,
                fields = fields,
                inbox = kind == Kind.NOTE,
            )
        val saved = repo.save(r)
        if (open) open(saved)
    }

    suspend fun add(r: Record) = repo.save(r)

    fun legacyBoard(root: Record) = action {
        val b = repo.snapshot(root.world)
        val existing = b.records.firstOrNull { it.kind == Kind.BOARD && it.ref("focus") == root.id }
        if (existing != null) {
            editing.value = null
            backStack.clear()
            boardRequest.value = existing.id
            return@action
        }
        var ids = setOf(root.id)
        repeat(2) {
            ids =
                ids +
                    b.records
                        .filter {
                            it.kind == Kind.RELATIONSHIP &&
                                (it.ref("source") in ids || it.ref("target") in ids)
                        }
                        .flatMap { listOfNotNull(it.ref("source"), it.ref("target")) }
        }
        val board =
            Record(
                world = root.world,
                kind = Kind.BOARD,
                title = "Legacy · ${root.title}",
                refs = mapOf("focus" to listOf(root.id)),
            )
        val cards =
            b.records
                .filter { it.id in ids && it.kind.lore }
                .mapIndexed { i, r ->
                    Record(
                        world = root.world,
                        kind = Kind.PLACEMENT,
                        title = r.title,
                        refs = mapOf("board" to listOf(board.id), "entry" to listOf(r.id)),
                        fields =
                            mapOf(
                                "x" to ((i % 3) * 230).toString(),
                                "y" to ((i / 3) * 160).toString(),
                                "width" to "190",
                                "height" to "112",
                            ),
                    )
                }
        val edges =
            b.records
                .filter {
                    it.kind == Kind.RELATIONSHIP &&
                        it.ref("source") in ids &&
                        it.ref("target") in ids
                }
                .map { rel ->
                    Record(
                        world = root.world,
                        kind = Kind.EDGE,
                        title = rel.title,
                        refs =
                            mapOf(
                                "board" to listOf(board.id),
                                "from" to
                                    listOf(cards.first { it.ref("entry") == rel.ref("source") }.id),
                                "to" to
                                    listOf(cards.first { it.ref("entry") == rel.ref("target") }.id),
                                "relationship" to listOf(rel.id),
                            ),
                    )
                }
        repo.batch(root.world, listOf(board) + cards + edges)
        editing.value = null
        backStack.clear()
        boardRequest.value = board.id
    }

    fun update(r: Record) = action {
        val saved = repo.save(r, checkpoint = true)
        if (editing.value?.id == r.id) {
            editing.value = saved
            expectedRevision = saved.revision
            saveState.value = if (saved.locked) "Protected" else "Saved"
        }
    }

    fun trash(r: Record) = update(r.copy(trashed = !r.trashed, locked = false))

    private suspend fun preserveAlternate(record: Record) {
        repo.save(
            Record(
                world = record.world,
                kind = Kind.REVISION,
                title = "Alternate draft • ${record.title}",
                refs = mapOf("owner" to listOf(record.id)),
                fields =
                    mapOf(
                        "snapshot" to codec.encodeToString(record),
                        "changed sections" to "Draft preserved during explicit recovery",
                    ),
            )
        )
    }

    fun resolveConflict(keepDraft: Boolean) {
        autoSave?.cancel()
        operation("Resolving revision conflict…") {
            busyCancellable.value = false
            withContext(NonCancellable) {
                editWriter.withLock {
                    val draft = editing.value ?: return@withLock
                    val live = repo.snapshot(draft.world).records.first { it.id == draft.id }
                    val saved =
                        if (keepDraft) repo.save(draft.copy(locked = false), live.revision, true)
                        else {
                            preserveAlternate(draft)
                            live
                        }
                    repo.clearDraft(saved.id, saved.world)
                    editing.value = saved
                    expectedRevision = saved.revision
                    editSequence = 0
                    checkpoint = true
                    saveState.value = if (saved.locked) "Protected" else "Saved"
                    operationCommitted = true
                }
            }
        }
    }

    fun restoreRevision(revision: Record) {
        autoSave?.cancel()
        operation("Restoring revision…") {
            busyCancellable.value = false
            withContext(NonCancellable) {
                editWriter.withLock {
                    val owner = revision.ref("owner") ?: error("No revision owner")
                    val old = codec.decodeFromString<Record>(revision.f("snapshot"))
                    editing.value
                        ?.takeIf {
                            it.id == owner && saveState.value !in listOf("Saved", "Protected")
                        }
                        ?.let { preserveAlternate(it) }
                    val now = repo.snapshot(old.world).records.first { it.id == owner }
                    val saved = repo.save(old.copy(id = owner, locked = false), now.revision, true)
                    repo.clearDraft(owner, old.world)
                    editing.value = saved
                    expectedRevision = saved.revision
                    editSequence = 0
                    checkpoint = true
                    saveState.value = "Saved"
                    operationCommitted = true
                }
            }
        }
    }

    fun template(template: Record, owner: Record) = action {
        val b = repo.snapshot(owner.world)
        val defs =
            b.records.filter {
                it.ref("owner") == template.id && !it.trashed && it.f("hidden") != "true"
            }
        repo.batch(
            owner.world,
            defs.map {
                it.copy(
                    id = newId(),
                    refs = it.refs + ("owner" to listOf(owner.id)),
                    fields = it.fields + ("definition" to it.id),
                    created = System.currentTimeMillis(),
                )
            },
        )
    }

    fun search(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(200)
            searchResults.value = selected.value?.let { repo.search(it, query) }.orEmpty()
        }
    }

    fun boardImage(uri: Uri, board: Record, position: Point) =
        operation("Adding image to board…") {
            busyCancellable.value = false
            withContext(NonCancellable) {
                files.placeImage(uri, board, position)
                operationCommitted = true
            }
            message.value = "Image added. Open its card to edit notes or Media."
        }

    fun attach(uri: Uri, owner: Record) =
        operation("Copying image…") {
            val pair = files.image(uri, owner)
            repo.batch(owner.world, listOf(pair.first, pair.second))
            message.value = "Image copied into private storage"
        }

    fun operation(label: String, block: suspend () -> Unit) {
        if (task?.isActive == true) {
            message.value = "An operation is already running"
            return
        }
        task = viewModelScope.launch {
            busy.value = label
            busyCancellable.value = true
            operationCommitted = false
            try {
                withContext(Dispatchers.IO) { block() }
            } catch (e: CancellationException) {
                if (!operationCommitted)
                    message.value = "Cancelled before commit. Existing worlds preserved."
            } catch (e: Exception) {
                message.value = e.message ?: "Operation failed"
            } finally {
                busy.value = null
                busyCancellable.value = true
            }
        }
    }

    fun cancelOperation() {
        if (busyCancellable.value) task?.cancel()
    }

    fun inspect(uri: Uri) =
        operation("Reading and validating import…") {
            val source = files.copyInput(uri)
            require(source.usableSpace > source.length() * 2 + 16L * 1024 * 1024) {
                "Insufficient free space for safe restore"
            }
            val isZip = source.inputStream().use { it.read() == 80 && it.read() == 75 }
            if (isZip) {
                val archive = runInterruptible {
                    Archives.inspect(
                        source,
                        File(getApplication<Application>().cacheDir, "restore-${newId()}"),
                    ) {
                        busy.value = "Validating $it"
                    }
                }
                pending.value = PendingImport(archive.workspace.worlds, archive.warnings, archive)
            } else {
                require(source.length() <= Archives.MAX_FILE)
                val text = boundedJson(source.readText())
                val root = codec.parseToJsonElement(text).toString()
                val preview =
                    if (root.contains("\"format\":\"nodenote\"")) Interchange.legacy(text)
                    else Interchange.canvas(text)
                pending.value = PendingImport(listOf(preview.bundle), preview.warnings)
            }
        }

    fun dismissImport() {
        pending.value?.archive?.root?.deleteRecursively()
        pending.value = null
    }

    fun restore() =
        operation("Restoring validated worlds…") {
            val p = pending.value ?: error("No validated import")
            val replacing = replaceRestore.value
            val copies =
                RestoreEngine(repo, files)
                    .restore(
                        p.bundles,
                        p.archive,
                        replacing,
                        beforeCommit = { busyCancellable.value = false },
                        afterCommit = { operationCommitted = true },
                    )
            withContext(NonCancellable) {
                val ids = mutableMapOf<String, String>()
                p.bundles.zip(copies).forEach { (old, new) ->
                    ids[old.world.id] = new.world.id
                    old.records.zip(new.records).forEach { (a, z) -> ids[a.id] = z.id }
                }
                val restoredWorld =
                    copies.firstOrNull {
                        it.world.id == ids[p.archive?.workspace?.preferences?.get("world")]
                    } ?: copies.first()
                val failure = runCatching {
                    getApplication<Application>().preferences.edit { target ->
                        p.archive?.workspace?.preferences.orEmpty().forEach { (key, value) ->
                            val pattern = Regex("[a-fA-F0-9-]{36}")
                            val remappedKey = pattern.replace(key) { ids[it.value] ?: it.value }
                            val remappedValue =
                                if (key.endsWith(".selection") || key.endsWith(".backstack"))
                                    pattern.replace(value) { ids[it.value] ?: it.value }
                                else ids[value] ?: value
                            target[stringPreferencesKey(remappedKey)] = remappedValue
                        }
                        target[stringPreferencesKey("world")] = restoredWorld.world.id
                        target[stringPreferencesKey("open entry")] = ""
                    }
                }
                    .exceptionOrNull()
                selected.value = restoredWorld.world.id
                editing.value = null
                historyAccount.value = null
                backStack.clear()
                pending.value = null
                replaceRestore.value = false
                p.archive?.root?.deleteRecursively()
                message.value =
                    "Restored ${copies.size} world(s). Original source retained in recovery." +
                        (if (replacing) " Pre-restore recovery backup saved internally."
                        else " Imported with new identities.") +
                        (if (failure != null)
                            " UI preference restore failed; world content was restored successfully."
                        else "")
            }
        }

    fun deleteWorld(world: World) =
        operation("Saving recovery backup before world deletion…") {
            val source = repo.snapshot(world.id)
            files.safetyCopy(Workspace(listOf(source)), "delete")
            repo.deleteWorld(world.id)
            if (selected.value == world.id) {
                selected.value = null
                editing.value = null
                backStack.clear()
            }
        }

    fun erase(r: Record) =
        operation("Saving recovery backup before content erasure…") {
            val source = repo.snapshot(r.world)
            files.safetyCopy(Workspace(listOf(source)), "erase")
            repo.eraseContent(r)
            editing.value = null
            backStack.clear()
        }

    fun worldSetting(key: String, value: String) = action {
        val b = repo.snapshot(selected.value ?: return@action)
        val settings =
            b.records.firstOrNull { it.kind == Kind.SETTINGS }
                ?: Record(world = b.world.id, kind = Kind.SETTINGS, title = "World preferences")
        repo.save(settings.withField(key, value))
    }

    fun export(uri: Uri, mode: String, board: String? = null, account: String? = null) =
        operation("Writing $mode…") {
            val b = selected.value?.let { repo.snapshot(it) } ?: error("Choose a world")
            val resolver = getApplication<Application>().contentResolver
            resolver.openOutputStream(uri, "wt")?.use { out ->
                when (mode) {
                    "backup" -> {
                        val snapshot = repo.workspace().copy(preferences = prefs.value)
                        runInterruptible {
                            Archives.write(snapshot, out, repo::asset) {
                                busy.value = "Writing $it"
                            }
                        }
                    }
                    "world" ->
                        runInterruptible {
                            Archives.write(Workspace(listOf(b), prefs.value), out, repo::asset)
                        }
                    "markdown" -> out.write(WorldBible.combined(b).toByteArray())
                    "bible zip" -> WorldBible.zip(b, out, repo::asset)
                    "public" ->
                        out.write(
                            Projection.markdown(
                                    Projection.account(b, account ?: error("Choose account"))
                                )
                                .toByteArray()
                        )
                    "canvas" ->
                        out.write(
                            Interchange.canvasExport(b, board ?: error("Choose board"))
                                .toByteArray()
                        )
                    "draft" -> out.write((editing.value?.body ?: "").toByteArray())
                    else -> error("Unsupported export")
                }
            } ?: error("Provider did not open output")
            currentCoroutineContext().ensureActive()
            if (mode == "backup" || mode == "world")
                getApplication<Application>().preferences.edit {
                    it[
                        stringPreferencesKey(
                            if (mode == "backup") "last workspace backup"
                            else "backup.${b.world.id}"
                        )] = System.currentTimeMillis().toString()
                }
            message.value =
                "Export finished. ${if(mode in listOf("backup","world"))"Contains all author notes and secrets; archive is not encrypted." else "Reading/interchange export; not a full backup."}"
        }
}
