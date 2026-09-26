package es.enylrad.nexusdm.domain.model

import kotlinx.serialization.Serializable

/**
 * Reference to a binary asset (map image, token, portrait, handout, audio) attached to a node.
 * Assets are managed by the app and are never exposed to the LLM.
 */
@Serializable
data class AssetRef(
    val kind: AssetKind,
    /** Local file path or Foundry data path (e.g. "worlds/my-world/maps/keep.webp"). */
    val path: String,
    val caption: String? = null,
)

@Serializable
enum class AssetKind {
    MAP_IMAGE,
    TOKEN,
    PORTRAIT,
    HANDOUT,
    AUDIO,
    OTHER,
}
