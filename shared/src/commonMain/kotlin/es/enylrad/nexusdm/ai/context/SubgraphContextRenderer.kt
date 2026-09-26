package es.enylrad.nexusdm.ai.context

import es.enylrad.nexusdm.domain.graph.Subgraph
import es.enylrad.nexusdm.domain.model.Campaign
import es.enylrad.nexusdm.domain.model.EdgeType
import es.enylrad.nexusdm.domain.model.GraphEdge
import es.enylrad.nexusdm.domain.model.GraphNode
import es.enylrad.nexusdm.domain.model.NodeType
import es.enylrad.nexusdm.domain.model.Visibility
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Renders a [Subgraph] as compact JSON for the LLM prompt.
 *
 * Only what the model needs to reason is included: Foundry links, assets, versions and
 * timestamps are left out. Long descriptions are only sent for the seed nodes, which are
 * the focus of the request; neighbors are represented by their summary.
 */
class SubgraphContextRenderer {

    private val json = Json {
        prettyPrint = false
        // Skip empty collections and default values to save tokens.
        encodeDefaults = false
        explicitNulls = false
    }

    fun render(campaign: Campaign, subgraph: Subgraph): String {
        require(subgraph.campaignId == campaign.id) { "Subgraph does not belong to campaign ${campaign.id.value}" }
        val document = ContextDocument(
            campaign = ContextCampaign(
                name = campaign.name,
                premise = campaign.premise,
                setting = campaign.setting,
                rulesEdition = campaign.rulesEdition,
            ),
            focusNodeIds = subgraph.seedIds.map { it.value },
            nodes = subgraph.nodes.map { it.toContext(isSeed = it.id in subgraph.seedIds) },
            edges = subgraph.edges.map { it.toContext() },
        )
        return json.encodeToString(ContextDocument.serializer(), document)
    }

    private fun GraphNode.toContext(isSeed: Boolean) = ContextNode(
        id = id.value,
        type = type,
        name = name,
        summary = summary,
        description = description.takeIf { isSeed && it.isNotBlank() },
        aliases = aliases.sorted(),
        tags = tags.sorted(),
        properties = properties.toSortedMap(),
        visibility = visibility,
        locked = isLocked,
    )

    private fun GraphEdge.toContext() = ContextEdge(
        id = id.value,
        source = sourceId.value,
        target = targetId.value,
        type = type,
        description = description.takeIf { it.isNotBlank() },
        properties = properties.toSortedMap(),
        visibility = visibility,
    )

    private fun Map<String, String>.toSortedMap(): Map<String, String> =
        entries.sortedBy { it.key }.associate { it.key to it.value }
}

@Serializable
private data class ContextDocument(
    val campaign: ContextCampaign,
    val focusNodeIds: List<String>,
    val nodes: List<ContextNode>,
    val edges: List<ContextEdge>,
)

@Serializable
private data class ContextCampaign(
    val name: String,
    val premise: String = "",
    val setting: String = "",
    val rulesEdition: String,
)

@Serializable
private data class ContextNode(
    val id: String,
    val type: NodeType,
    val name: String,
    val summary: String,
    val description: String? = null,
    val aliases: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val properties: Map<String, String> = emptyMap(),
    val visibility: Visibility = Visibility.DM_ONLY,
    val locked: Boolean = false,
)

@Serializable
private data class ContextEdge(
    val id: String,
    val source: String,
    val target: String,
    val type: EdgeType,
    val description: String? = null,
    val properties: Map<String, String> = emptyMap(),
    val visibility: Visibility = Visibility.DM_ONLY,
)
