package es.enylrad.nexusdm.ai.client

import es.enylrad.nexusdm.ai.client.anthropic.AnthropicLlmClient
import es.enylrad.nexusdm.ai.client.anthropic.AnthropicSettings
import es.enylrad.nexusdm.ai.client.ollama.OllamaLlmClient
import es.enylrad.nexusdm.ai.client.ollama.OllamaSettings

/** Desktop implementation of [LlmBackendGateway] backed by the Claude and Ollama clients. */
class JvmLlmBackendGateway : LlmBackendGateway {

    override fun createClient(settings: LlmBackendSettings): LlmClient = when (settings) {
        is LlmBackendSettings.Ollama -> OllamaLlmClient(settings.toOllamaSettings())
        is LlmBackendSettings.Claude -> AnthropicLlmClient(
            AnthropicSettings(apiKey = settings.apiKey?.takeIf { it.isNotBlank() }, model = settings.model),
        )
    }

    override suspend fun listOllamaModels(baseUrl: String): List<String> =
        // The model is irrelevant for listing; a placeholder satisfies the settings validation.
        OllamaLlmClient(OllamaSettings(model = LISTING_PLACEHOLDER_MODEL, baseUrl = baseUrl))
            .listModels()
            .map { it.name }

    private fun LlmBackendSettings.Ollama.toOllamaSettings() = OllamaSettings(
        model = model,
        baseUrl = baseUrl,
        contextLength = contextLength,
    )

    private companion object {
        const val LISTING_PLACEHOLDER_MODEL = "unused"
    }
}
