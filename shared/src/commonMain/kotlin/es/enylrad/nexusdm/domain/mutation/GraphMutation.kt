package es.enylrad.nexusdm.domain.mutation

import es.enylrad.nexusdm.domain.model.EdgeId
import es.enylrad.nexusdm.domain.model.EdgeType
import es.enylrad.nexusdm.domain.model.NodeId
import es.enylrad.nexusdm.domain.model.NodeType
import es.enylrad.nexusdm.domain.model.Visibility
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A single change to the campaign graph, as proposed by the LLM.
 *
 * Serialized as JSON with an "op" discriminator (see [MutationJson]). The LLM never
 * provides campaign ids, real ids for new elements, versions, timestamps, external refs
 * or assets: the mutation applier injects or preserves those.
 */
@Serializable
sealed interface GraphMutation {

    /** Creates a node. [tempId] lets later operations in the same batch reference it. */
    @Serializable
    @SerialName("add_node")
    data class AddNode(
        val tempId: String,
        val type: NodeType,
        val name: String,
        val summary: String,
        val description: String = "",
        val aliases: Set<String> = emptySet(),
        val tags: Set<String> = emptySet(),
        @Serializable(with = KeyValueMapSerializer::class)
        val properties: Map<String, String> = emptyMap(),
        val visibility: Visibility = Visibility.DM_ONLY,
    ) : GraphMutation

    @Serializable
    @SerialName("update_node")
    data class UpdateNode(
        val id: NodeId,
        val patch: NodePatch,
    ) : GraphMutation

    /** Deletes a node and, implicitly, every edge connected to it. */
    @Serializable
    @SerialName("remove_node")
    data class RemoveNode(
        val id: NodeId,
        val reason: String,
    ) : GraphMutation

    /**
     * Merges two nodes that describe the same entity (e.g. "Lord Varis" and "Varis").
     * Edges, aliases, tags and missing properties of [mergeId] move to [keepId],
     * then [mergeId] is deleted.
     */
    @Serializable
    @SerialName("merge_nodes")
    data class MergeNodes(
        val keepId: NodeId,
        val mergeId: NodeId,
        val reason: String,
    ) : GraphMutation

    @Serializable
    @SerialName("add_edge")
    data class AddEdge(
        val source: NodeRef,
        val target: NodeRef,
        val type: EdgeType,
        val description: String = "",
        @Serializable(with = KeyValueMapSerializer::class)
        val properties: Map<String, String> = emptyMap(),
        val visibility: Visibility = Visibility.DM_ONLY,
    ) : GraphMutation

    @Serializable
    @SerialName("update_edge")
    data class UpdateEdge(
        val id: EdgeId,
        val patch: EdgePatch,
    ) : GraphMutation

    @Serializable
    @SerialName("remove_edge")
    data class RemoveEdge(
        val id: EdgeId,
        val reason: String,
    ) : GraphMutation
}
