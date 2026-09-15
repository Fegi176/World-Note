package app.nodenote.core

import org.junit.Assert.*
import org.junit.Test

class ChronologyContractTest {
    private val w = World(name = "Chronology")

    private fun exact(year: Long) = TimeExpr(TimeKind.EXACT, year = year.toString())

    private fun period(
        start: TimeExpr = TimeExpr(),
        end: TimeExpr = TimeExpr(),
        parent: String? = null,
    ) =
        Record(
            world = w.id,
            kind = Kind.EPOCH,
            title = "Period",
            times = mapOf("start" to start, "end" to end),
            refs = mapOf("parent" to listOfNotNull(parent)),
        )

    @Test
    fun linkedBoundaryEventRecomputesWithoutCopyingDates() {
        val e =
            Record(
                world = w.id,
                kind = Kind.EVENT,
                title = "Boundary",
                times = mapOf("occurrence" to exact(-200)),
            )
        val p = period().withRef("opening event", e.id)
        assertEquals(-200L, Chronology(listOf(e, p)).resolve(p.id, "start").low)
        assertEquals(
            -150L,
            Chronology(listOf(e.copy(times = mapOf("occurrence" to exact(-150))), p))
                .resolve(p.id, "start")
                .low,
        )
        assertEquals(TimeKind.UNKNOWN, p.times.getValue("start").kind)
    }

    @Test
    fun parentContainmentRequiresExplicitDisputedMarker() {
        val p = period(exact(-10), exact(10))
        val c = period(exact(-15), exact(0), p.id)
        assertTrue(
            Chronology(listOf(p, c)).diagnostics().any { it.id == c.id && it.severity == "ERROR" }
        )
        assertTrue(
            Chronology(listOf(p, c.withField("association status", "disputed")))
                .diagnostics()
                .none { it.severity == "ERROR" }
        )
    }

    @Test
    fun emptyAndImpossibleSpansRejected() {
        assertTrue(
            Chronology(listOf(period(exact(5), exact(4)))).diagnostics().any {
                it.message.contains("after")
            }
        )
        assertTrue(
            Chronology(listOf(period(exact(5), exact(5)))).diagnostics().any {
                it.message.contains("Empty")
            }
        )
    }

    @Test
    fun crossRegionOverlapsAreValid() {
        val a = period(exact(-10), exact(10)).withField("track", "North")
        val b = period(exact(-5), exact(20)).withField("track", "South")
        assertTrue(Chronology(listOf(a, b)).diagnostics().isEmpty())
    }

    @Test
    fun unknownEpochAndLocalEventRemainUnresolved() {
        val p = period()
        val e =
            Record(
                world = w.id,
                kind = Kind.EVENT,
                title = "Local event",
                times =
                    mapOf(
                        "occurrence" to TimeExpr(TimeKind.LOCAL_YEAR, anchor = p.id, year = "412")
                    ),
            )
        assertEquals(ResolutionStatus.UNRESOLVED, Chronology(listOf(p, e)).resolve(e.id).status)
    }

    @Test
    fun orderOnlyIsBetweenAnchorsInPresentationButHasNoNumericYear() {
        val a = period(exact(-30000), exact(-10000))
        val lost = period()
        val z = period(exact(-9000), exact(0))
        fun order(x: Record, y: Record) =
            Record(
                world = w.id,
                kind = Kind.ORDER,
                title = "Order",
                refs = mapOf("before" to listOf(x.id), "after" to listOf(y.id)),
            )
        val engine = Chronology(listOf(a, lost, z, order(a, lost), order(lost, z)))
        assertEquals(listOf(a.id, lost.id, z.id), engine.ordered(listOf(z, a, lost)).map { it.id })
        val position = engine.resolve(lost.id, "start")
        assertNull(position.low)
        assertNull(position.estimate)
        assertEquals(ResolutionStatus.ORDER_ONLY, position.status)
    }

    @Test
    fun approximateOverlapDoesNotAssertContradiction() {
        val a =
            Record(
                world = w.id,
                kind = Kind.EVENT,
                title = "A",
                times = mapOf("occurrence" to TimeExpr(TimeKind.APPROXIMATE, year = "20")),
            )
        val b = a.copy(id = newId(), title = "B", times = mapOf("occurrence" to exact(10)))
        val order =
            Record(
                world = w.id,
                kind = Kind.ORDER,
                title = "After",
                refs = mapOf("before" to listOf(a.id), "after" to listOf(b.id)),
            )
        assertTrue(Chronology(listOf(a, b, order)).diagnostics().isEmpty())
    }

    @Test
    fun contradictoryKnownOrderIsDiagnosed() {
        val a =
            Record(
                world = w.id,
                kind = Kind.EVENT,
                title = "A",
                times = mapOf("occurrence" to exact(20)),
            )
        val b = a.copy(id = newId(), title = "B", times = mapOf("occurrence" to exact(10)))
        val order =
            Record(
                world = w.id,
                kind = Kind.ORDER,
                title = "After",
                refs = mapOf("before" to listOf(a.id), "after" to listOf(b.id)),
            )
        assertTrue(Chronology(listOf(a, b, order)).diagnostics().any { it.id == order.id })
    }

    @Test
    fun pointUncertaintyAndDurationAreDifferentOriginalModels() {
        val point =
            Record(
                world = w.id,
                kind = Kind.EVENT,
                title = "Uncertain",
                times =
                    mapOf(
                        "occurrence" to TimeExpr(TimeKind.RANGE, year = "-5200", upper = "-5000")
                    ),
            )
        val span =
            Record(
                world = w.id,
                kind = Kind.EVENT,
                title = "War",
                fields = mapOf("duration" to "true"),
                times = mapOf("start" to exact(-5200), "end" to exact(-5000)),
            )
        assertNull(point.times["start"])
        assertNull(span.times["occurrence"])
        assertNull(Chronology(listOf(point, span)).resolve(point.id).estimate)
    }

    @Test
    fun renamedOriginHasNoEffectOnHistory() {
        val p = period(exact(-100000), exact(0))
        assertEquals(
            Chronology(listOf(p), w).resolve(p.id, "start"),
            Chronology(listOf(p), w.copy(origin = "The New Name")).resolve(p.id, "start"),
        )
    }

    @Test
    fun localYearOverflowDoesNotWrap() {
        val p = period(exact(Long.MAX_VALUE - 1))
        val e = TimeExpr(TimeKind.LOCAL_YEAR, year = "100", anchor = p.id)
        assertEquals(ResolutionStatus.INVALID, Chronology(listOf(p)).expression(e).status)
    }

    @Test
    fun boundedOffsetNegativePropagation() {
        val p = period(TimeExpr(TimeKind.RANGE, year = "-10", upper = "10"))
        val e =
            TimeExpr(
                TimeKind.OFFSET,
                anchor = p.id,
                boundary = "start",
                minimum = "-20",
                maximum = "-5",
            )
        val result = Chronology(listOf(p)).expression(e)
        assertEquals(-30L, result.low)
        assertEquals(5L, result.high)
    }
}
