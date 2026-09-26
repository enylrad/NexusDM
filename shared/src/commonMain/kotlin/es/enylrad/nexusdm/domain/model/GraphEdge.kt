package es.enylrad.nexusdm.domain.model

import kotlinx.serialization.Serializable

/** A directed, typed relationship between two [GraphNode]s of the same campaign. */
@Serializable
data class GraphEdge(
    val id: EdgeId,
    val campaignId: CampaignId,
    val sourceId: NodeId,
    val targetId: NodeId,
    val type: EdgeType,
    /** Free text nuance of the relationship (e.g. "Owes a blood debt since the siege"). */
    val description: String = "",
    /** Relationship attributes (e.g. "count" -> "4" for creatures in an encounter). */
    val properties: Map<String, String> = emptyMap(),
    val visibility: Visibility = Visibility.DM_ONLY,
    /** Incremented on every change; used to detect mutations based on a stale subgraph. */
    val version: Long = 1,
    /** Creation time as epoch milliseconds. */
    val createdAt: Long,
    /** Last modification time as epoch milliseconds. */
    val updatedAt: Long,
)
