package app.nodenote.core

import java.io.File
import java.util.UUID
import kotlinx.serialization.encodeToString

object Fixtures {
    fun world(count: Int = 2000, relationships: Int = 5000, placements: Int = 400): WorldBundle {
        fun id(label: String) = UUID.nameUUIDFromBytes(label.toByteArray()).toString()
        val world =
            World(
                id = id("fixture-world"),
                name = "Deterministic $count-entry fixture",
                created = 0,
            )
        val lore =
            (0 until count).map { i ->
                Record(
                    id = id("lore-$i"),
                    world = world.id,
                    kind = if (i % 4 == 0) Kind.EVENT else Kind.CHARACTER,
                    title = "Synthetic record $i",
                    body =
                        "Deterministic multilingual prose: Łódź 王朝 日本語 😀. "
                            .repeat(if (i % 100 == 0) 2000 else 2),
                    created = 0,
                    updated = 0,
                    times =
                        if (i % 4 == 0)
                            mapOf(
                                "occurrence" to
                                    TimeExpr(
                                        TimeKind.EXACT,
                                        year = (-1_000_000_000L + i * 1000).toString(),
                                    )
                            )
                        else emptyMap(),
                )
            }
        val rels =
            (0 until relationships).map { i ->
                Record(
                    id = id("relationship-$i"),
                    world = world.id,
                    kind = Kind.RELATIONSHIP,
                    title = "Relation $i",
                    body = "Unique search marker relation-$i",
                    refs =
                        mapOf(
                            "source" to listOf(lore[i % count].id),
                            "target" to listOf(lore[(i * 7 + 1) % count].id),
                        ),
                    created = 0,
                    updated = 0,
                )
            }
        val board =
            Record(
                id = id("fixture-board"),
                world = world.id,
                kind = Kind.BOARD,
                title = "Stress board",
                created = 0,
                updated = 0,
            )
        val cards =
            (0 until placements).map { i ->
                Record(
                    id = id("placement-$i"),
                    world = world.id,
                    kind = Kind.PLACEMENT,
                    title = lore[i % count].title,
                    refs =
                        mapOf("board" to listOf(board.id), "entry" to listOf(lore[i % count].id)),
                    fields =
                        mapOf(
                            "x" to ((i % 40) * 230).toString(),
                            "y" to ((i / 40) * 150).toString(),
                            "width" to "190",
                            "height" to "112",
                        ),
                    created = 0,
                    updated = 0,
                )
            }
        val additionalBoards =
            (1 until 30).map { i ->
                board.copy(id = id("fixture-board-$i"), title = "Regional board $i")
            }
        val additionalCards = additionalBoards.flatMapIndexed { b, other ->
            (0 until 10).map { i ->
                cards[i % cards.size].copy(
                    id = id("regional-placement-$b-$i"),
                    refs =
                        mapOf(
                            "board" to listOf(other.id),
                            "entry" to listOf(lore[(b * 10 + i) % count].id),
                        ),
                )
            }
        }
        return WorldBundle(world, lore + rels + board + cards + additionalBoards + additionalCards)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val stress = args.firstOrNull() == "stress"
        val bundle =
            world(
                if (stress) 10000 else 2000,
                if (stress) 25000 else 5000,
                if (stress) 2000 else 400,
            )
        val out = File(args.getOrElse(1) { "fixture.json" })
        out.parentFile?.mkdirs()
        out.writeText(codec.encodeToString(bundle))
        println("Generated ${bundle.records.size} records at ${out.absolutePath}")
    }
}
