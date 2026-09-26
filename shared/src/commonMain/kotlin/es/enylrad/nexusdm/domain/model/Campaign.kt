package es.enylrad.nexusdm.domain.model

import kotlinx.serialization.Serializable

/**
 * A campaign is the container of one knowledge graph. It is not a node itself:
 * every [GraphNode] and [GraphEdge] belongs to exactly one campaign.
 */
@Serializable
data class Campaign(
    val id: CampaignId,
    val name: String,
    /** Elevator pitch of the campaign, used as top-level context for the LLM. */
    val premise: String = "",
    /** Name of the world or setting (e.g. "Forgotten Realms", homebrew world name). */
    val setting: String = "",
    val rulesEdition: String = DEFAULT_RULES_EDITION,
    val status: CampaignStatus = CampaignStatus.PLANNING,
    /** Identifier of the Foundry VTT world this campaign is played in, if any. */
    val foundryWorldId: String? = null,
    /** Creation time as epoch milliseconds. */
    val createdAt: Long,
    /** Last modification time as epoch milliseconds. */
    val updatedAt: Long,
) {
    companion object {
        const val DEFAULT_RULES_EDITION = "D&D 5e"
    }
}

@Serializable
enum class CampaignStatus {
    PLANNING,
    ACTIVE,
    PAUSED,
    FINISHED,
    ARCHIVED,
}
