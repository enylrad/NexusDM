package es.enylrad.nexusdm.ai.client.anthropic

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.errors.AnthropicException
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.errors.CredentialResolutionException
import com.anthropic.errors.NoCredentialsException
import com.anthropic.errors.PermissionDeniedException
import com.anthropic.errors.RateLimitException
import com.anthropic.errors.UnauthorizedException
import com.anthropic.models.messages.CacheControlEphemeral
import com.anthropic.models.messages.JsonOutputFormat
import com.anthropic.models.messages.Message
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.OutputConfig
import com.anthropic.models.messages.StopReason
import com.anthropic.models.messages.TextBlockParam
import com.anthropic.models.messages.ThinkingConfigAdaptive
import es.enylrad.nexusdm.ai.client.LlmClient
import es.enylrad.nexusdm.ai.client.LlmException
import es.enylrad.nexusdm.ai.client.StructuredRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * [LlmClient] backed by the Claude API through the official Anthropic Java SDK.
 *
 * The answer is constrained with structured outputs (`output_config.format`), so the
 * returned text is JSON that follows [StructuredRequest.outputSchema].
 */
class AnthropicLlmClient(
    private val settings: AnthropicSettings = AnthropicSettings(),
    /** Injected SDK client; when null, one is created from [settings] on first use. */
    sdkClient: AnthropicClient? = null,
) : LlmClient {

    // Created lazily so that missing credentials surface as an LlmException on the first request.
    private val client: AnthropicClient by lazy { sdkClient ?: createClient(settings) }

    override suspend fun completeStructured(request: StructuredRequest): String {
        val params = buildParams(request)
        val message = withContext(Dispatchers.IO) {
            try {
                client.messages().create(params)
            } catch (e: AnthropicException) {
                throw e.toLlmException()
            }
        }
        return readStructuredText(message)
    }

    internal fun buildParams(request: StructuredRequest): MessageCreateParams {
        val builder = MessageCreateParams.builder()
            .model(settings.model)
            .maxTokens(request.maxOutputTokens)
            .thinking(ThinkingConfigAdaptive.builder().build())
            // The system prompt is stable across calls, so it is marked for prompt caching.
            .systemOfTextBlockParams(
                listOf(
                    TextBlockParam.builder()
                        .text(request.systemPrompt)
                        .cacheControl(CacheControlEphemeral.builder().build())
                        .build(),
                ),
            )
            .addUserMessage(request.userPrompt)
            .outputConfig(
                OutputConfig.builder()
                    .effort(OutputConfig.Effort.of(settings.effort))
                    .format(JsonOutputFormat.builder().schema(request.outputSchema.toOutputSchema()).build())
                    .build(),
            )
        if (settings.useRefusalFallback) {
            // Server-side fallback: a declined request is retried on the recommended model.
            builder.putAdditionalHeader("anthropic-beta", REFUSAL_FALLBACK_BETA)
            builder.putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
        }
        return builder.build()
    }

    companion object {
        private const val REFUSAL_FALLBACK_BETA = "server-side-fallback-2026-07-01"

        private fun createClient(settings: AnthropicSettings): AnthropicClient =
            if (settings.apiKey.isNullOrBlank()) {
                AnthropicOkHttpClient.fromEnv()
            } else {
                AnthropicOkHttpClient.builder().apiKey(settings.apiKey).build()
            }

        /** Extracts the JSON answer, failing on refusals and truncated answers. */
        internal fun readStructuredText(message: Message): String {
            when (message.stopReason().orElse(null)) {
                StopReason.REFUSAL -> {
                    val details = message.stopDetails().orElse(null)
                    throw LlmException.Refused(
                        category = details?.category()?.orElse(null)?.asString(),
                        explanation = details?.explanation()?.orElse(null),
                    )
                }
                StopReason.MAX_TOKENS -> throw LlmException.Truncated("The answer reached the output token limit")
                StopReason.MODEL_CONTEXT_WINDOW_EXCEEDED -> throw LlmException.Truncated("The context window was exceeded")
                else -> Unit
            }
            val text = message.content()
                .mapNotNull { block -> block.text().orElse(null)?.text() }
                .joinToString(separator = "")
            if (text.isBlank()) throw LlmException.InvalidResponse("The model returned no text")
            return text
        }

        private fun AnthropicException.toLlmException(): LlmException = when (this) {
            is UnauthorizedException, is PermissionDeniedException ->
                LlmException.Authentication("The Claude API rejected the credentials", this)
            is NoCredentialsException, is CredentialResolutionException ->
                LlmException.Authentication("No Claude API credentials configured", this)
            is RateLimitException -> LlmException.RateLimited("Claude API rate limit reached", this)
            is AnthropicServiceException ->
                if (statusCode() >= 500) {
                    LlmException.Unavailable("Claude API error (HTTP ${statusCode()})", this)
                } else {
                    LlmException.RequestRejected("Claude API rejected the request (HTTP ${statusCode()}): $message", this)
                }
            else -> LlmException.Unavailable("Could not reach the Claude API: $message", this)
        }
    }
}
