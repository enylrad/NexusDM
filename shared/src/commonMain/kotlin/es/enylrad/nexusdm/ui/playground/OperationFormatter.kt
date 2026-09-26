package es.enylrad.nexusdm.ui.playground

import es.enylrad.nexusdm.domain.model.EdgeId
import es.enylrad.nexusdm.domain.model.EdgeType
import es.enylrad.nexusdm.domain.model.GraphEdge
import es.enylrad.nexusdm.domain.model.GraphNode
import es.enylrad.nexusdm.domain.model.Visibility
import es.enylrad.nexusdm.domain.mutation.EdgePatch
import es.enylrad.nexusdm.domain.mutation.GraphMutation
import es.enylrad.nexusdm.domain.mutation.MutationBatch
import es.enylrad.nexusdm.domain.mutation.NodePatch

enum class OperationKind {
    ADD_NODE,
    UPDATE_NODE,
    REMOVE_NODE,
    MERGE_NODES,
    ADD_EDGE,
    UPDATE_EDGE,
    REMOVE_EDGE,
}

/** Human readable view of one [GraphMutation]. The UI adds a localized label for [kind]. */
data class FormattedOperation(
    val kind: OperationKind,
    val title: String,
    val details: List<String> = emptyList(),
)

/**
 * Describes mutations with node names instead of ids. Ids that are not in the graph and are
 * not tempIds of the batch are shown as-is, so hallucinated references stay visible.
 */
class OperationFormatter(
    nodes: Collection<GraphNode>,
    edges: Collection<GraphEdge>,
) {
    private val nodeNames: Map<String, String> = nodes.associate { it.id.value to it.name }
    private val edgesById: Map<EdgeId, GraphEdge> = edges.associateBy { it.id }

    fun format(batch: MutationBatch): List<FormattedOperation> {
        // Names of nodes created in this batch, resolved in order like the applier will.
        val tempNames = mutableMapOf<String, String>()
        return batch.operations.map { operation ->
            if (operation is GraphMutation.AddNode) tempNames[operation.tempId] = operation.name
            format(operation) { ref -> tempNames[ref] ?: nodeNames[ref] ?: ref }
        }
    }

    private fun format(operation: GraphMutation, name: (String) -> String): FormattedOperation = when (operation) {
        is GraphMutation.AddNode -> FormattedOperation(
            kind = OperationKind.ADD_NODE,
            title = "${operation.type.name} \"${operation.name}\"",
            details = buildList {
                add(operation.summary)
                if (operation.description.isNotBlank()) add(operation.description)
                addAll(operation.properties.map { (key, value) -> "$key = $value" })
                if (operation.tags.isNotEmpty()) add("#" + operation.tags.joinToString(" #"))
                if (operation.aliases.isNotEmpty()) add("aka " + operation.aliases.joinToString(", "))
                if (operation.visibility == Visibility.REVEALED) add("visibility = REVEALED")
            },
        )
        is GraphMutation.UpdateNode -> FormattedOperation(
            kind = OperationKind.UPDATE_NODE,
            title = quoted(name(operation.id.value)),
            details = describe(operation.patch),
        )
        is GraphMutation.RemoveNode -> FormattedOperation(
            kind = OperationKind.REMOVE_NODE,
            title = quoted(name(operation.id.value)),
            details = listOf(operation.reason),
        )
        is GraphMutation.MergeNodes -> FormattedOperation(
            kind = OperationKind.MERGE_NODES,
            title = "${quoted(name(operation.keepId.value))} ← ${quoted(name(operation.mergeId.value))}",
            details = listOf(operation.reason),
        )
        is GraphMutation.AddEdge -> FormattedOperation(
            kind = OperationKind.ADD_EDGE,
            title = edgeTitle(name(operation.source.value), operation.type, name(operation.target.value)),
            details = buildList {
                if (operation.description.isNotBlank()) add(operation.description)
                addAll(operation.properties.map { (key, value) -> "$key = $value" })
                if (operation.visibility == Visibility.REVEALED) add("visibility = REVEALED")
            },
        )
        is GraphMutation.UpdateEdge -> FormattedOperation(
            kind = OperationKind.UPDATE_EDGE,
            title = existingEdgeTitle(operation.id, name),
            details = describe(operation.patch),
        )
        is GraphMutation.RemoveEdge -> FormattedOperation(
            kind = OperationKind.REMOVE_EDGE,
            title = existingEdgeTitle(operation.id, name),
            details = listOf(operation.reason),
        )
    }

    private fun describe(patch: NodePatch): List<String> = buildList {
        patch.name?.let { add("name → $it") }
        patch.summary?.let { add("summary → $it") }
        patch.description?.let { add("description → $it") }
        patch.visibility?.let { add("visibility → ${it.name}") }
        patch.setProperties.forEach { (key, value) -> add("$key = $value") }
        patch.removeProperties.forEach { add("− $it") }
        patch.addTags.forEach { add("+ #$it") }
        patch.removeTags.forEach { add("− #$it") }
        patch.addAliases.forEach { add("+ aka $it") }
        patch.removeAliases.forEach { add("− aka $it") }
    }

    private fun describe(patch: EdgePatch): List<String> = buildList {
        patch.type?.let { add("type → ${it.name}") }
        patch.description?.let { add("description → $it") }
        patch.visibility?.let { add("visibility → ${it.name}") }
        patch.setProperties.forEach { (key, value) -> add("$key = $value") }
        patch.removeProperties.forEach { add("− $it") }
    }

    private fun existingEdgeTitle(id: EdgeId, name: (String) -> String): String {
        val edge = edgesById[id] ?: return id.value
        return edgeTitle(name(edge.sourceId.value), edge.type, name(edge.targetId.value))
    }

    private fun edgeTitle(source: String, type: EdgeType, target: String) =
        "${quoted(source)} —${type.name}→ ${quoted(target)}"

    private fun quoted(text: String) = "\"$text\""
}
