package es.enylrad.nexusdm.ai.client.ollama

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// Wire types of the Ollama REST API (https://github.com/ollama/ollama/blob/main/docs/api.md).

@Serializable
internal data class OllamaChatRequest(
    val model: String,
    val messages: List<OllamaMessage>,
    /** Must be false: the client reads a single JSON response. */
    val stream: Boolean,
    /** JSON schema the answer must follow (structured outputs). */
    val format: JsonObject? = null,
    val options: OllamaOptions? = null,
    val think: Boolean? = null,
    @SerialName("keep_alive") val keepAlive: String? = null,
)

@Serializable
internal data class OllamaMessage(
    val role: String,
    val content: String,
    /** Reasoning text returned by thinking models; ignored by this client. */
    val thinking: String? = null,
)

@Serializable
internal data class OllamaOptions(
    @SerialName("num_ctx") val numCtx: Int? = null,
    @SerialName("num_predict") val numPredict: Long? = null,
    val temperature: Double? = null,
)

@Serializable
internal data class OllamaChatResponse(
    val model: String? = null,
    val message: OllamaMessage? = null,
    val done: Boolean = false,
    @SerialName("done_reason") val doneReason: String? = null,
    @SerialName("prompt_eval_count") val promptEvalCount: Long? = null,
    @SerialName("eval_count") val evalCount: Long? = null,
)

@Serializable
internal data class OllamaErrorResponse(
    val error: String,
)

@Serializable
internal data class OllamaTagsResponse(
    val models: List<OllamaModelInfo> = emptyList(),
)

@Serializable
internal data class OllamaModelInfo(
    val name: String,
    val size: Long? = null,
    val details: OllamaModelDetails? = null,
)

@Serializable
internal data class OllamaModelDetails(
    val family: String? = null,
    @SerialName("parameter_size") val parameterSize: String? = null,
    @SerialName("quantization_level") val quantizationLevel: String? = null,
)
