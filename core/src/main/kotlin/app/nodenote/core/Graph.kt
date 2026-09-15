package app.nodenote.core

import kotlin.math.*

data class Point(val x: Double, val y: Double) {
    operator fun plus(p: Point) = Point(x + p.x, y + p.y)

    operator fun minus(p: Point) = Point(x - p.x, y - p.y)

    operator fun times(s: Double) = Point(x * s, y * s)
}

data class Viewport(val scale: Double = 1.0, val translation: Point = Point(0.0, 0.0)) {
    fun screen(p: Point) = p * scale + translation

    fun world(p: Point) = (p - translation) * (1 / scale)

    fun zoom(centroid: Point, factor: Double, pan: Point = Point(0.0, 0.0)): Viewport {
        val newScale = (scale * factor).coerceIn(.15, 4.0)
        return Viewport(newScale, centroid - world(centroid) * newScale + pan)
    }
}

data class Rect(val x: Double, val y: Double, val width: Double, val height: Double) {
    fun contains(p: Point) = p.x in x..(x + width) && p.y in y..(y + height)
}

fun freePosition(
    origin: Point,
    occupied: List<Rect>,
    width: Double = 190.0,
    height: Double = 112.0,
): Point {
    var p = origin
    repeat(occupied.size + 1) {
        if (
            occupied.none { r ->
                p.x < r.x + r.width &&
                    p.x + width > r.x &&
                    p.y < r.y + r.height &&
                    p.y + height > r.y
            }
        )
            return p
        p = p + Point(0.0, height + 48.0)
    }
    return p
}

fun Record.rect() = Rect(n("x"), n("y"), n("width", 190.0), n("height", 112.0))

fun lineDistance(p: Point, a: Point, b: Point): Double {
    val v = b - a
    val len = v.x * v.x + v.y * v.y
    if (len == 0.0) return hypot(p.x - a.x, p.y - a.y)
    val t = (((p.x - a.x) * v.x + (p.y - a.y) * v.y) / len).coerceIn(0.0, 1.0)
    return hypot(p.x - a.x - v.x * t, p.y - a.y - v.y * t)
}

/** Subtract in integer space first; never convert two absolute years to Float. */
fun timelineX(year: Long, origin: Long, pixelsPerYear: Double): Double =
    java.math.BigInteger.valueOf(year).subtract(java.math.BigInteger.valueOf(origin)).toDouble() *
        pixelsPerYear

data class LaneItem(val id: String, val start: Long, val end: Long)

fun lanes(items: List<LaneItem>): Map<String, Int> {
    val ends = mutableListOf<Long>()
    val result = mutableMapOf<String, Int>()
    items
        .sortedBy { it.start }
        .forEach { item ->
            val lane = ends.indexOfFirst { it <= item.start }.let { if (it < 0) ends.size else it }
            if (lane == ends.size) ends += item.end else ends[lane] = item.end
            result[item.id] = lane
        }
    return result
}

class UndoHistory<T>(private val limit: Int = 100) {
    private val undo = ArrayDeque<Pair<T, T>>()
    private val redo = ArrayDeque<Pair<T, T>>()
    val canUndo
        get() = undo.isNotEmpty()

    val canRedo
        get() = redo.isNotEmpty()

    fun push(before: T, after: T) {
        if (before == after) return
        undo.addLast(before to after)
        while (undo.size > limit) undo.removeFirst()
        redo.clear()
    }

    fun undo(): T? = undo.removeLastOrNull()?.also { redo.addLast(it) }?.first

    fun redo(): T? = redo.removeLastOrNull()?.also { undo.addLast(it) }?.second
}
