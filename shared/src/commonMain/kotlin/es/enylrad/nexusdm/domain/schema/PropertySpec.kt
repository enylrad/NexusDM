package es.enylrad.nexusdm.domain.schema

import kotlinx.serialization.Serializable

/**
 * Describes one well-known property key of a node type. Used to validate values,
 * build editor forms and tell the LLM which keys it should use.
 */
@Serializable
data class PropertySpec(
    val key: String,
    val valueType: PropertyValueType,
    val description: String,
    /** Accepted values when [valueType] is [PropertyValueType.ENUM]. */
    val allowedValues: List<String> = emptyList(),
    val required: Boolean = false,
)

@Serializable
enum class PropertyValueType {
    TEXT,
    INTEGER,
    BOOLEAN,

    /** Real-world date in ISO format (yyyy-MM-dd). In-world dates are stored as TEXT. */
    DATE,
    ENUM,
}
