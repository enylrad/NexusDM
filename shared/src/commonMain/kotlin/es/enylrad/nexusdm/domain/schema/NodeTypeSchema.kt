package es.enylrad.nexusdm.domain.schema

import es.enylrad.nexusdm.domain.model.NodeType

/**
 * Registry of the well-known property keys for each [NodeType].
 *
 * The schema documents keys rather than forbidding others: nodes may carry extra
 * free-form properties, but known keys must respect their [PropertySpec].
 */
object NodeTypeSchema {

    private val alignments = listOf(
        "lawful good", "neutral good", "chaotic good",
        "lawful neutral", "true neutral", "chaotic neutral",
        "lawful evil", "neutral evil", "chaotic evil", "unaligned",
    )

    private val lifeStatuses = listOf("alive", "dead", "missing", "undead", "unknown")

    private val specs: Map<NodeType, List<PropertySpec>> = mapOf(
        NodeType.PLAYER_CHARACTER to listOf(
            text("player_name", "Real name of the player who controls the character."),
            text("species", "Species or race (e.g. elf, dwarf, tiefling)."),
            text("class", "Character class, or classes when multiclassed."),
            text("subclass", "Subclass or archetype."),
            integer("level", "Total character level."),
            text("background", "Character background."),
            enum("alignment", "Moral alignment.", alignments),
            enum("status", "Current status of the character.", listOf("alive", "dead", "missing", "retired")),
        ),
        NodeType.NPC to listOf(
            text("species", "Species or race."),
            text("occupation", "Job or social role (e.g. innkeeper, high priest)."),
            enum("alignment", "Moral alignment.", alignments),
            enum("attitude", "Attitude towards the party.", listOf("friendly", "indifferent", "hostile")),
            enum("status", "Current status.", lifeStatuses),
        ),
        NodeType.CREATURE to listOf(
            text("creature_type", "Creature type (e.g. dragon, undead, fiend)."),
            text("challenge_rating", "Challenge rating, e.g. \"1/4\" or \"17\"."),
            enum("size", "Creature size.", listOf("tiny", "small", "medium", "large", "huge", "gargantuan")),
            boolean("is_unique", "True for a named, one-of-a-kind creature; false for a bestiary entry."),
            enum("status", "Current status for unique creatures.", lifeStatuses),
        ),
        NodeType.FACTION to listOf(
            text("faction_kind", "Kind of organization (e.g. guild, cult, kingdom, mercenary company)."),
            text("goal", "What the faction ultimately wants."),
            enum("attitude", "Attitude towards the party.", listOf("friendly", "indifferent", "hostile")),
        ),
        NodeType.LOCATION to listOf(
            enum(
                "location_kind",
                "Scale of the place.",
                listOf("plane", "continent", "region", "settlement", "district", "building", "dungeon", "room", "landmark"),
            ),
            text("population", "Approximate population or inhabitants."),
        ),
        NodeType.MAP to listOf(
            enum("map_kind", "Kind of map.", listOf("world", "region", "settlement", "dungeon", "battle")),
            text("grid_size", "Grid cell size, e.g. \"5 ft\"."),
            boolean("revealed_to_players", "Whether the players have seen this map."),
        ),
        NodeType.EVENT to listOf(
            text("in_world_date", "Date in the setting's own calendar."),
            text("outcome", "How the event ended or what changed because of it."),
        ),
        NodeType.SESSION to listOf(
            integer("session_number", "Sequential number of the session.", required = true),
            enum("status", "Session status.", listOf("planned", "played", "cancelled"), required = true),
            date("played_on", "Real-world date the session was played."),
        ),
        NodeType.ENCOUNTER to listOf(
            enum("encounter_kind", "Kind of encounter.", listOf("combat", "social", "exploration", "trap", "puzzle")),
            enum("difficulty", "Expected difficulty.", listOf("trivial", "easy", "medium", "hard", "deadly")),
            enum("status", "Encounter status.", listOf("planned", "resolved", "skipped")),
        ),
        NodeType.QUEST to listOf(
            enum("quest_kind", "Weight of the quest in the story.", listOf("main", "side", "hook")),
            enum("status", "Quest status.", listOf("available", "active", "completed", "failed", "abandoned")),
        ),
        NodeType.ITEM to listOf(
            text("item_type", "Item category (e.g. weapon, armor, wondrous item, potion)."),
            enum("rarity", "Item rarity.", listOf("common", "uncommon", "rare", "very rare", "legendary", "artifact")),
            boolean("requires_attunement", "Whether the item requires attunement."),
        ),
        NodeType.DEITY to listOf(
            text("domains", "Divine domains, comma separated."),
            enum("alignment", "Moral alignment.", alignments),
            text("symbol", "Holy symbol."),
        ),
        NodeType.LORE to listOf(
            enum("lore_kind", "Kind of lore.", listOf("history", "legend", "rumor", "secret", "clue", "handout")),
            boolean("is_true", "False when the rumor or legend is misleading."),
        ),
    )

    /** Well-known properties for [type]. Every node type has an entry. */
    fun propertiesFor(type: NodeType): List<PropertySpec> = specs.getValue(type)

    /** Looks up the spec for [key] on [type], or null when the key is free-form. */
    fun specFor(type: NodeType, key: String): PropertySpec? = propertiesFor(type).firstOrNull { it.key == key }

    private fun text(key: String, description: String) =
        PropertySpec(key, PropertyValueType.TEXT, description)

    private fun integer(key: String, description: String, required: Boolean = false) =
        PropertySpec(key, PropertyValueType.INTEGER, description, required = required)

    private fun boolean(key: String, description: String) =
        PropertySpec(key, PropertyValueType.BOOLEAN, description)

    private fun date(key: String, description: String) =
        PropertySpec(key, PropertyValueType.DATE, description)

    private fun enum(key: String, description: String, values: List<String>, required: Boolean = false) =
        PropertySpec(key, PropertyValueType.ENUM, description, allowedValues = values, required = required)
}
