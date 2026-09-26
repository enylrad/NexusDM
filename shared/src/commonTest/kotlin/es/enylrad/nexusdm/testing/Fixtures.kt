package es.enylrad.nexusdm.testing

import es.enylrad.nexusdm.domain.graph.Subgraph
import es.enylrad.nexusdm.domain.model.AssetKind
import es.enylrad.nexusdm.domain.model.AssetRef
import es.enylrad.nexusdm.domain.model.Campaign
import es.enylrad.nexusdm.domain.model.CampaignId
import es.enylrad.nexusdm.domain.model.EdgeId
import es.enylrad.nexusdm.domain.model.EdgeType
import es.enylrad.nexusdm.domain.model.ExternalRef
import es.enylrad.nexusdm.domain.model.ExternalSystem
import es.enylrad.nexusdm.domain.model.FoundryDocumentTypes
import es.enylrad.nexusdm.domain.model.GraphEdge
import es.enylrad.nexusdm.domain.model.GraphNode
import es.enylrad.nexusdm.domain.model.NodeId
import es.enylrad.nexusdm.domain.model.NodeType

/** Small, reusable campaign graph for tests. */
object Fixtures {
    private const val NOW = 1_700_000_000_000L

    val campaignId = CampaignId("campaign_1")

    val campaign = Campaign(
        id = campaignId,
        name = "The Sunken Crown",
        premise = "A drowned kingdom rises again.",
        setting = "Homebrew",
        createdAt = NOW,
        updatedAt = NOW,
    )

    val varis = GraphNode(
        id = NodeId("npc_varis"),
        campaignId = campaignId,
        type = NodeType.NPC,
        name = "Lord Varis",
        summary = "Ambitious noble secretly leading the Iron Circle.",
        description = "Varis lost his lands in the flood and blames the crown.",
        properties = mapOf("occupation" to "noble", "attitude" to "indifferent"),
        externalRefs = listOf(ExternalRef(ExternalSystem.FOUNDRY_VTT, FoundryDocumentTypes.ACTOR, "Actor.secretUuid123")),
        assets = listOf(AssetRef(AssetKind.PORTRAIT, "worlds/sunken/portraits/varis.webp")),
        createdAt = NOW,
        updatedAt = NOW,
    )

    val ironCircle = GraphNode(
        id = NodeId("faction_iron_circle"),
        campaignId = campaignId,
        type = NodeType.FACTION,
        name = "Iron Circle",
        summary = "Secret society of dispossessed nobles.",
        description = "Long lore that should only be sent for seed nodes.",
        createdAt = NOW,
        updatedAt = NOW,
    )

    val hero = GraphNode(
        id = NodeId("pc_aria"),
        campaignId = campaignId,
        type = NodeType.PLAYER_CHARACTER,
        name = "Aria",
        summary = "Half-elf ranger played by Marta.",
        properties = mapOf("level" to "5"),
        isLocked = true,
        createdAt = NOW,
        updatedAt = NOW,
    )

    val leads = GraphEdge(
        id = EdgeId("edge_leads"),
        campaignId = campaignId,
        sourceId = varis.id,
        targetId = ironCircle.id,
        type = EdgeType.LEADS,
        createdAt = NOW,
        updatedAt = NOW,
    )

    /** Subgraph focused on Lord Varis. */
    val subgraph = Subgraph(
        campaignId = campaignId,
        seedIds = setOf(varis.id),
        nodes = listOf(varis, ironCircle, hero),
        edges = listOf(leads),
    )
}
