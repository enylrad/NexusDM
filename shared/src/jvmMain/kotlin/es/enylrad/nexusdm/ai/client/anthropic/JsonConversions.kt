package es.enylrad.nexusdm.ai.client.anthropic

import com.anthropic.core.JsonValue
import com.anthropic.models.messages.JsonOutputFormat
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/** Converts a kotlinx JSON schema into the SDK's output format schema. */
internal fun JsonObject.toOutputSchema(): JsonOutputFormat.Schema {
    val builder = JsonOutputFormat.Schema.builder()
    forEach { (key, value) -> builder.putAdditionalProperty(key, JsonValue.from(value.toPlainValue())) }
    return builder.build()
}

/** Converts a kotlinx [JsonElement] into plain Kotlin values (maps, lists, primitives) accepted by [JsonValue.from]. */
internal fun JsonElement.toPlainValue(): Any? = when (this) {
    is JsonNull -> null
    is JsonObject -> mapValues { (_, value) -> value.toPlainValue() }
    is JsonArray -> map { it.toPlainValue() }
    is JsonPrimitive -> when {
        isString -> content
        else -> booleanOrNull ?: longOrNull ?: doubleOrNull ?: content
    }
}
