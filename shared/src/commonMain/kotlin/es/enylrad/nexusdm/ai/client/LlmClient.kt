package es.enylrad.nexusdm.ai.client

import kotlinx.serialization.json.JsonObject

/**
 * Provider-agnostic access to a language model. Implementations exist per provider
 * (e.g. the Claude API); a local model backend can be plugged in behind the same interface.
 */
interface LlmClient {

    /**
     * Sends [request] and returns the raw JSON text produced by the model, which must
     * conform to [StructuredRequest.outputSchema].
     *
     * @throws LlmException when the model cannot produce a usable answer.
     */
    suspend fun completeStructured(request: StructuredRequest): String
}

/** A single-turn request whose answer is constrained to a JSON schema. */
data class StructuredRequest(
    /** Stable instructions. Kept identical across calls so providers can cache them. */
    val systemPrompt: String,
    /** Per-call content: campaign context, subgraph and the DM request. */
    val userPrompt: String,
    /** JSON schema the answer must follow. */
    val outputSchema: JsonObject,
    val maxOutputTokens: Long = DEFAULT_MAX_OUTPUT_TOKENS,
) {
    companion object {
        const val DEFAULT_MAX_OUTPUT_TOKENS = 16_000L
    }
}

/** Failures reported by an [LlmClient], grouped by how the caller should react. */
sealed class LlmException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** Missing or invalid credentials. The user must fix the configuration. */
    class Authentication(message: String, cause: Throwable? = null) : LlmException(message, cause)

    /** Too many requests. Retry later. */
    class RateLimited(message: String, cause: Throwable? = null) : LlmException(message, cause)

    /** Network failure or provider-side error. Retrying may succeed. */
    class Unavailable(message: String, cause: Throwable? = null) : LlmException(message, cause)

    /** The provider rejected the request itself (invalid parameters, unknown model...). */
    class RequestRejected(message: String, cause: Throwable? = null) : LlmException(message, cause)

    /** The model declined to answer. */
    class Refused(val category: String?, val explanation: String?) :
        LlmException("The model declined the request" + (category?.let { " ($it)" } ?: ""))

    /** The answer was cut off (output token limit or context window reached). */
    class Truncated(message: String) : LlmException(message)

    /** The answer could not be parsed into the expected structure. */
    class InvalidResponse(message: String, cause: Throwable? = null) : LlmException(message, cause)
}
