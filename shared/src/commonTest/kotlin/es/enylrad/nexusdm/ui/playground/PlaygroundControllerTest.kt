package es.enylrad.nexusdm.ui.playground

import es.enylrad.nexusdm.ai.client.LlmBackendGateway
import es.enylrad.nexusdm.ai.client.LlmBackendSettings
import es.enylrad.nexusdm.ai.client.LlmClient
import es.enylrad.nexusdm.ai.client.LlmException
import es.enylrad.nexusdm.ai.client.StructuredRequest
import es.enylrad.nexusdm.domain.model.NodeId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class PlaygroundControllerTest {

    private class FakeGateway(
        var models: List<String> = listOf("qwen3:14b", "llama3.1:8b"),
        var answer: suspend () -> String = { VALID_ANSWER },
    ) : LlmBackendGateway {
        val createdWith = mutableListOf<LlmBackendSettings>()
        var lastRequest: StructuredRequest? = null

        override fun createClient(settings: LlmBackendSettings): LlmClient {
            createdWith += settings
            return object : LlmClient {
                override suspend fun completeStructured(request: StructuredRequest): String {
                    lastRequest = request
                    return answer()
                }
            }
        }

        override suspend fun listOllamaModels(baseUrl: String): List<String> {
            if (models.isEmpty()) throw LlmException.Unavailable("Ollama is not reachable at $baseUrl")
            return models
        }
    }

    private fun TestScope.controller(gateway: FakeGateway) = PlaygroundController(gateway, this)

    @Test
    fun refreshSelectsFirstModelAlphabetically() = runTest {
        val controller = controller(FakeGateway())

        controller.refreshModels()
        advanceUntilIdle()

        val state = controller.state.value
        assertEquals(listOf("llama3.1:8b", "qwen3:14b"), state.ollamaModels)
        assertEquals("llama3.1:8b", state.selectedOllamaModel)
        assertFalse(state.loadingModels)
    }

    @Test
    fun refreshFailureShowsError() = runTest {
        val controller = controller(FakeGateway(models = emptyList()))

        controller.refreshModels()
        advanceUntilIdle()

        assertTrue(controller.state.value.modelsError!!.contains("not reachable"))
    }

    @Test
    fun cannotRunWithoutModelOrRequest() = runTest {
        val gateway = FakeGateway()
        val controller = controller(gateway)

        controller.setRequest("Something")
        assertFalse(controller.state.value.canRun)
        controller.run()
        advanceUntilIdle()

        assertTrue(gateway.createdWith.isEmpty())
    }

    @Test
    fun runProducesFormattedResult() = runTest {
        val gateway = FakeGateway()
        val controller = controller(gateway)
        controller.refreshModels()
        advanceUntilIdle()
        controller.selectOllamaModel("qwen3:14b")
        controller.setRequest("Varis contrata a una nigromante")

        controller.run()
        advanceUntilIdle()

        val state = controller.state.value
        assertNull(state.error)
        val result = assertNotNull(state.result)
        assertEquals("Varis hires a necromancer.", result.rationale)
        assertEquals("\"Sereth\" —SERVES→ \"Lord Varis Hale\"", result.operations[1].title)
        assertTrue(result.rawJson.contains("add_node"))
        assertTrue(result.contextNodeCount > 1)
        assertEquals(LlmBackendSettings.Ollama(model = "qwen3:14b"), gateway.createdWith.single())
        assertTrue("npc_varis" in gateway.lastRequest!!.userPrompt)
    }

    @Test
    fun claudeBackendUsesKeyAndModel() = runTest {
        val gateway = FakeGateway()
        val controller = controller(gateway)
        controller.selectBackend(BackendKind.CLAUDE)
        controller.setClaudeApiKey("  sk-test  ")
        controller.setRequest("Idea")

        controller.run()
        advanceUntilIdle()

        assertEquals(LlmBackendSettings.Claude(apiKey = "sk-test"), gateway.createdWith.single())
    }

    @Test
    fun llmErrorsAreShown() = runTest {
        val gateway = FakeGateway(answer = { throw LlmException.Truncated("The answer reached the output token limit") })
        val controller = controller(gateway)
        controller.selectBackend(BackendKind.CLAUDE)
        controller.setRequest("Idea")

        controller.run()
        advanceUntilIdle()

        val state = controller.state.value
        assertFalse(state.running)
        assertNull(state.result)
        assertEquals("The answer reached the output token limit", state.error)
    }

    @Test
    fun cancelStopsARunningRequest() = runTest {
        val never = CompletableDeferred<String>()
        val controller = controller(FakeGateway(answer = { never.await() }))
        controller.selectBackend(BackendKind.CLAUDE)
        controller.setRequest("Idea")

        controller.run()
        advanceUntilIdle()
        assertTrue(controller.state.value.running)

        controller.cancel()
        advanceUntilIdle()
        assertFalse(controller.state.value.running)
        assertNull(controller.state.value.error)
    }

    @Test
    fun focusCanBeToggled() = runTest {
        val controller = controller(FakeGateway())
        assertEquals(setOf(NodeId("npc_varis")), controller.state.value.focusNodeIds)

        controller.toggleFocus(NodeId("npc_varis"))
        controller.toggleFocus(NodeId("loc_saltmere"))

        assertEquals(setOf(NodeId("loc_saltmere")), controller.state.value.focusNodeIds)
    }

    @Test
    fun formatsElapsedSeconds() {
        assertEquals("12.3", formatSeconds(12_345))
        assertEquals("0.0", formatSeconds(42))
    }

    private companion object {
        val VALID_ANSWER = """
            {"rationale": "Varis hires a necromancer.", "operations": [
              {"op": "add_node", "tempId": "new_sereth", "type": "NPC", "name": "Sereth", "summary": "Necromancer.",
               "description": "", "aliases": [], "tags": [], "properties": [], "visibility": "DM_ONLY"},
              {"op": "add_edge", "source": "new_sereth", "target": "npc_varis", "type": "SERVES",
               "description": "", "properties": [], "visibility": "DM_ONLY"}
            ]}
        """.trimIndent()
    }
}
