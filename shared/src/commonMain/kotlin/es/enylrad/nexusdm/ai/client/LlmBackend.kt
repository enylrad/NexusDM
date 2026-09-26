package es.enylrad.nexusdm.ai.client

/** Which LLM backend to use and how to reach it. */
sealed interface LlmBackendSettings {

    /** A model served by a local Ollama instance. */
    data class Ollama(
        val model: String,
        val baseUrl: String = DEFAULT_OLLAMA_URL,
        val contextLength: Int = DEFAULT_CONTEXT_LENGTH,
    ) : LlmBackendSettings

    /** Claude through the Anthropic API. A null [apiKey] uses the environment (ANTHROPIC_API_KEY). */
    data class Claude(
        val apiKey: String? = null,
        val model: String = DEFAULT_CLAUDE_MODEL,
    ) : LlmBackendSettings

    companion object {
        const val DEFAULT_OLLAMA_URL = "http://localhost:11434"
        const val DEFAULT_CONTEXT_LENGTH = 16_384
        const val DEFAULT_CLAUDE_MODEL = "claude-opus-5"
    }
}

/**
 * Platform entry point to the available LLM backends. The UI depends on this interface,
 * so it stays independent from the concrete clients.
 */
interface LlmBackendGateway {

    fun createClient(settings: LlmBackendSettings): LlmClient

    /**
     * Names of the models installed in the Ollama instance at [baseUrl].
     *
     * @throws LlmException when Ollama cannot be reached.
     */
    suspend fun listOllamaModels(baseUrl: String): List<String>
}
