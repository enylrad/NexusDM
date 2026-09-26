package es.enylrad.nexusdm.ai

import es.enylrad.nexusdm.ai.client.LlmClient
import es.enylrad.nexusdm.ai.client.LlmException
import es.enylrad.nexusdm.ai.client.StructuredRequest
import es.enylrad.nexusdm.ai.schema.MutationBatchSchema
import es.enylrad.nexusdm.domain.model.EdgeType
import es.enylrad.nexusdm.domain.model.NodeType
import es.enylrad.nexusdm.domain.mutation.GraphMutation
import es.enylrad.nexusdm.testing.Fixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class MutationProposalServiceTest {

    private class FakeLlmClient(private val answer: String) : LlmClient {
        var lastRequest: StructuredRequest? = null

        override suspend fun completeStructured(request: StructuredRequest): String {
            lastRequest = request
            return answer
        }
    }

    private val validAnswer = """
        {"rationale": "Varis hires a necromancer.", "operations": [
          {"op": "add_node", "tempId": "new_sereth", "type": "NPC", "name": "Sereth", "summary": "Hired necromancer.",
           "description": "", "aliases": [], "tags": [], "properties": [{"key": "attitude", "value": "hostile"}], "visibility": "DM_ONLY"},
          {"op": "add_edge", "source": "new_sereth", "target": "npc_varis", "type": "SERVES",
           "description": "", "properties": [], "visibility": "DM_ONLY"}
        ]}
    """.trimIndent()

    @Test
    fun buildsRequestAndDecodesAnswer() = runTest {
        val client = FakeLlmClient(validAnswer)
        val service = MutationProposalService(client)

        val batch = service.propose(Fixtures.campaign, Fixtures.subgraph, "Lord Varis contrata a un nigromante")

        val addNode = assertIs<GraphMutation.AddNode>(batch.operations[0])
        assertEquals(NodeType.NPC, addNode.type)
        assertEquals(mapOf("attitude" to "hostile"), addNode.properties)
        assertEquals(EdgeType.SERVES, assertIs<GraphMutation.AddEdge>(batch.operations[1]).type)

        val request = client.lastRequest!!
        assertEquals(MutationBatchSchema.schema, request.outputSchema)
        assertTrue("Lord Varis contrata a un nigromante" in request.userPrompt)
        assertTrue("npc_varis" in request.userPrompt)
    }

    @Test
    fun systemPromptDescribesTheWholeDomainAndIsStable() = runTest {
        val first = FakeLlmClient(validAnswer)
        val second = FakeLlmClient(validAnswer)
        MutationProposalService(first).propose(Fixtures.campaign, Fixtures.subgraph, "Idea one")
        MutationProposalService(second).propose(Fixtures.campaign, Fixtures.subgraph, "Idea two")

        val systemPrompt = first.lastRequest!!.systemPrompt
        assertEquals(systemPrompt, second.lastRequest!!.systemPrompt)
        NodeType.entries.forEach { assertTrue(it.name in systemPrompt, "Missing node type $it") }
        EdgeType.entries.forEach { assertTrue(it.name in systemPrompt, "Missing edge type $it") }
        assertTrue("session_number" in systemPrompt)
    }

    @Test
    fun invalidAnswerRaisesInvalidResponse() = runTest {
        val service = MutationProposalService(FakeLlmClient("""{"rationale": "oops"}"""))

        assertFailsWith<LlmException.InvalidResponse> {
            service.propose(Fixtures.campaign, Fixtures.subgraph, "Anything")
        }
    }
}
