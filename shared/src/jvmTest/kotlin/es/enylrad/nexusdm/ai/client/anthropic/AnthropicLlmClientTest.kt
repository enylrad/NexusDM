package es.enylrad.nexusdm.ai.client.anthropic

import com.anthropic.core.JsonValue
import com.anthropic.core.jsonMapper
import com.anthropic.models.messages.Message
import com.anthropic.models.messages.OutputConfig
import es.enylrad.nexusdm.ai.client.LlmException
import es.enylrad.nexusdm.ai.client.StructuredRequest
import es.enylrad.nexusdm.ai.schema.MutationBatchSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AnthropicLlmClientTest {

    private val request = StructuredRequest(
        systemPrompt = "System",
        userPrompt = "User",
        outputSchema = MutationBatchSchema.schema,
    )

    @Test
    fun buildsStructuredOutputRequest() {
        val client = AnthropicLlmClient(AnthropicSettings(apiKey = "test-key", model = "claude-opus-5", effort = "medium"))

        val params = client.buildParams(request)

        assertEquals("claude-opus-5", params.model().asString())
        val outputConfig = params.outputConfig().get()
        assertEquals(OutputConfig.Effort.MEDIUM, outputConfig.effort().get())
        val schema = outputConfig.format().get().schema()._additionalProperties()
        assertEquals(JsonValue.from("object"), schema["type"])
        assertEquals(JsonValue.from(false), schema["additionalProperties"])
        assertEquals(listOf("server-side-fallback-2026-07-01"), params._headers().values("anthropic-beta"))
        assertEquals(JsonValue.from("default"), params._additionalBodyProperties()["fallbacks"])
    }

    @Test
    fun fallbackCanBeDisabled() {
        val client = AnthropicLlmClient(AnthropicSettings(apiKey = "test-key", useRefusalFallback = false))

        val params = client.buildParams(request)

        assertTrue(params._headers().values("anthropic-beta").isEmpty())
        assertTrue("fallbacks" !in params._additionalBodyProperties())
    }

    @Test
    fun readsTextOfSuccessfulAnswer() {
        val message = message(stopReason = "end_turn", content = """[{"type": "text", "text": "{\"rationale\":\"ok\",\"operations\":[]}"}]""")

        assertEquals("""{"rationale":"ok","operations":[]}""", AnthropicLlmClient.readStructuredText(message))
    }

    @Test
    fun refusalRaisesRefused() {
        val message = message(
            stopReason = "refusal",
            content = "[]",
            extra = """, "stop_details": {"type": "refusal", "category": "cyber", "explanation": "Declined."}""",
        )

        val error = assertFailsWith<LlmException.Refused> { AnthropicLlmClient.readStructuredText(message) }
        assertEquals("cyber", error.category)
        assertEquals("Declined.", error.explanation)
    }

    @Test
    fun truncatedAnswerRaisesTruncated() {
        val message = message(stopReason = "max_tokens", content = """[{"type": "text", "text": "{\"rationale\":"}]""")

        assertFailsWith<LlmException.Truncated> { AnthropicLlmClient.readStructuredText(message) }
    }

    @Test
    fun convertsKotlinxJsonToPlainValues() {
        val element = buildJsonObject {
            put("type", "object")
            put("additionalProperties", false)
            put("count", 3)
            put("list", buildJsonArray { add(JsonPrimitive("a")) })
        }

        assertEquals(
            mapOf("type" to "object", "additionalProperties" to false, "count" to 3L, "list" to listOf("a")),
            element.toPlainValue(),
        )
    }

    private fun message(stopReason: String, content: String, extra: String = ""): Message =
        jsonMapper().readValue(
            """
            {"id": "msg_1", "type": "message", "role": "assistant", "model": "claude-opus-5",
             "content": $content, "stop_reason": "$stopReason", "stop_sequence": null,
             "usage": {"input_tokens": 10, "output_tokens": 5}$extra}
            """.trimIndent(),
            Message::class.java,
        )
}
