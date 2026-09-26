package es.enylrad.nexusdm.domain.model

import kotlinx.serialization.Serializable

/**
 * Link from a graph element to a document living in an external tool (e.g. a Foundry VTT
 * Actor or Scene). External refs are managed by the app and are never exposed to the LLM.
 */
@Serializable
data class ExternalRef(
    val system: ExternalSystem,
    /** Document type inside the external system. For Foundry see [FoundryDocumentTypes]. */
    val documentType: String,
    /**
     * Identifier inside the external system. For Foundry this is the document UUID,
     * e.g. "Actor.a1B2c3D4" or "Compendium.dnd5e.monsters.Actor.xyz".
     */
    val uuid: String,
    /** Optional human readable label (e.g. the document name in Foundry). */
    val label: String? = null,
)

@Serializable
enum class ExternalSystem {
    FOUNDRY_VTT,
}

/**
 * Known Foundry VTT document types. Kept as plain strings instead of an enum so that
 * new or module-specific document types do not break deserialization.
 */
object FoundryDocumentTypes {
    const val ACTOR = "Actor"
    const val ITEM = "Item"
    const val SCENE = "Scene"
    const val JOURNAL_ENTRY = "JournalEntry"
    const val JOURNAL_ENTRY_PAGE = "JournalEntryPage"
    const val ROLL_TABLE = "RollTable"
    const val PLAYLIST = "Playlist"
    const val MACRO = "Macro"
}
