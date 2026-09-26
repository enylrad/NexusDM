package es.enylrad.nexusdm.ai.client.anthropic

/** Configuration of the Claude API client. */
data class AnthropicSettings(
    /**
     * API key. When null, credentials are resolved by the SDK from the environment
     * (ANTHROPIC_API_KEY, or a profile created with `ant auth login`).
     */
    val apiKey: String? = null,
    val model: String = DEFAULT_MODEL,
    /** Reasoning effort: "low", "medium", "high", "xhigh" or "max". */
    val effort: String = DEFAULT_EFFORT,
    /** Retry declined requests on Anthropic's recommended fallback model, server-side. */
    val useRefusalFallback: Boolean = true,
) {
    companion object {
        const val DEFAULT_MODEL = "claude-opus-5"
        const val DEFAULT_EFFORT = "high"
    }
}
