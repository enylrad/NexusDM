package es.enylrad.nexusdm.ai.client.ollama

import es.enylrad.nexusdm.ai.client.LlmClient
import es.enylrad.nexusdm.ai.client.LlmException
import es.enylrad.nexusdm.ai.client.StructuredRequest
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpTimeoutException
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import kotlinx.coroutines.future.await
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** A model installed in the local Ollama instance. */
data class OllamaModel(
    val name: String,
    val sizeBytes: Long?,
    val parameterSize: String?,
    val quantization: String?,
)

/**
 * [LlmClient] backed by a local Ollama server.
 *
 * Structured outputs are enforced by passing the JSON schema as the `format` of the
 * `/api/chat` request, so the answer follows [StructuredRequest.outputSchema].
 */
class OllamaLlmClient(
    private val settings: OllamaSettings,
    private val httpClient: HttpClient = defaultHttpClient(),
) : LlmClient {

    private val baseUri: URI = URI.create(settings.baseUrl.trimEnd('/') + "/")

    override suspend fun completeStructured(request: StructuredRequest): String {
        val body = OllamaChatRequest(
            model = settings.model,
            messages = listOf(
                OllamaMessage(role = "system", content = systemPrompt(request)),
                OllamaMessage(role = "user", content = request.userPrompt),
            ),
            stream = false,
            format = request.outputSchema,
            options = OllamaOptions(
                numCtx = settings.contextLength,
                numPredict = request.maxOutputTokens,
                temperature = settings.temperature,
            ),
            think = settings.think,
            keepAlive = settings.keepAlive,
        )
        val httpRequest = HttpRequest.newBuilder(baseUri.resolve("api/chat"))
            .timeout(settings.requestTimeout.toJavaDuration())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json.encodeToString(OllamaChatRequest.serializer(), body)))
            .build()

        val response = decode(OllamaChatResponse.serializer(), send(httpRequest))
        if (response.doneReason == DONE_REASON_LENGTH) {
            throw LlmException.Truncated("The answer reached the output token limit (${request.maxOutputTokens} tokens)")
        }
        val content = response.message?.content
        if (content.isNullOrBlank()) throw LlmException.InvalidResponse("Ollama returned an empty answer")
        return content
    }

    /** Lists the models installed in Ollama (`ollama list`). */
    suspend fun listModels(): List<OllamaModel> {
        val httpRequest = HttpRequest.newBuilder(baseUri.resolve("api/tags"))
            .timeout(LIST_TIMEOUT.toJavaDuration())
            .GET()
            .build()
        return decode(OllamaTagsResponse.serializer(), send(httpRequest)).models.map { info ->
            OllamaModel(
                name = info.name,
                sizeBytes = info.size,
                parameterSize = info.details?.parameterSize,
                quantization = info.details?.quantizationLevel,
            )
        }
    }

    private fun systemPrompt(request: StructuredRequest): String =
        if (settings.includeSchemaInPrompt) {
            request.systemPrompt + "\n\nRespond only with a JSON object that follows this JSON schema:\n" + request.outputSchema
        } else {
            request.systemPrompt
        }

    /** Sends the request and returns the body of a successful response. */
    private suspend fun send(httpRequest: HttpRequest): String {
        val response = try {
            httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString()).await()
        } catch (e: HttpTimeoutException) {
            throw LlmException.Unavailable("Ollama did not answer within the timeout (${settings.baseUrl})", e)
        } catch (e: IOException) {
            throw LlmException.Unavailable("Ollama is not reachable at ${settings.baseUrl}. Is `ollama serve` running?", e)
        }
        val status = response.statusCode()
        if (status in 200..299) return response.body()

        val error = runCatching { json.decodeFromString(OllamaErrorResponse.serializer(), response.body()).error }
            .getOrElse { response.body() }
        throw when {
            status == 404 && "not found" in error ->
                LlmException.RequestRejected("Model '${settings.model}' is not installed. Run `ollama pull ${settings.model}`. ($error)")
            status >= 500 -> LlmException.Unavailable("Ollama error (HTTP $status): $error")
            else -> LlmException.RequestRejected("Ollama rejected the request (HTTP $status): $error")
        }
    }

    private fun <T> decode(deserializer: DeserializationStrategy<T>, body: String): T =
        try {
            json.decodeFromString(deserializer, body)
        } catch (e: SerializationException) {
            throw LlmException.InvalidResponse("Unexpected response from Ollama", e)
        }

    companion object {
        private const val DONE_REASON_LENGTH = "length"
        private val LIST_TIMEOUT = 10.seconds
        private val CONNECT_TIMEOUT = 5.seconds

        private val json = Json {
            ignoreUnknownKeys = true
            // Omit unset optional fields so Ollama applies its own defaults.
            explicitNulls = false
        }

        private fun defaultHttpClient(): HttpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT.toJavaDuration())
            .build()
    }
}
