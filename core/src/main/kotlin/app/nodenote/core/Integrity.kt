package app.nodenote.core

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

object Integrity {
    private val uuid =
        Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    val internalLink = Regex("nodenote://entry/([a-zA-Z0-9-]+)")

    fun validate(bundle: WorldBundle, logical: Boolean = true): List<Diagnostic> = buildList {
        val records = bundle.records
        val byId = records.associateBy { it.id }
        fun error(id: String, msg: String) {
            add(Diagnostic("ERROR", id, msg))
        }
        if (!uuid.matches(bundle.world.id)) error(bundle.world.id, "Malformed world ID")
        if (bundle.world.name.isBlank()) error(bundle.world.id, "World name is required")
        if (bundle.world.present.toLongOrNull() == null)
            error(bundle.world.id, "Invalid fixed present anchor")
        if (byId.size != records.size) error(bundle.world.id, "Duplicate record IDs")
        if (records.size > 100000)
            error(bundle.world.id, "World import exceeds 100,000 record limit")
        records.forEach { r ->
            if (!uuid.matches(r.id)) error(r.id, "Malformed record ID")
            if (r.world != bundle.world.id) error(r.id, "Cross-world record")
            if (r.title.isBlank()) error(r.id, "Title is required")
            if (r.body.length > 5_000_000 || r.title.length > 10_000)
                error(r.id, "Record exceeds documented text limit")
            if (r.canon !in Schema.canon || r.writing !in Schema.writing)
                error(r.id, "Unknown status")
            r.refs.forEach { (role, ids) ->
                if (!Schema.isMultiple(r.kind, role) && ids.size > 1)
                    error(r.id, "$role accepts one target")
                ids.forEach { id ->
                    if (role !in Schema.roles(r.kind)) error(r.id, "Unknown reference role $role")
                    val target = byId[id]
                    if (target == null) error(r.id, "Missing $role reference $id")
                    else if (Schema.roles(r.kind)[role]?.contains(target.kind) == false)
                        error(r.id, "Wrong target type for $role")
                }
            }
            r.times.forEach { (_, e) ->
                e.anchor?.let { id ->
                    if (
                        id != "@present" &&
                            byId[id]?.kind?.let { it.period || it == Kind.EVENT } != true
                    )
                        error(r.id, "Time anchor must be a period or event")
                    if (
                        e.kind in setOf(TimeKind.LOCAL_YEAR, TimeKind.WITHIN) &&
                            byId[id]?.kind?.period != true
                    )
                        error(r.id, "Local-year and within anchors must be periods")
                }
            }
            r.times.values.forEach { e ->
                if (e.boundary !in setOf("start", "end", "occurrence"))
                    error(r.id, "Unsupported temporal boundary")
            }
            if (
                r.kind == Kind.CLAIM &&
                    (r.f("truth", "UNDECIDED") !in Schema.truth ||
                        r.f("role", "FACT") !in Schema.roles)
            )
                error(r.id, "Unknown claim truth or role")
            if (r.kind == Kind.KNOWLEDGE && r.f("state", "Unspecified") !in Schema.knowledge)
                error(r.id, "Unknown knowledge state")
            if (
                r.kind == Kind.RELATIONSHIP &&
                    r.f("direction", "Directed") !in listOf("Directed", "Symmetric")
            )
                error(r.id, "Unknown relationship direction")
            if (
                r.kind == Kind.PLACEMENT ||
                    r.kind == Kind.GROUP &&
                        r.fields.keys.any { it in setOf("x", "y", "width", "height") }
            ) {
                if (r.ref("board") == null) error(r.id, "Placement needs a board")
                listOf("x", "y", "width", "height").forEach { key ->
                    val v = r.f(key).toDoubleOrNull()
                    if (
                        v == null ||
                            !v.isFinite() ||
                            kotlin.math.abs(v) > 1e12 ||
                            (key in listOf("width", "height") && v <= 0)
                    )
                        error(r.id, "Invalid placement $key")
                }
            }
            if (r.kind == Kind.BOARD) {
                if (
                    !r.n("scale", 1.0).isFinite() ||
                        r.n("scale", 1.0) !in .15..4.0 ||
                        !r.n("pan x").isFinite() ||
                        !r.n("pan y").isFinite()
                )
                    error(r.id, "Invalid board viewport")
            }
            if (r.kind == Kind.EDGE) {
                val board = r.ref("board")
                val a = byId[r.ref("from")]
                val b = byId[r.ref("to")]
                if (
                    board == null ||
                        a == null ||
                        b == null ||
                        a.ref("board") != board ||
                        b.ref("board") != board
                )
                    error(r.id, "Edge endpoints must belong to its board")
                r.ref("relationship")?.let { rel ->
                    val rr = byId[rel]
                    if (
                        rr != null &&
                            (rr.ref("source") != a?.ref("entry") ||
                                rr.ref("target") != b?.ref("entry")) &&
                            !(rr.f("direction") == "Symmetric" &&
                                rr.ref("target") == a?.ref("entry") &&
                                rr.ref("source") == b?.ref("entry"))
                    )
                        error(r.id, "Edge does not match semantic endpoints")
                }
            }
            if (r.kind == Kind.RELATIONSHIP && (r.ref("source") == null || r.ref("target") == null))
                error(r.id, "Relationship needs two lore endpoints")
            val required =
                when (r.kind) {
                    Kind.KNOWLEDGE -> listOf("holder", "claim")
                    Kind.REVEAL -> listOf("story", "target")
                    Kind.ASSERTION -> listOf("account", "subject")
                    Kind.STATE -> listOf("entry")
                    Kind.MEDIA -> listOf("owner", "attachment")
                    Kind.ORDER -> listOf("before", "after")
                    Kind.STAGE -> listOf("scheme")
                    else -> emptyList()
                }
            required.forEach { if (r.ref(it) == null) error(r.id, "Choose $it") }
            if (
                r.kind == Kind.ATTACHMENT &&
                    (!Regex("[a-f0-9]{64}").matches(r.f("sha256")) ||
                        r.f("path") != "assets/${r.id}")
            )
                error(r.id, "Unsafe attachment metadata")
            if (r.kind in setOf(Kind.REVISION, Kind.DRAFT)) {
                val old = runCatching {
                    codec.decodeFromString<Record>(r.f("snapshot"))
                }
                    .getOrNull()
                if (
                    old == null ||
                        old.id != r.ref("owner") ||
                        old.world != r.world ||
                        old.kind in setOf(Kind.REVISION, Kind.DRAFT)
                )
                    error(r.id, "Invalid recovery snapshot")
            }
            if (r.kind == Kind.FIELD)
                when (r.f("field type")) {
                    "Number" ->
                        if (r.f("value").isNotBlank() && r.f("value").toBigDecimalOrNull() == null)
                            error(r.id, "Invalid numeric field")
                    "Boolean" ->
                        if (r.f("value") !in listOf("", "true", "false"))
                            error(r.id, "Boolean must be true or false")
                }
            internalLink.findAll(r.body).forEach { link ->
                if (byId[link.groupValues[1]] == null)
                    add(
                        Diagnostic(
                            "WARNING",
                            r.id,
                            "Unresolved internal link ${link.groupValues[1]}",
                        )
                    )
            }
        }
        listOf(Kind.EPOCH, Kind.ERA, Kind.STORY, Kind.GROUP).forEach { kind ->
            val subset = records.filter { if (kind.period) it.kind.period else it.kind == kind }
            cycleNodes(subset.mapNotNull { r -> r.ref("parent")?.let { r.id to it } }).forEach {
                error(it, "Cyclic parent hierarchy")
            }
        }
        if (logical) addAll(Chronology(records, bundle.world).diagnostics())
        records
            .filter { it.kind.lore && !it.trashed }
            .groupBy { it.title.lowercase() }
            .filterValues { it.size > 1 }
            .values
            .forEach { same ->
                same.forEach {
                    add(Diagnostic("WARNING", it.id, "Duplicate title; identities remain separate"))
                }
            }
        records
            .filter { it.kind == Kind.STATE && !it.trashed }
            .groupBy { it.ref("entry") }
            .values
            .forEach { states ->
                states.forEachIndexed { i, a ->
                    states.drop(i + 1).forEach { b ->
                        if (
                            a.refs["periods"]
                                .orEmpty()
                                .intersect(b.refs["periods"].orEmpty().toSet())
                                .isNotEmpty()
                        )
                            add(
                                Diagnostic(
                                    "WARNING",
                                    a.id,
                                    "Multiple explicit historical states for the same period; review both",
                                )
                            )
                    }
                }
            }
    }

    fun requireValid(bundle: WorldBundle) {
        val errors = validate(bundle).filter { it.severity == "ERROR" }
        require(errors.isEmpty()) { errors.take(8).joinToString("\n") { it.message } }
    }
}

object Remap {
    fun world(source: WorldBundle, name: String = source.world.name + " (copy)"): WorldBundle {
        val ids = (source.records.map { it.id } + source.world.id).associateWith { newId() }
        fun text(s: String): String =
            Integrity.internalLink.replace(s) { m ->
                "nodenote://entry/" + (ids[m.groupValues[1]] ?: m.groupValues[1])
            }
        fun record(r: Record): Record {
            val fields =
                r.fields.mapValues { (key, value) ->
                    when {
                        key == "snapshot" && r.kind in setOf(Kind.REVISION, Kind.DRAFT) ->
                            codec.encodeToString(record(codec.decodeFromString<Record>(value)))
                        key == "path" && r.kind == Kind.ATTACHMENT -> "assets/${ids.getValue(r.id)}"
                        key in setOf("definition", "period") && value in ids -> ids.getValue(value)
                        else -> text(value)
                    }
                }
            return r.copy(
                id = ids.getValue(r.id),
                world = ids.getValue(r.world),
                body = text(r.body),
                summary = text(r.summary),
                fields = fields,
                refs =
                    r.refs.mapValues { (_, v) ->
                        v.map { ids[it] ?: error("Unknown remapped reference $it") }
                    },
                times =
                    r.times.mapValues { (_, e) ->
                        e.copy(
                            anchor =
                                e.anchor?.let {
                                    if (it == "@present") it else ids[it] ?: error("Unknown anchor")
                                }
                        )
                    },
            )
        }
        return WorldBundle(
            source.world.copy(id = ids.getValue(source.world.id), name = name, archived = false),
            source.records.map(::record),
        )
    }
}
