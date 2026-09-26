package es.enylrad.nexusdm.ai.client

import es.enylrad.nexusdm.ai.client.anthropic.AnthropicLlmClient
import es.enylrad.nexusdm.ai.client.anthropic.AnthropicSettings
import es.enylrad.nexusdm.ai.client.ollama.OllamaLlmClient
import es.enylrad.nexusdm.ai.client.ollama.OllamaSettings

/** Available LLM backends and their configuration. */
sealed interface LlmProvider {

    /** Claude through the Anthropic API (cloud). */
    data class Claude(val settings: AnthropicSettings = AnthropicSettings()) : LlmProvider

    /** A model served by a local Ollama instance. */
    data class Ollama(val settings: OllamaSettings) : LlmProvider

    fun createClient(): LlmClient = when (this) {
        is Claude -> AnthropicLlmClient(settings)
        is Ollama -> OllamaLlmClient(settings)
    }
}
