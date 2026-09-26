package es.enylrad.nexusdm.sample

import es.enylrad.nexusdm.domain.model.AssetKind
import es.enylrad.nexusdm.domain.model.AssetRef
import es.enylrad.nexusdm.domain.model.Campaign
import es.enylrad.nexusdm.domain.model.CampaignId
import es.enylrad.nexusdm.domain.model.CampaignStatus
import es.enylrad.nexusdm.domain.model.EdgeId
import es.enylrad.nexusdm.domain.model.EdgeType
import es.enylrad.nexusdm.domain.model.ExternalRef
import es.enylrad.nexusdm.domain.model.ExternalSystem
import es.enylrad.nexusdm.domain.model.FoundryDocumentTypes
import es.enylrad.nexusdm.domain.model.GraphEdge
import es.enylrad.nexusdm.domain.model.GraphNode
import es.enylrad.nexusdm.domain.model.NodeId
import es.enylrad.nexusdm.domain.model.NodeType
import es.enylrad.nexusdm.domain.model.Visibility

/**
 * Built-in demo campaign used to try the AI features before real campaigns can be
 * imported and persisted.
 */
object SampleCampaign {

    private const val CREATED_AT = 1_758_000_000_000L

    val campaign = Campaign(
        id = CampaignId("sample_sunken_crown"),
        name = "The Sunken Crown",
        premise = "Forty years after the Black Tide drowned the old capital, a secret society of " +
            "dispossessed nobles hunts for the crown that could raise the sunken kingdom again.",
        setting = "Homebrew coastal kingdom",
        status = CampaignStatus.ACTIVE,
        createdAt = CREATED_AT,
        updatedAt = CREATED_AT,
    )

    val nodes: List<GraphNode> = listOf(
        node(
            "pc_aria", NodeType.PLAYER_CHARACTER, "Aria Windrunner",
            "Half-elf ranger searching for her missing brother along the coast.",
            properties = mapOf(
                "player_name" to "Player 1", "species" to "half-elf", "class" to "ranger",
                "level" to "5", "status" to "alive",
            ),
            visibility = Visibility.REVEALED,
            isLocked = true,
        ),
        node(
            "pc_borin", NodeType.PLAYER_CHARACTER, "Borin Deepstone",
            "Dwarf cleric of Nerissa who survived the Black Tide as a child.",
            properties = mapOf(
                "player_name" to "Player 2", "species" to "dwarf", "class" to "cleric",
                "level" to "5", "status" to "alive",
            ),
            visibility = Visibility.REVEALED,
            isLocked = true,
        ),
        node(
            "npc_varis", NodeType.NPC, "Lord Varis Hale",
            "Charming noble of Saltmere who secretly leads the Iron Circle.",
            description = "Varis lost his family lands in the Black Tide and blames the crown for abandoning " +
                "the coast. He funds the Iron Circle with smuggled relics and wants the Sunken Crown to " +
                "rule the risen kingdom himself.",
            properties = mapOf("species" to "human", "occupation" to "noble", "attitude" to "indifferent", "status" to "alive"),
            visibility = Visibility.REVEALED,
        ),
        node(
            "npc_mirela", NodeType.NPC, "Mirela Brine",
            "Innkeeper of the Gull's Rest who hears every rumor in Saltmere.",
            properties = mapOf("species" to "human", "occupation" to "innkeeper", "attitude" to "friendly", "status" to "alive"),
            visibility = Visibility.REVEALED,
        ),
        node(
            "faction_iron_circle", NodeType.FACTION, "The Iron Circle",
            "Secret society of dispossessed nobles plotting to raise the drowned kingdom.",
            properties = mapOf("faction_kind" to "secret society", "goal" to "Recover the Sunken Crown", "attitude" to "hostile"),
        ),
        node(
            "faction_tide_temple", NodeType.FACTION, "Temple of the Tide",
            "Clergy of Nerissa that guards the drowned crypts and their dead.",
            properties = mapOf("faction_kind" to "temple", "goal" to "Keep the dead of the Black Tide at rest", "attitude" to "friendly"),
            visibility = Visibility.REVEALED,
        ),
        node(
            "deity_nerissa", NodeType.DEITY, "Nerissa",
            "Goddess of the sea and safe passage, patron of Saltmere.",
            properties = mapOf("domains" to "Tempest, Life", "alignment" to "neutral good", "symbol" to "A silver wave"),
            visibility = Visibility.REVEALED,
        ),
        node(
            "loc_saltmere", NodeType.LOCATION, "Saltmere",
            "Fishing town built on the cliffs above the drowned capital.",
            properties = mapOf("location_kind" to "settlement", "population" to "About 3,000"),
            visibility = Visibility.REVEALED,
        ),
        node(
            "loc_drowned_crypt", NodeType.LOCATION, "The Drowned Crypt",
            "Royal crypt flooded by the Black Tide, reachable only at low tide.",
            properties = mapOf("location_kind" to "dungeon"),
        ),
        node(
            "map_drowned_crypt", NodeType.MAP, "Drowned Crypt - Level 1",
            "Battle map of the crypt entrance and the flooded ossuary.",
            properties = mapOf("map_kind" to "battle", "grid_size" to "5 ft", "revealed_to_players" to "false"),
            externalRefs = listOf(
                ExternalRef(ExternalSystem.FOUNDRY_VTT, FoundryDocumentTypes.SCENE, "Scene.dRwnCrypt01", "Drowned Crypt L1"),
            ),
            assets = listOf(AssetRef(AssetKind.MAP_IMAGE, "worlds/sunken-crown/maps/drowned-crypt-1.webp")),
        ),
        node(
            "item_sunken_crown", NodeType.ITEM, "The Sunken Crown",
            "Coral-encrusted crown of the last queen; whoever wears it commands the drowned.",
            properties = mapOf("item_type" to "wondrous item", "rarity" to "artifact", "requires_attunement" to "true"),
        ),
        node(
            "creature_drowned_one", NodeType.CREATURE, "Drowned One",
            "Waterlogged undead soldier of the old capital, bound to the crypt.",
            properties = mapOf("creature_type" to "undead", "challenge_rating" to "1/2", "size" to "medium", "is_unique" to "false"),
        ),
        node(
            "quest_sunken_crown", NodeType.QUEST, "Beneath the Tide",
            "Find the Sunken Crown before the Iron Circle does.",
            properties = mapOf("quest_kind" to "main", "status" to "active"),
            visibility = Visibility.REVEALED,
        ),
        node(
            "event_black_tide", NodeType.EVENT, "The Black Tide",
            "A cursed wave that drowned the old capital forty years ago.",
            properties = mapOf("in_world_date" to "Year 1102, Month of Storms", "outcome" to "The capital sank; the royal line vanished."),
            visibility = Visibility.REVEALED,
        ),
        node(
            "session_1", NodeType.SESSION, "Session 1: Arrival at Saltmere",
            "The party reached Saltmere, met Mirela and accepted the search for the crown.",
            properties = mapOf("session_number" to "1", "status" to "played", "played_on" to "2026-09-19"),
            visibility = Visibility.REVEALED,
        ),
        node(
            "lore_crown_curse", NodeType.LORE, "The Crown's Curse",
            "The Black Tide was summoned by the crown itself when the queen tried to use it.",
            properties = mapOf("lore_kind" to "secret", "is_true" to "true"),
        ),
    )

    val edges: List<GraphEdge> = listOf(
        edge("e_varis_leads", "npc_varis", "faction_iron_circle", EdgeType.LEADS),
        edge("e_varis_enemy_temple", "npc_varis", "faction_tide_temple", EdgeType.ENEMY_OF, "Varis blames the temple for sealing the crypts."),
        edge("e_varis_seeks_crown", "npc_varis", "item_sunken_crown", EdgeType.SEEKS),
        edge("e_varis_knows_curse", "npc_varis", "lore_crown_curse", EdgeType.KNOWS_ABOUT, "Believes he can control the curse."),
        edge("e_mirela_in_saltmere", "npc_mirela", "loc_saltmere", EdgeType.LOCATED_IN, visibility = Visibility.REVEALED),
        edge("e_mirela_quest", "npc_mirela", "quest_sunken_crown", EdgeType.GIVES_QUEST, visibility = Visibility.REVEALED),
        edge("e_aria_knows_mirela", "pc_aria", "npc_mirela", EdgeType.KNOWS, visibility = Visibility.REVEALED),
        edge("e_borin_worships", "pc_borin", "deity_nerissa", EdgeType.WORSHIPS, visibility = Visibility.REVEALED),
        edge("e_temple_worships", "faction_tide_temple", "deity_nerissa", EdgeType.WORSHIPS, visibility = Visibility.REVEALED),
        edge("e_temple_in_saltmere", "faction_tide_temple", "loc_saltmere", EdgeType.LOCATED_IN, visibility = Visibility.REVEALED),
        edge("e_crypt_in_saltmere", "loc_drowned_crypt", "loc_saltmere", EdgeType.LOCATED_IN),
        edge("e_map_depicts_crypt", "map_drowned_crypt", "loc_drowned_crypt", EdgeType.DEPICTS),
        edge("e_crown_in_crypt", "item_sunken_crown", "loc_drowned_crypt", EdgeType.LOCATED_IN),
        edge("e_drowned_in_crypt", "creature_drowned_one", "loc_drowned_crypt", EdgeType.LOCATED_IN),
        edge("e_quest_involves_crown", "quest_sunken_crown", "item_sunken_crown", EdgeType.INVOLVES, visibility = Visibility.REVEALED),
        edge("e_tide_at_saltmere", "event_black_tide", "loc_saltmere", EdgeType.OCCURRED_AT, visibility = Visibility.REVEALED),
        edge("e_curse_about_crown", "lore_crown_curse", "item_sunken_crown", EdgeType.ABOUT),
        edge("e_aria_session_1", "pc_aria", "session_1", EdgeType.FEATURED_IN, visibility = Visibility.REVEALED),
        edge("e_borin_session_1", "pc_borin", "session_1", EdgeType.FEATURED_IN, visibility = Visibility.REVEALED),
        edge("e_mirela_session_1", "npc_mirela", "session_1", EdgeType.FEATURED_IN, visibility = Visibility.REVEALED),
    )

    private fun node(
        id: String,
        type: NodeType,
        name: String,
        summary: String,
        description: String = "",
        properties: Map<String, String> = emptyMap(),
        visibility: Visibility = Visibility.DM_ONLY,
        isLocked: Boolean = false,
        externalRefs: List<ExternalRef> = emptyList(),
        assets: List<AssetRef> = emptyList(),
    ) = GraphNode(
        id = NodeId(id),
        campaignId = campaign.id,
        type = type,
        name = name,
        summary = summary,
        description = description,
        properties = properties,
        visibility = visibility,
        externalRefs = externalRefs,
        assets = assets,
        isLocked = isLocked,
        createdAt = CREATED_AT,
        updatedAt = CREATED_AT,
    )

    private fun edge(
        id: String,
        source: String,
        target: String,
        type: EdgeType,
        description: String = "",
        visibility: Visibility = Visibility.DM_ONLY,
    ) = GraphEdge(
        id = EdgeId(id),
        campaignId = campaign.id,
        sourceId = NodeId(source),
        targetId = NodeId(target),
        type = type,
        description = description,
        visibility = visibility,
        createdAt = CREATED_AT,
        updatedAt = CREATED_AT,
    )
}
