package es.enylrad.nexusdm.domain.mutation

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

/**
 * Reference to a node from inside a [MutationBatch]. It is either:
 * - the id of an existing node included in the subgraph sent to the LLM, or
 * - the [GraphMutation.AddNode.tempId] of a node created earlier in the same batch.
 *
 * This lets the LLM connect new nodes without knowing the ids the app will assign.
 */
@JvmInline
@Serializable
value class NodeRef(val value: String)
