package es.enylrad.nexusdm.domain.model

import kotlinx.serialization.Serializable

/**
 * Who is allowed to know about a node or edge. Drives the split between
 * DM-facing documents and player-facing documents, and lets secrets live in the graph.
 */
@Serializable
enum class Visibility {
    /** Only the DM knows it (secrets, plans, hidden motives). */
    DM_ONLY,

    /** The party has discovered it; safe to include in player-facing documents. */
    REVEALED,
}
