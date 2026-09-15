package app.nodenote.worldbuilder.ui

import android.graphics.Paint
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.nodenote.core.*
import app.nodenote.worldbuilder.AppModel
import app.nodenote.worldbuilder.R
import java.math.BigDecimal
import java.math.BigInteger

@Composable
fun TimelineScreen(vm: AppModel, b: WorldBundle) {
    val stateKey = "view.${b.world.id}.timeline"
    var tab by rememberText(vm, "$stateKey.tab", "Epochs")
    var scaled by rememberFlag(vm, "$stateKey.scaled")
    var query by rememberText(vm, "$stateKey.query")
    var track by rememberText(vm, "$stateKey.track", "All")
    val account by vm.historyAccount.collectAsState()
    var create by remember { mutableStateOf<Kind?>(null) }
    var pickAccount by remember { mutableStateOf(false) }
    val all = b.records
    val chronology = remember(b) { Chronology(all, b.world) }
    val accounts = all.filter { it.kind == Kind.ACCOUNT && !it.trashed }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Heading(
                stringResource(R.string.ui_the_epoch_codex_9fa88c),
                "History, uncertainty, and the order of discovery.",
            )
            ActionRow {
                listOf("Epochs", "Events", "Reveals").forEach { name ->
                    FilterChip(
                        tab == name,
                        {
                            tab = name
                            vm.preference("timeline tab", name)
                        },
                        label = { Text(name) },
                    )
                }
            }
            TextButton(onClick = { pickAccount = true }) {
                Text(
                    if (account == null) "Author's History"
                    else "Recorded History — ${accounts.find { it.id==account }?.title.orEmpty()}"
                )
            }
        }
        Column(
            Modifier.weight(1f)
                .verticalScroll(rememberDurableScroll(vm, "$stateKey.$tab.scroll"))
                .padding(horizontal = 16.dp)
        ) {
            if (account != null) {
                Heading(
                    stringResource(R.string.ui_recorded_history_2e8365),
                    "An authoring perspective; only approved account prose is shown.",
                )
                Projection.account(b, account!!).forEach { page ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(page.title, style = MaterialTheme.typography.titleLarge)
                            MarkdownPreview(page.body) {}
                        }
                    }
                }
                TextButton(onClick = { vm.setHistoryAccount(null) }) {
                    Text(stringResource(R.string.ui_return_to_author_s_history_f161ce))
                }
            } else if (tab == "Reveals") {
                Heading(
                    stringResource(R.string.ui_story_order_d2fb87),
                    "Reordering scenes never changes historical dates or character knowledge.",
                )
                ActionRow {
                    Button(onClick = { create = Kind.STORY }) {
                        Text(stringResource(R.string.ui_add_story_node_ffba3e))
                    }
                    OutlinedButton(onClick = { create = Kind.REVEAL }) {
                        Text(stringResource(R.string.ui_add_reveal_2b47d1))
                    }
                }
                val stories = all.filter { it.kind == Kind.STORY && !it.trashed }
                @Composable
                fun Story(r: Record, depth: Int) {
                    if (depth < 32)
                        Column(Modifier.padding(start = (depth.coerceAtMost(5) * 8).dp)) {
                            RecordCard(
                                r,
                                { vm.open(r) },
                                "${r.f("level","Scene")} · order ${r.f("order","0")}",
                            )
                            all.filter {
                                    it.kind == Kind.REVEAL && it.ref("story") == r.id && !it.trashed
                                }
                                .sortedBy { it.n("order") }
                                .forEach { reveal ->
                                    RecordCard(
                                        reveal,
                                        { vm.open(reveal) },
                                        "${reveal.f("information layer")} · ${reveal.f("audience")}\n" +
                                            reveal.refs["target"].orEmpty().joinToString { id ->
                                                all.find { it.id == id }?.title ?: "Missing"
                                            },
                                    )
                                }
                            stories
                                .filter { it.ref("parent") == r.id }
                                .sortedBy { it.n("order") }
                                .forEach { Story(it, depth + 1) }
                        }
                }
                stories
                    .filter { it.ref("parent") == null }
                    .sortedBy { it.n("order") }
                    .forEach { Story(it, 0) }
                if (stories.isEmpty())
                    Empty(stringResource(R.string.ui_create_a_storyline_chapter_or_scene__afb9d9))
            } else {
                ActionRow {
                    FilterChip(
                        !scaled,
                        { scaled = false },
                        label = { Text(stringResource(R.string.ui_overview_d4b1ea)) },
                    )
                    FilterChip(
                        scaled,
                        { scaled = true },
                        label = { Text(stringResource(R.string.ui_scaled_24e0a1)) },
                    )
                    TextButton(
                        onClick = { create = if (tab == "Epochs") Kind.EPOCH else Kind.EVENT }
                    ) {
                        Text("+ ${if(tab=="Epochs")b.world.epochLabel else "Event"}")
                    }
                }
                Field("Find ${tab.lowercase()}", query, { query = it })
                Choice(
                    stringResource(R.string.ui_track_051f01),
                    track,
                    listOf("All") +
                        all.filter { it.kind.period || it.kind == Kind.EVENT }
                            .map { it.f("track", "Global") }
                            .distinct(),
                ) {
                    track = it
                }
                var period by rememberIdentity(vm, "$stateKey.period")
                var participant by rememberIdentity(vm, "$stateKey.participant")
                var pick by remember { mutableStateOf("") }
                if (tab == "Events") {
                    ActionRow {
                        TextButton(onClick = { pick = "period" }) {
                            Text(
                                stringResource(R.string.ui_period_8ac7d1) +
                                    (all.find { it.id == period }?.title ?: "All")
                            )
                        }
                        TextButton(onClick = { pick = "participant" }) {
                            Text(
                                stringResource(R.string.ui_participant_place_d5d4fa) +
                                    (all.find { it.id == participant }?.title ?: "All")
                            )
                        }
                    }
                    if (pick.isNotEmpty())
                        Picker(
                            "Filter $pick",
                            all.filter { if (pick == "period") it.kind.period else it.kind.lore },
                            onDismiss = { pick = "" },
                            onSelect = {
                                if (pick == "period") period = it.firstOrNull()
                                else participant = it.firstOrNull()
                            },
                        )
                }
                val items = all.filter {
                    !it.trashed &&
                        (if (tab == "Epochs") it.kind.period else it.kind == Kind.EVENT) &&
                        (track == "All" || it.f("track", "Global") == track) &&
                        (query.isBlank() ||
                            it.title.contains(query, true) ||
                            it.body.contains(query, true)) &&
                        (period == null || it.has("periods", period!!)) &&
                        (participant == null ||
                            it.has("participants", participant!!) ||
                            it.has("locations", participant!!))
                }
                if (scaled) ScaledTimeline(vm, items, chronology, vm::open)
                else {
                    Text(
                        stringResource(R.string.ui_not_to_scale_card_size_does_not_repr_9b3348),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    @Composable
                    fun Period(r: Record, depth: Int) {
                        var expanded by rememberFlag(vm, "expanded-${r.id}")
                        val start =
                            chronology.resolve(
                                r.id,
                                if (r.kind.period || r.f("duration") == "true") "start"
                                else "occurrence",
                            )
                        val end = chronology.resolve(r.id, "end")
                        val label =
                            if (r.kind.period || r.f("duration") == "true")
                                "${start.label()} → ${end.label()}"
                            else start.label()
                        Column(Modifier.padding(start = (depth.coerceAtMost(5) * 10).dp)) {
                            RecordCard(
                                r,
                                { vm.open(r) },
                                "$label\n${r.f("track","Global")}\n${r.summary.ifBlank { r.body }.take(160)}",
                            )
                            if (r.kind.period && depth < 32) {
                                val children = all.filter {
                                    !it.trashed &&
                                        (it.ref("parent") == r.id ||
                                            it.kind == Kind.EVENT && it.has("periods", r.id))
                                }
                                if (children.isNotEmpty())
                                    TextButton(
                                        onClick = {
                                            expanded = !expanded
                                            vm.preference("expanded-${r.id}", expanded.toString())
                                        }
                                    ) {
                                        Text(
                                            if (expanded) "Collapse eras & events"
                                            else "Expand ${children.size} eras & events"
                                        )
                                    }
                                if (expanded)
                                    chronology.ordered(children).forEach { Period(it, depth + 1) }
                            }
                        }
                    }
                    val ordered =
                        chronology.ordered(
                            items.filter {
                                tab != "Epochs" ||
                                    it.ref("parent") == null ||
                                    items.none { p -> p.id == it.ref("parent") }
                            }
                        )
                    val numeric = ordered.filter {
                        val p =
                            chronology.resolve(
                                it.id,
                                if (it.kind.period || it.f("duration") == "true") "start"
                                else "occurrence",
                            )
                        p.low != null ||
                            p.estimate != null ||
                            p.status == ResolutionStatus.ORDER_ONLY
                    }
                    numeric.forEach { Period(it, 0) }
                    val undated = ordered - numeric.toSet()
                    if (undated.isNotEmpty())
                        Heading(
                            stringResource(R.string.ui_order_only_unplaced_c0b64c),
                            "No year has been invented for these records.",
                        )
                    undated.forEach { Period(it, 0) }
                }
                val diagnostics = chronology.diagnostics()
                if (diagnostics.isNotEmpty()) {
                    Heading(stringResource(R.string.ui_chronology_review_6b14e9))
                    diagnostics.distinct().forEach { d ->
                        TextButton(onClick = { all.find { it.id == d.id }?.let(vm::open) }) {
                            Text(d.message)
                        }
                    }
                }
            }
            Spacer(Modifier.height(110.dp))
        }
    }
    if (create != null) CreateRecordDialog(vm, b, create!!, { create = null })
    if (pickAccount)
        Picker(
            stringResource(R.string.ui_recorded_account_clear_for_author_b72e1f),
            accounts,
            onDismiss = { pickAccount = false },
            onSelect = { vm.setHistoryAccount(it.firstOrNull()) },
        )
}

private data class DrawPeriod(
    val record: Record,
    val start: Long,
    val end: Long,
    val uncertain: Boolean,
    val open: Boolean,
    val label: String,
)

@Composable
fun ScaledTimeline(
    vm: AppModel,
    records: List<Record>,
    chronology: Chronology,
    open: (Record) -> Unit,
) {
    val data = records.mapNotNull { r ->
        val span = r.kind.period || r.f("duration") == "true"
        val p = chronology.resolve(r.id, if (span) "start" else "occurrence")
        val e = if (span) chronology.resolve(r.id, "end") else p
        val start = p.low ?: p.estimate ?: return@mapNotNull null
        DrawPeriod(
            r,
            start,
            e.high ?: e.estimate ?: start,
            !p.exact || !e.exact,
            span && e.high == null,
            "${p.label()}${if(span)" → ${e.label()}" else ""}",
        )
    }
    val worldId by vm.selected.collectAsState()
    var origin by
        rememberSetting(
            vm,
            "view.${worldId}.timeline.origin",
            data.minOfOrNull { it.start } ?: 0L,
            { it.toLong() },
            { it.toString() },
        )
    var zoom by
        rememberSetting(
            vm,
            "view.${worldId}.timeline.zoom",
            .002,
            { it.toDouble().coerceIn(1e-12, 100.0) },
            { it.toString() },
        )
    var jump by remember { mutableStateOf(false) }
    val laneMap =
        lanes(
            data.map {
                LaneItem(
                    it.record.id,
                    it.start,
                    if (it.end == it.start && it.end < Long.MAX_VALUE) it.end + 1 else it.end,
                )
            }
        )
    ActionRow {
        TextButton(onClick = { zoom = (zoom * 2).coerceAtMost(100.0) }) {
            Text(stringResource(R.string.ui_zoom_a69bfe))
        }
        TextButton(onClick = { zoom = (zoom / 2).coerceAtLeast(1e-12) }) {
            Text(stringResource(R.string.ui_zoom_263f36))
        }
        TextButton(
            onClick = {
                origin = data.minOfOrNull { it.start } ?: 0
                val max = data.maxOfOrNull { it.end } ?: origin
                val span =
                    BigInteger.valueOf(max)
                        .subtract(BigInteger.valueOf(origin))
                        .toDouble()
                        .coerceAtLeast(1.0)
                zoom = 270.0 / span
            }
        ) {
            Text(stringResource(R.string.ui_fit_all_d30cd7))
        }
        TextButton(onClick = { jump = true }) {
            Text(stringResource(R.string.ui_jump_fit_period_b31c0f))
        }
    }
    Text(
        "Origin $origin · ${"%.5g".format(1/zoom)} years/dp · drag to pan",
        style = MaterialTheme.typography.labelSmall,
    )
    Canvas(
        Modifier.fillMaxWidth()
            .clipToBounds()
            .height((100 + (laneMap.values.maxOrNull() ?: 0) * 60).coerceIn(200, 480).dp)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .pointerInput(origin, zoom) {
                detectTransformGestures { _, pan, scale, _ ->
                    val shift = pan.x.toDouble() / density / zoom
                    origin =
                        BigDecimal.valueOf(origin)
                            .subtract(BigDecimal.valueOf(shift))
                            .toBigInteger()
                            .coerceIn(
                                BigInteger.valueOf(Long.MIN_VALUE),
                                BigInteger.valueOf(Long.MAX_VALUE),
                            )
                            .toLong()
                    zoom = (zoom * scale).coerceIn(1e-12, 100.0)
                }
            }
    ) {
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                textSize = 12.dp.toPx()
            }
        data.forEach { d ->
            val x = timelineX(d.start, origin, zoom).toFloat() * density + 12.dp.toPx()
            val end = timelineX(d.end, origin, zoom).toFloat() * density + 12.dp.toPx()
            val y = 30.dp.toPx() + (laneMap[d.record.id] ?: 0) * 60.dp.toPx()
            if (x <= size.width && end >= 0) {
                val left = x.coerceAtLeast(0f)
                val right = end.coerceAtMost(size.width)
                if (d.record.kind.period || d.record.f("duration") == "true")
                    drawRect(
                        Color(0xFF36785D),
                        Offset(left, y),
                        Size((right - left).coerceAtLeast(1f), 20.dp.toPx()),
                    )
                else if (d.uncertain && end > x)
                    drawLine(
                        Color(0xFFD5B575),
                        Offset(left, y + 10.dp.toPx()),
                        Offset(right, y + 10.dp.toPx()),
                        5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f)),
                    )
                else
                    drawCircle(
                        if (d.uncertain) Color(0xFFD5B575) else Color(0xFFA9BFFF),
                        5.dp.toPx(),
                        Offset(x, y + 10.dp.toPx()),
                    )
                if (d.uncertain)
                    drawLine(
                        Color(0xFFD5B575),
                        Offset(left, y),
                        Offset(right.coerceAtLeast(left + 4), y),
                        2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f)),
                    )
                drawContext.canvas.nativeCanvas.drawText(
                    (if (d.uncertain) "≈ " else "") +
                        d.record.title.take(24) +
                        (if (d.open) " → unknown" else ""),
                    left,
                    y + 39.dp.toPx(),
                    paint,
                )
            }
        }
    }
    Text(
        stringResource(R.string.ui_solid_spans_duration_dashed_uncertai_3fc25c),
        style = MaterialTheme.typography.bodySmall,
    )
    data.forEach { d -> RecordCard(d.record, { open(d.record) }, d.label) }
    val unresolved = records.filter { r -> data.none { it.record.id == r.id } }
    if (unresolved.isNotEmpty())
        Heading(
            stringResource(R.string.ui_unresolved_tray_ab3195),
            "These records are not plotted at year zero.",
        )
    chronology.ordered(unresolved).forEach {
        RecordCard(it, { open(it) }, "Unknown / order-only chronology")
    }
    if (jump)
        Picker(
            stringResource(R.string.ui_jump_to_resolved_period_event_4c2cda),
            data.map { it.record },
            onDismiss = { jump = false },
            onSelect = { ids ->
                data
                    .find { it.record.id == ids.firstOrNull() }
                    ?.let { d ->
                        origin = d.start
                        val span =
                            BigInteger.valueOf(d.end)
                                .subtract(BigInteger.valueOf(d.start))
                                .toDouble()
                                .coerceAtLeast(1.0)
                        zoom = (250.0 / span).coerceIn(1e-12, 100.0)
                    }
            },
        )
}
