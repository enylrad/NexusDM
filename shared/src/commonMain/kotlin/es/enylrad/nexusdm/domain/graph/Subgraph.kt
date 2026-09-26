package es.enylrad.nexusdm.domain.graph

import es.enylrad.nexusdm.domain.model.CampaignId
import es.enylrad.nexusdm.domain.model.EdgeType
import es.enylrad.nexusdm.domain.model.GraphEdge
import es.enylrad.nexusdm.domain.model.GraphNode
import es.enylrad.nexusdm.domain.model.NodeId
import es.enylrad.nexusdm.domain.model.NodeType

/**
 * Describes which slice of the graph to extract as LLM context: the neighborhood of
 * [seedIds] up to [maxDepth] hops, optionally filtered by edge and node types.
 */
data class SubgraphQuery(
    val campaignId: CampaignId,
    val seedIds: Set<NodeId>,
    val maxDepth: Int = 1,
    /** Only traverse these edge types; null means all. */
    val edgeTypes: Set<EdgeType>? = null,
    /** Only include these node types (seeds are always included); null means all. */
    val nodeTypes: Set<NodeType>? = null,
    /** Hard cap on returned nodes to keep the prompt small. */
    val maxNodes: Int = 50,
) {
    init {
        require(maxDepth >= 0) { "maxDepth must be >= 0" }
        require(maxNodes > 0) { "maxNodes must be > 0" }
    }
}

/** A self-contained slice of the campaign graph, sent to the LLM as context. */
data class Subgraph(
    val campaignId: CampaignId,
    val seedIds: Set<NodeId>,
    val nodes: List<GraphNode>,
    /** Only edges whose both endpoints are in [nodes]. */
    val edges: List<GraphEdge>,
) {
    /**
     * Snapshot of node versions at extraction time. Compared before applying a
     * [es.enylrad.nexusdm.domain.mutation.MutationBatch] to detect concurrent edits.
     */
    fun nodeVersions(): Map<NodeId, Long> = nodes.associate { it.id to it.version }
}
