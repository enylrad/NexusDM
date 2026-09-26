package es.enylrad.nexusdm.domain.model

import kotlinx.serialization.Serializable

/**
 * An entity of the campaign knowledge graph (NPC, location, quest, session...).
 *
 * The model is intentionally generic: the [type] tells what the node is, and
 * type-specific data lives in [properties], whose expected keys are documented in
 * [es.enylrad.nexusdm.domain.schema.NodeTypeSchema].
 */
@Serializable
data class GraphNode(
    val id: NodeId,
    val campaignId: CampaignId,
    val type: NodeType,
    val name: String,
    /** One or two sentences. This is what gets sent to the LLM as context. */
    val summary: String,
    /** Long-form text used when rendering campaign documents. */
    val description: String = "",
    /** Alternative names used to resolve mentions during ingestion. */
    val aliases: Set<String> = emptySet(),
    val tags: Set<String> = emptySet(),
    /** Type-specific attributes (e.g. "level" -> "5"). */
    val properties: Map<String, String> = emptyMap(),
    val visibility: Visibility = Visibility.DM_ONLY,
    /** Links to Foundry VTT documents. Never sent to the LLM. */
    val externalRefs: List<ExternalRef> = emptyList(),
    /** Attached files such as maps, portraits or handouts. Never sent to the LLM. */
    val assets: List<AssetRef> = emptyList(),
    /** When true, LLM mutations targeting this node are rejected (e.g. player-owned characters). */
    val isLocked: Boolean = false,
    /** Incremented on every change; used to detect mutations based on a stale subgraph. */
    val version: Long = 1,
    /** Creation time as epoch milliseconds. */
    val createdAt: Long,
    /** Last modification time as epoch milliseconds. */
    val updatedAt: Long,
)
