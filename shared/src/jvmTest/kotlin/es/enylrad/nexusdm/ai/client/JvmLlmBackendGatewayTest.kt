package es.enylrad.nexusdm.ai.client

import com.sun.net.httpserver.HttpServer
import es.enylrad.nexusdm.ai.client.anthropic.AnthropicLlmClient
import es.enylrad.nexusdm.ai.client.ollama.OllamaLlmClient
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.runBlocking

class JvmLlmBackendGatewayTest {

    private val gateway = JvmLlmBackendGateway()

    @Test
    fun createsClientForEachBackend() {
        assertIs<OllamaLlmClient>(gateway.createClient(LlmBackendSettings.Ollama(model = "qwen3:14b")))
        assertIs<AnthropicLlmClient>(gateway.createClient(LlmBackendSettings.Claude(apiKey = "sk-test")))
    }

    @Test
    fun listsOllamaModels() = runBlocking {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/tags") { exchange ->
            val body = """{"models": [{"name": "qwen3:14b"}, {"name": "mistral:7b"}]}""".encodeToByteArray()
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()
        try {
            val models = gateway.listOllamaModels("http://127.0.0.1:${server.address.port}")

            assertEquals(listOf("qwen3:14b", "mistral:7b"), models)
        } finally {
            server.stop(0)
        }
    }
}
