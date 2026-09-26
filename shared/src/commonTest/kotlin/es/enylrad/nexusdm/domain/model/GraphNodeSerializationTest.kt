package es.enylrad.nexusdm.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class GraphNodeSerializationTest {

    @Test
    fun nodeWithFoundryLinksRoundTrips() {
        val node = GraphNode(
            id = NodeId("map_sunken_keep"),
            campaignId = CampaignId("campaign_1"),
            type = NodeType.MAP,
            name = "Sunken Keep - Level 1",
            summary = "Battle map of the flooded keep's ground floor.",
            properties = mapOf("map_kind" to "battle", "grid_size" to "5 ft"),
            externalRefs = listOf(
                ExternalRef(
                    system = ExternalSystem.FOUNDRY_VTT,
                    documentType = FoundryDocumentTypes.SCENE,
                    uuid = "Scene.k3Ep9aZ1",
                    label = "Sunken Keep L1",
                ),
            ),
            assets = listOf(AssetRef(AssetKind.MAP_IMAGE, "worlds/campaign/maps/sunken-keep-1.webp")),
            createdAt = 1_700_000_000_000,
            updatedAt = 1_700_000_000_000,
        )

        val decoded = Json.decodeFromString(GraphNode.serializer(), Json.encodeToString(GraphNode.serializer(), node))

        assertEquals(node, decoded)
    }

    @Test
    fun edgeTypeConstraints() {
        assertTrue(EdgeType.MEMBER_OF.accepts(NodeType.NPC, NodeType.FACTION))
        assertFalse(EdgeType.MEMBER_OF.accepts(NodeType.FACTION, NodeType.NPC))
        // Symmetric edges accept both directions.
        assertTrue(EdgeType.ALLY_OF.accepts(NodeType.FACTION, NodeType.PLAYER_CHARACTER))
        assertTrue(EdgeType.DEPICTS.accepts(NodeType.MAP, NodeType.LOCATION))
        assertFalse(EdgeType.DEPICTS.accepts(NodeType.LOCATION, NodeType.MAP))
        // Unconstrained types accept anything.
        assertTrue(EdgeType.RELATED_TO.accepts(NodeType.ITEM, NodeType.SESSION))
    }
}
