package app.nodenote.core

import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.serialization.json.*

data class ImportPreview(val bundle: WorldBundle, val warnings: List<String>)

object Interchange {
    fun legacy(text: String): ImportPreview {
        require(text.length <= 64 * 1024 * 1024) { "JSON input exceeds 64 MiB" }
        val root = codec.parseToJsonElement(boundedJson(text)).jsonObject
        require(root.s("format") == "nodenote" && root["version"]?.jsonPrimitive?.intOrNull == 1) {
            "Expected NodeNote v1; unsupported format/version"
        }
        val world = World(name = "Imported NodeNote workspace")
        val records = mutableListOf<Record>()
        val maps = root["maps"]?.jsonArray ?: error("Missing maps")
        require(maps.map { it.jsonObject.s("id") }.distinct().size == maps.size) {
            "Duplicate map ID"
        }
        maps.forEach { item ->
            val map = item.jsonObject
            val view = map["view"]?.jsonObject
            val board =
                Record(
                    world = world.id,
                    kind = Kind.BOARD,
                    title = map.s("name", "Imported board"),
                    fields =
                        mapOf(
                            "pan x" to (view?.s("x", "0") ?: "0"),
                            "pan y" to (view?.s("y", "0") ?: "0"),
                            "scale" to (view?.s("s", "1") ?: "1"),
                        ),
                )
            records += board
            val nodes = map["nodes"]?.jsonArray ?: error("Missing nodes")
            val mapped = mutableMapOf<String, Pair<Record, Record>>()
            nodes.forEach { n ->
                val node = n.jsonObject
                val old = node.s("id")
                require(old.isNotBlank() && old !in mapped) {
                    "Duplicate/missing node ID within map"
                }
                val entry =
                    Record(
                        world = world.id,
                        kind = Kind.NOTE,
                        title = node.s("title", "Untitled").ifBlank { "Untitled" },
                        body = node.s("note"),
                        fields = mapOf("legacy map" to map.s("id"), "legacy node" to old),
                    )
                val placement =
                    Record(
                        world = world.id,
                        kind = Kind.PLACEMENT,
                        title = entry.title,
                        refs = mapOf("board" to listOf(board.id), "entry" to listOf(entry.id)),
                        fields =
                            mapOf(
                                "x" to node.s("x", "0"),
                                "y" to node.s("y", "0"),
                                "width" to "190",
                                "height" to "112",
                                "color" to node.s("color", "#b9a1ff"),
                            ),
                    )
                records += entry
                records += placement
                mapped[old] = entry to placement
            }
            map["edges"]?.jsonArray.orEmpty().forEach { itemEdge ->
                val edge = itemEdge.jsonObject
                val a = mapped[edge.s("from")] ?: error("Dangling legacy edge")
                val b = mapped[edge.s("to")] ?: error("Dangling legacy edge")
                val relationship =
                    Record(
                        world = world.id,
                        kind = Kind.RELATIONSHIP,
                        title = edge.s("label", "Related").ifBlank { "Related" },
                        fields = mapOf("direction" to "Directed", "type" to edge.s("label")),
                        refs =
                            mapOf("source" to listOf(a.first.id), "target" to listOf(b.first.id)),
                    )
                records += relationship
                records +=
                    Record(
                        world = world.id,
                        kind = Kind.EDGE,
                        title = relationship.title,
                        refs =
                            mapOf(
                                "board" to listOf(board.id),
                                "from" to listOf(a.second.id),
                                "to" to listOf(b.second.id),
                                "relationship" to listOf(relationship.id),
                            ),
                    )
            }
        }
        records +=
            Record(
                world = world.id,
                kind = Kind.IMPORT,
                title = "NodeNote v1 recovery source",
                body = text,
                fields =
                    mapOf(
                        "source hash" to sha256(text.toByteArray()),
                        "warnings" to
                            "Original input retained, including unknown fields. Nodes remain map-scoped.",
                    ),
            )
        val bundle = WorldBundle(world, records)
        Integrity.requireValid(bundle)
        return ImportPreview(
            bundle,
            listOf(
                "Imported into a new world. No entries merged. Viewport uses screen = world × scale + translation; source retained."
            ),
        )
    }

    fun canvas(text: String): ImportPreview {
        require(text.length <= 64 * 1024 * 1024)
        val root = codec.parseToJsonElement(boundedJson(text)).jsonObject
        val world = World(name = "Imported Canvas")
        val records = mutableListOf<Record>()
        val warnings = mutableListOf<String>()
        val board = Record(world = world.id, kind = Kind.BOARD, title = "Imported Canvas")
        records += board
        val ids = mutableMapOf<String, String>()
        val groups = mutableSetOf<String>()
        root["nodes"]?.jsonArray?.forEach { item ->
            val n = item.jsonObject
            val old = n.s("id")
            require(old.isNotBlank() && old !in ids)
            val type = n.s("type")
            if (type == "group") {
                val group =
                    Record(
                        world = world.id,
                        kind = Kind.GROUP,
                        title = n.s("label", "Canvas group").ifBlank { "Canvas group" },
                        refs = mapOf("board" to listOf(board.id)),
                        fields =
                            listOf("x", "y", "width", "height", "color").associateWith {
                                n.s(
                                    it,
                                    when (it) {
                                        "width",
                                        "height" -> "400"
                                        "color" -> "#b9a1ff"
                                        else -> "0"
                                    },
                                )
                            },
                    )
                records += group
                ids[old] = group.id
                groups += group.id
                return@forEach
            }
            val body =
                if (type == "text") n.s("text")
                else
                    "Unsupported $type card\n\n${item}"
                        .also {
                            warnings +=
                                "Retained $type node as a placeholder; no external files or URLs fetched."
                        }
            val entry =
                Record(
                    world = world.id,
                    kind = Kind.NOTE,
                    title =
                        body.lineSequence().firstOrNull()?.take(120)?.ifBlank { "Canvas note" }
                            ?: "Canvas note",
                    body = body,
                )
            val p =
                Record(
                    world = world.id,
                    kind = Kind.PLACEMENT,
                    title = entry.title,
                    refs = mapOf("board" to listOf(board.id), "entry" to listOf(entry.id)),
                    fields =
                        listOf("x", "y", "width", "height", "color").associateWith {
                            n.s(
                                it,
                                when (it) {
                                    "width" -> "190"
                                    "height" -> "112"
                                    "color" -> "#b9a1ff"
                                    else -> "0"
                                },
                            )
                        },
                )
            records += entry
            records += p
            ids[old] = p.id
        } ?: error("Missing Canvas nodes")
        records.indices
            .filter { records[it].kind == Kind.PLACEMENT }
            .forEach { i ->
                val p = records[i]
                val group =
                    records
                        .filter { it.kind == Kind.GROUP }
                        .filter {
                            it.rect().contains(Point(p.n("x"), p.n("y"))) &&
                                it.rect()
                                    .contains(
                                        Point(p.n("x") + p.n("width"), p.n("y") + p.n("height"))
                                    )
                        }
                        .minByOrNull { it.n("width") * it.n("height") }
                if (group != null) records[i] = p.withRef("group", group.id)
            }
        root["edges"]?.jsonArray.orEmpty().forEach { item ->
            val e = item.jsonObject
            if (ids[e.s("fromNode")] in groups || ids[e.s("toNode")] in groups) {
                warnings +=
                    "Connection to group retained in raw import source; group boundary edges are not flattened into lore."
                return@forEach
            }
            records +=
                Record(
                    world = world.id,
                    kind = Kind.EDGE,
                    title = e.s("label", "Board annotation").ifBlank { "Board annotation" },
                    fields =
                        mapOf(
                            "fromEnd" to e.s("fromEnd", "none"),
                            "toEnd" to e.s("toEnd", "arrow"),
                            "color" to e.s("color"),
                        ),
                    refs =
                        mapOf(
                            "board" to listOf(board.id),
                            "from" to listOf(ids[e.s("fromNode")] ?: error("Dangling Canvas edge")),
                            "to" to listOf(ids[e.s("toNode")] ?: error("Dangling Canvas edge")),
                        ),
                )
        }
        records +=
            Record(
                world = world.id,
                kind = Kind.IMPORT,
                title = "JSON Canvas recovery source",
                body = text,
                fields = mapOf("source hash" to sha256(text.toByteArray())),
            )
        val bundle = WorldBundle(world, records)
        Integrity.requireValid(bundle)
        return ImportPreview(bundle, warnings)
    }

    fun canvasExport(bundle: WorldBundle, board: String): String {
        val byId = bundle.records.associateBy { it.id }
        val placements =
            bundle.records.filter {
                it.kind == Kind.PLACEMENT && it.ref("board") == board && !it.trashed
            }
        val nodes =
            placements.map { p ->
                val entry = byId[p.ref("entry")]
                buildJsonObject {
                    put("id", p.id)
                    put("type", "text")
                    put("text", entry?.body ?: p.body)
                    put("x", p.n("x"))
                    put("y", p.n("y"))
                    put("width", p.n("width", 190.0))
                    put("height", p.n("height", 112.0))
                    put("color", p.f("color", "#b9a1ff"))
                }
            } +
                bundle.records
                    .filter { it.kind == Kind.GROUP && it.ref("board") == board && !it.trashed }
                    .map { g ->
                        val members = placements.filter { it.ref("group") == g.id }
                        val x = members.minOfOrNull { it.n("x") - 20 } ?: g.n("x")
                        val y = members.minOfOrNull { it.n("y") - 40 } ?: g.n("y")
                        val width =
                            (members.maxOfOrNull { it.n("x") + it.n("width", 190.0) + 20 }
                                ?: (x + g.n("width", 400.0))) - x
                        val height =
                            (members.maxOfOrNull { it.n("y") + it.n("height", 112.0) + 20 }
                                ?: (y + g.n("height", 400.0))) - y
                        buildJsonObject {
                            put("id", g.id)
                            put("type", "group")
                            put("label", g.title)
                            put("x", x)
                            put("y", y)
                            put("width", width)
                            put("height", height)
                        }
                    }
        val edges =
            bundle.records
                .filter {
                    it.kind == Kind.EDGE &&
                        it.ref("board") == board &&
                        !it.trashed &&
                        placements.any { p -> p.id == it.ref("from") } &&
                        placements.any { p -> p.id == it.ref("to") }
                }
                .map { e ->
                    buildJsonObject {
                        put("id", e.id)
                        put("fromNode", e.ref("from"))
                        put("toNode", e.ref("to"))
                        put("label", byId[e.ref("relationship")]?.title ?: e.title)
                        put("fromEnd", e.f("fromEnd", "none"))
                        put("toEnd", e.f("toEnd", "arrow"))
                    }
                }
        return buildJsonObject {
            put("nodes", JsonArray(nodes))
            put("edges", JsonArray(edges))
        }
            .toString()
    }

    private fun JsonObject.s(key: String, default: String = "") =
        this[key]?.jsonPrimitive?.contentOrNull ?: default
}

data class PublicPage(val title: String, val body: String)

object Projection {
    /**
     * Only expressly authored account prose is allowed through. No canonical title/body/ID/metadata
     * fallback.
     */
    fun account(bundle: WorldBundle, accountId: String): List<PublicPage> =
        bundle.records
            .filter {
                it.kind == Kind.ASSERTION &&
                    it.ref("account") == accountId &&
                    it.f("include") == "true" &&
                    !it.trashed
            }
            .map { assertion ->
                val sections =
                    bundle.records.filter {
                        it.kind == Kind.SECTION &&
                            it.id in assertion.refs["shared sections"].orEmpty() &&
                            it.f("shared") == "true" &&
                            !it.trashed
                    }
                PublicPage(
                    assertion.f("public title", "Untitled account entry"),
                    stripInternal(
                        assertion.f("public summary") +
                            sections.joinToString("") {
                                "\n\n### ${escape(it.title)}\n\n${it.body}"
                            }
                    ),
                )
            }

    fun stripInternal(s: String) =
        s.replace(Regex("\\[([^]]*)]\\(nodenote://[^)]*\\)"), "$1")
            .replace(Regex("nodenote://[^\\s)]+"), "[reference]")

    fun markdown(pages: List<PublicPage>) =
        "# Recorded history\n\n" +
            pages.joinToString("\n\n") { "## ${escape(it.title)}\n\n${it.body}" }
}

fun escape(s: String) =
    s.replace("\\", "\\\\")
        .replace("[", "\\[")
        .replace("]", "\\]")
        .replace("#", "\\#")
        .replace("*", "\\*")
        .replace("_", "\\_")

object WorldBible {
    private fun file(r: Record) = "entry-${r.id}.md"

    private fun links(s: String, byId: Map<String, Record>, combined: Boolean): String =
        Integrity.internalLink.replace(s) { m ->
            byId[m.groupValues[1]]?.let { if (combined) "#entry-${it.id}" else file(it) }
                ?: "#unresolved-reference"
        }

    fun entry(bundle: WorldBundle, r: Record, combined: Boolean = false): String {
        val byId = bundle.records.associateBy { it.id }
        val time = Chronology(bundle.records, bundle.world)
        return buildString {
            append(
                "<a id=\"entry-${r.id}\"></a>\n# ${escape(r.title)}\n\n${r.kind.label} • ${r.canon} • ${r.writing}\n\n${r.summary}\n\n${links(r.body,byId,combined)}\n"
            )
            r.times.forEach { (key, _) -> append("\n$key: ${time.resolve(r.id,key).label()}\n") }
            r.fields
                .filterKeys { it !in listOf("snapshot", "path") }
                .forEach { (key, value) ->
                    append("\n**${escape(key)}:** ${links(value,byId,combined)}\n")
                }
            r.refs.forEach { (role, targets) ->
                append("\n**${escape(role)}:** ")
                append(
                    targets.joinToString { id ->
                        byId[id]?.let {
                            "[${escape(it.title)}](${if(combined) "#entry-$id" else file(it)})"
                        } ?: "Unresolved reference"
                    }
                )
                append('\n')
            }
            bundle.records
                .filter {
                    !it.trashed &&
                        (it.has("owner", r.id) ||
                            it.has("entry", r.id) ||
                            it.has("source", r.id) ||
                            it.has("target", r.id)) &&
                        it.kind in
                            setOf(
                                Kind.SECTION,
                                Kind.FIELD,
                                Kind.STATE,
                                Kind.RELATIONSHIP,
                                Kind.MEDIA,
                            )
                }
                .sortedBy { it.n("order") }
                .forEach { child ->
                    if (child.kind == Kind.MEDIA) {
                        val a = byId[child.ref("attachment")]
                        if (a != null)
                            append("\n![${escape(child.f("caption"))}](assets/${a.id})\n")
                    } else
                        append(
                            "\n## ${escape(child.title)}\n\n${links(child.body,byId,combined)}\n${child.f("value")}\n"
                        )
                }
        }
    }

    fun combined(bundle: WorldBundle): String {
        val entries =
            bundle.records.filter {
                !it.trashed &&
                    it.kind !in
                        setOf(
                            Kind.REVISION,
                            Kind.DRAFT,
                            Kind.IMPORT,
                            Kind.ATTACHMENT,
                            Kind.PLACEMENT,
                            Kind.EDGE,
                            Kind.GROUP,
                            Kind.FILTER,
                        )
            }
        return "# ${escape(bundle.world.name)} — Author edition\n\nContains author notes and secrets. Reading export, not a restorable backup.\n\n## Contents\n\n" +
            entries.joinToString("\n") { "- [${escape(it.title)}](#entry-${it.id})" } +
            "\n\n" +
            entries.joinToString("\n\n---\n\n") { entry(bundle, it, true) }
    }

    fun zip(bundle: WorldBundle, out: OutputStream, asset: (Record) -> File) {
        ZipOutputStream(out).use { z ->
            fun put(name: String, bytes: ByteArray) {
                z.putNextEntry(ZipEntry(name))
                z.write(bytes)
                z.closeEntry()
            }
            put("README.md", combined(bundle).toByteArray())
            bundle.records
                .filter {
                    !it.trashed &&
                        it.kind !in setOf(Kind.ATTACHMENT, Kind.REVISION, Kind.DRAFT, Kind.IMPORT)
                }
                .forEach { put(file(it), entry(bundle, it).toByteArray()) }
            bundle.records
                .filter { it.kind == Kind.ATTACHMENT }
                .forEach { a ->
                    z.putNextEntry(ZipEntry("assets/${a.id}"))
                    asset(a).inputStream().use { it.copyTo(z) }
                    z.closeEntry()
                }
        }
    }
}
