package es.enylrad.nexusdm.ai.client.ollama

import es.enylrad.nexusdm.ai.MutationProposalService
import es.enylrad.nexusdm.domain.mutation.MutationBatch
import es.enylrad.nexusdm.domain.mutation.MutationJson
import es.enylrad.nexusdm.testing.Fixtures
import kotlin.test.Test
import kotlinx.coroutines.runBlocking

/**
 * Runs a real request against a local Ollama instance. Skipped unless the OLLAMA_MODEL
 * environment variable is set, e.g.:
 *
 *     OLLAMA_MODEL=qwen3:14b ./gradlew :shared:jvmTest --tests '*OllamaIntegrationTest*' -i
 *
 * OLLAMA_BASE_URL can point to a non-default server.
 */
class OllamaIntegrationTest {

    @Test
    fun proposesMutationsWithLocalModel() = runBlocking {
        val model = System.getenv("OLLAMA_MODEL")
        if (model.isNullOrBlank()) {
            println("OLLAMA_MODEL not set: skipping Ollama integration test")
            return@runBlocking
        }
        val settings = OllamaSettings(
            model = model,
            baseUrl = System.getenv("OLLAMA_BASE_URL") ?: OllamaSettings.DEFAULT_BASE_URL,
        )
        val service = MutationProposalService(OllamaLlmClient(settings))

        val batch = service.propose(
            Fixtures.campaign,
            Fixtures.subgraph,
            "Lord Varis contrata a una nigromante llamada Sereth para saquear la cripta real.",
        )

        println(MutationJson.json.encodeToString(MutationBatch.serializer(), batch))
    }
}
