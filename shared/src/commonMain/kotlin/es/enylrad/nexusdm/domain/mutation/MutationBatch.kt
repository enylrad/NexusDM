package es.enylrad.nexusdm.domain.mutation

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Root object the LLM must return. Operations are applied in order, atomically:
 * either the whole batch is applied or none of it.
 */
@Serializable
data class MutationBatch(
    /** Short explanation of the proposed changes, shown to the DM for review. */
    val rationale: String,
    val operations: List<GraphMutation>,
)

/** JSON configuration shared by everything that reads or writes [MutationBatch]es. */
object MutationJson {
    const val DISCRIMINATOR = "op"

    val json: Json = Json {
        classDiscriminator = DISCRIMINATOR
        // LLMs sometimes add extra fields; ignore them instead of failing the whole batch.
        ignoreUnknownKeys = true
        explicitNulls = false
        prettyPrint = false
    }

    fun decode(text: String): MutationBatch = json.decodeFromString(MutationBatch.serializer(), text)

    fun encode(batch: MutationBatch): String = json.encodeToString(MutationBatch.serializer(), batch)
}
