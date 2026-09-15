@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.nodenote.worldbuilder.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.nodenote.core.*
import app.nodenote.worldbuilder.*
import app.nodenote.worldbuilder.R
import java.text.DateFormat
import java.util.Date

@Composable
private fun RecordedHistory(vm: AppModel, b: WorldBundle, account: String, tab: String) {
    var query by rememberText(vm, "view.$account.publicQuery")
    // Project before filtering or rendering: canonical search, metadata and counts never enter this
    // view.
    val pages = remember(b, account) { Projection.account(b, account) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Heading(
                stringResource(R.string.atlas_30effb8078),
                "Only explicitly included account prose is shown across all tabs.",
            )
            TextButton(onClick = { vm.setHistoryAccount(null) }) {
                Text(stringResource(R.string.atlas_bbafbee877))
            }
            if (tab == "Search" || tab == "Library")
                OutlinedTextField(
                    query,
                    { query = it },
                    label = { Text(stringResource(R.string.atlas_0d7c5880d3)) },
                    modifier = Modifier.fillMaxWidth(),
                )
        }
        items(
            pages.filter {
                query.isBlank() || it.title.contains(query, true) || it.body.contains(query, true)
            }
        ) { page ->
            Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(page.title, style = MaterialTheme.typography.titleLarge)
                    MarkdownPreview(page.body) {}
                }
            }
        }
    }
}

@Composable
fun NodeNoteApp(vm: AppModel) {
    val largeText = androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f
    val ready by vm.prefsReady.collectAsStateWithLifecycle()
    val storageReady by vm.storageReady.collectAsStateWithLifecycle()
    if (!ready || !storageReady) {
        Box(
            Modifier.fillMaxSize().background(Color(0xFF080D10)),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            CircularProgressIndicator(color = Color(0xFF97B7FF))
        }
        return
    }
    val worlds by vm.worlds.collectAsStateWithLifecycle()
    val b by vm.bundle.collectAsStateWithLifecycle()
    val edit by vm.editing.collectAsStateWithLifecycle()
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val pending by vm.pending.collectAsStateWithLifecycle()
    val historyAccount by vm.historyAccount.collectAsStateWithLifecycle()
    var root by rememberText(vm, "nav.${b?.world?.id}.root", "Library")
    var menu by remember { mutableStateOf(false) }
    var worldMenu by remember { mutableStateOf(false) }
    var screen by rememberText(vm, "nav.${b?.world?.id}.screen")
    var board by rememberIdentity(vm, "nav.${b?.world?.id}.board")
    var deleteWorld by remember { mutableStateOf<World?>(null) }
    var create by remember { mutableStateOf<Kind?>(null) }
    var worldDialog by remember { mutableStateOf(false) }
    var worldName by remember { mutableStateOf("") }
    var exportMode by rememberSaveable { mutableStateOf("backup") }
    var accountId by rememberSaveable { mutableStateOf<String?>(null) }
    var askExport by remember { mutableStateOf<String?>(null) }
    val boardRequest by vm.boardRequest.collectAsStateWithLifecycle()
    LaunchedEffect(boardRequest) {
        if (boardRequest != null) {
            board = boardRequest
            screen = ""
            vm.boardRequest.value = null
        }
    }
    LaunchedEffect(b?.world?.id, b?.records?.size, board) {
        if (b != null && board != null && b!!.records.none { it.id == board && !it.trashed })
            board = null
    }
    val importPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
            if (it != null) vm.inspect(it)
        }
    val exportPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/octet-stream")
        ) {
            if (it != null) vm.export(it, exportMode, board, accountId)
        }
    fun requestExport(mode: String) {
        askExport = mode
    }
    val dark =
        when (prefs["theme"]) {
            "Light" -> false
            "System" -> isSystemInDarkTheme()
            else -> true
        }
    val view = androidx.compose.ui.platform.LocalView.current
    SideEffect {
        val activity = view.context as? android.app.Activity
        activity?.let {
            androidx.core.view.WindowCompat.getInsetsController(it.window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(
        colorScheme = if (dark) AtlasDark else AtlasLight,
        typography = AtlasTypography,
        shapes =
            Shapes(
                small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            ),
    ) {
        CompositionLocalProvider(
            LocalStatusLabels provides
                b?.records?.firstOrNull { it.kind == Kind.SETTINGS }?.fields.orEmpty()
        ) {
            BackHandler(edit != null || board != null || screen.isNotEmpty()) {
                when {
                    edit != null -> vm.close()
                    board != null -> board = null
                    else -> screen = ""
                }
            }
            Scaffold(
                topBar = {
                    AtlasTopBar(
                        title = {
                            TextButton(onClick = { worldMenu = true }) {
                                Column(Modifier.weight(1f, fill = false)) {
                                    Text(
                                        stringResource(R.string.atlas_c167fb329d),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        b?.world?.name ?: "Your next world",
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow =
                                            androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                AtlasIcon("Chevron", Modifier.size(16.dp))
                            }
                        },
                        actions = {
                            TextButton(onClick = { menu = true }) {
                                AtlasIcon("Menu", Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.ui_menu_99af66))
                            }
                            DropdownMenu(menu, { menu = false }) {
                                (if (historyAccount != null) listOf("Return to author's history")
                                    else
                                        listOf(
                                            "Worlds",
                                            "Settings & backup",
                                            "Storage & recovery",
                                            "Templates",
                                            "Intrigue & history",
                                            "Trash",
                                            "Consistency review",
                                        ))
                                    .forEach { name ->
                                        DropdownMenuItem(
                                            text = { Text(name) },
                                            onClick = {
                                                menu = false
                                                if (historyAccount != null) {
                                                    vm.setHistoryAccount(null)
                                                    return@DropdownMenuItem
                                                }
                                                vm.navigate {
                                                    vm.editing.value = null
                                                    screen = name
                                                    board = null
                                                }
                                            },
                                        )
                                    }
                            }
                        },
                    )
                },
                bottomBar = {
                    if (edit == null && board == null && screen.isEmpty())
                        Column {
                            if (largeText && historyAccount == null && b != null)
                                Button(
                                    onClick = {
                                        create =
                                            when (root) {
                                                "Boards" -> Kind.BOARD
                                                "Timeline" -> Kind.EPOCH
                                                else -> Kind.NOTE
                                            }
                                    },
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 4.dp),
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor = Cobalt,
                                            contentColor = Color.White,
                                        ),
                                ) {
                                    AtlasIcon("Add")
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        when (root) {
                                            "Boards" -> "New board"
                                            "Timeline" -> "New epoch"
                                            else -> "New entry"
                                        }
                                    )
                                }
                            AtlasNavigation(root) { root = it }
                        }
                },
                floatingActionButton = {
                    if (
                        !largeText &&
                            historyAccount == null &&
                            b != null &&
                            edit == null &&
                            board == null &&
                            screen.isEmpty()
                    )
                        ExtendedFloatingActionButton(
                            containerColor = Cobalt,
                            contentColor = Color.White,
                            onClick = {
                                create =
                                    when (root) {
                                        "Boards" -> Kind.BOARD
                                        "Timeline" -> Kind.EPOCH
                                        else -> Kind.NOTE
                                    }
                            },
                        ) {
                            AtlasIcon("Add")
                            Spacer(Modifier.width(8.dp))
                            Text(
                                when (root) {
                                    "Boards" -> "New board"
                                    "Timeline" -> "New epoch"
                                    else -> "New entry"
                                }
                            )
                        }
                },
            ) { padding ->
                Box(Modifier.padding(padding).fillMaxSize().imePadding()) {
                    val bundle = b
                    when {
                        worlds.isEmpty() ->
                            Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                                Spacer(Modifier.height(48.dp))
                                AtlasHero(
                                    stringResource(R.string.ui_a_world_worth_remembering_ce5ea2),
                                    "Build a private library of people, places, and histories. Everything stays on this device.",
                                    "World Note / world atlas",
                                )
                                Button(onClick = { worldDialog = true }, Modifier.fillMaxWidth()) {
                                    Text(stringResource(R.string.ui_create_my_world_fde6e0))
                                }
                                OutlinedButton(
                                    onClick = { vm.newWorld("", true) },
                                    Modifier.fillMaxWidth(),
                                ) {
                                    Text(stringResource(R.string.ui_explore_sample_world_5748de))
                                }
                                TextButton(onClick = { importPicker.launch(arrayOf("*/*")) }) {
                                    Text(
                                        stringResource(
                                            R.string.ui_restore_or_import_existing_notes_7c3066
                                        )
                                    )
                                }
                            }
                        bundle == null -> CircularProgressIndicator(Modifier.padding(24.dp))
                        historyAccount != null ->
                            RecordedHistory(vm, bundle, historyAccount!!, root)
                        edit != null ->
                            Editor(vm, edit!!, bundle, onExportDraft = { requestExport("draft") })
                        board != null ->
                            bundle.records
                                .find { it.id == board }
                                ?.let {
                                    BoardScreen(
                                        vm,
                                        bundle,
                                        it,
                                        onBack = { board = null },
                                        onExport = { requestExport("canvas") },
                                    )
                                }
                        screen == "Worlds" ->
                            LazyColumn(Modifier.padding(16.dp)) {
                                item {
                                    Heading(stringResource(R.string.ui_your_worlds_bd4313))
                                    Button(onClick = { worldDialog = true }) {
                                        Text(stringResource(R.string.ui_create_world_ce05cb))
                                    }
                                }
                                items(worlds) { w ->
                                    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                        Column(Modifier.padding(16.dp)) {
                                            Text(
                                                w.name,
                                                style = MaterialTheme.typography.titleLarge,
                                            )
                                            Text(if (w.archived) "Archived" else w.description)
                                            ActionRow {
                                                TextButton(
                                                    onClick = {
                                                        vm.chooseWorld(w.id)
                                                        screen = ""
                                                    }
                                                ) {
                                                    Text(stringResource(R.string.ui_open_ed077f))
                                                }
                                                TextButton(
                                                    onClick = {
                                                        vm.action {
                                                            val copy = vm.repo.duplicate(w.id)
                                                            vm.chooseWorld(copy.world.id)
                                                        }
                                                    }
                                                ) {
                                                    Text(
                                                        stringResource(R.string.ui_duplicate_02cdaa)
                                                    )
                                                }
                                                TextButton(
                                                    onClick = {
                                                        vm.action {
                                                            vm.repo.world(
                                                                w.copy(archived = !w.archived)
                                                            )
                                                        }
                                                    }
                                                ) {
                                                    Text(if (w.archived) "Unarchive" else "Archive")
                                                }
                                                if (w.archived)
                                                    TextButton(onClick = { deleteWorld = w }) {
                                                        Text(
                                                            stringResource(
                                                                R.string.ui_delete_world_c49b06
                                                            )
                                                        )
                                                    }
                                            }
                                        }
                                    }
                                }
                            }
                        screen == "Storage & recovery" -> StorageScreen(vm)
                        screen == "Settings & backup" ->
                            Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                                Heading(
                                    stringResource(R.string.ui_settings_backup_fdb209),
                                    "Complete archives include private author notes, drafts, revisions, trash, and images.",
                                )
                                Choice(
                                    stringResource(R.string.ui_theme_efb52e),
                                    prefs["theme"] ?: "Dark",
                                    listOf("Dark", "Light", "System"),
                                ) {
                                    vm.preference("theme", it)
                                }
                                var name by
                                    remember(bundle.world.id, bundle.world.name) {
                                        mutableStateOf(bundle.world.name)
                                    }
                                Field(
                                    stringResource(R.string.ui_world_name_bf050f),
                                    name,
                                    { name = it },
                                )
                                Button(
                                    onClick = {
                                        vm.action { vm.repo.world(bundle.world.copy(name = name)) }
                                    }
                                ) {
                                    Text(stringResource(R.string.ui_rename_world_00ef80))
                                }
                                var origin by
                                    remember(bundle.world.id) {
                                        mutableStateOf(bundle.world.origin)
                                    }
                                Field(
                                    stringResource(
                                        R.string.ui_origin_label_coordinates_remain_fixe_673d85
                                    ),
                                    origin,
                                    { origin = it },
                                )
                                TextButton(
                                    onClick = {
                                        vm.action {
                                            vm.repo.world(bundle.world.copy(origin = origin))
                                        }
                                    }
                                ) {
                                    Text(stringResource(R.string.ui_save_origin_label_72bf62))
                                }
                                var epochLabel by
                                    remember(bundle.world.id) {
                                        mutableStateOf(bundle.world.epochLabel)
                                    }
                                var eraLabel by
                                    remember(bundle.world.id) {
                                        mutableStateOf(bundle.world.eraLabel)
                                    }
                                Field(
                                    stringResource(R.string.ui_epoch_terminology_eb3593),
                                    epochLabel,
                                    { epochLabel = it },
                                )
                                Field(
                                    stringResource(R.string.ui_era_terminology_e5b436),
                                    eraLabel,
                                    { eraLabel = it },
                                )
                                TextButton(
                                    onClick = {
                                        vm.action {
                                            vm.repo.world(
                                                bundle.world.copy(
                                                    epochLabel = epochLabel,
                                                    eraLabel = eraLabel,
                                                )
                                            )
                                        }
                                    }
                                ) {
                                    Text(stringResource(R.string.ui_save_period_terminology_ca92eb))
                                }
                                Heading(stringResource(R.string.ui_protect_your_writing_f79c98))
                                val last =
                                    (prefs["backup.${bundle.world.id}"]
                                            ?: prefs["last workspace backup"])
                                        ?.toLongOrNull()
                                Text(
                                    stringResource(
                                        R.string.ui_last_successful_external_backup_56304b
                                    ) +
                                        (last?.let {
                                            DateFormat.getDateTimeInstance().format(Date(it))
                                        } ?: "Never")
                                )
                                if (last == null || bundle.records.any { it.updated > last })
                                    Text(
                                        stringResource(
                                            R.string.ui_there_are_changes_after_the_last_rec_acc54c
                                        )
                                    )
                                Button(
                                    onClick = { requestExport("backup") },
                                    Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        stringResource(R.string.ui_complete_workspace_backup_696632)
                                    )
                                }
                                OutlinedButton(
                                    onClick = { requestExport("world") },
                                    Modifier.fillMaxWidth(),
                                ) {
                                    Text(stringResource(R.string.ui_back_up_selected_world_c507ca))
                                }
                                OutlinedButton(
                                    onClick = { importPicker.launch(arrayOf("*/*")) },
                                    Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        stringResource(
                                            R.string.ui_restore_nodenote_json_canvas_import_4ffdda
                                        )
                                    )
                                }
                                Text(
                                    stringResource(
                                        R.string.ui_internal_recovery_survives_editing_m_0d2054
                                    )
                                )
                                Heading(stringResource(R.string.ui_readable_exports_655b57))
                                Button(onClick = { requestExport("markdown") }) {
                                    Text(
                                        stringResource(R.string.ui_combined_author_markdown_c951a0)
                                    )
                                }
                                Button(onClick = { requestExport("bible zip") }) {
                                    Text(
                                        stringResource(
                                            R.string.ui_linked_markdown_images_zip_38caa4
                                        )
                                    )
                                }
                                bundle.records
                                    .filter { it.kind == Kind.ACCOUNT && !it.trashed }
                                    .forEach { account ->
                                        OutlinedButton(
                                            onClick = {
                                                accountId = account.id
                                                requestExport("public")
                                            }
                                        ) {
                                            Text("Share ${account.title}")
                                        }
                                    }
                                val settings =
                                    bundle.records.firstOrNull { it.kind == Kind.SETTINGS }
                                var retentionConfirm by remember { mutableStateOf<String?>(null) }
                                Choice(
                                    stringResource(
                                        R.string.ui_revision_retention_per_record_06a9b2
                                    ),
                                    settings?.f("revision retention", "Unlimited") ?: "Unlimited",
                                    listOf("Unlimited", "10", "50", "100"),
                                ) {
                                    retentionConfirm = it
                                }
                                if (retentionConfirm != null)
                                    AlertDialog(
                                        onDismissRequest = { retentionConfirm = null },
                                        title = {
                                            Text(
                                                stringResource(
                                                    R.string.ui_set_revision_retention_e2edb1
                                                )
                                            )
                                        },
                                        text = {
                                            Text(
                                                "At the next editing checkpoint, only the newest ${retentionConfirm} snapshots per record will be kept. Choosing Unlimited disables automatic pruning. Export a complete backup before selecting a finite limit. Original attachments remain pinned."
                                            )
                                        },
                                        confirmButton = {
                                            TextButton(
                                                onClick = {
                                                    vm.worldSetting(
                                                        "revision retention",
                                                        retentionConfirm!!,
                                                    )
                                                    retentionConfirm = null
                                                }
                                            ) {
                                                Text(
                                                    stringResource(R.string.ui_set_retention_8864a1)
                                                )
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { retentionConfirm = null }) {
                                                Text(stringResource(R.string.ui_cancel_19766e))
                                            }
                                        },
                                    )
                                Text(
                                    stringResource(
                                        R.string.ui_board_undo_retains_100_commands_for__dfd48d
                                    )
                                )
                                var customize by remember { mutableStateOf(false) }
                                TextButton(onClick = { customize = !customize }) {
                                    Text(
                                        stringResource(
                                            R.string.ui_customize_canon_writing_labels_c1ef11
                                        )
                                    )
                                }
                                if (customize) {
                                    (Schema.canon.map { "canon.$it" } +
                                            Schema.writing.map { "writing.$it" })
                                        .forEach { key ->
                                            var value by
                                                remember(bundle.world.id, key) {
                                                    mutableStateOf(
                                                        settings?.f(key)
                                                            ?: key.substringAfter('.')
                                                                .replace('_', ' ')
                                                    )
                                                }
                                            Field(key, value, { value = it })
                                            TextButton(onClick = { vm.worldSetting(key, value) }) {
                                                Text(stringResource(R.string.ui_save_label_f724b6))
                                            }
                                        }
                                }
                            }
                        screen == "Consistency review" ->
                            LazyColumn(Modifier.padding(16.dp)) {
                                item {
                                    Heading(
                                        stringResource(R.string.ui_consistency_review_84895b),
                                        "Diagnostics never rewrite canon.",
                                    )
                                }
                                val diagnostics = Integrity.validate(bundle)
                                if (diagnostics.isEmpty())
                                    item {
                                        Empty(
                                            stringResource(
                                                R.string
                                                    .ui_no_structural_or_chronology_conflict_4daaa6
                                            )
                                        )
                                    }
                                items(diagnostics) { d ->
                                    TextButton(
                                        onClick = {
                                            bundle.records.find { it.id == d.id }?.let(vm::open)
                                        }
                                    ) {
                                        Text("${d.severity}: ${d.message}")
                                    }
                                }
                            }
                        screen.isNotEmpty() -> {
                            val records =
                                when (screen) {
                                    "Trash" -> bundle.records.filter { it.trashed }
                                    "Templates" ->
                                        bundle.records.filter {
                                            it.kind in
                                                setOf(Kind.TEMPLATE, Kind.RELATIONSHIP_TYPE) &&
                                                !it.trashed
                                        }
                                    else ->
                                        bundle.records.filter {
                                            it.kind in
                                                setOf(
                                                    Kind.CLAIM,
                                                    Kind.KNOWLEDGE,
                                                    Kind.ACCOUNT,
                                                    Kind.ASSERTION,
                                                    Kind.STORY,
                                                    Kind.REVEAL,
                                                    Kind.STAGE,
                                                    Kind.STATE,
                                                    Kind.ORDER,
                                                    Kind.SCHEME,
                                                    Kind.MYSTERY,
                                                    Kind.CLUE,
                                                    Kind.ARC,
                                                ) && !it.trashed
                                        }
                                }
                            LazyColumn(Modifier.padding(16.dp)) {
                                item {
                                    Heading(screen)
                                    if (screen != "Trash")
                                        Button(
                                            onClick = {
                                                create =
                                                    if (screen == "Templates") Kind.TEMPLATE
                                                    else Kind.CLAIM
                                            }
                                        ) {
                                            Text(stringResource(R.string.ui_create_record_3ec9f1))
                                        }
                                }
                                items(records, key = { it.id }) { RecordCard(it, { vm.open(it) }) }
                                item { Spacer(Modifier.height(80.dp)) }
                            }
                        }
                        root == "Library" || root == "Search" ->
                            Library(vm, bundle, root == "Search")
                        root == "Boards" ->
                            LazyColumn(Modifier.padding(16.dp)) {
                                item {
                                    Heading(
                                        stringResource(R.string.ui_boards_4fcd39),
                                        "Arrange shared lore into a visual story.",
                                    )
                                }
                                items(
                                    bundle.records.filter { it.kind == Kind.BOARD && !it.trashed },
                                    key = { it.id },
                                ) { r ->
                                    BoardMiniature(
                                        bundle.records.filter { it.ref("board") == r.id }
                                    )
                                    RecordCard(
                                        r,
                                        { board = r.id },
                                        "${bundle.records.count { it.kind==Kind.PLACEMENT && it.ref("board")==r.id }} shared cards · ${r.f("filter").ifBlank { "No saved filter" }}",
                                    )
                                }
                                item { Spacer(Modifier.height(96.dp)) }
                            }
                        root == "Timeline" -> TimelineScreen(vm, bundle)
                    }
                }
            }
            if (deleteWorld != null)
                AlertDialog(
                    onDismissRequest = { deleteWorld = null },
                    title = { Text(stringResource(R.string.ui_delete_archived_world_2beac1)) },
                    text = {
                        Text(
                            "All records in ${deleteWorld!!.name} will be removed from the workspace. An internal recovery backup will be saved first. Make an external backup for protection against device loss."
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                vm.deleteWorld(deleteWorld!!)
                                deleteWorld = null
                            }
                        ) {
                            Text(stringResource(R.string.ui_back_up_and_delete_cad08a))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { deleteWorld = null }) {
                            Text(stringResource(R.string.ui_cancel_19766e))
                        }
                    },
                )
            if (worldMenu)
                PickerWorld(
                    worlds,
                    { worldMenu = false },
                    {
                        vm.chooseWorld(it)
                        screen = ""
                    },
                    {
                        worldMenu = false
                        worldDialog = true
                    },
                )
            if (worldDialog)
                AlertDialog(
                    onDismissRequest = { worldDialog = false },
                    title = { Text(stringResource(R.string.ui_create_world_ce05cb)) },
                    text = {
                        Field(
                            stringResource(R.string.ui_name_dcd1d5),
                            worldName,
                            { worldName = it },
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                vm.newWorld(worldName)
                                worldDialog = false
                                worldName = ""
                            }
                        ) {
                            Text(stringResource(R.string.ui_create_475949))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { worldDialog = false }) {
                            Text(stringResource(R.string.ui_cancel_19766e))
                        }
                    },
                )
            if (create != null && b != null)
                CreateRecordDialog(vm, b!!, create!!, { create = null })
            if (message != null)
                AlertDialog(
                    onDismissRequest = { vm.message.value = null },
                    title = { Text(stringResource(R.string.ui_nodenote_6bb5c7)) },
                    text = { Text(message!!) },
                    confirmButton = {
                        TextButton(onClick = { vm.message.value = null }) {
                            Text(stringResource(R.string.ui_ok_565339))
                        }
                    },
                )
            if (busy != null)
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text(stringResource(R.string.ui_working_a92f04)) },
                    text = {
                        Column {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                            Text(busy!!)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { vm.cancelOperation() }) {
                            Text(stringResource(R.string.ui_cancel_19766e))
                        }
                    },
                )
            if (pending != null && busy == null) {
                val replace by vm.replaceRestore.collectAsStateWithLifecycle()
                AlertDialog(
                    onDismissRequest = { vm.dismissImport() },
                    title = { Text(stringResource(R.string.ui_import_validated_bad364)) },
                    text = {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            pending!!.bundles.forEach {
                                Text(
                                    "${it.world.name}: ${it.records.size} records, ${it.records.count { r->r.kind==Kind.ATTACHMENT }} attachments"
                                )
                            }
                            pending!!.warnings.distinct().take(20).forEach { Text(it) }
                            if (
                                pending!!.bundles.any { imported ->
                                    worlds.any { it.id == imported.world.id }
                                }
                            )
                                Check(
                                    stringResource(
                                        R.string.ui_replace_matching_world_identities_a066b7
                                    ),
                                    replace,
                                    { vm.replaceRestore.value = it },
                                )
                            Text(
                                if (replace)
                                    "This replaces matching worlds after saving an internal pre-restore backup. All changes are committed in one database transaction."
                                else
                                    "Destination: import as new worlds. Existing worlds remain intact."
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = { vm.restore() }) {
                            Text(
                                if (replace) "Confirm replacement restore"
                                else "Import as new worlds"
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { vm.dismissImport() }) {
                            Text(stringResource(R.string.ui_cancel_19766e))
                        }
                    },
                )
            }
            if (askExport != null)
                AlertDialog(
                    onDismissRequest = { askExport = null },
                    title = { Text("Export ${askExport}") },
                    text = {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            Text(
                                if (askExport == "public")
                                    "Only explicitly included account prose is shared. Review this preview for spoilers you typed into public fields."
                                else if (askExport == "canvas")
                                    "One board as text nodes and edges. Full notes are included; titles, structured lore, and chronology are flattened. This is not a complete backup."
                                else
                                    "Author exports include private writing and secrets. Complete backup archives are not encrypted."
                            )
                            if (askExport == "public" && b != null)
                                Text(
                                    Projection.markdown(
                                        Projection.account(b!!, accountId.orEmpty())
                                    )
                                )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                exportMode = askExport!!
                                askExport = null
                                exportPicker.launch(
                                    "World-Note-${exportMode.replace(' ','-')}.${when(exportMode){"backup"->"nnbackup"
"world"->"nnworld"
"markdown","public","draft"->"md"
"canvas"->"canvas"
else->"zip"}}"
                                )
                            }
                        ) {
                            Text(stringResource(R.string.ui_choose_destination_5853e2))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { askExport = null }) {
                            Text(stringResource(R.string.ui_cancel_19766e))
                        }
                    },
                )
        }
    }
}

@Composable
fun PickerWorld(
    worlds: List<World>,
    dismiss: () -> Unit,
    select: (String) -> Unit,
    create: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(stringResource(R.string.ui_switch_world_5c4e2c)) },
        text = {
            LazyColumn {
                items(worlds) { w ->
                    TextButton(
                        onClick = {
                            select(w.id)
                            dismiss()
                        }
                    ) {
                        Text(w.name + if (w.archived) " (archived)" else "")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = create) { Text(stringResource(R.string.ui_new_world_3e2818)) }
        },
    )
}

@Composable
fun CreateRecordDialog(
    vm: AppModel,
    b: WorldBundle,
    initial: Kind,
    onDismiss: () -> Unit,
    initialRefs: Map<String, List<String>> = emptyMap(),
) {
    var r by remember {
        mutableStateOf(Record(world = b.world.id, kind = initial, title = "", refs = initialRefs))
    }
    var creating by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create ${r.kind.label}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Choice(
                    stringResource(R.string.ui_type_baaddf),
                    r.kind.label,
                    Kind.entries
                        .filter {
                            it !in
                                setOf(
                                    Kind.PLACEMENT,
                                    Kind.EDGE,
                                    Kind.GROUP,
                                    Kind.ATTACHMENT,
                                    Kind.MEDIA,
                                    Kind.REVISION,
                                    Kind.DRAFT,
                                    Kind.IMPORT,
                                    Kind.SETTINGS,
                                )
                        }
                        .map { it.label },
                ) { label ->
                    r = r.copy(kind = Kind.entries.first { it.label == label })
                }
                Field(stringResource(R.string.ui_title_7e8cd2), r.title, { r = r.copy(title = it) })
                Schema.roles(r.kind).forEach { (role, kinds) ->
                    References(role, kinds, r, b.records, { r = it })
                }
                if (r.kind == Kind.RELATIONSHIP)
                    Choice(
                        stringResource(R.string.ui_direction_9c8a95),
                        r.f("direction", "Directed"),
                        listOf("Directed", "Symmetric"),
                    ) {
                        r = r.withField("direction", it)
                    }
            }
        },
        confirmButton = {
            Button(
                enabled = r.title.isNotBlank() && !creating,
                onClick = {
                    creating = true
                    vm.action {
                        try {
                            val saved = vm.add(r.copy(inbox = r.kind == Kind.NOTE))
                            vm.open(saved)
                            onDismiss()
                        } finally {
                            creating = false
                        }
                    }
                },
            ) {
                Text(stringResource(R.string.ui_create_475949))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel_19766e)) }
        },
    )
}

@Composable
fun Library(vm: AppModel, b: WorldBundle, search: Boolean) {
    val stateKey = "view.${b.world.id}.${if (search) "search" else "library"}"
    var query by rememberText(vm, "$stateKey.query")
    var type by rememberText(vm, "$stateKey.type", "All")
    var tag by rememberText(vm, "$stateKey.tag", "")
    var canon by rememberText(vm, "$stateKey.canon", "All")
    var writing by rememberText(vm, "$stateKey.writing", "All")
    var period by rememberText(vm, "$stateKey.period", "")
    var filterOpen by rememberFlag(vm, "$stateKey.filterOpen")
    var favorites by rememberFlag(vm, "$stateKey.favorites")
    var inbox by rememberFlag(vm, "$stateKey.inbox")
    var imageOnly by rememberFlag(vm, "$stateKey.imageOnly")
    var includeTrash by rememberFlag(vm, "$stateKey.includeTrash")
    var pickPeriod by remember { mutableStateOf(false) }
    val hits by vm.searchResults.collectAsStateWithLifecycle()
    LaunchedEffect(query, b.world.id) { vm.search(query) }
    val byId = remember(b.records) { b.records.associateBy { it.id } }
    val resultSources =
        remember(b.records, hits) {
            val hidden =
                setOf(
                    Kind.REVISION,
                    Kind.DRAFT,
                    Kind.IMPORT,
                    Kind.FILTER,
                    Kind.SETTINGS,
                    Kind.ATTACHMENT,
                    Kind.EDGE,
                    Kind.GROUP,
                    Kind.ORDER,
                )
            b.records
                .filter { it.id in hits && it.kind !in hidden }
                .mapNotNull { source ->
                    val owner =
                        when (source.kind) {
                            Kind.FIELD,
                            Kind.SECTION,
                            Kind.MEDIA -> source.ref("owner") ?: source.id
                            Kind.PLACEMENT ->
                                source.ref("entry") ?: source.ref("board") ?: source.id
                            else -> source.id
                        }
                    byId[owner]?.id?.let { it to source }
                }
                .groupBy({ it.first }, { it.second })
        }
    val records =
        b.records.filter { r ->
            (!r.trashed || includeTrash) &&
                (if (search && query.isNotBlank()) r.id in resultSources else r.kind.lore) &&
                (query.isBlank() || r.id in resultSources) &&
                (type == "All" || r.kind.label == type) &&
                (tag.isBlank() || r.f("tags").contains(tag, true)) &&
                (canon == "All" || r.canon == canon) &&
                (writing == "All" || r.writing == writing) &&
                (!favorites || r.favorite) &&
                (!inbox || r.inbox) &&
                (!imageOnly ||
                    b.records.any {
                        it.kind == Kind.MEDIA && it.ref("owner") == r.id && !it.trashed
                    }) &&
                (period.isBlank() ||
                    r.has("periods", period) ||
                    b.records.any {
                        it.kind in setOf(Kind.ASSOCIATION, Kind.STATE) &&
                            it.ref("entry") == r.id &&
                            it.has("periods", period)
                    })
        }
    LazyColumn(
        Modifier.padding(horizontal = 16.dp).testTag("library-results"),
        state = rememberDurableList(vm, "$stateKey.scroll"),
    ) {
        item {
            if (search)
                Heading(
                    stringResource(R.string.atlas_9113f65c77),
                    "Find an idea across notes, relationships and histories.",
                )
            else
                AtlasHero(
                    "Lore library",
                    "People, places and the threads that connect them.",
                    "${b.records.count { it.kind.lore && !it.trashed }} entries · ${b.records.count { it.kind.period && !it.trashed }} periods",
                )
            Field(
                stringResource(R.string.ui_search_title_alias_or_full_notes_6ead78),
                query,
                { query = it },
            )
            ActionRow {
                FilterChip(
                    favorites,
                    { favorites = !favorites },
                    label = { Text(stringResource(R.string.ui_favorites_7a1f2a)) },
                )
                FilterChip(
                    inbox,
                    { inbox = !inbox },
                    label = { Text(stringResource(R.string.ui_inbox_94835e)) },
                )
                FilterChip(
                    filterOpen,
                    { filterOpen = !filterOpen },
                    label = { Text(stringResource(R.string.ui_filters_546ebb)) },
                )
            }
        }
        if (filterOpen)
            item {
                Choice(
                    stringResource(R.string.ui_type_baaddf),
                    type,
                    listOf("All") + Kind.entries.filter { it.lore }.map { it.label },
                ) {
                    type = it
                }
                Field(stringResource(R.string.ui_tag_150391), tag, { tag = it })
                Choice(
                    stringResource(R.string.ui_canon_778017),
                    canon,
                    listOf("All") + Schema.canon,
                ) {
                    canon = it
                }
                Choice(
                    stringResource(R.string.ui_writing_a8bfae),
                    writing,
                    listOf("All") + Schema.writing,
                ) {
                    writing = it
                }
                Check(stringResource(R.string.ui_has_image_5e2b9b), imageOnly, { imageOnly = it })
                Check(
                    stringResource(R.string.atlas_5ce67e9a25),
                    includeTrash,
                    { includeTrash = it },
                )
                TextButton(onClick = { pickPeriod = true }) {
                    Text(
                        stringResource(R.string.atlas_6a2299cf17) +
                            (b.records.find { it.id == period }?.title ?: "All")
                    )
                }
                if (pickPeriod)
                    Picker(
                        "Filter by period identity",
                        b.records.filter { it.kind.period },
                        onDismiss = { pickPeriod = false },
                        onSelect = { period = it.firstOrNull().orEmpty() },
                    )
                TextButton(
                    onClick = {
                        vm.create(
                            Kind.FILTER,
                            "Library filter ${type}",
                            fields =
                                mapOf(
                                    "type" to type,
                                    "tag" to tag,
                                    "canon" to canon,
                                    "writing" to writing,
                                    "period" to period,
                                    "favorites" to favorites.toString(),
                                    "inbox" to inbox.toString(),
                                    "images" to imageOnly.toString(),
                                    "include trash" to includeTrash.toString(),
                                    "query" to query,
                                ),
                            open = false,
                        )
                    }
                ) {
                    Text(stringResource(R.string.ui_save_filter_9fe519))
                }
                b.records
                    .filter { it.kind == Kind.FILTER }
                    .forEach { filter ->
                        TextButton(
                            onClick = {
                                type = filter.f("type", "All")
                                tag = filter.f("tag")
                                canon = filter.f("canon", "All")
                                writing = filter.f("writing", "All")
                                period = filter.f("period")
                                favorites = filter.f("favorites") == "true"
                                inbox = filter.f("inbox") == "true"
                                imageOnly = filter.f("images") == "true"
                                includeTrash = filter.f("include trash") == "true"
                                query = filter.f("query")
                            }
                        ) {
                            Text(filter.title)
                        }
                    }
            }
        if (records.isEmpty())
            item { Empty(stringResource(R.string.ui_no_matching_lore_yet_use_to_capture__f28700)) }
        items(records.sortedByDescending { it.updated }, key = { it.id }) { r ->
            RecordCard(
                r,
                { if (r.kind == Kind.BOARD) vm.boardRequest.value = r.id else vm.open(r) },
                if (query.isNotBlank())
                    searchSnippet(
                        resultSources[r.id]?.firstOrNull { it.id == r.id }
                            ?: resultSources[r.id]?.firstOrNull()
                            ?: r,
                        query,
                    )
                else "",
            )
        }
        item { Spacer(Modifier.height(100.dp)) }
    }
}

private fun searchSnippet(r: Record, query: String): String {
    val pairs =
        listOf("Title" to r.title, "Summary" to r.summary, "Notes" to r.body) + r.fields.toList()
    val match = pairs.firstOrNull { it.second.contains(query, true) } ?: return r.summary
    val pos = match.second.indexOf(query, ignoreCase = true)
    return "${match.first}: …" +
        match.second.substring(
            (pos - 35).coerceAtLeast(0),
            (pos + 160).coerceAtMost(match.second.length),
        )
}
