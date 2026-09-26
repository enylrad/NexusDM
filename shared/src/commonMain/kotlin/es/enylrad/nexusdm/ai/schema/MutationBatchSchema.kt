package es.enylrad.nexusdm.ai.schema

import es.enylrad.nexusdm.domain.model.EdgeType
import es.enylrad.nexusdm.domain.model.NodeType
import es.enylrad.nexusdm.domain.model.Visibility
import es.enylrad.nexusdm.domain.mutation.MutationJson
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * JSON schema of [es.enylrad.nexusdm.domain.mutation.MutationBatch], used to constrain the
 * LLM answer. It must stay in sync with the serialized form produced by [MutationJson]:
 * operations are discriminated by "op" and free-form maps travel as key/value arrays.
 *
 * Every object declares `additionalProperties: false`, as required by structured outputs.
 */
object MutationBatchSchema {

    val schema: JsonObject by lazy { buildSchema() }

    private fun buildSchema(): JsonObject = objectSchema(
        properties = mapOf(
            "rationale" to stringSchema("Short explanation of the proposed changes for the DM, in the campaign language."),
            "operations" to arraySchema(
                items = buildJsonObject {
                    put("anyOf", JsonArray(operations()))
                },
                description = "Ordered list of graph changes. Empty when nothing should change.",
            ),
        ),
        required = listOf("rationale", "operations"),
    )

    private fun operations(): List<JsonObject> = listOf(
        operation(
            op = "add_node",
            description = "Create a new node.",
            properties = mapOf(
                "tempId" to stringSchema("Temporary id, unique in this batch, used by later operations to reference the new node."),
                "type" to enumSchema(NodeType.entries.map { it.name }),
                "name" to stringSchema(),
                "summary" to stringSchema("One or two sentences."),
                "description" to stringSchema("Long-form text. Empty string when not needed."),
                "aliases" to stringArraySchema(),
                "tags" to stringArraySchema(),
                "properties" to keyValueArraySchema(),
                "visibility" to enumSchema(Visibility.entries.map { it.name }),
            ),
            required = listOf("tempId", "type", "name", "summary", "description", "aliases", "tags", "properties", "visibility"),
        ),
        operation(
            op = "update_node",
            description = "Partially update an existing node. Omitted patch fields stay unchanged.",
            properties = mapOf(
                "id" to stringSchema("Id of an existing node from the context."),
                "patch" to objectSchema(
                    properties = mapOf(
                        "name" to stringSchema(),
                        "summary" to stringSchema(),
                        "description" to stringSchema(),
                        "visibility" to enumSchema(Visibility.entries.map { it.name }),
                        "addAliases" to stringArraySchema(),
                        "removeAliases" to stringArraySchema(),
                        "addTags" to stringArraySchema(),
                        "removeTags" to stringArraySchema(),
                        "setProperties" to keyValueArraySchema(),
                        "removeProperties" to stringArraySchema(),
                    ),
                    required = emptyList(),
                ),
            ),
            required = listOf("id", "patch"),
        ),
        operation(
            op = "remove_node",
            description = "Delete an existing node and all its edges.",
            properties = mapOf(
                "id" to stringSchema("Id of an existing node from the context."),
                "reason" to stringSchema(),
            ),
            required = listOf("id", "reason"),
        ),
        operation(
            op = "merge_nodes",
            description = "Merge two existing nodes that describe the same entity.",
            properties = mapOf(
                "keepId" to stringSchema("Node that survives."),
                "mergeId" to stringSchema("Node absorbed into keepId and then deleted."),
                "reason" to stringSchema(),
            ),
            required = listOf("keepId", "mergeId", "reason"),
        ),
        operation(
            op = "add_edge",
            description = "Create a relationship between two nodes.",
            properties = mapOf(
                "source" to stringSchema("Existing node id or tempId of a node added earlier in this batch."),
                "target" to stringSchema("Existing node id or tempId of a node added earlier in this batch."),
                "type" to enumSchema(EdgeType.entries.map { it.name }),
                "description" to stringSchema("Nuance of the relationship. Empty string when not needed."),
                "properties" to keyValueArraySchema(),
                "visibility" to enumSchema(Visibility.entries.map { it.name }),
            ),
            required = listOf("source", "target", "type", "description", "properties", "visibility"),
        ),
        operation(
            op = "update_edge",
            description = "Partially update an existing edge. Omitted patch fields stay unchanged.",
            properties = mapOf(
                "id" to stringSchema("Id of an existing edge from the context."),
                "patch" to objectSchema(
                    properties = mapOf(
                        "type" to enumSchema(EdgeType.entries.map { it.name }),
                        "description" to stringSchema(),
                        "visibility" to enumSchema(Visibility.entries.map { it.name }),
                        "setProperties" to keyValueArraySchema(),
                        "removeProperties" to stringArraySchema(),
                    ),
                    required = emptyList(),
                ),
            ),
            required = listOf("id", "patch"),
        ),
        operation(
            op = "remove_edge",
            description = "Delete an existing edge.",
            properties = mapOf(
                "id" to stringSchema("Id of an existing edge from the context."),
                "reason" to stringSchema(),
            ),
            required = listOf("id", "reason"),
        ),
    )

    private fun operation(
        op: String,
        description: String,
        properties: Map<String, JsonObject>,
        required: List<String>,
    ): JsonObject = objectSchema(
        properties = mapOf(MutationJson.DISCRIMINATOR to constSchema(op)) + properties,
        required = listOf(MutationJson.DISCRIMINATOR) + required,
        description = description,
    )

    private fun objectSchema(
        properties: Map<String, JsonObject>,
        required: List<String>,
        description: String? = null,
    ): JsonObject = buildJsonObject {
        put("type", "object")
        description?.let { put("description", it) }
        put("properties", JsonObject(properties))
        put("required", JsonArray(required.map(::JsonPrimitive)))
        put("additionalProperties", false)
    }

    private fun stringSchema(description: String? = null): JsonObject = buildJsonObject {
        put("type", "string")
        description?.let { put("description", it) }
    }

    private fun constSchema(value: String): JsonObject = buildJsonObject {
        put("type", "string")
        put("const", value)
    }

    private fun enumSchema(values: List<String>): JsonObject = buildJsonObject {
        put("type", "string")
        put("enum", JsonArray(values.map(::JsonPrimitive)))
    }

    private fun arraySchema(items: JsonObject, description: String? = null): JsonObject = buildJsonObject {
        put("type", "array")
        description?.let { put("description", it) }
        put("items", items)
    }

    private fun stringArraySchema(): JsonObject = arraySchema(stringSchema())

    private fun keyValueArraySchema(): JsonObject = arraySchema(
        items = objectSchema(
            properties = mapOf("key" to stringSchema(), "value" to stringSchema()),
            required = listOf("key", "value"),
        ),
        description = "Free-form properties as key/value pairs. Prefer the well-known keys of the node type.",
    )
}
