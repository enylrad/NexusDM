package es.enylrad.nexusdm.ai

import es.enylrad.nexusdm.ai.client.LlmClient
import es.enylrad.nexusdm.ai.client.LlmException
import es.enylrad.nexusdm.ai.client.StructuredRequest
import es.enylrad.nexusdm.ai.context.MutationPromptBuilder
import es.enylrad.nexusdm.ai.schema.MutationBatchSchema
import es.enylrad.nexusdm.domain.graph.Subgraph
import es.enylrad.nexusdm.domain.model.Campaign
import es.enylrad.nexusdm.domain.mutation.MutationBatch
import es.enylrad.nexusdm.domain.mutation.MutationJson
import kotlinx.serialization.SerializationException

/**
 * Turns a DM request into a proposed [MutationBatch], using only the given [Subgraph] as context.
 *
 * The returned batch is a proposal: it still has to be validated against the graph and
 * reviewed by the DM before being applied.
 */
class MutationProposalService(
    private val llmClient: LlmClient,
    private val promptBuilder: MutationPromptBuilder = MutationPromptBuilder(),
) {

    /** @throws LlmException when the model fails or returns an unusable answer. */
    suspend fun propose(campaign: Campaign, subgraph: Subgraph, request: String): MutationBatch {
        require(request.isNotBlank()) { "The DM request must not be blank" }
        val structuredRequest = StructuredRequest(
            systemPrompt = promptBuilder.systemPrompt,
            userPrompt = promptBuilder.userPrompt(campaign, subgraph, request),
            outputSchema = MutationBatchSchema.schema,
        )
        val answer = llmClient.completeStructured(structuredRequest)
        return try {
            MutationJson.decode(answer)
        } catch (e: SerializationException) {
            throw LlmException.InvalidResponse("The model answer is not a valid mutation batch", e)
        }
    }
}
