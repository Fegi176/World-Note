package app.nodenote.core

import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val codec = Json {
    encodeDefaults = true
    ignoreUnknownKeys = false
    explicitNulls = false
}

fun newId(): String = UUID.randomUUID().toString()

@Serializable
enum class Kind {
    NOTE,
    CHARACTER,
    FACTION,
    PLACE,
    CREATURE,
    DEITY,
    RELIGION,
    POWER,
    ARTIFACT,
    EVENT,
    EPOCH,
    ERA,
    SCHEME,
    MYSTERY,
    CLUE,
    ARC,
    RELATIONSHIP,
    BOARD,
    PLACEMENT,
    EDGE,
    GROUP,
    SECTION,
    FIELD,
    TEMPLATE,
    STATE,
    ASSOCIATION,
    STORY,
    REVEAL,
    CLAIM,
    KNOWLEDGE,
    ACCOUNT,
    ASSERTION,
    STAGE,
    ORDER,
    ATTACHMENT,
    MEDIA,
    REVISION,
    DRAFT,
    IMPORT,
    FILTER,
    RELATIONSHIP_TYPE,
    SETTINGS;

    val lore: Boolean
        get() = ordinal <= ARC.ordinal

    val period: Boolean
        get() = this == EPOCH || this == ERA

    val label: String
        get() = name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
}

@Serializable
data class World(
    val id: String = newId(),
    val name: String,
    val description: String = "",
    val archived: Boolean = false,
    val origin: String = "Present",
    val present: String = "0",
    val epochLabel: String = "Epoch",
    val eraLabel: String = "Era",
    val created: Long = System.currentTimeMillis(),
)

/**
 * Registry identity and scalar authored columns. Each field/reference/expression is normalized by
 * the Room adapter.
 */
@Serializable
data class Record(
    val id: String = newId(),
    val world: String,
    val kind: Kind,
    val title: String,
    val summary: String = "",
    val body: String = "",
    val canon: String = "UNDECIDED",
    val writing: String = "IDEA",
    val favorite: Boolean = false,
    val inbox: Boolean = false,
    val locked: Boolean = false,
    val trashed: Boolean = false,
    val revision: Long = 0,
    val created: Long = System.currentTimeMillis(),
    val updated: Long = created,
    val fields: Map<String, String> = emptyMap(),
    val refs: Map<String, List<String>> = emptyMap(),
    val times: Map<String, TimeExpr> = emptyMap(),
) {
    fun f(key: String, default: String = "") = fields[key] ?: default

    fun ref(key: String) = refs[key]?.firstOrNull()

    fun has(key: String, id: String) = refs[key]?.contains(id) == true

    fun n(key: String, default: Double = 0.0) = fields[key]?.toDoubleOrNull() ?: default

    fun withField(key: String, value: String) = copy(fields = fields + (key to value))

    fun withRef(key: String, value: String?) = copy(refs = refs + (key to listOfNotNull(value)))
}

@Serializable data class WorldBundle(val world: World, val records: List<Record>)

@Serializable
data class Workspace(
    val worlds: List<WorldBundle>,
    val preferences: Map<String, String> = emptyMap(),
)

object Schema {
    val canon = listOf("UNDECIDED", "CANON", "NON_CANON", "ALTERNATIVE")
    val writing = listOf("IDEA", "DRAFT", "NEEDS_DEVELOPMENT", "REVIEWED", "COMPLETE")
    val truth = listOf("CONFIRMED", "FALSE_IN_CANON", "UNDECIDED", "ALTERNATIVE")
    val roles = listOf("FACT", "BELIEF", "RUMOR", "DELIBERATE_LIE", "SECRET")
    val knowledge =
        listOf("Knows", "Believes", "Suspects", "Rejects", "Has not encountered", "Unspecified")
    val relations =
        listOf(
            "allies with",
            "opposes",
            "belongs to",
            "leads",
            "founded",
            "created",
            "worships",
            "secretly funds",
            "manipulates",
            "contains",
            "originates from",
            "caused",
            "inherited from",
            "survives as",
        )
    val templates =
        mapOf(
            Kind.NOTE to "Open questions",
            Kind.CHARACTER to
                "Appearance|Personality|Desires|Beliefs|Fears|History|Abilities|Limitations|Secrets|What do they want?|What belief justifies their actions?|What are they right about?|Who benefits from their actions?|What line would they never cross?|What could make them cross it?|What would a convincing defeat or change of heart require?",
            Kind.FACTION to
                "Public mission|Hidden agenda|Ideology|Leadership|Resources|Internal conflicts|Rivals",
            Kind.PLACE to "Atmosphere|Geography|Environment|Inhabitants|Laws|Dangers|History",
            Kind.CREATURE to "Appearance|Behavior|Origin|Abilities|Weaknesses|Ecological role",
            Kind.DEITY to "Domains|Identity|Worship|Doctrine|Taboos|Miracles|Concealed truth",
            Kind.RELIGION to "Beliefs|Institutions|Rituals|Competing interpretations|Schisms",
            Kind.POWER to "Rules|Ranks|Progression|Costs|Limits|Exceptions|Consequences",
            Kind.ARTIFACT to
                "Origin|Function|Activation|Price of use|Owners|Historical significance",
            Kind.EVENT to "Causes|Actual outcome|Consequences|Accounts",
            Kind.EPOCH to
                "Defining character|Geography and environment|Civilizations and ordinary life|Power and belief|Major developments|Legacy / What remains?|Historical knowledge|Author planning",
            Kind.ERA to "Defining changes|Institutions|Major events",
            Kind.SCHEME to
                "Objective|Motive|Public cover story|Resources|Assumptions|Contingencies|Failure points|Consequences",
            Kind.MYSTERY to
                "Central question|Author answer|Competing hypotheses|Evidence|Misdirection|Witnesses|Reveal plan",
            Kind.CLUE to "Observation|Source|Apparent interpretation|Actual meaning|Discovery",
            Kind.ARC to
                "Starting belief|Desire|Pressures|Meaningful decisions|Turning points|Changing loyalties|Ending belief",
        )

    fun textFields(kind: Kind): List<String> =
        when (kind) {
            Kind.EPOCH,
            Kind.ERA -> listOf("track", "association status")
            Kind.EVENT -> listOf("track", "event type", "sequence")
            Kind.RELATIONSHIP -> listOf("type", "motivation", "public explanation")
            Kind.STATE -> listOf("historical aliases", "field overrides", "source")
            Kind.STORY -> listOf("level", "order")
            Kind.REVEAL -> listOf("information layer", "audience", "order")
            Kind.CLAIM -> listOf("source")
            Kind.KNOWLEDGE -> listOf("evidence")
            Kind.ACCOUNT -> listOf("completeness")
            Kind.ASSERTION -> listOf("public title", "public summary", "status", "evidence")
            Kind.STAGE ->
                listOf("order", "planned outcome", "actual outcome", "dependencies", "resources")
            Kind.SECTION -> listOf("order")
            Kind.FIELD -> listOf("order", "choices", "value")
            Kind.MEDIA -> listOf("caption", "source URL", "credit")
            Kind.TEMPLATE -> listOf("entry type")
            Kind.RELATIONSHIP_TYPE -> listOf("default direction")
            Kind.IMPORT -> listOf("source hash", "warnings")
            else -> emptyList()
        }

    /**
     * Reference roles have explicit target kinds. Unlisted optional links target lore by default.
     */
    fun roles(kind: Kind): Map<String, Set<Kind>> {
        val lore = Kind.entries.filter { it.lore }.toSet()
        val period = setOf(Kind.EPOCH, Kind.ERA)
        val anyContent = lore + setOf(Kind.CLAIM, Kind.SECTION, Kind.RELATIONSHIP)
        return when (kind) {
            Kind.EPOCH,
            Kind.ERA ->
                mapOf(
                    "parent" to period,
                    "opening event" to setOf(Kind.EVENT),
                    "closing event" to setOf(Kind.EVENT),
                )
            Kind.EVENT ->
                mapOf("periods" to period, "participants" to lore, "locations" to setOf(Kind.PLACE))
            Kind.SCHEME ->
                mapOf("planner" to lore, "manipulated" to lore, "claims" to setOf(Kind.CLAIM))
            Kind.MYSTERY ->
                mapOf(
                    "clues" to setOf(Kind.CLUE),
                    "witnesses" to lore,
                    "hypotheses" to setOf(Kind.CLAIM),
                )
            Kind.CLUE ->
                mapOf(
                    "mysteries" to setOf(Kind.MYSTERY),
                    "supports" to setOf(Kind.CLAIM),
                    "refutes" to setOf(Kind.CLAIM),
                    "discovery" to setOf(Kind.EVENT, Kind.STORY),
                )
            Kind.ARC ->
                mapOf(
                    "character" to setOf(Kind.CHARACTER),
                    "turning points" to setOf(Kind.EVENT, Kind.STORY),
                )
            Kind.RELATIONSHIP ->
                mapOf(
                    "source" to lore,
                    "target" to lore,
                    "periods" to period,
                    "claims" to setOf(Kind.CLAIM),
                    "relationship type" to setOf(Kind.RELATIONSHIP_TYPE),
                )
            Kind.BOARD -> mapOf("focus" to lore)
            Kind.PLACEMENT ->
                mapOf("board" to setOf(Kind.BOARD), "entry" to lore, "group" to setOf(Kind.GROUP))
            Kind.EDGE ->
                mapOf(
                    "board" to setOf(Kind.BOARD),
                    "from" to setOf(Kind.PLACEMENT),
                    "to" to setOf(Kind.PLACEMENT),
                    "relationship" to setOf(Kind.RELATIONSHIP),
                )
            Kind.GROUP -> mapOf("board" to setOf(Kind.BOARD), "parent" to setOf(Kind.GROUP))
            Kind.SECTION,
            Kind.FIELD -> mapOf("owner" to (anyContent + Kind.TEMPLATE), "values" to lore)
            Kind.STATE ->
                mapOf(
                    "entry" to lore,
                    "periods" to period,
                    "relationships" to setOf(Kind.RELATIONSHIP),
                    "image" to setOf(Kind.ATTACHMENT),
                )
            Kind.ASSOCIATION -> mapOf("entry" to lore, "periods" to period)
            Kind.STORY -> mapOf("parent" to setOf(Kind.STORY))
            Kind.REVEAL -> mapOf("story" to setOf(Kind.STORY), "target" to anyContent)
            Kind.CLAIM ->
                mapOf(
                    "subjects" to anyContent,
                    "supports" to setOf(Kind.CLUE),
                    "refutes" to setOf(Kind.CLUE),
                )
            Kind.KNOWLEDGE ->
                mapOf(
                    "holder" to lore,
                    "claim" to setOf(Kind.CLAIM),
                    "learned at" to setOf(Kind.EVENT, Kind.STORY),
                    "until" to setOf(Kind.EVENT, Kind.STORY),
                )
            Kind.ACCOUNT -> mapOf("owner" to lore)
            Kind.ASSERTION ->
                mapOf(
                    "account" to setOf(Kind.ACCOUNT),
                    "subject" to lore,
                    "evidence" to lore,
                    "shared sections" to setOf(Kind.SECTION),
                )
            Kind.STAGE ->
                mapOf(
                    "scheme" to setOf(Kind.SCHEME),
                    "required events" to setOf(Kind.EVENT),
                    "participants" to lore,
                    "claims" to setOf(Kind.CLAIM),
                    "actual events" to setOf(Kind.EVENT),
                )
            Kind.ORDER -> mapOf("before" to (period + Kind.EVENT), "after" to (period + Kind.EVENT))
            Kind.MEDIA ->
                mapOf(
                    "owner" to (lore + setOf(Kind.STATE, Kind.SECTION)),
                    "attachment" to setOf(Kind.ATTACHMENT),
                )
            Kind.REVISION,
            Kind.DRAFT ->
                mapOf(
                    "owner" to
                        (anyContent +
                            setOf(
                                Kind.STATE,
                                Kind.KNOWLEDGE,
                                Kind.ASSERTION,
                                Kind.STAGE,
                                Kind.REVEAL,
                                Kind.FIELD,
                            ))
                )
            else -> emptyMap()
        }
    }

    fun timeSlots(kind: Kind) =
        when {
            kind.period ||
                kind in setOf(Kind.RELATIONSHIP, Kind.STATE, Kind.CLAIM, Kind.ASSERTION) ->
                listOf("start", "end")
            kind == Kind.EVENT -> listOf("occurrence", "start", "end")
            kind == Kind.FIELD -> listOf("value")
            else -> emptyList()
        }

    fun isMultiple(kind: Kind, role: String): Boolean =
        role in
            setOf(
                "periods",
                "participants",
                "locations",
                "manipulated",
                "claims",
                "clues",
                "witnesses",
                "hypotheses",
                "mysteries",
                "supports",
                "refutes",
                "turning points",
                "values",
                "relationships",
                "subjects",
                "evidence",
                "required events",
                "actual events",
                "shared sections",
            ) || kind == Kind.REVEAL && role == "target"
}
