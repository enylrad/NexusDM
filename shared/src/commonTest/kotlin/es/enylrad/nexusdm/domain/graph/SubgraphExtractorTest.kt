package es.enylrad.nexusdm.domain.graph

import es.enylrad.nexusdm.domain.model.CampaignId
import es.enylrad.nexusdm.domain.model.EdgeType
import es.enylrad.nexusdm.domain.model.NodeId
import es.enylrad.nexusdm.domain.model.NodeType
import es.enylrad.nexusdm.sample.SampleCampaign
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SubgraphExtractorTest {

    private val campaignId = SampleCampaign.campaign.id
    private val varis = NodeId("npc_varis")

    private fun extract(query: SubgraphQuery) = extractSubgraph(SampleCampaign.nodes, SampleCampaign.edges, query)

    private fun Subgraph.ids() = nodes.map { it.id.value }.toSet()

    @Test
    fun depthZeroReturnsOnlySeeds() {
        val subgraph = extract(SubgraphQuery(campaignId, setOf(varis), maxDepth = 0))

        assertEquals(setOf("npc_varis"), subgraph.ids())
        assertTrue(subgraph.edges.isEmpty())
    }

    @Test
    fun depthOneReturnsDirectNeighborsBothDirections() {
        val subgraph = extract(SubgraphQuery(campaignId, setOf(NodeId("item_sunken_crown")), maxDepth = 1))

        // Incoming (SEEKS, INVOLVES, ABOUT) and outgoing (LOCATED_IN) edges are followed.
        assertEquals(
            setOf("item_sunken_crown", "npc_varis", "quest_sunken_crown", "lore_crown_curse", "loc_drowned_crypt"),
            subgraph.ids(),
        )
    }

    @Test
    fun returnsEveryEdgeBetweenIncludedNodes() {
        val subgraph = extract(SubgraphQuery(campaignId, setOf(NodeId("item_sunken_crown")), maxDepth = 1))

        // Varis -> Curse is not incident to the seed but both endpoints are included.
        assertTrue(subgraph.edges.any { it.id.value == "e_varis_knows_curse" })
        subgraph.edges.forEach { edge ->
            assertTrue(edge.sourceId.value in subgraph.ids() && edge.targetId.value in subgraph.ids())
        }
    }

    @Test
    fun depthTwoReachesNeighborsOfNeighbors() {
        val subgraph = extract(SubgraphQuery(campaignId, setOf(varis), maxDepth = 2))

        // Varis -> Crown -> Crypt.
        assertTrue("loc_drowned_crypt" in subgraph.ids())
    }

    @Test
    fun edgeTypeFilterLimitsTraversal() {
        val subgraph = extract(SubgraphQuery(campaignId, setOf(varis), maxDepth = 3, edgeTypes = setOf(EdgeType.LEADS)))

        assertEquals(setOf("npc_varis", "faction_iron_circle"), subgraph.ids())
    }

    @Test
    fun nodeTypeFilterKeepsSeeds() {
        val subgraph = extract(SubgraphQuery(campaignId, setOf(varis), maxDepth = 1, nodeTypes = setOf(NodeType.FACTION)))

        assertEquals(setOf("npc_varis", "faction_iron_circle", "faction_tide_temple"), subgraph.ids())
    }

    @Test
    fun maxNodesCapsTheResult() {
        val subgraph = extract(SubgraphQuery(campaignId, setOf(varis), maxDepth = 3, maxNodes = 3))

        assertEquals(3, subgraph.nodes.size)
        assertEquals(varis, subgraph.nodes.first().id)
    }

    @Test
    fun ignoresUnknownSeedsAndOtherCampaigns() {
        val subgraph = extract(SubgraphQuery(CampaignId("other"), setOf(varis), maxDepth = 1))
        assertTrue(subgraph.nodes.isEmpty())

        val unknownSeed = extract(SubgraphQuery(campaignId, setOf(NodeId("missing")), maxDepth = 1))
        assertTrue(unknownSeed.nodes.isEmpty())
        assertTrue(unknownSeed.seedIds.isEmpty())
    }
}
