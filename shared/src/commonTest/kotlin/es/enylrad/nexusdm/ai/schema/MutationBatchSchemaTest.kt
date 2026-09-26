package es.enylrad.nexusdm.ai.schema

import es.enylrad.nexusdm.domain.model.EdgeId
import es.enylrad.nexusdm.domain.model.EdgeType
import es.enylrad.nexusdm.domain.model.NodeId
import es.enylrad.nexusdm.domain.model.NodeType
import es.enylrad.nexusdm.domain.model.Visibility
import es.enylrad.nexusdm.domain.mutation.EdgePatch
import es.enylrad.nexusdm.domain.mutation.GraphMutation
import es.enylrad.nexusdm.domain.mutation.MutationBatch
import es.enylrad.nexusdm.domain.mutation.MutationJson
import es.enylrad.nexusdm.domain.mutation.NodePatch
import es.enylrad.nexusdm.domain.mutation.NodeRef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class MutationBatchSchemaTest {

    private val schema = MutationBatchSchema.schema

    // One of each operation, with every field filled in.
    private val fullBatch = MutationBatch(
        rationale = "Everything at once.",
        operations = listOf(
            GraphMutation.AddNode(
                tempId = "new_1", type = NodeType.NPC, name = "Sereth", summary = "Necromancer.",
                description = "Long lore.", aliases = setOf("The Pale"), tags = setOf("villain"),
                properties = mapOf("attitude" to "hostile"), visibility = Visibility.DM_ONLY,
            ),
            GraphMutation.UpdateNode(
                id = NodeId("npc_1"),
                patch = NodePatch(
                    name = "n", summary = "s", description = "d", visibility = Visibility.REVEALED,
                    addAliases = setOf("a"), removeAliases = setOf("b"), addTags = setOf("c"), removeTags = setOf("d"),
                    setProperties = mapOf("k" to "v"), removeProperties = setOf("old"),
                ),
            ),
            GraphMutation.RemoveNode(NodeId("npc_2"), reason = "Retcon."),
            GraphMutation.MergeNodes(NodeId("npc_3"), NodeId("npc_4"), reason = "Same person."),
            GraphMutation.AddEdge(
                source = NodeRef("new_1"), target = NodeRef("faction_1"), type = EdgeType.SERVES,
                description = "Paid.", properties = mapOf("since" to "the siege"), visibility = Visibility.DM_ONLY,
            ),
            GraphMutation.UpdateEdge(
                id = EdgeId("edge_1"),
                patch = EdgePatch(
                    type = EdgeType.ENEMY_OF, description = "Betrayed.", visibility = Visibility.REVEALED,
                    setProperties = mapOf("k" to "v"), removeProperties = setOf("old"),
                ),
            ),
            GraphMutation.RemoveEdge(EdgeId("edge_2"), reason = "Broken alliance."),
        ),
    )

    @Test
    fun everyObjectForbidsAdditionalProperties() {
        forEachObjectSchema(schema) { objectSchema ->
            assertEquals(JsonPrimitive(false), objectSchema["additionalProperties"], "Missing additionalProperties=false in $objectSchema")
            val properties = objectSchema["properties"]!!.jsonObject.keys
            val required = objectSchema["required"]!!.jsonArray.map { it.jsonPrimitive.content }
            assertTrue(properties.containsAll(required), "Required keys $required not all declared in $properties")
        }
    }

    @Test
    fun operationsMatchSerialNames() {
        val serialNames = GraphMutation.serializer().descriptor.getElementDescriptor(1).elementNames.toSet()
        val schemaOps = operationSchemas().map { it["properties"]!!.jsonObject[MutationJson.DISCRIMINATOR]!!.jsonObject["const"]!!.jsonPrimitive.content }

        assertEquals(serialNames, schemaOps.toSet())
    }

    @Test
    fun serializedBatchConformsToSchema() {
        // Encode with defaults so that every field the model must send is present.
        val json = Json(from = MutationJson.json) { encodeDefaults = true }
        val encoded = json.encodeToJsonElement(MutationBatch.serializer(), fullBatch)

        val errors = validate(encoded, schema, path = "$")

        assertEquals(emptyList(), errors)
    }

    @Test
    fun schemaConformingAnswerDecodes() {
        val json = Json(from = MutationJson.json) { encodeDefaults = true }
        val answer = json.encodeToString(MutationBatch.serializer(), fullBatch)

        assertEquals(fullBatch, MutationJson.decode(answer))
    }

    private fun operationSchemas(): List<JsonObject> =
        schema["properties"]!!.jsonObject["operations"]!!.jsonObject["items"]!!.jsonObject["anyOf"]!!.jsonArray.map { it.jsonObject }

    private fun forEachObjectSchema(element: JsonElement, action: (JsonObject) -> Unit) {
        when (element) {
            is JsonObject -> {
                if (element["type"] == JsonPrimitive("object")) action(element)
                element.values.forEach { forEachObjectSchema(it, action) }
            }
            is JsonArray -> element.forEach { forEachObjectSchema(it, action) }
            else -> Unit
        }
    }

    /** Minimal validator for the JSON schema subset used by [MutationBatchSchema]. */
    private fun validate(value: JsonElement, schema: JsonObject, path: String): List<String> {
        schema["anyOf"]?.let { options ->
            val matches = options.jsonArray.any { validate(value, it.jsonObject, path).isEmpty() }
            return if (matches) emptyList() else listOf("$path matches no anyOf option")
        }
        val errors = mutableListOf<String>()
        when (schema["type"]?.jsonPrimitive?.content) {
            "object" -> {
                if (value !is JsonObject) return listOf("$path is not an object")
                val properties = schema["properties"]!!.jsonObject
                schema["required"]!!.jsonArray.map { it.jsonPrimitive.content }
                    .filter { it !in value }
                    .forEach { errors += "$path.$it is required" }
                value.forEach { (key, child) ->
                    val childSchema = properties[key]?.jsonObject
                    if (childSchema == null) errors += "$path.$key is not allowed" else errors += validate(child, childSchema, "$path.$key")
                }
            }
            "array" -> {
                if (value !is JsonArray) return listOf("$path is not an array")
                value.forEachIndexed { index, child -> errors += validate(child, schema["items"]!!.jsonObject, "$path[$index]") }
            }
            "string" -> {
                if (value !is JsonPrimitive || !value.isString) return listOf("$path is not a string")
                schema["const"]?.let { if (it.jsonPrimitive.content != value.content) errors += "$path must be ${it.jsonPrimitive.content}" }
                schema["enum"]?.let { allowed ->
                    if (allowed.jsonArray.none { it.jsonPrimitive.content == value.content }) errors += "$path has invalid value ${value.content}"
                }
            }
        }
        return errors
    }
}
