package es.enylrad.nexusdm.domain.model

import kotlinx.serialization.Serializable

/**
 * Kind of entity a [GraphNode] represents. The set of expected properties for each
 * type is described by [es.enylrad.nexusdm.domain.schema.NodeTypeSchema].
 */
@Serializable
enum class NodeType(val description: String) {
    PLAYER_CHARACTER("A character controlled by one of the players."),
    NPC("A named non-player character: ally, villain, merchant, patron, etc."),
    CREATURE("A monster or enemy, either a unique creature or a bestiary entry."),
    FACTION("An organization: guild, cult, kingdom, army, noble house, etc."),
    LOCATION("A place, from planes and regions down to buildings and rooms. Nested via LOCATED_IN."),
    MAP("A world, region, settlement, dungeon or battle map (usually a Foundry Scene)."),
    EVENT("Something that happened or will happen, either in the world history or during play."),
    SESSION("A play session, with its number, status, date and recap."),
    ENCOUNTER("A combat, social, exploration, trap or puzzle encounter."),
    QUEST("A main story arc, side quest or plot hook."),
    ITEM("A magic item, artifact or story-relevant object."),
    DEITY("A god, patron or other powerful entity."),
    LORE("History, legends, rumors, secrets, clues or handouts."),
}
