package es.enylrad.nexusdm.ai.client.ollama

import com.sun.net.httpserver.HttpServer
import es.enylrad.nexusdm.ai.MutationProposalService
import es.enylrad.nexusdm.ai.client.LlmException
import es.enylrad.nexusdm.ai.client.StructuredRequest
import es.enylrad.nexusdm.ai.schema.MutationBatchSchema
import es.enylrad.nexusdm.domain.mutation.GraphMutation
import es.enylrad.nexusdm.testing.Fixtures
import java.net.InetSocketAddress
import java.net.ServerSocket
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class OllamaLlmClientTest {

    /** Minimal fake of the Ollama HTTP API that records the last request body. */
    private class FakeOllama(status: Int, responseBody: String) {
        val server: HttpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        var lastPath: String? = null
        var lastBody: String? = null

        init {
            server.createContext("/") { exchange ->
                lastPath = exchange.requestURI.path
                lastBody = exchange.requestBody.readBytes().decodeToString()
                val bytes = responseBody.encodeToByteArray()
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(status, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            server.start()
        }

        val baseUrl get() = "http://127.0.0.1:${server.address.port}"
    }

    private var fake: FakeOllama? = null

    @AfterTest
    fun stopServer() {
        fake?.server?.stop(0)
    }

    private fun serve(status: Int, body: String): FakeOllama = FakeOllama(status, body).also { fake = it }

    private val request = StructuredRequest(
        systemPrompt = "System rules",
        userPrompt = "User request",
        outputSchema = MutationBatchSchema.schema,
        maxOutputTokens = 4_000,
    )

    private fun chatResponse(content: String, doneReason: String = "stop"): String =
        buildJsonObject {
            put("model", JsonPrimitive("qwen3:14b"))
            put("message", buildJsonObject {
                put("role", JsonPrimitive("assistant"))
                put("content", JsonPrimitive(content))
            })
            put("done", JsonPrimitive(true))
            put("done_reason", JsonPrimitive(doneReason))
        }.toString()

    @Test
    fun sendsStructuredChatRequest() = runBlocking {
        val server = serve(200, chatResponse("""{"rationale":"ok","operations":[]}"""))
        val client = OllamaLlmClient(OllamaSettings(model = "qwen3:14b", baseUrl = server.baseUrl, think = false))

        val answer = client.completeStructured(request)

        assertEquals("""{"rationale":"ok","operations":[]}""", answer)
        assertEquals("/api/chat", server.lastPath)
        val body = Json.parseToJsonElement(server.lastBody!!).jsonObject
        assertEquals("qwen3:14b", body["model"]!!.jsonPrimitive.content)
        assertEquals(JsonPrimitive(false), body["stream"])
        assertEquals(JsonPrimitive(false), body["think"])
        assertEquals(MutationBatchSchema.schema, body["format"])
        val options = body["options"]!!.jsonObject
        assertEquals(OllamaSettings.DEFAULT_CONTEXT_LENGTH, options["num_ctx"]!!.jsonPrimitive.int)
        assertEquals(4_000, options["num_predict"]!!.jsonPrimitive.int)
        assertTrue("temperature" !in options, "Unset options must be omitted")
        val messages = body["messages"]!!.jsonArray.map { it.jsonObject }
        assertEquals(listOf("system", "user"), messages.map { it["role"]!!.jsonPrimitive.content })
        assertTrue(messages[0]["content"]!!.jsonPrimitive.content.startsWith("System rules"))
        assertTrue("JSON schema" in messages[0]["content"]!!.jsonPrimitive.content)
    }

    @Test
    fun schemaCanBeLeftOutOfThePrompt() = runBlocking {
        val server = serve(200, chatResponse("{}"))
        val client = OllamaLlmClient(OllamaSettings(model = "m", baseUrl = server.baseUrl, includeSchemaInPrompt = false))

        client.completeStructured(request)

        val system = Json.parseToJsonElement(server.lastBody!!).jsonObject["messages"]!!.jsonArray[0].jsonObject
        assertEquals("System rules", system["content"]!!.jsonPrimitive.content)
    }

    @Test
    fun lengthStopRaisesTruncated() = runBlocking {
        val server = serve(200, chatResponse("""{"rationale":""", doneReason = "length"))
        val client = OllamaLlmClient(OllamaSettings(model = "m", baseUrl = server.baseUrl))

        assertFailsWith<LlmException.Truncated> { client.completeStructured(request) }
        Unit
    }

    @Test
    fun missingModelSuggestsPull() = runBlocking {
        val server = serve(404, """{"error":"model 'qwen3:14b' not found"}""")
        val client = OllamaLlmClient(OllamaSettings(model = "qwen3:14b", baseUrl = server.baseUrl))

        val error = assertFailsWith<LlmException.RequestRejected> { client.completeStructured(request) }
        assertTrue("ollama pull qwen3:14b" in error.message!!)
    }

    @Test
    fun serverErrorRaisesUnavailable() = runBlocking {
        val server = serve(500, """{"error":"out of memory"}""")
        val client = OllamaLlmClient(OllamaSettings(model = "m", baseUrl = server.baseUrl))

        val error = assertFailsWith<LlmException.Unavailable> { client.completeStructured(request) }
        assertTrue("out of memory" in error.message!!)
    }

    @Test
    fun stoppedServerRaisesUnavailable() = runBlocking {
        val freePort = ServerSocket(0).use { it.localPort }
        val client = OllamaLlmClient(OllamaSettings(model = "m", baseUrl = "http://127.0.0.1:$freePort"))

        val error = assertFailsWith<LlmException.Unavailable> { client.completeStructured(request) }
        assertTrue("ollama serve" in error.message!!)
    }

    @Test
    fun listsInstalledModels() = runBlocking {
        val server = serve(
            200,
            """
            {"models": [
              {"name": "qwen3:14b", "model": "qwen3:14b", "size": 9276198565,
               "details": {"family": "qwen3", "parameter_size": "14.8B", "quantization_level": "Q4_K_M"}},
              {"name": "llama3.1:8b", "model": "llama3.1:8b", "size": 4920753328}
            ]}
            """.trimIndent(),
        )
        val client = OllamaLlmClient(OllamaSettings(model = "qwen3:14b", baseUrl = server.baseUrl))

        val models = client.listModels()

        assertEquals("/api/tags", server.lastPath)
        assertEquals(listOf("qwen3:14b", "llama3.1:8b"), models.map { it.name })
        assertEquals("14.8B", models[0].parameterSize)
        assertEquals("Q4_K_M", models[0].quantization)
    }

    @Test
    fun worksEndToEndWithTheProposalService() = runBlocking {
        val answer = """
            {"rationale": "Sereth sirve a Varis.", "operations": [
              {"op": "add_node", "tempId": "new_sereth", "type": "NPC", "name": "Sereth", "summary": "Nigromante.",
               "description": "", "aliases": [], "tags": [], "properties": [], "visibility": "DM_ONLY"},
              {"op": "add_edge", "source": "new_sereth", "target": "npc_varis", "type": "SERVES",
               "description": "", "properties": [], "visibility": "DM_ONLY"}
            ]}
        """.trimIndent()
        val server = serve(200, chatResponse(answer))
        val service = MutationProposalService(OllamaLlmClient(OllamaSettings(model = "m", baseUrl = server.baseUrl)))

        val batch = service.propose(Fixtures.campaign, Fixtures.subgraph, "Varis contrata a un nigromante")

        assertEquals(2, batch.operations.size)
        assertIs<GraphMutation.AddNode>(batch.operations[0])
        val sentUserPrompt = Json.parseToJsonElement(server.lastBody!!).jsonObject["messages"]!!.jsonArray[1]
            .jsonObject["content"]!!.jsonPrimitive.content
        assertTrue("npc_varis" in sentUserPrompt)
        assertTrue(sentUserPrompt.contains("Varis contrata a un nigromante"))
        Unit
    }
}
