package es.enylrad.nexusdm.domain.model

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

/** Unique identifier of a [Campaign]. */
@JvmInline
@Serializable
value class CampaignId(val value: String)

/** Unique identifier of a [GraphNode]. Stable across edits, never reused. */
@JvmInline
@Serializable
value class NodeId(val value: String)

/** Unique identifier of a [GraphEdge]. Stable across edits, never reused. */
@JvmInline
@Serializable
value class EdgeId(val value: String)
