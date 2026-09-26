package es.enylrad.nexusdm.domain.mutation

import es.enylrad.nexusdm.domain.model.EdgeType
import es.enylrad.nexusdm.domain.model.Visibility
import kotlinx.serialization.Serializable

/**
 * Partial update of a node. Null fields stay unchanged. Collections are changed through
 * explicit add/remove sets so the LLM cannot wipe data it was not shown.
 * External refs and assets are intentionally not patchable by the LLM.
 */
@Serializable
data class NodePatch(
    val name: String? = null,
    val summary: String? = null,
    val description: String? = null,
    val visibility: Visibility? = null,
    val addAliases: Set<String> = emptySet(),
    val removeAliases: Set<String> = emptySet(),
    val addTags: Set<String> = emptySet(),
    val removeTags: Set<String> = emptySet(),
    /** Properties to insert or overwrite. */
    val setProperties: Map<String, String> = emptyMap(),
    /** Property keys to delete. */
    val removeProperties: Set<String> = emptySet(),
)

/** Partial update of an edge. Same semantics as [NodePatch]. */
@Serializable
data class EdgePatch(
    val type: EdgeType? = null,
    val description: String? = null,
    val visibility: Visibility? = null,
    val setProperties: Map<String, String> = emptyMap(),
    val removeProperties: Set<String> = emptySet(),
)
