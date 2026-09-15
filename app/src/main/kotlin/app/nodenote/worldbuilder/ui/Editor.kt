package app.nodenote.worldbuilder.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.nodenote.core.*
import app.nodenote.worldbuilder.AppModel
import app.nodenote.worldbuilder.R
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun Editor(vm: AppModel, r: Record, b: WorldBundle, onExportDraft: () -> Unit) {
    var tab by rememberText(vm, "view.${r.id}.tab", if (r.kind.period) "Codex" else "Note")
    var preview by rememberFlag(vm, "view.${r.id}.preview")
    var linkPicker by remember { mutableStateOf(false) }
    var create by remember { mutableStateOf<Kind?>(null) }
    var createRefs by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    var impact by remember { mutableStateOf<String?>(null) }
    var typeChange by remember { mutableStateOf<Kind?>(null) }
    val saved by vm.saveState.collectAsStateWithLifecycle()
    val all = b.records
    val existing = all.associateBy { it.id }
    val scope = rememberCoroutineScope()
    val imagePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
            if (it != null) vm.attach(it, r)
        }
    fun add(kind: Kind, refs: Map<String, List<String>>) {
        create = kind
        createRefs = refs
    }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    r.kind.label + if (r.locked) " · PROTECTED" else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(saved, style = MaterialTheme.typography.labelSmall, maxLines = 3)
            }
            TextButton(onClick = { vm.retry() }) { Text(stringResource(R.string.ui_save_1509f5)) }
            TextButton(onClick = { vm.close() }) { Text(stringResource(R.string.ui_done_11a676)) }
        }
        if (saved.startsWith("Save failed"))
            TextButton(onClick = onExportDraft) {
                Text(stringResource(R.string.ui_export_recoverable_text_161238))
            }
        if (saved.contains("Revision conflict"))
            ActionRow {
                TextButton(onClick = { vm.resolveConflict(true) }) {
                    Text(stringResource(R.string.atlas_0c67be7951))
                }
                TextButton(onClick = { vm.resolveConflict(false) }) {
                    Text(stringResource(R.string.atlas_ee666709b9))
                }
            }
        ActionRow {
            (if (r.kind.period) listOf("Codex") else emptyList())
                .plus(listOf("Note", "Details", "Links", "History", "Media", "Revisions"))
                .forEach { name ->
                    FilterChip(
                        tab == name,
                        {
                            vm.flush()
                            tab = name
                        },
                        label = { Text(name) },
                    )
                }
        }
        Column(
            Modifier.weight(1f)
                .verticalScroll(rememberDurableScroll(vm, "view.${r.id}.$tab.scroll"))
                .padding(16.dp)
        ) {
            if (r.locked) {
                Heading(r.title, "This approved content is protected.")
                Button(onClick = { vm.update(r.copy(locked = false)) }) {
                    Text(stringResource(R.string.ui_create_revision_unlock_for_editing_6d8225))
                }
                MarkdownPreview(r.body) { id -> existing[id]?.let(vm::open) }
                return@Column
            }
            when (tab) {
                "Codex" -> EpochCodex(vm, r, b, edit = { tab = "Note" }, add = ::add)
                "Note" -> {
                    val cover = all.firstOrNull {
                        it.kind == Kind.MEDIA &&
                            it.ref("owner") == r.id &&
                            it.f("cover") == "true" &&
                            !it.trashed
                    }
                    cover
                        ?.ref("attachment")
                        ?.let { existing[it] }
                        ?.let { CoverImage(vm, it, cover?.f("caption").orEmpty()) }
                    Field(
                        stringResource(R.string.ui_title_7e8cd2),
                        r.title,
                        { vm.edit(r.copy(title = it)) },
                    )
                    Field(
                        stringResource(R.string.ui_summary_8e76a9),
                        r.summary,
                        { vm.edit(r.copy(summary = it)) },
                        3,
                    )
                    ActionRow {
                        FilterChip(
                            preview,
                            { preview = !preview },
                            label = {
                                Text(if (preview) "Markdown preview" else "Markdown source")
                            },
                        )
                        TextButton(onClick = { linkPicker = true }) {
                            Text(stringResource(R.string.ui_insert_entry_link_fb550b))
                        }
                    }
                    if (preview)
                        MarkdownPreview(r.body) { id ->
                            existing[id]?.let(vm::open)
                                ?: run { vm.message.value = "Unresolved or missing entry $id" }
                        }
                    else
                        Field(
                            stringResource(R.string.ui_full_notes_markdown_507f1f),
                            r.body,
                            { vm.edit(r.copy(body = it)) },
                            18,
                        )
                    val wiki = Regex("\\[\\[([^]\\n]*)$").find(r.body)?.groupValues?.get(1)
                    if (wiki != null) {
                        Text(stringResource(R.string.ui_link_suggestions_choose_identity_216702))
                        all.filter {
                                it.kind.lore &&
                                    !it.trashed &&
                                    (it.title.contains(wiki, true) ||
                                        it.f("aliases").contains(wiki, true))
                            }
                            .take(8)
                            .forEach { target ->
                                TextButton(
                                    onClick = {
                                        vm.edit(
                                            r.copy(
                                                body =
                                                    r.body.substringBeforeLast("[[") +
                                                        "[${escape(target.title)}](nodenote://entry/${target.id})"
                                            )
                                        )
                                    }
                                ) {
                                    Text("${target.title} · ${target.id.take(8)}")
                                }
                            }
                    }
                    Heading(stringResource(R.string.ui_optional_sections_fields_0bf057))
                    val fields =
                        all.filter {
                                it.kind in setOf(Kind.FIELD, Kind.SECTION) &&
                                    it.ref("owner") == r.id &&
                                    !it.trashed
                            }
                            .sortedBy { it.n("order") }
                    fields
                        .filter { it.f("hidden") != "true" }
                        .forEach { field ->
                            RecordCard(
                                field,
                                { vm.open(field) },
                                field
                                    .f("value")
                                    .ifBlank { field.body }
                                    .ifBlank {
                                        "${field.f("field type","Markdown")} · Optional, ready to write"
                                    },
                            )
                        }
                    ActionRow {
                        TextButton(
                            onClick = { add(Kind.SECTION, mapOf("owner" to listOf(r.id))) }
                        ) {
                            Text(stringResource(R.string.ui_add_section_7c3690))
                        }
                        TextButton(onClick = { add(Kind.FIELD, mapOf("owner" to listOf(r.id))) }) {
                            Text(stringResource(R.string.ui_add_custom_field_53db6a))
                        }
                    }
                    all.filter {
                            it.kind == Kind.TEMPLATE &&
                                it.f("entry type") == r.kind.name &&
                                !it.trashed
                        }
                        .forEach { template ->
                            TextButton(onClick = { vm.template(template, r) }) {
                                Text("Apply ${template.title}")
                            }
                        }
                    if (r.kind.period) {
                        Heading(
                            stringResource(R.string.ui_epoch_codex_c2578a),
                            "Boundaries, nested eras, world states, linked lore, and what remains.",
                        )
                        val chrono = Chronology(all, b.world)
                        Text(
                            "Start: ${chrono.resolve(r.id,"start").label()}\nEnd: ${chrono.resolve(r.id,"end").label()}\nTrack: ${r.f("track","Global")}"
                        )
                        ActionRow {
                            TextButton(
                                onClick = { add(Kind.ERA, mapOf("parent" to listOf(r.id))) }
                            ) {
                                Text(stringResource(R.string.ui_add_era_61c667))
                            }
                            TextButton(
                                onClick = { add(Kind.EVENT, mapOf("periods" to listOf(r.id))) }
                            ) {
                                Text(stringResource(R.string.ui_add_event_57d208))
                            }
                            TextButton(
                                onClick = {
                                    add(Kind.ASSOCIATION, mapOf("periods" to listOf(r.id)))
                                }
                            ) {
                                Text(stringResource(R.string.ui_link_entry_c7db67))
                            }
                        }
                        all.filter {
                                !it.trashed && (it.ref("parent") == r.id || it.has("periods", r.id))
                            }
                            .forEach { child -> RecordCard(child, { vm.open(child) }) }
                        Heading(stringResource(R.string.ui_legacy_what_remains_1181f0))
                        all.filter {
                                it.kind == Kind.RELATIONSHIP &&
                                    (it.ref("source") == r.id || it.ref("target") == r.id) &&
                                    !it.trashed
                            }
                            .forEach { relation ->
                                RecordCard(
                                    relation,
                                    { vm.open(relation) },
                                    "${existing[relation.ref("source")]?.title} → ${existing[relation.ref("target")]?.title}\n${relation.body}",
                                )
                            }
                        TextButton(
                            onClick = { add(Kind.RELATIONSHIP, mapOf("source" to listOf(r.id))) }
                        ) {
                            Text(stringResource(R.string.ui_create_legacy_relationship_7a5db9))
                        }
                    }
                    if (r.kind == Kind.CLAIM) {
                        Heading(stringResource(R.string.ui_who_knows_or_believes_this_abd26d))
                        all.filter {
                                it.kind == Kind.KNOWLEDGE && it.ref("claim") == r.id && !it.trashed
                            }
                            .forEach { k ->
                                RecordCard(
                                    k,
                                    { vm.open(k) },
                                    "${existing[k.ref("holder")]?.title}: ${k.f("state","Unspecified")}",
                                )
                            }
                        TextButton(
                            onClick = { add(Kind.KNOWLEDGE, mapOf("claim" to listOf(r.id))) }
                        ) {
                            Text(stringResource(R.string.ui_add_knowledge_state_b760f1))
                        }
                    }
                    if (r.kind == Kind.SCHEME) {
                        Heading(
                            stringResource(R.string.ui_planned_execution_and_actual_events_26d12c)
                        )
                        all.filter {
                                it.kind == Kind.STAGE && it.ref("scheme") == r.id && !it.trashed
                            }
                            .sortedBy { it.n("order") }
                            .forEach { stage ->
                                RecordCard(
                                    stage,
                                    { vm.open(stage) },
                                    "${stage.f("state","planned")}\nPlan: ${stage.f("planned outcome")}\nActual: ${stage.f("actual outcome")}",
                                )
                            }
                        TextButton(onClick = { add(Kind.STAGE, mapOf("scheme" to listOf(r.id))) }) {
                            Text(stringResource(R.string.ui_add_stage_76fa77))
                        }
                    }
                    if (r.kind == Kind.ACCOUNT) {
                        Heading(
                            stringResource(R.string.ui_recorded_account_70305a),
                            "Only explicitly included public title and public summary are used in shareable history.",
                        )
                        all.filter {
                                it.kind == Kind.ASSERTION &&
                                    it.ref("account") == r.id &&
                                    !it.trashed
                            }
                            .forEach {
                                RecordCard(
                                    it,
                                    { vm.open(it) },
                                    if (it.f("include") == "true") it.f("public summary")
                                    else "Excluded from recorded account",
                                )
                            }
                        TextButton(
                            onClick = { add(Kind.ASSERTION, mapOf("account" to listOf(r.id))) }
                        ) {
                            Text(stringResource(R.string.ui_add_account_assertion_f21802))
                        }
                    }
                    if (r.kind == Kind.TEMPLATE) {
                        Heading(
                            stringResource(R.string.ui_template_field_definitions_005e1a),
                            "Hiding/removing definitions never deletes values already copied into entries.",
                        )
                        all.filter { it.kind == Kind.FIELD && it.ref("owner") == r.id }
                            .sortedBy { it.n("order") }
                            .forEach { RecordCard(it, { vm.open(it) }) }
                        TextButton(onClick = { add(Kind.FIELD, mapOf("owner" to listOf(r.id))) }) {
                            Text(stringResource(R.string.ui_add_field_definition_dd77f6))
                        }
                    }
                }
                "Details" -> {
                    Heading("${r.kind.label} details")
                    if (r.kind.lore) {
                        Choice(
                            stringResource(R.string.ui_entry_type_e8aa77),
                            r.kind.label,
                            Kind.entries.filter { it.lore }.map { it.label },
                        ) { label ->
                            typeChange = Kind.entries.first { it.label == label }
                        }
                        Field(
                            stringResource(R.string.ui_aliases_comma_separated_732ccf),
                            r.f("aliases"),
                            { vm.edit(r.withField("aliases", it)) },
                        )
                        Field(
                            stringResource(R.string.ui_tags_comma_separated_35b1f0),
                            r.f("tags"),
                            { vm.edit(r.withField("tags", it)) },
                        )
                    }
                    StatusChoice("Canon", r.canon, Schema.canon, "canon") {
                        vm.edit(r.copy(canon = it))
                    }
                    StatusChoice("Writing", r.writing, Schema.writing, "writing") {
                        vm.edit(r.copy(writing = it))
                    }
                    Check(
                        stringResource(R.string.ui_favorite_ea713e),
                        r.favorite,
                        { vm.edit(r.copy(favorite = it)) },
                    )
                    Check(
                        stringResource(R.string.ui_quick_capture_inbox_198528),
                        r.inbox,
                        { vm.edit(r.copy(inbox = it)) },
                    )
                    when (r.kind) {
                        Kind.RELATIONSHIP ->
                            Choice(
                                stringResource(R.string.ui_direction_9c8a95),
                                r.f("direction", "Directed"),
                                listOf("Directed", "Symmetric"),
                            ) {
                                vm.edit(r.withField("direction", it))
                            }
                        Kind.CLAIM -> {
                            Choice(
                                stringResource(R.string.ui_author_truth_96433b),
                                r.f("truth", "UNDECIDED"),
                                Schema.truth,
                            ) {
                                vm.edit(r.withField("truth", it))
                            }
                            Choice(
                                stringResource(R.string.ui_information_role_0c5ddb),
                                r.f("role", "FACT"),
                                Schema.roles,
                            ) {
                                vm.edit(r.withField("role", it))
                            }
                        }
                        Kind.KNOWLEDGE -> {
                            Choice(
                                stringResource(R.string.ui_knowledge_state_c1b06a),
                                r.f("state", "Unspecified"),
                                Schema.knowledge,
                            ) {
                                vm.edit(r.withField("state", it))
                            }
                            Text(
                                stringResource(
                                    R.string.ui_no_ledger_record_means_unspecified_k_4b9aad
                                )
                            )
                        }
                        Kind.STAGE ->
                            Choice(
                                stringResource(R.string.ui_stage_state_806445),
                                r.f("state", "planned"),
                                listOf(
                                    "planned",
                                    "attempted",
                                    "succeeded",
                                    "failed",
                                    "changed",
                                    "unresolved",
                                ),
                            ) {
                                vm.edit(r.withField("state", it))
                            }
                        Kind.ASSERTION ->
                            Check(
                                stringResource(
                                    R.string.ui_include_in_recorded_public_history_52b83c
                                ),
                                r.f("include") == "true",
                                { vm.edit(r.withField("include", it.toString())) },
                            )
                        Kind.SECTION ->
                            Check(
                                stringResource(
                                    R.string.ui_explicitly_shareable_section_still_r_c1ffd4
                                ),
                                r.f("shared") == "true",
                                { vm.edit(r.withField("shared", it.toString())) },
                            )
                        Kind.FIELD -> {
                            Choice(
                                stringResource(R.string.ui_field_type_01f04f),
                                r.f("field type", "Markdown"),
                                listOf(
                                    "Short text",
                                    "Markdown",
                                    "Number",
                                    "Boolean",
                                    "Choice",
                                    "Multi-choice",
                                    "Entry reference",
                                    "Reference list",
                                    "Chronology",
                                ),
                            ) {
                                vm.edit(r.withField("field type", it))
                            }
                            Check(
                                stringResource(
                                    R.string.ui_hide_deprecate_field_preserve_value_7b0816
                                ),
                                r.f("hidden") == "true",
                                { vm.edit(r.withField("hidden", it.toString())) },
                            )
                            if (r.f("field type") == "Boolean")
                                Check(
                                    stringResource(R.string.ui_value_8e3795),
                                    r.f("value") == "true",
                                    { vm.edit(r.withField("value", it.toString())) },
                                )
                            if (r.f("field type") in listOf("Choice", "Multi-choice"))
                                Choice(
                                    stringResource(R.string.ui_value_8e3795),
                                    r.f("value"),
                                    r.f("choices").split(',').filter { it.isNotBlank() },
                                ) { value ->
                                    vm.edit(
                                        r.withField(
                                            "value",
                                            if (r.f("field type") == "Multi-choice")
                                                (r.f("value").split(',').filter {
                                                        it.isNotBlank()
                                                    } + value)
                                                    .distinct()
                                                    .joinToString(",")
                                            else value,
                                        )
                                    )
                                }
                        }
                        Kind.MEDIA ->
                            Check(
                                stringResource(R.string.ui_use_as_entry_cover_cb1811),
                                r.f("cover") == "true",
                                { vm.edit(r.withField("cover", it.toString())) },
                            )
                        Kind.ORDER -> {
                            Choice(
                                stringResource(R.string.ui_before_boundary_12a4ea),
                                r.f("before boundary", "occurrence"),
                                listOf("occurrence", "start", "end"),
                            ) {
                                vm.edit(r.withField("before boundary", it))
                            }
                            Choice(
                                stringResource(R.string.ui_after_boundary_7d0108),
                                r.f("after boundary", "occurrence"),
                                listOf("occurrence", "start", "end"),
                            ) {
                                vm.edit(r.withField("after boundary", it))
                            }
                            Text(
                                stringResource(
                                    R.string.ui_same_year_sequence_is_valid_unknown__4a17ff
                                )
                            )
                        }
                        else -> Unit
                    }
                    Schema.textFields(r.kind).forEach { key ->
                        Field(
                            key.replaceFirstChar { it.uppercase() },
                            r.f(key),
                            { vm.edit(r.withField(key, it)) },
                            if (
                                key.contains("summary") ||
                                    key.contains("outcome") ||
                                    key.contains("explanation")
                            )
                                4
                            else 1,
                        )
                    }
                    if (r.kind == Kind.EVENT) {
                        Check(
                            stringResource(R.string.ui_duration_event_separate_start_and_en_7fbc6e),
                            r.f("duration") == "true",
                            { vm.edit(r.withField("duration", it.toString())) },
                        )
                        Text(
                            stringResource(R.string.ui_occurrence_uncertainty_is_separate_f_d2b31a)
                        )
                    }
                    Schema.timeSlots(r.kind)
                        .filter { r.kind != Kind.FIELD || r.f("field type") == "Chronology" }
                        .forEach { slot ->
                            TimeEditor(slot, r.times[slot] ?: TimeExpr(), all) {
                                vm.edit(r.copy(times = r.times + (slot to it)))
                            }
                        }
                    Heading(stringResource(R.string.ui_protection_recovery_1b23ae))
                    Button(onClick = { vm.navigate { vm.update(r.copy(locked = true)) } }) {
                        Text(stringResource(R.string.ui_protect_approved_content_7b6d25))
                    }
                    OutlinedButton(onClick = { impact = "trash" }) {
                        Text(if (r.trashed) "Restore from trash" else "Move to trash")
                    }
                    if (r.trashed)
                        OutlinedButton(onClick = { impact = "erase" }) {
                            Text(stringResource(R.string.ui_permanently_erase_content_c8b29c))
                        }
                }
                "Links" -> {
                    Heading(
                        stringResource(R.string.ui_stable_references_ad92f0),
                        "Renaming entries does not break their links.",
                    )
                    Schema.roles(r.kind).forEach { (role, kinds) ->
                        References(role, kinds, r, all, { vm.edit(it) })
                    }
                    Heading(stringResource(R.string.ui_relationships_85752a))
                    all.filter {
                            it.kind == Kind.RELATIONSHIP &&
                                !it.trashed &&
                                (it.ref("source") == r.id || it.ref("target") == r.id)
                        }
                        .forEach { relation ->
                            RecordCard(
                                relation,
                                { vm.open(relation) },
                                "${existing[relation.ref("source")]?.title} ${if(relation.f("direction")=="Symmetric")"↔" else "→"} ${existing[relation.ref("target")]?.title}\n${relation.body}",
                            )
                        }
                    if (r.kind.lore)
                        TextButton(
                            onClick = { add(Kind.RELATIONSHIP, mapOf("source" to listOf(r.id))) }
                        ) {
                            Text(
                                stringResource(
                                    R.string.ui_new_relationship_with_detailed_notes_87ca6b
                                )
                            )
                        }
                    Heading(stringResource(R.string.ui_backlinks_b4278c))
                    all.filter { source ->
                            !source.trashed &&
                                source.kind !in setOf(Kind.DRAFT, Kind.REVISION) &&
                                (source.refs.values.any { r.id in it } ||
                                    Integrity.internalLink.findAll(source.body).any {
                                        it.groupValues[1] == r.id
                                    })
                        }
                        .distinctBy { it.id }
                        .forEach { RecordCard(it, { vm.open(it) }) }
                    if (r.kind in setOf(Kind.CHARACTER, Kind.FACTION, Kind.DEITY)) {
                        Heading(stringResource(R.string.ui_knowledge_ledger_a5e1c3))
                        all.filter { it.kind == Kind.KNOWLEDGE && it.ref("holder") == r.id }
                            .forEach { k ->
                                RecordCard(
                                    k,
                                    { vm.open(k) },
                                    "${k.f("state","Unspecified")}: ${existing[k.ref("claim")]?.title}",
                                )
                            }
                        TextButton(
                            onClick = { add(Kind.KNOWLEDGE, mapOf("holder" to listOf(r.id))) }
                        ) {
                            Text(stringResource(R.string.ui_add_knowledge_state_b760f1))
                        }
                    }
                }
                "History" -> {
                    Heading(
                        stringResource(R.string.ui_explicit_historical_states_88ef85),
                        "The current entry is never substituted for an unrecorded past.",
                    )
                    val states = all.filter {
                        it.kind == Kind.STATE && it.ref("entry") == r.id && !it.trashed
                    }
                    var period by rememberIdentity(vm, "view.${r.id}.history.period")
                    var pick by remember { mutableStateOf(false) }
                    TextButton(onClick = { pick = true }) {
                        Text(
                            stringResource(R.string.ui_period_8ac7d1) +
                                (existing[period]?.title ?: "All recorded states")
                        )
                    }
                    if (pick)
                        Picker(
                            stringResource(R.string.ui_period_context_fd5a2d),
                            all.filter { it.kind.period },
                            onDismiss = { pick = false },
                            onSelect = { period = it.firstOrNull() },
                        )
                    val visible = states.filter { period == null || it.has("periods", period!!) }
                    if (visible.isEmpty())
                        Empty(stringResource(R.string.ui_no_state_recorded_for_this_period_826ab1))
                    visible.forEach { state ->
                        RecordCard(
                            state,
                            { vm.open(state) },
                            state.refs["periods"].orEmpty().joinToString {
                                existing[it]?.title ?: "Unknown period"
                            } + "\n" + state.body,
                        )
                    }
                    if (visible.size > 1 && period != null)
                        Text(
                            stringResource(R.string.ui_multiple_states_recorded_review_each_3361db)
                        )
                    if (r.kind.lore)
                        TextButton(
                            onClick = {
                                add(
                                    Kind.STATE,
                                    mapOf(
                                        "entry" to listOf(r.id),
                                        "periods" to listOfNotNull(period),
                                    ),
                                )
                            }
                        ) {
                            Text(stringResource(R.string.ui_add_historical_state_c379f7))
                        }
                    Heading(stringResource(R.string.ui_recorded_accounts_f695e9))
                    all.filter { it.kind == Kind.ASSERTION && it.ref("subject") == r.id }
                        .forEach { assertion ->
                            RecordCard(
                                assertion,
                                { vm.open(assertion) },
                                assertion.f("public title") + "\n" + assertion.f("public summary"),
                            )
                        }
                    Heading(stringResource(R.string.ui_story_reveals_separate_from_history_f19252))
                    all.filter { it.kind == Kind.REVEAL && it.has("target", r.id) }
                        .forEach { reveal ->
                            RecordCard(
                                reveal,
                                { vm.open(reveal) },
                                existing[reveal.ref("story")]?.title ?: "Choose a story beat",
                            )
                        }
                    if (r.kind.lore || r.kind == Kind.CLAIM)
                        TextButton(
                            onClick = { add(Kind.REVEAL, mapOf("target" to listOf(r.id))) }
                        ) {
                            Text(stringResource(R.string.ui_plan_a_reveal_f3f02a))
                        }
                }
                "Media" -> {
                    Heading(
                        stringResource(R.string.ui_managed_gallery_2a7dff),
                        "Accepted originals are copied into private storage and complete backups.",
                    )
                    if (r.kind.lore || r.kind in setOf(Kind.STATE, Kind.SECTION))
                        Button(onClick = { imagePicker.launch(arrayOf("image/*")) }) {
                            Text(stringResource(R.string.ui_import_image_bf2f1b))
                        }
                    all.filter { it.kind == Kind.MEDIA && it.ref("owner") == r.id && !it.trashed }
                        .forEach { media ->
                            val attachment = existing[media.ref("attachment")]
                            if (attachment != null) {
                                var bitmap by
                                    remember(attachment.id) {
                                        mutableStateOf<android.graphics.Bitmap?>(null)
                                    }
                                var error by remember { mutableStateOf("") }
                                LaunchedEffect(attachment.id) {
                                    withContext(Dispatchers.IO) {
                                        runCatching {
                                            BitmapFactory.decodeFile(
                                                vm.files.thumbnail(attachment).path
                                            )
                                        }
                                            .onSuccess { bitmap = it }
                                            .onFailure { error = it.message.orEmpty() }
                                    }
                                }
                                bitmap?.let {
                                    Image(
                                        it.asImageBitmap(),
                                        contentDescription =
                                            media.f("caption").ifBlank { attachment.title },
                                        modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                                    )
                                }
                                if (error.isNotEmpty()) Text("Missing/corrupt attachment: $error")
                                RecordCard(
                                    media,
                                    { vm.open(media) },
                                    "${if(media.f("cover")=="true")"Cover · " else ""}${media.f("caption")}\n${media.f("credit")}",
                                )
                            }
                        }
                }
                "Revisions" -> {
                    Heading(
                        stringResource(R.string.ui_recovery_history_23f8e0),
                        "Restoring creates a new current revision and retains the old one. Revisions are retained indefinitely.",
                    )
                    all.filter {
                            it.kind in setOf(Kind.REVISION, Kind.DRAFT) && it.ref("owner") == r.id
                        }
                        .sortedByDescending { it.created }
                        .forEach { revision ->
                            Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(revision.title)
                                    Text(
                                        DateFormat.getDateTimeInstance()
                                            .format(Date(revision.updated))
                                    )
                                    Text(revision.f("changed sections"))
                                    Button(onClick = { vm.restoreRevision(revision) }) {
                                        Text(
                                            stringResource(
                                                R.string.ui_restore_as_new_revision_b5a566
                                            )
                                        )
                                    }
                                }
                            }
                        }
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
    if (linkPicker)
        Picker(
            stringResource(R.string.ui_insert_stable_entry_link_7a4850),
            all.filter { it.kind.lore },
            onDismiss = { linkPicker = false },
            onSelect = { ids ->
                ids.firstOrNull()?.let { id ->
                    val target = existing.getValue(id)
                    vm.edit(
                        r.copy(body = r.body + "\n[${escape(target.title)}](nodenote://entry/$id)")
                    )
                }
            },
        )
    if (create != null) CreateRecordDialog(vm, b, create!!, { create = null }, createRefs)
    if (typeChange != null)
        AlertDialog(
            onDismissRequest = { typeChange = null },
            title = { Text(stringResource(R.string.ui_change_display_type_1909c6)) },
            text = {
                Text(stringResource(R.string.ui_all_existing_notes_fields_period_eve_dad350))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.edit(r.copy(kind = typeChange!!))
                        typeChange = null
                    }
                ) {
                    Text(stringResource(R.string.ui_preserve_data_and_convert_6f6ce0))
                }
            },
            dismissButton = {
                TextButton(onClick = { typeChange = null }) {
                    Text(stringResource(R.string.ui_cancel_19766e))
                }
            },
        )
    if (impact == "erase")
        AlertDialog(
            onDismissRequest = { impact = null },
            title = { Text(stringResource(R.string.ui_permanently_erase_content_a5b191)) },
            text = {
                Text(stringResource(R.string.ui_writing_owned_fields_historical_stat_6a2b04))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.erase(r)
                        impact = null
                    }
                ) {
                    Text(stringResource(R.string.ui_back_up_and_erase_bb60fc))
                }
            },
            dismissButton = {
                TextButton(onClick = { impact = null }) {
                    Text(stringResource(R.string.ui_cancel_19766e))
                }
            },
        )
    if (impact == "trash")
        AlertDialog(
            onDismissRequest = { impact = null },
            title = { Text(if (r.trashed) "Restore entry?" else "Move to trash?") },
            text = {
                Text(
                    "${all.count { source->source.refs.values.any { r.id in it } }} records reference this identity. References, placements, historical states, and attachments are retained for recovery."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.trash(r)
                        impact = null
                    }
                ) {
                    Text(if (r.trashed) "Restore" else "Move to trash")
                }
            },
            dismissButton = {
                TextButton(onClick = { impact = null }) {
                    Text(stringResource(R.string.ui_cancel_19766e))
                }
            },
        )
}

@Composable
fun EpochCodex(
    vm: AppModel,
    r: Record,
    b: WorldBundle,
    edit: () -> Unit,
    add: (Kind, Map<String, List<String>>) -> Unit,
) {
    val all = b.records
    val byId = all.associateBy { it.id }
    val chronology = Chronology(all, b.world)
    Heading(r.title, "${r.f("track","Global")} · ${r.canon} · ${r.writing.replace('_',' ')}")
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Start · ${chronology.resolve(r.id,"start").label()}")
            Text("End · ${chronology.resolve(r.id,"end").label()}")
            val start = chronology.resolve(r.id, "start")
            val end = chronology.resolve(r.id, "end")
            if (start.exact && end.exact)
                Text(
                    "Duration · ${java.math.BigInteger.valueOf(end.high!!).subtract(java.math.BigInteger.valueOf(start.low!!))} years [start, end)"
                )
            all.filter {
                    it.kind == Kind.ORDER && (it.ref("before") == r.id || it.ref("after") == r.id)
                }
                .forEach { order ->
                    Text(
                        "${byId[order.ref("before")]?.title} → ${byId[order.ref("after")]?.title} (distance unspecified)"
                    )
                }
        }
    }
    if (r.summary.isNotBlank()) Text(r.summary, Modifier.padding(vertical = 12.dp))
    MarkdownPreview(r.body) { id -> byId[id]?.let(vm::open) }
    Button(onClick = edit) { Text(stringResource(R.string.ui_write_edit_epoch_notes_2ae8b5)) }
    Heading(stringResource(R.string.ui_chronology_children_405e7e))
    ActionRow {
        TextButton(onClick = { add(Kind.ERA, mapOf("parent" to listOf(r.id))) }) {
            Text(stringResource(R.string.ui_add_era_61c667))
        }
        TextButton(onClick = { add(Kind.EVENT, mapOf("periods" to listOf(r.id))) }) {
            Text(stringResource(R.string.ui_add_event_57d208))
        }
        TextButton(onClick = { add(Kind.ASSOCIATION, mapOf("periods" to listOf(r.id))) }) {
            Text(stringResource(R.string.ui_link_lore_46ed17))
        }
    }
    chronology
        .ordered(
            all.filter {
                !it.trashed &&
                    (it.ref("parent") == r.id || it.kind == Kind.EVENT && it.has("periods", r.id))
            }
        )
        .forEach { child ->
            RecordCard(
                child,
                { vm.open(child) },
                chronology
                    .resolve(child.id, if (child.kind.period) "start" else "occurrence")
                    .label(),
            )
        }
    Heading(stringResource(R.string.ui_world_state_in_this_period_953c05))
    val states = all.filter { it.kind == Kind.STATE && it.has("periods", r.id) && !it.trashed }
    if (states.isEmpty())
        Text(stringResource(R.string.ui_no_explicit_world_states_recorded_fo_056867))
    states.forEach { state ->
        RecordCard(state, { vm.open(state) }, "${byId[state.ref("entry")]?.title}\n${state.body}")
    }
    Heading(stringResource(R.string.ui_legacy_what_remains_1181f0))
    all.filter {
            it.kind == Kind.RELATIONSHIP &&
                !it.trashed &&
                (it.ref("source") == r.id || it.ref("target") == r.id || it.has("periods", r.id))
        }
        .forEach { relation ->
            RecordCard(
                relation,
                { vm.open(relation) },
                "${byId[relation.ref("source")]?.title} → ${byId[relation.ref("target")]?.title}\n${relation.body}",
            )
        }
    TextButton(onClick = { add(Kind.RELATIONSHIP, mapOf("source" to listOf(r.id))) }) {
        Text(stringResource(R.string.ui_link_a_surviving_consequence_e38d06))
    }
    Button(onClick = { vm.navigate { vm.legacyBoard(r) } }) {
        Text(stringResource(R.string.ui_open_focused_legacy_board_8fe42b))
    }
    Heading(stringResource(R.string.ui_recorded_accounts_f695e9))
    all.filter { it.kind == Kind.ASSERTION && it.ref("subject") == r.id }
        .forEach { assertion ->
            RecordCard(assertion, { vm.open(assertion) }, assertion.f("public summary"))
        }
    all.filter {
            it.kind in setOf(Kind.FIELD, Kind.SECTION) &&
                it.ref("owner") == r.id &&
                !it.trashed &&
                it.f("hidden") != "true"
        }
        .sortedBy { it.n("order") }
        .forEach { RecordCard(it, { vm.open(it) }, it.f("value").ifBlank { it.body }) }
}

@Composable
fun CoverImage(vm: AppModel, attachment: Record, caption: String) {
    var bitmap by remember(attachment.id) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var error by remember(attachment.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(attachment.id) {
        withContext(Dispatchers.IO) {
            runCatching { BitmapFactory.decodeFile(vm.files.thumbnail(attachment).path) }
                .onSuccess { bitmap = it }
                .onFailure { error = it.message }
        }
    }
    bitmap?.let {
        Image(
            it.asImageBitmap(),
            caption.ifBlank { attachment.title },
            Modifier.fillMaxWidth().heightIn(max = 180.dp),
        )
    }
    if (error != null) Text("Cover unavailable: $error")
}
