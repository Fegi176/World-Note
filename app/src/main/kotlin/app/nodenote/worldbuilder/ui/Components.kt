@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.nodenote.worldbuilder.ui

import android.content.Intent
import android.net.Uri
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.View
import android.widget.TextView
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.nodenote.core.*
import app.nodenote.worldbuilder.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val LocalStatusLabels = staticCompositionLocalOf<Map<String, String>> { emptyMap() }

@Composable
fun StatusChoice(
    label: String,
    value: String,
    options: List<String>,
    prefix: String,
    onChange: (String) -> Unit,
) {
    val labels = LocalStatusLabels.current
    val choices = options.associateWith { code ->
        (labels["$prefix.$code"] ?: code.replace('_', ' ')) + " ($code)"
    }
    Choice(label, choices[value] ?: value, choices.values.toList()) { displayed ->
        onChange(choices.entries.first { it.value == displayed }.key)
    }
}

@Composable
fun Heading(title: String, subtitle: String = "") {
    Column(
        Modifier.padding(top = 20.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Normal,
        )
        if (subtitle.isNotEmpty())
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
    }
}

@Composable
fun Empty(text: String) {
    Text(
        text,
        Modifier.padding(24.dp),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun ActionRow(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    lines: Int = 1,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value,
        onChange,
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        label = { Text(label) },
        minLines = lines,
        enabled = enabled,
        singleLine = lines == 1,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    )
}

@Composable
fun Choice(label: String, value: String, options: List<String>, onChange: (String) -> Unit) {
    var show by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { show = true }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text("$label: $value", Modifier.weight(1f))
    }
    if (show)
        AlertDialog(
            onDismissRequest = { show = false },
            title = { Text(label) },
            text = {
                LazyColumn {
                    items(options) { o ->
                        TextButton(
                            onClick = {
                                onChange(o)
                                show = false
                            },
                            Modifier.fillMaxWidth(),
                        ) {
                            Text(o, Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { show = false }) {
                    Text(stringResource(R.string.ui_close_7d9eb7))
                }
            },
        )
}

@Composable
fun Check(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(
                Modifier.toggleable(
                    value = value,
                    role = androidx.compose.ui.semantics.Role.Checkbox,
                    onValueChange = onChange,
                )
            ),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Checkbox(value, null)
        Text(label, Modifier.weight(1f))
    }
}

@Composable
fun RecordCard(record: Record, onClick: () -> Unit, extra: String = "") {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Box(Modifier.size(42.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    AtlasIcon(
                        when {
                            record.kind.period || record.kind == Kind.EVENT -> "Timeline"
                            record.kind == Kind.BOARD || record.kind == Kind.RELATIONSHIP ->
                                "Boards"
                            else -> "Library"
                        },
                        Modifier.size(20.dp),
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "${record.kind.label.uppercase()}${if(record.favorite)" • FAVORITE" else ""}${if(record.trashed)" • IN TRASH" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    record.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 5.dp),
                )
                val preview = extra.ifBlank { record.summary.ifBlank { record.body } }
                if (preview.isNotBlank())
                    Text(
                        preview.take(300),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                if (record.kind.lore)
                    Text(
                        "${LocalStatusLabels.current["canon.${record.canon}"] ?: record.canon.replace('_',' ')} · ${LocalStatusLabels.current["writing.${record.writing}"] ?: record.writing.replace('_',' ')}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
            }
        }
    }
}

@Composable
fun Picker(
    title: String,
    records: List<Record>,
    selected: List<String> = emptyList(),
    multiple: Boolean = false,
    onDismiss: () -> Unit,
    onSelect: (List<String>) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var ids by remember { mutableStateOf(selected) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Field(
                    stringResource(R.string.ui_find_by_title_or_alias_c58481),
                    query,
                    { query = it },
                )
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(
                        records.filter {
                            !it.trashed &&
                                (query.isBlank() ||
                                    it.title.contains(query, true) ||
                                    it.f("aliases").contains(query, true))
                        },
                        key = { it.id },
                    ) { r ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable {
                                    if (multiple) ids = if (r.id in ids) ids - r.id else ids + r.id
                                    else {
                                        onSelect(listOf(r.id))
                                        onDismiss()
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            if (multiple)
                                Checkbox(r.id in ids, { ids = if (it) ids + r.id else ids - r.id })
                            Column(Modifier.weight(1f)) {
                                Text(r.title)
                                Text(
                                    "${r.kind.label} · ${r.id.take(8)}",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSelect(ids)
                    onDismiss()
                }
            ) {
                Text(if (multiple) "Apply" else "Close")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onSelect(emptyList())
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.ui_clear_83b12c))
            }
        },
    )
}

@Composable
fun References(
    label: String,
    allowed: Set<Kind>,
    record: Record,
    all: List<Record>,
    onChange: (Record) -> Unit,
    multiple: Boolean = Schema.isMultiple(record.kind, label),
) {
    var show by remember { mutableStateOf(false) }
    val ids = record.refs[label].orEmpty()
    OutlinedButton(onClick = { show = true }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(
            "$label: " +
                ids.joinToString { id -> all.find { it.id == id }?.title ?: "Unresolved $id" }
                    .ifBlank { "Choose…" },
            Modifier.fillMaxWidth(),
        )
    }
    if (show)
        Picker(
            label,
            all.filter { it.kind in allowed && it.id != record.id },
            ids,
            multiple,
            { show = false },
        ) {
            onChange(record.copy(refs = record.refs + (label to it)))
        }
}

@Composable
fun TimeEditor(
    slot: String,
    expression: TimeExpr,
    all: List<Record>,
    onChange: (TimeExpr) -> Unit,
) {
    var show by remember { mutableStateOf(false) }
    OutlinedCard(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(
                slot.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleSmall,
            )
            Choice(
                stringResource(R.string.ui_date_form_2f666b),
                expression.kind.name,
                TimeKind.entries.map { it.name },
            ) {
                onChange(TimeExpr(kind = TimeKind.valueOf(it)))
            }
            when (expression.kind) {
                TimeKind.UNKNOWN ->
                    Field(
                        stringResource(R.string.ui_optional_explanation_aff5b5),
                        expression.label,
                        { onChange(expression.copy(label = it)) },
                    )
                TimeKind.EXACT ->
                    Field(
                        stringResource(R.string.ui_signed_year_coordinate_a2b928),
                        expression.year,
                        { onChange(expression.copy(year = it)) },
                    )
                TimeKind.APPROXIMATE -> {
                    Field(
                        stringResource(R.string.ui_estimated_year_408aca),
                        expression.year,
                        { onChange(expression.copy(year = it)) },
                    )
                    Field(
                        stringResource(R.string.ui_tolerance_in_years_optional_c0bfa2),
                        expression.tolerance,
                        { onChange(expression.copy(tolerance = it)) },
                    )
                    Text(
                        stringResource(R.string.ui_no_tolerance_means_uncertainty_is_un_8aa5f5),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                TimeKind.RANGE -> {
                    Field(
                        stringResource(R.string.ui_earliest_possible_year_d03b50),
                        expression.year,
                        { onChange(expression.copy(year = it)) },
                    )
                    Field(
                        stringResource(R.string.ui_latest_possible_year_4a2f42),
                        expression.upper,
                        { onChange(expression.copy(upper = it)) },
                    )
                    Text(
                        stringResource(R.string.ui_an_uncertain_occurrence_window_not_a_3e7e2d),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                TimeKind.OFFSET,
                TimeKind.LOCAL_YEAR,
                TimeKind.WITHIN -> {
                    OutlinedButton(onClick = { show = true }) {
                        Text(
                            stringResource(R.string.ui_anchor_3c53bb) +
                                (if (expression.anchor == "@present") "Fixed Present"
                                else all.find { it.id == expression.anchor }?.title ?: "Choose…")
                        )
                    }
                    if (expression.kind == TimeKind.OFFSET) {
                        TextButton(onClick = { onChange(expression.copy(anchor = "@present")) }) {
                            Text(stringResource(R.string.ui_use_fixed_present_anchor_6f5342))
                        }
                        Choice(
                            stringResource(R.string.ui_anchor_boundary_5c9493),
                            expression.boundary,
                            listOf("occurrence", "start", "end"),
                        ) {
                            onChange(expression.copy(boundary = it))
                        }
                        Field(
                            stringResource(R.string.ui_minimum_offset_negative_before_1c2e56),
                            expression.minimum,
                            { onChange(expression.copy(minimum = it)) },
                        )
                        Field(
                            stringResource(R.string.ui_maximum_offset_939c98),
                            expression.maximum,
                            { onChange(expression.copy(maximum = it)) },
                        )
                    }
                    if (expression.kind == TimeKind.LOCAL_YEAR)
                        Field(
                            stringResource(R.string.ui_local_year_numbering_starts_at_1_6ca71d),
                            expression.year,
                            { onChange(expression.copy(year = it)) },
                        )
                }
            }
            Text(
                Chronology(all).expression(expression).label(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
    if (show)
        Picker(
            stringResource(R.string.ui_choose_time_anchor_19282f),
            all.filter {
                it.kind.period || expression.kind == TimeKind.OFFSET && it.kind == Kind.EVENT
            },
            onDismiss = { show = false },
            onSelect = { onChange(expression.copy(anchor = it.firstOrNull())) },
        )
}

@Composable
fun MarkdownPreview(source: String, onInternal: (String) -> Unit) {
    val context = LocalContext.current
    val color = MaterialTheme.colorScheme.onSurface.toArgb()
    val linkColor = MaterialTheme.colorScheme.primary.toArgb()
    var html by remember { mutableStateOf("") }
    LaunchedEffect(source) { html = withContext(Dispatchers.Default) { Markdown.html(source) } }
    AndroidView(
        factory = {
            TextView(it).apply {
                textSize = 17f
                setTextIsSelectable(true)
                movementMethod = LinkMovementMethod.getInstance()
            }
        },
        update = { view ->
            view.setTextColor(color)
            view.setLinkTextColor(linkColor)
            val text = SpannableStringBuilder(Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT))
            text.getSpans(0, text.length, URLSpan::class.java).forEach { span ->
                val start = text.getSpanStart(span)
                val end = text.getSpanEnd(span)
                text.removeSpan(span)
                text.setSpan(
                    object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            val uri = Uri.parse(span.url)
                            if (uri.scheme == "nodenote" && uri.host == "entry")
                                onInternal(uri.lastPathSegment.orEmpty())
                            else if (uri.scheme in listOf("https", "http", "mailto"))
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                }
                        }
                    },
                    start,
                    end,
                    0,
                )
            }
            view.text = text
        },
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
    )
}
