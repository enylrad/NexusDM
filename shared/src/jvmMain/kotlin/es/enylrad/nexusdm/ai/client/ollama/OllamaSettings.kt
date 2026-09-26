package es.enylrad.nexusdm.ai.client.ollama

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/** Configuration of the local Ollama client. */
data class OllamaSettings(
    /** Name of an installed model, as shown by `ollama list` (e.g. "qwen3:14b"). */
    val model: String,
    val baseUrl: String = DEFAULT_BASE_URL,
    /**
     * Context window requested from Ollama (`num_ctx`). Ollama's default is small and silently
     * drops the start of long prompts, so it is set explicitly. Must fit the system prompt,
     * the rendered subgraph and the answer.
     */
    val contextLength: Int = DEFAULT_CONTEXT_LENGTH,
    /** Sampling temperature; null keeps the model default. */
    val temperature: Double? = null,
    /** Enables or disables reasoning on thinking models; null keeps the model default. */
    val think: Boolean? = null,
    /** How long Ollama keeps the model loaded after the request (e.g. "10m"); null keeps the default. */
    val keepAlive: String? = null,
    /** Local models can be slow on long prompts, so the timeout is generous. */
    val requestTimeout: Duration = 10.minutes,
    /**
     * Also writes the JSON schema into the system prompt. Ollama enforces the schema through
     * `format`, but smaller models produce better content when they can read it too.
     */
    val includeSchemaInPrompt: Boolean = true,
) {
    init {
        require(model.isNotBlank()) { "The Ollama model name must not be blank" }
        require(contextLength > 0) { "contextLength must be > 0" }
    }

    companion object {
        const val DEFAULT_BASE_URL = "http://localhost:11434"
        const val DEFAULT_CONTEXT_LENGTH = 16_384
    }
}
