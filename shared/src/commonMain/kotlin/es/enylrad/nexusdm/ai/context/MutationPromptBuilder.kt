package es.enylrad.nexusdm.ai.context

import es.enylrad.nexusdm.domain.graph.Subgraph
import es.enylrad.nexusdm.domain.model.Campaign
import es.enylrad.nexusdm.domain.model.EdgeType
import es.enylrad.nexusdm.domain.model.NodeType
import es.enylrad.nexusdm.domain.schema.NodeTypeSchema
import es.enylrad.nexusdm.domain.schema.PropertyValueType

/**
 * Builds the prompts used to turn a DM request into a
 * [es.enylrad.nexusdm.domain.mutation.MutationBatch].
 *
 * The system prompt only depends on the domain model, so it is identical across calls
 * and can be cached by the provider. Everything that varies goes into the user prompt.
 */
class MutationPromptBuilder(
    private val contextRenderer: SubgraphContextRenderer = SubgraphContextRenderer(),
) {

    val systemPrompt: String by lazy { buildSystemPrompt() }

    fun userPrompt(campaign: Campaign, subgraph: Subgraph, request: String): String = buildString {
        appendLine("<campaign_context>")
        appendLine(contextRenderer.render(campaign, subgraph))
        appendLine("</campaign_context>")
        appendLine()
        appendLine("<dm_request>")
        appendLine(request.trim())
        append("</dm_request>")
    }

    private fun buildSystemPrompt(): String = buildString {
        appendLine(
            """
            You are the co-writer of a Dungeons & Dragons campaign. The campaign is stored as a knowledge graph
            of nodes (NPCs, locations, quests, sessions...) and typed edges between them. The graph is the single
            source of truth: campaign documents are generated from it.

            You receive a slice of the graph (<campaign_context>) around the elements affected by the DM's request
            (<dm_request>). Answer only with the list of graph operations that implement the request, plus a short
            rationale. You never see the full campaign, so do not contradict or remove facts you were not shown.

            Rules:
            - Reference existing nodes and edges only by the ids present in the context. New nodes get a unique
              tempId; later operations in the same batch may use that tempId as an edge source or target.
            - Never modify, remove or merge a node marked "locked": true. You may still connect edges to it.
            - Prefer updating an existing node over creating a near-duplicate. Use merge_nodes when two nodes
              clearly describe the same entity.
            - Keep "summary" to one or two sentences; put longer lore in "description".
            - Use the well-known property keys listed below for each node type, with the allowed values when
              the key has them. Other keys are allowed when nothing fits.
            - Respect the source and target types of each edge type. Symmetric edge types are created only once,
              in either direction.
            - Visibility: DM_ONLY for secrets and anything the players have not discovered; REVEALED only when the
              request says the party learned it. Fields omitted in the context take their default values
              (visibility DM_ONLY, locked false, empty collections).
            - Write names, summaries, descriptions and the rationale in the language used by the DM request.
            - If the request is unclear or needs no change, return an empty operations list and explain why in
              the rationale.
            """.trimIndent(),
        )
        appendLine()
        appendLine("Node types and their well-known properties:")
        NodeType.entries.forEach { type ->
            appendLine("- ${type.name}: ${type.description}")
            NodeTypeSchema.propertiesFor(type).forEach { spec ->
                val values = if (spec.valueType == PropertyValueType.ENUM) " one of [${spec.allowedValues.joinToString()}]" else ""
                val required = if (spec.required) ", required" else ""
                appendLine("    - ${spec.key} (${spec.valueType.name.lowercase()}$required)$values: ${spec.description}")
            }
        }
        appendLine()
        appendLine("Edge types (source -> target):")
        EdgeType.entries.forEach { type ->
            val sources = type.allowedSources?.joinToString("|") { it.name } ?: "ANY"
            val targets = type.allowedTargets?.joinToString("|") { it.name } ?: "ANY"
            val symmetric = if (type.isSymmetric) ", symmetric" else ""
            appendLine("- ${type.name} ($sources -> $targets$symmetric): ${type.description}")
        }
    }.trimEnd()
}
