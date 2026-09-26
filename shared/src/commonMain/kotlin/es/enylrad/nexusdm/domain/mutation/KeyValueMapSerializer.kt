package es.enylrad.nexusdm.domain.mutation

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.Serializable

/** A single entry of a free-form string map, as exchanged with the LLM. */
@Serializable
data class KeyValue(val key: String, val value: String)

/**
 * Serializes a `Map<String, String>` as an array of `{"key": ..., "value": ...}` objects.
 *
 * LLM structured outputs require every object schema to declare its properties
 * (`additionalProperties: false`), so free-form maps cannot be sent as JSON objects.
 * Decoding also accepts a plain JSON object for leniency.
 */
object KeyValueMapSerializer : KSerializer<Map<String, String>> {

    private val listSerializer = ListSerializer(KeyValue.serializer())
    private val objectSerializer = MapSerializer(String.serializer(), String.serializer())

    override val descriptor: SerialDescriptor = listSerializer.descriptor

    override fun serialize(encoder: Encoder, value: Map<String, String>) {
        listSerializer.serialize(encoder, value.map { (key, entryValue) -> KeyValue(key, entryValue) })
    }

    override fun deserialize(decoder: Decoder): Map<String, String> {
        val jsonDecoder = decoder as? JsonDecoder
            ?: return listSerializer.deserialize(decoder).toMap()
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonArray -> jsonDecoder.json.decodeFromJsonElement(listSerializer, element).toMap()
            is JsonObject -> jsonDecoder.json.decodeFromJsonElement(objectSerializer, element)
            else -> throw SerializationException("Expected an array of key/value pairs or an object, got: $element")
        }
    }

    private fun List<KeyValue>.toMap(): Map<String, String> = associate { it.key to it.value }
}
