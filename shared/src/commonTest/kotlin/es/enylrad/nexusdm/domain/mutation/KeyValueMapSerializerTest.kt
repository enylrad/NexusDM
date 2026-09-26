package es.enylrad.nexusdm.domain.mutation

import es.enylrad.nexusdm.domain.model.NodeId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.SerializationException

class KeyValueMapSerializerTest {

    @Test
    fun encodesMapsAsKeyValueArrays() {
        val batch = MutationBatch(
            rationale = "Level up.",
            operations = listOf(
                GraphMutation.UpdateNode(NodeId("pc_1"), NodePatch(setProperties = mapOf("level" to "6"))),
            ),
        )

        val text = MutationJson.encode(batch)

        assertEquals(
            """{"rationale":"Level up.","operations":[{"op":"update_node","id":"pc_1","patch":{"setProperties":[{"key":"level","value":"6"}]}}]}""",
            text,
        )
    }

    @Test
    fun decodesKeyValueArrays() {
        val text = """
            {"rationale": "r", "operations": [
              {"op": "update_node", "id": "pc_1", "patch": {"setProperties": [{"key": "level", "value": "6"}, {"key": "status", "value": "alive"}]}}
            ]}
        """.trimIndent()

        val update = MutationJson.decode(text).operations.single() as GraphMutation.UpdateNode

        assertEquals(mapOf("level" to "6", "status" to "alive"), update.patch.setProperties)
    }

    @Test
    fun rejectsInvalidMapShapes() {
        val text = """{"rationale": "r", "operations": [{"op": "update_node", "id": "pc_1", "patch": {"setProperties": "level=6"}}]}"""

        assertFailsWith<SerializationException> { MutationJson.decode(text) }
    }
}
