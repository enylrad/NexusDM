package es.enylrad.nexusdm.domain.graph

import es.enylrad.nexusdm.domain.model.GraphEdge
import es.enylrad.nexusdm.domain.model.GraphNode
import es.enylrad.nexusdm.domain.model.NodeId

/**
 * Extracts a [Subgraph] from an in-memory graph with a breadth-first traversal from the seeds.
 *
 * Edges are followed in both directions. Seeds are always included (when they exist in the
 * campaign); other nodes are included while they respect [SubgraphQuery.nodeTypes] and the
 * [SubgraphQuery.maxNodes] cap, closest nodes first. [SubgraphQuery.edgeTypes] only limits which
 * edges are followed: every edge between two included nodes is returned, so the model sees all
 * the relationships among the nodes it receives.
 */
fun extractSubgraph(
    nodes: Collection<GraphNode>,
    edges: Collection<GraphEdge>,
    query: SubgraphQuery,
): Subgraph {
    val nodesById = nodes.filter { it.campaignId == query.campaignId }.associateBy { it.id }
    val campaignEdges = edges.filter { it.campaignId == query.campaignId }
    val traversableEdges = campaignEdges.filter { query.edgeTypes == null || it.type in query.edgeTypes }
    val neighbors: Map<NodeId, List<NodeId>> = buildMap<NodeId, MutableList<NodeId>> {
        traversableEdges.forEach { edge ->
            getOrPut(edge.sourceId) { mutableListOf() } += edge.targetId
            getOrPut(edge.targetId) { mutableListOf() } += edge.sourceId
        }
    }

    val seeds = query.seedIds.filter { it in nodesById }
    val included = LinkedHashSet<NodeId>(seeds)
    var frontier = seeds
    var depth = 0
    while (frontier.isNotEmpty() && depth < query.maxDepth && included.size < query.maxNodes) {
        val next = mutableListOf<NodeId>()
        for (current in frontier) {
            for (neighborId in neighbors[current].orEmpty()) {
                if (included.size >= query.maxNodes) break
                val neighbor = nodesById[neighborId] ?: continue
                if (neighborId in included) continue
                if (query.nodeTypes != null && neighbor.type !in query.nodeTypes) continue
                included += neighborId
                next += neighborId
            }
        }
        frontier = next
        depth++
    }

    return Subgraph(
        campaignId = query.campaignId,
        seedIds = seeds.toSet(),
        nodes = included.map { nodesById.getValue(it) },
        edges = campaignEdges.filter { it.sourceId in included && it.targetId in included },
    )
}
