package app.nodenote.worldbuilder.ui

import android.graphics.Paint
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.nodenote.core.*
import app.nodenote.worldbuilder.AppModel
import app.nodenote.worldbuilder.R
import kotlin.math.*

private enum class Gesture {
    IDLE,
    PENDING_TAP,
    DRAGGING,
    PANNING,
    PINCHING,
    CONNECTING,
    MULTI_SELECT,
}

@Composable
fun BoardScreen(
    vm: AppModel,
    b: WorldBundle,
    board: Record,
    onBack: () -> Unit,
    onExport: () -> Unit,
) {
    val density = LocalDensity.current.density.toDouble()
    fun local(records: List<Record>) = records.filter {
        it.kind in setOf(Kind.PLACEMENT, Kind.EDGE, Kind.GROUP) && it.ref("board") == board.id
    }
    var layout by remember(board.id) { mutableStateOf(local(b.records)) }
    val history = remember(board.id) { UndoHistory<List<Record>>() }
    var historyVersion by remember { mutableIntStateOf(0) }
    var viewport by
        remember(board.id) {
            mutableStateOf(
                Viewport(
                    board.n("scale", .65),
                    Point(board.n("pan x", 12.0), board.n("pan y", 20.0)),
                )
            )
        }
    var selection by
        rememberSetting(
            vm,
            "view.${board.id}.selection",
            emptyList<String>(),
            { it.split(",").filter(String::isNotEmpty) },
            { it.joinToString(",") },
        )
    var toolsExpanded by rememberFlag(vm, "view.${board.id}.tools")
    var outline by rememberFlag(vm, "view.${board.id}.outline")
    var multi by rememberFlag(vm, "view.${board.id}.multi")
    var connecting by remember { mutableStateOf(false) }
    var chooseExisting by remember { mutableStateOf(false) }
    var create by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var connectPair by remember { mutableStateOf<Pair<Record, Record>?>(null) }
    var filter by rememberText(vm, "view.${board.id}.filter", board.f("filter"))
    var focus by remember { mutableIntStateOf(0) }
    var preview by remember { mutableStateOf<List<Record>?>(null) }
    var contentDialog by remember { mutableStateOf(false) }
    var contentChoice by remember { mutableStateOf(CardContent(false, false, false)) }
    var canvasSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    var remove by remember { mutableStateOf(false) }
    var edgeEdit by remember { mutableStateOf<Record?>(null) }
    val byId = remember(b.records) { b.records.associateBy { it.id } }
    val selected = layout.filter { it.id in selection }
    LaunchedEffect(b.records, board.id) { layout = local(b.records) }
    fun persistView() {
        val savedViewport = viewport
        val savedFilter = filter
        vm.action { vm.repo.boardView(board.id, savedViewport, savedFilter) }
    }
    fun apply(next: List<Record>, remember: Boolean = true) {
        val before = layout
        if (remember) history.push(before, next)
        historyVersion++
        layout = next
        vm.action {
            try {
                vm.repo.batch(
                    b.world.id,
                    next,
                    before.map { it.id }.toSet() - next.map { it.id }.toSet(),
                )
            } catch (e: Exception) {
                layout = before
                throw e
            }
        }
    }
    var insertionPoint by remember(board.id) { mutableStateOf(viewport.world(Point(30.0, 80.0))) }
    fun place(
        entry: Record?,
        sticky: String = "",
        position: Point = insertionPoint,
        parent: Record? = null,
    ) {
        val available =
            freePosition(position, layout.filter { it.kind == Kind.PLACEMENT }.map { it.rect() })
        val p =
            Record(
                world = b.world.id,
                kind = Kind.PLACEMENT,
                title = entry?.title ?: "Sticky note",
                body = sticky,
                refs = mapOf("board" to listOf(board.id), "entry" to listOfNotNull(entry?.id)),
                fields =
                    mapOf(
                        "x" to available.x.toString(),
                        "y" to available.y.toString(),
                        "width" to "190",
                        "height" to "112",
                        "color" to "#A9BFFF",
                    ),
            )
        val branch = parent?.let {
            Record(
                world = b.world.id,
                kind = Kind.EDGE,
                title = "Branch",
                refs =
                    mapOf(
                        "board" to listOf(board.id),
                        "from" to listOf(it.id),
                        "to" to listOf(p.id),
                    ),
            )
        }
        apply(layout + p + listOfNotNull(branch))
        selection = listOf(p.id)
    }
    val allPlacements = layout.filter { it.kind == Kind.PLACEMENT && !it.trashed }
    val allEdges = layout.filter { it.kind == Kind.EDGE && !it.trashed }
    var focused = selection.toSet()
    repeat(focus) {
        focused =
            focused +
                allEdges
                    .filter { it.ref("from") in focused || it.ref("to") in focused }
                    .flatMap { listOfNotNull(it.ref("from"), it.ref("to")) }
    }
    val placements = allPlacements.filter { p ->
        val entry = byId[p.ref("entry")]
        val g = layout.find { it.id == p.ref("group") }
        (focus == 0 || selection.isEmpty() || p.id in focused) &&
            (filter.isBlank() ||
                listOf(
                        entry?.title,
                        entry?.kind?.label,
                        entry?.f("tags"),
                        entry?.canon,
                        entry?.writing,
                        p.title,
                    )
                    .any { it?.contains(filter, true) == true }) &&
            g?.f("collapsed") != "true"
    }
    val covers = remember(b.records) { boardCovers(b.records) }
    val imageRequests =
        remember(placements, covers, viewport, canvasSize, outline) {
            if (outline) emptyList()
            else
                placements
                    .asSequence()
                    .filter { p ->
                        val at = viewport.screen(Point(p.n("x"), p.n("y")))
                        p.f("show image") == "true" &&
                            at.x * density < canvasSize.width &&
                            at.y * density < canvasSize.height &&
                            (at.x + p.n("width", 190.0) * viewport.scale) * density > 0 &&
                            (at.y + p.n("height", 112.0) * viewport.scale) * density > 0
                    }
                    .mapNotNull { covers[it.ref("entry")] }
                    .distinctBy { it.id }
                    .take(64)
                    .toList()
        }
    val thumbnails = boardThumbnails(vm, imageRequests)
    val currentPlacements by rememberUpdatedState(placements)
    val currentLayout by rememberUpdatedState(layout)
    val currentEdges by rememberUpdatedState(allEdges)
    val currentSelection by rememberUpdatedState(selection)
    val currentMulti by rememberUpdatedState(multi)
    val currentConnecting by rememberUpdatedState(connecting)
    fun tap(p: Record?) {
        if (p == null) {
            selection = emptyList()
            return
        }
        if (connecting) {
            val a = layout.find { it.id == selection.firstOrNull() }
            if (a != null && a.id != p.id) {
                connectPair = a to p
                connecting = false
            }
        } else
            selection =
                if (multi) {
                    if (p.id in selection) selection - p.id else selection + p.id
                } else listOf(p.id)
    }
    fun fit(items: List<Record>) {
        if (items.isEmpty()) return
        val minX = items.minOf { it.n("x") }
        val minY = items.minOf { it.n("y") }
        val maxX = items.maxOf { it.n("x") + it.n("width", 190.0) }
        val maxY = items.maxOf { it.n("y") + it.n("height", 112.0) }
        val scale = min(320.0 / (maxX - minX), 420.0 / (maxY - minY)).coerceIn(.15, 4.0)
        viewport = Viewport(scale, Point(12 - minX * scale, 20 - minY * scale))
        persistView()
    }
    val imagePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null)
                vm.boardImage(
                    uri,
                    board,
                    freePosition(
                        insertionPoint,
                        layout.filter { it.kind == Kind.PLACEMENT }.map { it.rect() },
                    ),
                )
        }
    BackHandler(connecting) { connecting = false }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.padding(horizontal = 12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text(board.title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            TextButton(
                onClick = {
                    persistView()
                    onBack()
                }
            ) {
                Text(stringResource(R.string.ui_boards_4fcd39))
            }
        }
        ActionRow {
            TextButton(onClick = { create = "entry" }) {
                Text(stringResource(R.string.ui_card_8b3f06))
            }
            TextButton(onClick = { imagePicker.launch(arrayOf("image/*")) }) {
                Text("Image")
            }
            FilterChip(
                outline,
                { outline = !outline },
                label = { Text(stringResource(R.string.ui_outline_eabbf3)) },
            )
            TextButton(onClick = { chooseExisting = true }) {
                Text(stringResource(R.string.ui_place_existing_b1ef46))
            }
            TextButton(onClick = { create = "sticky" }) {
                Text(stringResource(R.string.ui_sticky_874717))
            }
            FilterChip(
                multi,
                { multi = !multi },
                label = { Text(stringResource(R.string.ui_multi_select_e9f589)) },
            )
        }
        TextButton(onClick = { toolsExpanded = !toolsExpanded }) {
            Text(if (toolsExpanded) "Hide board tools" else "Board tools · zoom, layout & export")
        }
        if (toolsExpanded)
            Column {
                ActionRow {
                    TextButton(
                        onClick = {
                            viewport = viewport.zoom(Point(160.0, 200.0), 1.25)
                            persistView()
                        }
                    ) {
                        Text(stringResource(R.string.ui_zoom_a69bfe))
                    }
                    TextButton(
                        onClick = {
                            viewport = viewport.zoom(Point(160.0, 200.0), .8)
                            persistView()
                        }
                    ) {
                        Text(stringResource(R.string.ui_zoom_263f36))
                    }
                    TextButton(
                        onClick = {
                            viewport = Viewport()
                            persistView()
                        }
                    ) {
                        Text(stringResource(R.string.ui_reset_daee76))
                    }
                    TextButton(
                        onClick = { fit(if (selected.isEmpty()) placements else selected) }
                    ) {
                        Text(if (selected.isEmpty()) "Fit board" else "Fit selection")
                    }
                    TextButton(
                        enabled = history.canUndo,
                        onClick = { history.undo()?.let { apply(it, false) } },
                    ) {
                        Text(stringResource(R.string.ui_undo_a8283a))
                    }
                    TextButton(
                        enabled = history.canRedo,
                        onClick = { history.redo()?.let { apply(it, false) } },
                    ) {
                        Text(stringResource(R.string.ui_redo_742739))
                    }
                }
                ActionRow {
                    TextButton(
                        onClick = {
                            create = "filter"
                            name = filter
                        }
                    ) {
                        Text(if (filter.isBlank()) "Filter" else "Filter: $filter")
                    }
                    TextButton(onClick = { focus = (focus + 1) % 3 }) {
                        Text(if (focus == 0) "Focus off" else "$focus-hop focus")
                    }
                    TextButton(
                        onClick = {
                            preview = layout.mapIndexed { i, r ->
                                if (r.kind == Kind.PLACEMENT)
                                    r.copy(
                                        fields =
                                            r.fields +
                                                mapOf(
                                                    "x" to ((i % 3) * 230).toString(),
                                                    "y" to ((i / 3) * 160).toString(),
                                                )
                                    )
                                else r
                            }
                        }
                    ) {
                        Text(stringResource(R.string.ui_layout_preview_045d7c))
                    }
                    TextButton(onClick = onExport) {
                        Text(stringResource(R.string.ui_export_canvas_715fa4))
                    }
                }
            }
        if (selection.isNotEmpty())
            ActionRow {
                TextButton(
                    onClick = {
                        selected.firstOrNull()?.let { contentChoice = CardContent.from(it) }
                        contentDialog = true
                    }
                ) {
                    Text("Card content")
                }
                TextButton(
                    onClick = {
                        val p = selected.firstOrNull()
                        byId[p?.ref("entry")]?.let(vm::open)
                            ?: run {
                                create = "edit sticky"
                                name = p?.body.orEmpty()
                            }
                    }
                ) {
                    Text(stringResource(R.string.ui_open_edit_9214dc))
                }
                TextButton(
                    onClick = {
                        connecting = true
                        multi = false
                    }
                ) {
                    Text(stringResource(R.string.ui_connect_1a2303))
                }
                TextButton(onClick = { create = "branch" }) {
                    Text(stringResource(R.string.ui_add_child_97fd94))
                }
                TextButton(
                    onClick = {
                        create = "appearance"
                        name = selected.firstOrNull()?.f("color", "#A9BFFF").orEmpty()
                    }
                ) {
                    Text(stringResource(R.string.ui_card_color_470f3a))
                }
                TextButton(
                    onClick = {
                        create = "size"
                        name =
                            "${selected.firstOrNull()?.n("width",190.0)},${selected.firstOrNull()?.n("height",112.0)}"
                    }
                ) {
                    Text(stringResource(R.string.ui_card_size_c8d980))
                }
                TextButton(onClick = { create = "group" }) {
                    Text(stringResource(R.string.ui_group_34ca0e))
                }
                TextButton(onClick = { remove = true }) {
                    Text(stringResource(R.string.ui_remove_from_board_1c6124))
                }
                if (selected.firstOrNull()?.ref("entry") == null)
                    TextButton(
                        onClick = {
                            vm.action {
                                val p = selected.first()
                                val entry =
                                    vm.add(
                                        Record(
                                            world = b.world.id,
                                            kind = Kind.NOTE,
                                            title = p.title,
                                            body = p.body,
                                        )
                                    )
                                apply(
                                    layout.map {
                                        if (it.id == p.id) it.withRef("entry", entry.id) else it
                                    }
                                )
                            }
                        }
                    ) {
                        Text(stringResource(R.string.ui_convert_sticky_to_lore_478d58))
                    }
            }
        if (connecting)
            Text(
                stringResource(R.string.ui_connecting_tap_the_target_card_back__377456),
                Modifier.padding(8.dp),
                color = MaterialTheme.colorScheme.primary,
            )
        val groups = layout.filter { it.kind == Kind.GROUP }
        if (groups.isNotEmpty())
            ActionRow {
                groups.forEach { g ->
                    TextButton(
                        onClick = {
                            apply(
                                layout.map {
                                    if (it.id == g.id)
                                        it.withField(
                                            "collapsed",
                                            (g.f("collapsed") != "true").toString(),
                                        )
                                    else it
                                }
                            )
                            selection =
                                allPlacements.filter { it.ref("group") == g.id }.map { it.id }
                        }
                    ) {
                        Text("${if(g.f("collapsed")=="true")"▸" else "▾"} ${g.title}")
                    }
                }
            }
        if (outline)
            LazyColumn(Modifier.weight(1f).padding(12.dp)) {
                items(allPlacements, key = { it.id }) { p ->
                    val entry = byId[p.ref("entry")]
                    if (CardContent.from(p).image)
                        covers[p.ref("entry")]?.let {
                            CoverImage(vm, it, "${entry?.title ?: p.title} · board image")
                        }
                    RecordCard(
                        entry ?: p,
                        {
                            tap(p)
                            entry?.let(vm::open)
                        },
                        if (entry?.trashed == true) "Entry in trash — identity retained"
                        else if (CardContent.from(p).notes)
                            entry?.summary?.ifBlank { entry.body } ?: p.body
                        else
                            "Placement ${p.id.take(8)} · x ${p.n("x").toInt()}, y ${p.n("y").toInt()}",
                    )
                    TextButton(onClick = { tap(p) }) {
                        Text(stringResource(R.string.ui_select_this_placement_37c08e))
                    }
                }
                items(allEdges, key = { it.id }) { edge ->
                    RecordCard(
                        byId[edge.ref("relationship")] ?: edge,
                        {
                            byId[edge.ref("relationship")]?.let(vm::open) ?: run { edgeEdit = edge }
                        },
                        "${if(edge.ref("relationship")==null)"Board annotation" else "Lore relationship"} · ${layout.find { it.id==edge.ref("from") }?.title} → ${layout.find { it.id==edge.ref("to") }?.title}",
                    )
                }
            }
        else
            Canvas(
                Modifier.weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
                    .onSizeChanged { canvasSize = it }
                    .background(Color(0xFF080F12))
                    .semantics {
                        contentDescription =
                            "Board canvas. Use Outline for accessible entry and relationship controls."
                    }
                    .pointerInput(board.id) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val start = Point(down.position.x / density, down.position.y / density)
                            val worldStart = viewport.world(start)
                            val hit =
                                currentPlacements.asReversed().firstOrNull {
                                    it.rect().contains(worldStart)
                                }
                            var mode = Gesture.PENDING_TAP
                            var last = down.position
                            val initial = currentLayout
                            var moved = Point(0.0, 0.0)
                            var dragIds =
                                if (hit?.id in currentSelection) currentSelection.toSet()
                                else setOfNotNull(hit?.id)
                            hit?.ref("group")?.let { group ->
                                dragIds =
                                    dragIds +
                                        initial.filter { it.ref("group") == group }.map { it.id }
                            }
                            do {
                                val event = awaitPointerEvent()
                                val pressed = event.changes.count { it.pressed }
                                if (pressed >= 2) {
                                    if (mode != Gesture.PINCHING) {
                                        layout = initial
                                        mode = Gesture.PINCHING
                                    }
                                    val centroid = event.calculateCentroid(useCurrent = false)
                                    val pan = event.calculatePan()
                                    val zoom = event.calculateZoom()
                                    if (centroid.x.isFinite())
                                        viewport =
                                            viewport.zoom(
                                                Point(centroid.x / density, centroid.y / density),
                                                zoom.toDouble(),
                                                Point(pan.x / density, pan.y / density),
                                            )
                                } else if (mode != Gesture.PINCHING) {
                                    val change = event.changes.first()
                                    val delta = change.position - last
                                    if (
                                        mode == Gesture.PENDING_TAP &&
                                            (change.position - down.position).getDistance() >
                                                viewConfiguration.touchSlop
                                    )
                                        mode =
                                            if (hit != null && !currentConnecting) Gesture.DRAGGING
                                            else Gesture.PANNING
                                    when (mode) {
                                        Gesture.DRAGGING -> {
                                            moved =
                                                moved +
                                                    Point(
                                                        delta.x / density / viewport.scale,
                                                        delta.y / density / viewport.scale,
                                                    )
                                            layout = initial.map { r ->
                                                if (r.id in dragIds)
                                                    r.copy(
                                                        fields =
                                                            r.fields +
                                                                mapOf(
                                                                    "x" to
                                                                        (r.n("x") + moved.x)
                                                                            .toString(),
                                                                    "y" to
                                                                        (r.n("y") + moved.y)
                                                                            .toString(),
                                                                )
                                                    )
                                                else r
                                            }
                                        }
                                        Gesture.PANNING ->
                                            viewport =
                                                viewport.copy(
                                                    translation =
                                                        viewport.translation +
                                                            Point(
                                                                delta.x / density,
                                                                delta.y / density,
                                                            )
                                                )
                                        else -> Unit
                                    }
                                    last = change.position
                                }
                                event.changes.forEach {
                                    if (mode != Gesture.PENDING_TAP) it.consume()
                                }
                                if (pressed == 0) {
                                    when (mode) {
                                        Gesture.PENDING_TAP ->
                                            if (hit != null) tap(hit)
                                            else {
                                                val edge = currentEdges.firstOrNull { e ->
                                                    val a = currentPlacements.find {
                                                        it.id == e.ref("from")
                                                    }
                                                    val z = currentPlacements.find {
                                                        it.id == e.ref("to")
                                                    }
                                                    a != null &&
                                                        z != null &&
                                                        lineDistance(
                                                            worldStart,
                                                            Point(a.n("x") + 95, a.n("y") + 56),
                                                            Point(z.n("x") + 95, z.n("y") + 56),
                                                        ) * viewport.scale < 16
                                                }
                                                if (edge != null)
                                                    byId[edge.ref("relationship")]?.let(vm::open)
                                                        ?: run { edgeEdit = edge }
                                                else {
                                                    insertionPoint = worldStart
                                                    tap(null)
                                                }
                                            }
                                        Gesture.DRAGGING -> {
                                            val after = layout
                                            layout = initial
                                            apply(after)
                                            selection = dragIds.toList()
                                        }
                                        Gesture.PANNING,
                                        Gesture.PINCHING -> persistView()
                                        else -> Unit
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }
            ) {
                val paint =
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.WHITE
                        textSize = (14 * viewport.scale.coerceAtLeast(.6) * density).toFloat()
                    }
                fun screen(x: Double, y: Double): Offset {
                    val p = viewport.screen(Point(x, y))
                    return Offset((p.x * density).toFloat(), (p.y * density).toFloat())
                }
                val step = (40 * viewport.scale * density).toFloat()
                if (step > 10) {
                    var x = (viewport.translation.x * density % step).toFloat()
                    while (x < size.width) {
                        var y = (viewport.translation.y * density % step).toFloat()
                        while (y < size.height) {
                            drawCircle(Color(0xFF294139), 1f, Offset(x, y))
                            y += step
                        }
                        x += step
                    }
                }
                val visibleIds = placements.map { it.id }.toSet()
                allEdges
                    .filter { it.ref("from") in visibleIds && it.ref("to") in visibleIds }
                    .forEach { e ->
                        val a = placements.first { it.id == e.ref("from") }
                        val z = placements.first { it.id == e.ref("to") }
                        val from =
                            screen(
                                a.n("x") + a.n("width", 190.0) / 2,
                                a.n("y") + a.n("height", 112.0) / 2,
                            )
                        val to =
                            screen(
                                z.n("x") + z.n("width", 190.0) / 2,
                                z.n("y") + z.n("height", 112.0) / 2,
                            )
                        drawLine(Color(0xFF98BEAD), from, to, 2.dp.toPx())
                        val rel = byId[e.ref("relationship")]
                        val angle = atan2(to.y - from.y, to.x - from.x)
                        val center = Offset((from.x + to.x) / 2, (from.y + to.y) / 2)
                        val arrow = 12.dp.toPx()
                        if (rel?.f("direction") != "Symmetric") {
                            drawLine(
                                Color(0xFFA9BFFF),
                                center,
                                Offset(
                                    center.x - arrow * cos(angle - .5f),
                                    center.y - arrow * sin(angle - .5f),
                                ),
                                2.dp.toPx(),
                            )
                            drawLine(
                                Color(0xFFA9BFFF),
                                center,
                                Offset(
                                    center.x - arrow * cos(angle + .5f),
                                    center.y - arrow * sin(angle + .5f),
                                ),
                                2.dp.toPx(),
                            )
                        }
                        if (viewport.scale > .35)
                            drawContext.canvas.nativeCanvas.drawText(
                                (rel?.title ?: e.title).take(24),
                                center.x,
                                center.y - 10,
                                paint,
                            )
                    }
                placements.forEach { p ->
                    val at = screen(p.n("x"), p.n("y"))
                    val width = (p.n("width", 190.0) * viewport.scale * density).toFloat()
                    val height = (p.n("height", 112.0) * viewport.scale * density).toFloat()
                    if (
                        at.x > size.width ||
                            at.y > size.height ||
                            at.x + width < 0 ||
                            at.y + height < 0
                    )
                        return@forEach
                    val entry = byId[p.ref("entry")]
                    val accent = runCatching {
                        Color(android.graphics.Color.parseColor(p.f("color", "#A9BFFF")))
                    }
                        .getOrDefault(Color(0xFFA9BFFF))
                    drawRoundRect(
                        Color(0xFF15372F),
                        at,
                        Size(width, height),
                        CornerRadius(12f, 12f),
                    )
                    drawRoundRect(
                        if (p.id in selection) Color.White else accent,
                        at,
                        Size(width, height),
                        CornerRadius(12f, 12f),
                        style = Stroke(if (p.id in selection) 4f else 1.5f),
                    )
                    drawBoardCardContent(
                        drawContext.canvas.nativeCanvas,
                        p,
                        entry,
                        thumbnails[covers[p.ref("entry")]?.id],
                        covers.containsKey(p.ref("entry")),
                        android.graphics.RectF(at.x, at.y, at.x + width, at.y + height),
                        (viewport.scale * density).toFloat(),
                    )
                }
            }
        Text(
            "${"%.0f".format(viewport.scale*100)}% · ${selection.size} selected · pan blank space / pinch / drag cards",
            Modifier.padding(8.dp),
            style = MaterialTheme.typography.labelSmall,
        )
    }
    if (contentDialog)
        AlertDialog(
            onDismissRequest = { contentDialog = false },
            title = { Text("Card content") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "Choose what these placements show. Images use the entry’s cover, or its first image. Notes stay linked to the original entry."
                    )
                    Check("Cover image", contentChoice.image) {
                        contentChoice = contentChoice.copy(image = it)
                    }
                    Check("Note preview", contentChoice.notes) {
                        contentChoice = contentChoice.copy(notes = it)
                    }
                    Check("Tags and status", contentChoice.details) {
                        contentChoice = contentChoice.copy(details = it)
                    }
                    Text(
                        "Cards grow to fit. Other boards keep their own appearance. Card size can be adjusted separately."
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        apply(
                            layout.map { if (it.id in selection) contentChoice.applyTo(it) else it }
                        )
                        contentDialog = false
                    }
                ) {
                    Text("Apply to selected cards")
                }
            },
            dismissButton = { TextButton(onClick = { contentDialog = false }) { Text("Cancel") } },
        )
    if (chooseExisting)
        Picker(
            stringResource(R.string.ui_reuse_lore_on_this_board_7fd860),
            b.records.filter { it.kind.lore },
            onDismiss = { chooseExisting = false },
            onSelect = { ids -> ids.firstOrNull()?.let { byId[it]?.let { r -> place(r) } } },
        )
    if (create.isNotBlank())
        AlertDialog(
            onDismissRequest = {
                create = ""
                name = ""
            },
            title = {
                Text(
                    when (create) {
                        "appearance" -> "Card accent color (#RRGGBB)"
                        "size" -> "Card width,height in board units"
                        "filter" -> "Filter visible cards by type, tag, status, or title"
                        "group" -> "Name group"
                        "sticky",
                        "edit sticky" -> "Board-local sticky"
                        else -> "Create linked lore card"
                    }
                )
            },
            text = {
                Field(
                    if (create in listOf("sticky", "edit sticky")) "Full text" else "Name / value",
                    name,
                    { name = it },
                    if (create in listOf("sticky", "edit sticky")) 8 else 1,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mode = create
                        val text = name
                        create = ""
                        name = ""
                        when (mode) {
                            "filter" -> {
                                filter = text
                                vm.action {
                                    val latest =
                                        vm.repo.snapshot(b.world.id).records.first {
                                            it.id == board.id
                                        }
                                    vm.repo.save(latest.withField("filter", text))
                                }
                            }
                            "appearance" ->
                                vm.action {
                                    require(Regex("#[a-fA-F0-9]{6}").matches(text)) {
                                        "Use a six-digit hex color, such as #A9BFFF"
                                    }
                                    apply(
                                        layout.map {
                                            if (it.id in selection) it.withField("color", text)
                                            else it
                                        }
                                    )
                                }
                            "size" ->
                                vm.action {
                                    val parts =
                                        text.split(',').map {
                                            it.trim().toDoubleOrNull()
                                                ?: error("Enter width,height")
                                        }
                                    require(
                                        parts.size == 2 &&
                                            parts.all { it.isFinite() && it in 80.0..1200.0 }
                                    )
                                    apply(
                                        layout.map {
                                            if (it.id in selection)
                                                it.copy(
                                                    fields =
                                                        it.fields +
                                                            mapOf(
                                                                "width" to parts[0].toString(),
                                                                "height" to parts[1].toString(),
                                                            )
                                                )
                                            else it
                                        }
                                    )
                                }
                            "sticky" -> place(null, text)
                            "edit sticky" ->
                                apply(
                                    layout.map {
                                        if (it.id == selection.firstOrNull())
                                            it.copy(
                                                body = text,
                                                title =
                                                    text.lineSequence().first().take(100).ifBlank {
                                                        "Sticky"
                                                    },
                                            )
                                        else it
                                    }
                                )
                            "group" -> {
                                val g =
                                    Record(
                                        world = b.world.id,
                                        kind = Kind.GROUP,
                                        title = text.ifBlank { "Group" },
                                        refs = mapOf("board" to listOf(board.id)),
                                    )
                                apply(
                                    layout.map {
                                        if (it.id in selection) it.withRef("group", g.id) else it
                                    } + g
                                )
                            }
                            else ->
                                vm.action {
                                    val entry =
                                        vm.add(
                                            Record(
                                                world = b.world.id,
                                                kind = Kind.NOTE,
                                                title = text.ifBlank { "Untitled" },
                                            )
                                        )
                                    val parent = selected.firstOrNull()
                                    val position =
                                        if (mode == "branch" && parent != null)
                                            Point(parent.n("x") + 240, parent.n("y") + 160)
                                        else insertionPoint
                                    place(
                                        entry,
                                        position = position,
                                        parent = if (mode == "branch") parent else null,
                                    )
                                }
                        }
                    }
                ) {
                    Text(stringResource(R.string.ui_apply_31e392))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        create = ""
                        name = ""
                    }
                ) {
                    Text(stringResource(R.string.ui_cancel_19766e))
                }
            },
        )
    if (connectPair != null) {
        var reuse by remember { mutableStateOf<String?>(null) }
        var pickReuse by remember { mutableStateOf(false) }
        var label by remember { mutableStateOf("") }
        var semantic by remember { mutableStateOf(true) }
        var symmetric by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { connectPair = null },
            title = { Text(stringResource(R.string.ui_connect_cards_963b41)) },
            text = {
                Column {
                    Field(stringResource(R.string.ui_label_0e6637), label, { label = it })
                    Check(
                        stringResource(R.string.ui_lore_relationship_shared_across_worl_c53221),
                        semantic,
                        { semantic = it },
                    )
                    Check(
                        stringResource(R.string.ui_symmetric_relationship_5bc42c),
                        symmetric,
                        { symmetric = it },
                    )
                    Text(stringResource(R.string.ui_a_board_annotation_is_local_and_does_a030c4))
                    if (semantic)
                        TextButton(onClick = { pickReuse = true }) {
                            Text(
                                stringResource(R.string.ui_reuse_167317) +
                                    (byId[reuse]?.title ?: "Choose existing relationship")
                            )
                        }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pair = connectPair!!
                        connectPair = null
                        vm.action {
                            val rel =
                                if (reuse != null && semantic)
                                    byId[reuse] ?: error("Relationship missing")
                                else if (semantic) {
                                    require(
                                        pair.first.ref("entry") != null &&
                                            pair.second.ref("entry") != null
                                    ) {
                                        "Convert sticky cards to lore before adding a semantic relationship"
                                    }
                                    vm.add(
                                        Record(
                                            world = b.world.id,
                                            kind = Kind.RELATIONSHIP,
                                            title = label.ifBlank { "Related" },
                                            refs =
                                                mapOf(
                                                    "source" to listOf(pair.first.ref("entry")!!),
                                                    "target" to listOf(pair.second.ref("entry")!!),
                                                ),
                                            fields =
                                                mapOf(
                                                    "direction" to
                                                        if (symmetric) "Symmetric" else "Directed",
                                                    "type" to label,
                                                ),
                                        )
                                    )
                                } else null
                            val edge =
                                Record(
                                    world = b.world.id,
                                    kind = Kind.EDGE,
                                    title = label.ifBlank { "Board annotation" },
                                    refs =
                                        mapOf(
                                            "board" to listOf(board.id),
                                            "from" to listOf(pair.first.id),
                                            "to" to listOf(pair.second.id),
                                            "relationship" to listOfNotNull(rel?.id),
                                        ),
                                )
                            apply(layout + edge)
                        }
                    }
                ) {
                    Text(stringResource(R.string.ui_connect_1a2303))
                }
            },
            dismissButton = {
                TextButton(onClick = { connectPair = null }) {
                    Text(stringResource(R.string.ui_cancel_19766e))
                }
            },
        )
        if (pickReuse) {
            val pair = connectPair!!
            Picker(
                stringResource(R.string.ui_existing_semantic_relationship_368080),
                b.records.filter { rel ->
                    rel.kind == Kind.RELATIONSHIP &&
                        ((rel.ref("source") == pair.first.ref("entry") &&
                            rel.ref("target") == pair.second.ref("entry")) ||
                            rel.f("direction") == "Symmetric" &&
                                rel.ref("target") == pair.first.ref("entry") &&
                                rel.ref("source") == pair.second.ref("entry"))
                },
                onDismiss = { pickReuse = false },
                onSelect = { reuse = it.firstOrNull() },
            )
        }
    }
    if (preview != null)
        AlertDialog(
            onDismissRequest = { preview = null },
            title = { Text(stringResource(R.string.ui_layout_preview_045d7c)) },
            text = {
                Column {
                    BoardMiniature(preview.orEmpty())
                    Text(
                        "Arrange ${placements.size} cards in three columns, 230 × 160 board units apart. Only placement coordinates change; Undo restores the previous layout."
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        apply(preview!!)
                        preview = null
                        fit(placements)
                    }
                ) {
                    Text(stringResource(R.string.ui_apply_layout_f2c282))
                }
            },
            dismissButton = {
                TextButton(onClick = { preview = null }) {
                    Text(stringResource(R.string.ui_cancel_19766e))
                }
            },
        )
    if (remove)
        AlertDialog(
            onDismissRequest = { remove = false },
            title = { Text("Remove ${selection.size} placements?") },
            text = {
                Text(stringResource(R.string.ui_only_these_board_placements_and_thei_1fb4e2))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        apply(
                            layout.filterNot {
                                it.id in selection ||
                                    it.kind == Kind.EDGE &&
                                        (it.ref("from") in selection || it.ref("to") in selection)
                            }
                        )
                        selection = emptyList()
                        remove = false
                    }
                ) {
                    Text(stringResource(R.string.ui_remove_from_board_1c6124))
                }
            },
            dismissButton = {
                TextButton(onClick = { remove = false }) {
                    Text(stringResource(R.string.ui_cancel_19766e))
                }
            },
        )
    edgeEdit?.let { edge ->
        var label by remember(edge.id) { mutableStateOf(edge.title) }
        AlertDialog(
            onDismissRequest = { edgeEdit = null },
            title = { Text(stringResource(R.string.ui_board_annotation_0f6df8)) },
            text = { Field(stringResource(R.string.ui_label_0e6637), label, { label = it }) },
            confirmButton = {
                TextButton(
                    onClick = {
                        apply(
                            layout.map {
                                if (it.id == edge.id)
                                    it.copy(title = label.ifBlank { "Annotation" })
                                else it
                            }
                        )
                        edgeEdit = null
                    }
                ) {
                    Text(stringResource(R.string.ui_save_1509f5))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        apply(layout.filterNot { it.id == edge.id })
                        edgeEdit = null
                    }
                ) {
                    Text(stringResource(R.string.ui_remove_local_line_4d4d6a))
                }
            },
        )
    }
}

@Composable
fun BoardMiniature(records: List<Record>) {
    val cards = records.filter { it.kind == Kind.PLACEMENT && !it.trashed }
    Canvas(
        Modifier.fillMaxWidth()
            .height(100.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .semantics { contentDescription = "Preview of saved board positions and connections" }
    ) {
        if (cards.isNotEmpty()) {
            val minX = cards.minOf { it.n("x") }
            val minY = cards.minOf { it.n("y") }
            val width = cards.maxOf { it.n("x") + it.n("width", 190.0) } - minX
            val height = cards.maxOf { it.n("y") + it.n("height", 112.0) } - minY
            val scale = min((size.width - 16) / width, (size.height - 16) / height)
            fun point(r: Record) =
                Offset(
                    ((r.n("x") - minX + 95) * scale + 8).toFloat(),
                    ((r.n("y") - minY + 56) * scale + 8).toFloat(),
                )
            records
                .filter { it.kind == Kind.EDGE && !it.trashed }
                .forEach { e ->
                    val a = cards.find { it.id == e.ref("from") }
                    val b = cards.find { it.id == e.ref("to") }
                    if (a != null && b != null) drawLine(Color(0xFFA9BFFF), point(a), point(b), 2f)
                }
            cards.forEach { r ->
                drawRoundRect(
                    Color(0xFF85B49E),
                    Offset(
                        ((r.n("x") - minX) * scale + 8).toFloat(),
                        ((r.n("y") - minY) * scale + 8).toFloat(),
                    ),
                    Size(
                        (r.n("width", 190.0) * scale).toFloat(),
                        (r.n("height", 112.0) * scale).toFloat(),
                    ),
                    CornerRadius(3f, 3f),
                )
            }
        }
    }
}
