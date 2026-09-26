package es.enylrad.nexusdm.domain.mutation

import es.enylrad.nexusdm.domain.model.EdgeId
import es.enylrad.nexusdm.domain.model.EdgeType
import es.enylrad.nexusdm.domain.model.NodeId
import es.enylrad.nexusdm.domain.model.NodeType
import es.enylrad.nexusdm.domain.model.Visibility
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class MutationBatchSerializationTest {

    // A realistic LLM answer to "The Iron Circle hires a necromancer to raid the crypt".
    private val llmResponse = """
        {
          "rationale": "Adds a necromancer hired by the Iron Circle and a crypt ambush.",
          "operations": [
            {
              "op": "add_node",
              "tempId": "new_necromancer",
              "type": "NPC",
              "name": "Sereth Vale",
              "summary": "Exiled necromancer working for the Iron Circle.",
              "properties": { "occupation": "necromancer", "attitude": "hostile" },
              "tags": ["villain"]
            },
            {
              "op": "add_edge",
              "source": "new_necromancer",
              "target": "faction_iron_circle",
              "type": "SERVES",
              "description": "Paid in forbidden tomes."
            },
            {
              "op": "add_node",
              "tempId": "new_crypt_ambush",
              "type": "ENCOUNTER",
              "name": "Crypt Ambush",
              "summary": "Skeletons rise when the party opens the sarcophagus.",
              "properties": { "encounter_kind": "combat", "difficulty": "hard" }
            },
            {
              "op": "add_edge",
              "source": "new_crypt_ambush",
              "target": "creature_skeleton",
              "type": "INVOLVES",
              "properties": { "count": "6" }
            },
            {
              "op": "update_node",
              "id": "quest_lost_relic",
              "patch": {
                "setProperties": { "status": "active" },
                "addTags": ["undead"],
                "visibility": "REVEALED"
              }
            },
            { "op": "remove_edge", "id": "edge_42", "reason": "The alliance was broken." },
            { "op": "merge_nodes", "keepId": "npc_varis", "mergeId": "npc_lord_varis", "reason": "Same person." }
          ]
        }
    """.trimIndent()

    @Test
    fun decodesLlmResponse() {
        val batch = MutationJson.decode(llmResponse)

        assertEquals(7, batch.operations.size)

        val necromancer = assertIs<GraphMutation.AddNode>(batch.operations[0])
        assertEquals("new_necromancer", necromancer.tempId)
        assertEquals(NodeType.NPC, necromancer.type)
        assertEquals("hostile", necromancer.properties["attitude"])
        assertEquals(Visibility.DM_ONLY, necromancer.visibility)

        val serves = assertIs<GraphMutation.AddEdge>(batch.operations[1])
        assertEquals(NodeRef("new_necromancer"), serves.source)
        assertEquals(NodeRef("faction_iron_circle"), serves.target)
        assertEquals(EdgeType.SERVES, serves.type)

        val involves = assertIs<GraphMutation.AddEdge>(batch.operations[3])
        assertEquals("6", involves.properties["count"])

        val update = assertIs<GraphMutation.UpdateNode>(batch.operations[4])
        assertEquals(NodeId("quest_lost_relic"), update.id)
        assertEquals(mapOf("status" to "active"), update.patch.setProperties)
        assertEquals(Visibility.REVEALED, update.patch.visibility)
        assertNull(update.patch.name)

        val removeEdge = assertIs<GraphMutation.RemoveEdge>(batch.operations[5])
        assertEquals(EdgeId("edge_42"), removeEdge.id)

        val merge = assertIs<GraphMutation.MergeNodes>(batch.operations[6])
        assertEquals(NodeId("npc_varis"), merge.keepId)
        assertEquals(NodeId("npc_lord_varis"), merge.mergeId)
    }

    @Test
    fun roundTripPreservesBatch() {
        val batch = MutationJson.decode(llmResponse)

        val decoded = MutationJson.decode(MutationJson.encode(batch))

        assertEquals(batch, decoded)
    }

    @Test
    fun encodesOperationDiscriminator() {
        val batch = MutationBatch(
            rationale = "Remove a stale rumor.",
            operations = listOf(GraphMutation.RemoveNode(NodeId("lore_1"), reason = "Retconned.")),
        )

        val text = MutationJson.encode(batch)

        assertEquals(
            """{"rationale":"Remove a stale rumor.","operations":[{"op":"remove_node","id":"lore_1","reason":"Retconned."}]}""",
            text,
        )
    }

    @Test
    fun ignoresUnknownKeys() {
        val text = """
            {
              "rationale": "Rename.",
              "confidence": 0.9,
              "operations": [
                { "op": "update_node", "id": "npc_1", "patch": { "name": "Old Tom" }, "note": "extra" }
              ]
            }
        """.trimIndent()

        val batch = MutationJson.decode(text)

        val update = assertIs<GraphMutation.UpdateNode>(batch.operations.single())
        assertEquals("Old Tom", update.patch.name)
    }
}
