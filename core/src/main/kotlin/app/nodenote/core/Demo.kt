package app.nodenote.core

object Demo {
    fun create(): WorldBundle {
        val w =
            World(
                name = "The Ashen Meridian",
                description = "Sample world • Original editable demonstration lore",
            )
        val records = mutableListOf<Record>()
        fun add(
            kind: Kind,
            title: String,
            body: String = "",
            refs: Map<String, List<String>> = emptyMap(),
            fields: Map<String, String> = emptyMap(),
            times: Map<String, TimeExpr> = emptyMap(),
        ): Record =
            Record(
                    world = w.id,
                    kind = kind,
                    title = title,
                    body = body,
                    refs = refs,
                    fields = fields,
                    times = times,
                )
                .also { records += it }
        fun exact(y: Long) = TimeExpr(TimeKind.EXACT, year = y.toString())
        val first =
            add(
                Kind.EPOCH,
                "The First Radiance",
                "For almost ninety millennia, glass towers gathered light above a living sea. Their keepers believed nothing precious should ever be forgotten.",
                fields = mapOf("track" to "Global"),
                times =
                    mapOf(
                        "start" to TimeExpr(TimeKind.APPROXIMATE, year = "-120000"),
                        "end" to exact(-30000),
                    ),
            )
        val crowns =
            add(
                Kind.EPOCH,
                "The Hollow Crowns",
                "Two institutions inherited an empire and disagreed about what it had been.",
                fields = mapOf("track" to "Global"),
                times = mapOf("start" to exact(-30000), "end" to exact(-10000)),
            )
        val lost =
            add(
                Kind.EPOCH,
                "The Lost Interval",
                "Its name survives only in a list of forbidden measurements. Neither boundary has been decided by the author.",
                fields = mapOf("track" to "Global"),
            )
        val ash =
            add(
                Kind.EPOCH,
                "The Ash Dominion",
                "Oaths and furnaces held scattered settlements together.",
                fields = mapOf("track" to "Global"),
                times = mapOf("start" to exact(-9000), "end" to exact(-200)),
            )
        val lantern =
            add(
                Kind.EPOCH,
                "The Lantern Age",
                "Relief caravans carry medicine, rumors, and fragments of the old archives.",
                fields = mapOf("track" to "Global"),
                times = mapOf("start" to exact(-200)),
            )
        add(
            Kind.ORDER,
            "Crowns before Lost Interval",
            refs = mapOf("before" to listOf(crowns.id), "after" to listOf(lost.id)),
            fields = mapOf("before boundary" to "end", "after boundary" to "start"),
        )
        add(
            Kind.ORDER,
            "Lost Interval before Dominion",
            refs = mapOf("before" to listOf(lost.id), "after" to listOf(ash.id)),
            fields = mapOf("before boundary" to "end", "after boundary" to "start"),
        )
        val epochs = listOf(first, crowns, lost, ash, lantern)
        repeat(6) { i ->
            val parent = if (i < 2) crowns else if (i < 4) ash else lantern
            add(
                Kind.ERA,
                listOf(
                    "The Salt Succession",
                    "The Silent Senate",
                    "The Iron Concord",
                    "The Furnace Schism",
                    "The Lantern Compact",
                    "The Open Road",
                )[i],
                "An era within ${parent.title}. Boundaries remain open to research.",
                refs = mapOf("parent" to listOf(parent.id)),
                fields = mapOf("track" to if (i == 5) "Eastern coast" else "Global"),
            )
        }
        val physician =
            add(
                Kind.CHARACTER,
                "The Ash Physician",
                "# A physician with divided loyalties\n\nShe purchases relief supplies with money earned by dismantling ancient engines. Her quiet generosity keeps people alive, but her silence protects the cause of their suffering.\n\n## An unresolved choice\n\nWill she publish the records if doing so destroys the relief network?",
                fields = mapOf("aliases" to "Mara of the Kilns", "tags" to "medicine, archives"),
            )
        val order =
            add(
                Kind.FACTION,
                "The Lantern Order",
                "A public relief institution with private archives beneath its travelling infirmaries.",
            )
        val observatory =
            add(
                Kind.PLACE,
                "The Buried Observatory",
                "A half-excavated ring of black glass. Residents use its foundations as a marketplace.",
            )
        val shattering =
            add(
                Kind.EVENT,
                "The Shattering",
                "A deliberate dismantling was remembered as a natural catastrophe.",
                refs = mapOf("periods" to listOf(crowns.id)),
                times =
                    mapOf(
                        "occurrence" to TimeExpr(TimeKind.RANGE, year = "-5000", upper = "-4900")
                    ),
            )
        val plague =
            add(
                Kind.EVENT,
                "The Glass Plague",
                "Crystalline scars appeared in towns that used recovered mirror wells.",
                refs = mapOf("periods" to listOf(first.id), "participants" to listOf(physician.id)),
                times =
                    mapOf(
                        "occurrence" to
                            TimeExpr(TimeKind.APPROXIMATE, year = "-100000", tolerance = "400")
                    ),
            )
        val artifact =
            add(
                Kind.ARTIFACT,
                "The Mercy Lens",
                "An optical instrument that transfers pain into a living witness.",
            )
        val curse =
            add(
                Kind.NOTE,
                "The Inherited Echo",
                "Children hear memories that no surviving person could possess.",
            )
        val scheme =
            add(
                Kind.SCHEME,
                "The Quiet Cure",
                "Redirect dangerous relics away from public wells without exposing the archive.",
                refs = mapOf("planner" to listOf(physician.id)),
                fields =
                    mapOf(
                        "Objective" to "End the recurring sickness",
                        "Public cover story" to "Replace worn municipal equipment",
                    ),
            )
        add(
            Kind.STAGE,
            "Exchange the lenses",
            refs = mapOf("scheme" to listOf(scheme.id), "actual events" to listOf(plague.id)),
            fields =
                mapOf(
                    "order" to "1",
                    "state" to "changed",
                    "planned outcome" to "Relics disappear unnoticed",
                    "actual outcome" to "A child carries one lens to the market",
                ),
        )
        val mystery =
            add(
                Kind.MYSTERY,
                "Why do the wells sing?",
                "The observation is public; its explanation remains contested.",
            )
        val clue =
            add(
                Kind.CLUE,
                "A tune under glass",
                "Every recovered lens resonates with a slightly different voice.",
                refs = mapOf("mysteries" to listOf(mystery.id)),
                fields =
                    mapOf(
                        "Apparent interpretation" to "An old prayer",
                        "Actual meaning" to "Stored witness memories",
                    ),
            )
        val truth =
            add(
                Kind.CLAIM,
                "The wells preserve witnesses",
                "The old experiment stored living memories in the well lenses.",
                refs =
                    mapOf(
                        "subjects" to listOf(plague.id, artifact.id),
                        "supports" to listOf(clue.id),
                    ),
                fields = mapOf("truth" to "CONFIRMED", "role" to "SECRET"),
            )
        val lie =
            add(
                Kind.CLAIM,
                "The sickness is a judgment",
                "The plague is punishment for reopening the towers.",
                refs = mapOf("subjects" to listOf(plague.id)),
                fields = mapOf("truth" to "FALSE_IN_CANON", "role" to "BELIEF"),
            )
        add(
            Kind.CLAIM,
            "Can a witness be released?",
            "Author question: decide whether extraction preserves a whole person.",
            fields = mapOf("truth" to "UNDECIDED", "role" to "FACT"),
        )
        add(
            Kind.KNOWLEDGE,
            "Physician knows the mechanism",
            refs = mapOf("holder" to listOf(physician.id), "claim" to listOf(truth.id)),
            fields = mapOf("state" to "Knows"),
        )
        add(
            Kind.KNOWLEDGE,
            "Order suspects divine judgment",
            refs = mapOf("holder" to listOf(order.id), "claim" to listOf(lie.id)),
            fields = mapOf("state" to "Believes"),
        )
        listOf(first, crowns, lantern).forEachIndexed { i, p ->
            add(
                Kind.STATE,
                listOf("The Bright Sanctuary", "The Occupied Fortress", "The Excavation Market")[i],
                listOf(
                    "A temple-city devoted to a living light.",
                    "The senate occupied the towers and sealed the lower rooms.",
                    "Market stalls stand above the buried instruments.",
                )[i],
                refs = mapOf("entry" to listOf(observatory.id), "periods" to listOf(p.id)),
                fields =
                    mapOf(
                        "historical aliases" to
                            listOf("Bright Sanctuary", "Glass Bastion", "The Ring")[i]
                    ),
            )
        }
        fun relationship(a: Record, b: Record, title: String, note: String): Record =
            add(
                Kind.RELATIONSHIP,
                title,
                note,
                refs = mapOf("source" to listOf(a.id), "target" to listOf(b.id)),
                fields = mapOf("direction" to "Directed", "type" to title),
            )
        val funds =
            relationship(
                physician,
                order,
                "secretly funds",
                "She pays the caravan masters through an intermediary because public funding would expose her work.",
            )
        relationship(
            physician,
            order,
            "opposes",
            "She rejects the Order's decision to seal the public archive.",
        )
        relationship(
            first,
            artifact,
            "survives as",
            "The lens retains a fragment of the age's witness experiments.",
        )
        relationship(artifact, curse, "created", "Repeated use leaves an echo in descendants.")
        relationship(
            curse,
            order,
            "shaped",
            "The Order developed its rituals around patients who heard inherited voices.",
        )
        val story =
            add(
                Kind.STORY,
                "The Wells Beneath",
                fields = mapOf("level" to "Storyline", "order" to "0"),
            )
        listOf(1, 2, 7).forEach { n ->
            val chapter =
                add(
                    Kind.STORY,
                    "Chapter $n",
                    refs = mapOf("parent" to listOf(story.id)),
                    fields = mapOf("level" to "Chapter", "order" to n.toString()),
                )
            add(
                Kind.REVEAL,
                when (n) {
                    1 -> "The strange tune"
                    2 -> "The official account"
                    else -> "The stored witnesses"
                },
                refs =
                    mapOf(
                        "story" to listOf(chapter.id),
                        "target" to listOf(plague.id, if (n == 7) truth.id else clue.id),
                    ),
                fields =
                    mapOf(
                        "information layer" to if (n == 7) "Author truth" else "Public explanation",
                        "audience" to "Reader",
                    ),
            )
        }
        val account =
            add(
                Kind.ACCOUNT,
                "The Caravan Almanac",
                refs = mapOf("owner" to listOf(order.id)),
                fields = mapOf("completeness" to "Documented public account"),
            )
        epochs
            .filter { it != lost }
            .forEach { p ->
                add(
                    Kind.ASSERTION,
                    "Almanac: ${p.title}",
                    refs = mapOf("account" to listOf(account.id), "subject" to listOf(p.id)),
                    fields =
                        mapOf(
                            "include" to "true",
                            "public title" to p.title,
                            "public summary" to
                                "The almanac records ${p.title.lowercase()} as an age of changing institutions.",
                        ),
                )
            }
        repeat(3) { i ->
            val board =
                add(
                    Kind.BOARD,
                    listOf("The living world", "Ancient causes", "The quiet conspiracy")[i],
                )
            val a =
                add(
                    Kind.PLACEMENT,
                    physician.title,
                    refs = mapOf("board" to listOf(board.id), "entry" to listOf(physician.id)),
                    fields =
                        mapOf(
                            "x" to "30",
                            "y" to "60",
                            "width" to "190",
                            "height" to "112",
                            "color" to "#b9a1ff",
                        ),
                )
            val b =
                add(
                    Kind.PLACEMENT,
                    order.title,
                    refs = mapOf("board" to listOf(board.id), "entry" to listOf(order.id)),
                    fields =
                        mapOf(
                            "x" to "260",
                            "y" to "240",
                            "width" to "190",
                            "height" to "112",
                            "color" to "#8bd5b2",
                        ),
                )
            add(
                Kind.EDGE,
                funds.title,
                refs =
                    mapOf(
                        "board" to listOf(board.id),
                        "from" to listOf(a.id),
                        "to" to listOf(b.id),
                        "relationship" to listOf(funds.id),
                    ),
            )
        }
        repeat(18) { i ->
            add(
                Kind.EVENT,
                "Meridian chronicle ${i+1}",
                "An original planning prompt: a migration, discovery, negotiation, or consequence to develop.",
                refs = mapOf("periods" to listOf(epochs[i % epochs.size].id)),
                times =
                    mapOf(
                        "occurrence" to
                            if (i % 3 == 0)
                                TimeExpr(
                                    TimeKind.OFFSET,
                                    anchor = shattering.id,
                                    minimum = (i * 200).toString(),
                                    maximum = (i * 200).toString(),
                                )
                            else TimeExpr()
                    ),
            )
        }
        add(
            Kind.EVENT,
            "Year 412 of the Hollow Crowns",
            times =
                mapOf(
                    "occurrence" to TimeExpr(TimeKind.LOCAL_YEAR, year = "412", anchor = crowns.id)
                ),
        )
        return WorldBundle(w, records + templates(w.id))
    }

    fun templates(world: String): List<Record> =
        Schema.templates.flatMap { (kind, prompts) ->
            val template =
                Record(
                    world = world,
                    kind = Kind.TEMPLATE,
                    title = "${kind.label} template",
                    fields = mapOf("entry type" to kind.name),
                )
            listOf(template) +
                prompts.split('|').mapIndexed { i, p ->
                    Record(
                        world = world,
                        kind = Kind.FIELD,
                        title = p,
                        fields = mapOf("field type" to "Markdown", "order" to i.toString()),
                        refs = mapOf("owner" to listOf(template.id)),
                    )
                }
        } +
            Schema.relations.map {
                Record(
                    world = world,
                    kind = Kind.RELATIONSHIP_TYPE,
                    title = it,
                    fields = mapOf("default direction" to "Directed"),
                )
            }
}
