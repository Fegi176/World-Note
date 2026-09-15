package app.nodenote.core

import kotlinx.serialization.Serializable

@Serializable
enum class TimeKind {
    UNKNOWN,
    EXACT,
    APPROXIMATE,
    RANGE,
    OFFSET,
    LOCAL_YEAR,
    WITHIN,
}

@Serializable
data class TimeExpr(
    val kind: TimeKind = TimeKind.UNKNOWN,
    // Decimal strings deliberately preserve every signed 64-bit value in portable JSON.
    val year: String = "",
    val upper: String = "",
    val tolerance: String = "",
    val anchor: String? = null,
    val boundary: String = "occurrence",
    val minimum: String = "0",
    val maximum: String = "0",
    val label: String = "",
)

enum class ResolutionStatus {
    EXACT,
    BOUNDED,
    APPROXIMATE,
    ORDER_ONLY,
    UNRESOLVED,
    INVALID,
}

data class Position(
    val low: Long? = null,
    val high: Long? = null,
    val estimate: Long? = null,
    val status: ResolutionStatus = ResolutionStatus.UNRESOLVED,
    val provenance: Set<String> = emptySet(),
    val diagnostics: List<String> = emptyList(),
) {
    val exact
        get() = status == ResolutionStatus.EXACT

    fun label(): String =
        when (status) {
            ResolutionStatus.EXACT -> low.toString()
            ResolutionStatus.BOUNDED -> "Possible $low … $high"
            ResolutionStatus.APPROXIMATE ->
                "≈ ${estimate ?: "unknown"}" +
                    if (low != null) " ($low … $high)" else " (tolerance unknown)"
            ResolutionStatus.ORDER_ONLY -> "Order only • year unknown"
            ResolutionStatus.UNRESOLVED -> "Unknown / unresolved"
            ResolutionStatus.INVALID -> "Needs correction: ${diagnostics.joinToString()}"
        }
}

class Chronology(private val records: List<Record>, private val world: World? = null) {
    private val byId = records.associateBy { it.id }

    fun resolve(id: String, boundary: String = "occurrence"): Position =
        resolveKey(id, boundary, emptySet())

    private fun resolveKey(id: String, boundary: String, visiting: Set<String>): Position {
        val key = "$id:$boundary"
        if (visiting.size > 128) return invalid("Date dependency exceeds 128 anchors")
        if (key in visiting) return invalid("Cyclic date dependency at $key")
        if (id == "@present")
            return Position(
                world?.present?.toLongOrNull() ?: 0,
                world?.present?.toLongOrNull() ?: 0,
                status = ResolutionStatus.EXACT,
            )
        val r = byId[id] ?: return invalid("Missing date anchor $id")
        val authored = r.times[boundary] ?: TimeExpr()
        val boundaryEvent =
            if (r.kind.period && authored.kind == TimeKind.UNKNOWN)
                r.ref(
                    if (boundary == "start") "opening event"
                    else if (boundary == "end") "closing event" else ""
                )
            else null
        val expression =
            boundaryEvent?.let { event ->
                TimeExpr(
                    TimeKind.OFFSET,
                    anchor = event,
                    boundary = if (byId[event]?.f("duration") == "true") boundary else "occurrence",
                )
            } ?: authored
        val result = expression(expression, visiting + key)
        return if (
            result.status == ResolutionStatus.UNRESOLVED &&
                records.any {
                    it.kind == Kind.ORDER && (it.ref("before") == id || it.ref("after") == id)
                }
        )
            result.copy(status = ResolutionStatus.ORDER_ONLY)
        else result
    }

    fun expression(e: TimeExpr, visiting: Set<String> = emptySet()): Position =
        try {
            fun year(s: String) =
                s.toLongOrNull()
                    ?: throw IllegalArgumentException("A signed whole year is required")
            fun bounded(
                a: Long,
                b: Long,
                status: ResolutionStatus = ResolutionStatus.BOUNDED,
            ): Position {
                require(a <= b) { "Earliest year exceeds latest year" }
                return Position(
                    a,
                    b,
                    status =
                        if (a == b && status != ResolutionStatus.APPROXIMATE) ResolutionStatus.EXACT
                        else status,
                )
            }
            when (e.kind) {
                TimeKind.UNKNOWN -> Position()
                TimeKind.EXACT -> year(e.year).let { Position(it, it, it, ResolutionStatus.EXACT) }
                TimeKind.APPROXIMATE -> {
                    val c = year(e.year)
                    if (e.tolerance.isBlank())
                        Position(estimate = c, status = ResolutionStatus.APPROXIMATE)
                    else {
                        val t = year(e.tolerance)
                        require(t >= 0) { "Tolerance cannot be negative" }
                        bounded(
                                Math.subtractExact(c, t),
                                Math.addExact(c, t),
                                ResolutionStatus.APPROXIMATE,
                            )
                            .copy(estimate = c)
                    }
                }
                TimeKind.RANGE -> bounded(year(e.year), year(e.upper))
                TimeKind.OFFSET,
                TimeKind.LOCAL_YEAR -> {
                    val anchor = requireNotNull(e.anchor) { "Choose an anchor" }
                    val base =
                        resolveKey(
                            anchor,
                            if (e.kind == TimeKind.LOCAL_YEAR) "start" else e.boundary,
                            visiting,
                        )
                    val lo =
                        if (e.kind == TimeKind.LOCAL_YEAR) Math.subtractExact(year(e.year), 1L)
                        else year(e.minimum)
                    val hi = if (e.kind == TimeKind.LOCAL_YEAR) lo else year(e.maximum)
                    require(lo <= hi) { "Minimum offset exceeds maximum" }
                    base.copy(
                        low = base.low?.let { Math.addExact(it, lo) },
                        high = base.high?.let { Math.addExact(it, hi) },
                        estimate =
                            if (lo == hi) base.estimate?.let { Math.addExact(it, lo) } else null,
                        status =
                            if (base.exact && lo != hi) ResolutionStatus.BOUNDED else base.status,
                        provenance = base.provenance + anchor,
                    )
                }
                TimeKind.WITHIN -> {
                    val anchor = requireNotNull(e.anchor) { "Choose a period" }
                    require(byId[anchor]?.kind?.period == true) { "Within requires a period" }
                    val start = resolveKey(anchor, "start", visiting)
                    val end = resolveKey(anchor, "end", visiting)
                    if (
                        start.status == ResolutionStatus.INVALID ||
                            end.status == ResolutionStatus.INVALID
                    )
                        invalid("Invalid containing period")
                    else if (start.low != null && end.high != null)
                        bounded(start.low, Math.subtractExact(end.high, 1))
                            .copy(provenance = setOf(anchor))
                    else Position(provenance = setOf(anchor))
                }
            }
        } catch (e: Exception) {
            invalid(e.message ?: "Invalid time expression")
        }

    fun diagnostics(): List<Diagnostic> = buildList {
        records.forEach { r ->
            r.times.keys.forEach { slot ->
                val p = resolve(r.id, slot)
                p.diagnostics.forEach { add(Diagnostic("ERROR", r.id, it)) }
            }
            val s = resolve(r.id, "start")
            val e = resolve(r.id, "end")
            if (s.low != null && e.high != null && s.low > e.high)
                add(Diagnostic("ERROR", r.id, "Start is after end"))
            if (r.kind.period && s.exact && e.exact && s.low == e.low)
                add(Diagnostic("ERROR", r.id, "Empty half-open period"))
            r.ref("parent")?.let { parent ->
                if (r.kind.period) {
                    val ps = resolve(parent, "start")
                    val pe = resolve(parent, "end")
                    if (
                        s.exact && ps.exact && s.low!! < ps.low!! ||
                            e.exact && pe.exact && e.high!! > pe.high!!
                    )
                        add(
                            Diagnostic(
                                if (r.f("association status") in listOf("disputed", "incomplete"))
                                    "WARNING"
                                else "ERROR",
                                r.id,
                                "Child period outside parent; fix or mark disputed/incomplete",
                            )
                        )
                }
            }
        }
        val orders = records.filter { it.kind == Kind.ORDER }
        val edges = orders.mapNotNull { r ->
            r.ref("before")?.let { a -> r.ref("after")?.let { b -> a to b } }
        }
        cycleNodes(edges).forEach { add(Diagnostic("ERROR", it, "Cyclic chronological order")) }
        orders.forEach { r ->
            val a = r.ref("before")?.let { resolve(it, r.f("before boundary", "occurrence")) }
            val b = r.ref("after")?.let { resolve(it, r.f("after boundary", "occurrence")) }
            // Same-year events can have a meaningful sequence. Equality is not a conflict.
            if (a?.low != null && b?.high != null && a.low > b.high)
                add(Diagnostic("ERROR", r.id, "Order contradicts known years"))
        }
    }

    fun ordered(items: List<Record>): List<Record> {
        val ids = items.map { it.id }.toSet()
        val edges =
            records
                .filter { it.kind == Kind.ORDER }
                .mapNotNull { r ->
                    r.ref("before")?.let { a -> r.ref("after")?.let { b -> a to b } }
                }
                .filter { it.first in ids && it.second in ids }
        val numeric = items.associate { r ->
            val p =
                resolve(
                    r.id,
                    if (r.kind.period || r.f("duration") == "true") "start" else "occurrence",
                )
            r.id to (p.low ?: p.estimate)
        }
        val successors = edges.groupBy({ it.first }, { it.second })
        fun presentationRank(id: String, seen: Set<String> = emptySet()): Long {
            numeric[id]?.let {
                return it
            }
            if (id in seen || seen.size > 128) return Long.MAX_VALUE
            return successors[id].orEmpty().minOfOrNull { presentationRank(it, seen + id) }
                ?: Long.MAX_VALUE
        }
        val remaining =
            items
                .sortedWith(
                    compareBy<Record> { presentationRank(it.id) }
                        .thenBy { it.n("sequence") }
                        .thenBy { it.title }
                )
                .toMutableList()
        val result = mutableListOf<Record>()
        while (remaining.isNotEmpty()) {
            val next =
                remaining.firstOrNull { r ->
                    edges.none { it.second == r.id && remaining.any { x -> x.id == it.first } }
                } ?: remaining.first()
            result += next
            remaining.remove(next)
        }
        return result
    }

    companion object {
        fun invalid(message: String) =
            Position(status = ResolutionStatus.INVALID, diagnostics = listOf(message))
    }
}

data class Diagnostic(val severity: String, val id: String, val message: String)

fun cycleNodes(edges: List<Pair<String, String>>): Set<String> {
    val next = edges.groupBy({ it.first }, { it.second })
    val done = mutableSetOf<String>()
    val path = linkedSetOf<String>()
    val bad = mutableSetOf<String>()
    fun walk(id: String) {
        if (id in path) {
            bad += path.dropWhile { it != id }
            return
        }
        if (path.size > 128) {
            bad += id
            return
        }
        if (!done.add(id)) return
        path += id
        next[id].orEmpty().forEach(::walk)
        path -= id
    }
    (edges.map { it.first } + edges.map { it.second }).forEach(::walk)
    return bad
}
