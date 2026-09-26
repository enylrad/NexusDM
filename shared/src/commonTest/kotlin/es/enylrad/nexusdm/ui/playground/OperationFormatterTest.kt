package es.enylrad.nexusdm.ui.playground

import es.enylrad.nexusdm.domain.model.EdgeId
import es.enylrad.nexusdm.domain.model.EdgeType
import es.enylrad.nexusdm.domain.model.NodeId
import es.enylrad.nexusdm.domain.model.NodeType
import es.enylrad.nexusdm.domain.mutation.EdgePatch
import es.enylrad.nexusdm.domain.mutation.GraphMutation
import es.enylrad.nexusdm.domain.mutation.MutationBatch
import es.enylrad.nexusdm.domain.mutation.NodePatch
import es.enylrad.nexusdm.domain.mutation.NodeRef
import es.enylrad.nexusdm.sample.SampleCampaign
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OperationFormatterTest {

    private val formatter = OperationFormatter(SampleCampaign.nodes, SampleCampaign.edges)

    @Test
    fun resolvesTempIdsAndExistingNames() {
        val batch = MutationBatch(
            rationale = "r",
            operations = listOf(
                GraphMutation.AddNode(
                    tempId = "new_sereth", type = NodeType.NPC, name = "Sereth", summary = "Necromancer.",
                    properties = mapOf("attitude" to "hostile"),
                ),
                GraphMutation.AddEdge(NodeRef("new_sereth"), NodeRef("npc_varis"), EdgeType.SERVES),
            ),
        )

        val lines = formatter.format(batch)

        assertEquals(OperationKind.ADD_NODE, lines[0].kind)
        assertEquals("NPC \"Sereth\"", lines[0].title)
        assertEquals(listOf("Necromancer.", "attitude = hostile"), lines[0].details)
        assertEquals(OperationKind.ADD_EDGE, lines[1].kind)
        assertEquals("\"Sereth\" —SERVES→ \"Lord Varis Hale\"", lines[1].title)
    }

    @Test
    fun describesPatchesAndExistingEdges() {
        val batch = MutationBatch(
            rationale = "r",
            operations = listOf(
                GraphMutation.UpdateNode(
                    NodeId("quest_sunken_crown"),
                    NodePatch(setProperties = mapOf("status" to "completed"), addTags = setOf("done")),
                ),
                GraphMutation.UpdateEdge(EdgeId("e_varis_leads"), EdgePatch(description = "Openly now.")),
                GraphMutation.RemoveEdge(EdgeId("e_varis_enemy_temple"), reason = "Truce."),
            ),
        )

        val lines = formatter.format(batch)

        assertEquals("\"Beneath the Tide\"", lines[0].title)
        assertEquals(listOf("status = completed", "+ #done"), lines[0].details)
        assertEquals("\"Lord Varis Hale\" —LEADS→ \"The Iron Circle\"", lines[1].title)
        assertEquals(listOf("description → Openly now."), lines[1].details)
        assertEquals(OperationKind.REMOVE_EDGE, lines[2].kind)
        assertEquals(listOf("Truce."), lines[2].details)
    }

    @Test
    fun keepsUnknownReferencesVisible() {
        val batch = MutationBatch(
            rationale = "r",
            operations = listOf(
                GraphMutation.MergeNodes(NodeId("npc_varis"), NodeId("npc_ghost"), reason = "Same person."),
                GraphMutation.RemoveEdge(EdgeId("e_unknown"), reason = "x"),
            ),
        )

        val lines = formatter.format(batch)

        assertEquals("\"Lord Varis Hale\" ← \"npc_ghost\"", lines[0].title)
        assertTrue(lines[1].title == "e_unknown")
    }
}
