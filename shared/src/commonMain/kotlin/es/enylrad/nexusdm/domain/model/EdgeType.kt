package es.enylrad.nexusdm.domain.model

import kotlinx.serialization.Serializable

/**
 * Kind of relationship a [GraphEdge] represents. Edges are directed from source to target;
 * symmetric types read the same in both directions and should be stored only once.
 *
 * [allowedSources] and [allowedTargets] constrain which node types may be connected
 * (null means any type). They are used to validate LLM mutations and to guide prompts.
 */
@Serializable
enum class EdgeType(
    val description: String,
    val isSymmetric: Boolean = false,
    val allowedSources: Set<NodeType>? = null,
    val allowedTargets: Set<NodeType>? = null,
) {
    // Social relationships
    KNOWS("Source and target know each other.", isSymmetric = true, BEINGS, BEINGS),
    ALLY_OF("Source and target are allies.", isSymmetric = true, ACTORS, ACTORS),
    ENEMY_OF("Source and target are enemies.", isSymmetric = true, ACTORS, ACTORS),
    RIVAL_OF("Source and target are rivals.", isSymmetric = true, ACTORS, ACTORS),
    FAMILY_OF("Source and target are relatives; the relation goes in properties.", isSymmetric = true, BEINGS, BEINGS),
    ROMANCE_WITH("Source and target are romantically involved.", isSymmetric = true, BEINGS, BEINGS),
    SERVES("Source works for or is loyal to target.", allowedSources = BEINGS, allowedTargets = ACTORS),

    // Membership and belief
    MEMBER_OF("Source belongs to the target faction.", allowedSources = BEINGS + NodeType.FACTION, allowedTargets = setOf(NodeType.FACTION)),
    LEADS("Source leads the target faction.", allowedSources = BEINGS, allowedTargets = setOf(NodeType.FACTION)),
    WORSHIPS("Source worships the target deity.", allowedSources = ACTORS, allowedTargets = setOf(NodeType.DEITY)),

    // Spatial relationships
    LOCATED_IN("Source is (or lives) inside the target location. Also nests locations.", allowedTargets = setOf(NodeType.LOCATION)),
    CONTROLS("Source rules or controls the target location.", allowedSources = ACTORS, allowedTargets = setOf(NodeType.LOCATION)),
    CONNECTED_TO("Source and target locations are connected by a route or passage.", isSymmetric = true, setOf(NodeType.LOCATION), setOf(NodeType.LOCATION)),
    DEPICTS("Source map shows the target location or encounter.", allowedSources = setOf(NodeType.MAP), allowedTargets = setOf(NodeType.LOCATION, NodeType.ENCOUNTER)),

    // Possession and goals
    OWNS("Source possesses the target item or location.", allowedSources = ACTORS, allowedTargets = setOf(NodeType.ITEM, NodeType.LOCATION)),
    SEEKS("Source wants to obtain, find or achieve the target.", allowedSources = ACTORS),

    // Story and timeline
    INVOLVES("Source event, encounter or quest involves the target as a participant or element.", allowedSources = setOf(NodeType.EVENT, NodeType.ENCOUNTER, NodeType.QUEST)),
    OCCURRED_AT("Source event or encounter took place at the target location.", allowedSources = setOf(NodeType.EVENT, NodeType.ENCOUNTER), allowedTargets = setOf(NodeType.LOCATION)),
    CAUSED("Source caused the target event.", allowedTargets = setOf(NodeType.EVENT)),
    PRECEDES("Source happens before the target (timeline ordering).", allowedSources = TIMELINE, allowedTargets = TIMELINE),
    FEATURED_IN("Source appeared or happened during the target session.", allowedTargets = setOf(NodeType.SESSION)),
    GIVES_QUEST("Source offers the target quest.", allowedSources = ACTORS, allowedTargets = setOf(NodeType.QUEST)),
    REWARDS("Completing the source quest or encounter grants the target.", allowedSources = setOf(NodeType.QUEST, NodeType.ENCOUNTER)),

    // Knowledge
    KNOWS_ABOUT("Source is aware of the target lore or secret.", allowedSources = ACTORS, allowedTargets = setOf(NodeType.LORE)),
    ABOUT("Source lore refers to the target subject.", allowedSources = setOf(NodeType.LORE)),

    // Fallback
    RELATED_TO("Generic relationship; describe it in the edge description.", isSymmetric = true),
    ;

    /** Returns true when an edge of this type may go from [source] to [target]. */
    fun accepts(source: NodeType, target: NodeType): Boolean {
        val forward = allowedSources.allows(source) && allowedTargets.allows(target)
        val backward = isSymmetric && allowedSources.allows(target) && allowedTargets.allows(source)
        return forward || backward
    }
}

private fun Set<NodeType>?.allows(type: NodeType): Boolean = this == null || type in this

/** Node types that represent individual living (or unliving) beings. */
private val BEINGS: Set<NodeType>
    get() = setOf(NodeType.PLAYER_CHARACTER, NodeType.NPC, NodeType.CREATURE, NodeType.DEITY)

/** Node types that can act with intent: beings plus factions. */
private val ACTORS: Set<NodeType>
    get() = BEINGS + NodeType.FACTION

/** Node types that can be ordered on a timeline. */
private val TIMELINE: Set<NodeType>
    get() = setOf(NodeType.EVENT, NodeType.SESSION, NodeType.ENCOUNTER)
