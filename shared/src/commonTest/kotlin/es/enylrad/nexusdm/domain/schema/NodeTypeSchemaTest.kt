package es.enylrad.nexusdm.domain.schema

import es.enylrad.nexusdm.domain.model.NodeType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NodeTypeSchemaTest {

    @Test
    fun everyNodeTypeHasProperties() {
        NodeType.entries.forEach { type ->
            assertTrue(NodeTypeSchema.propertiesFor(type).isNotEmpty(), "No schema for $type")
        }
    }

    @Test
    fun keysAreUniquePerType() {
        NodeType.entries.forEach { type ->
            val keys = NodeTypeSchema.propertiesFor(type).map { it.key }
            assertEquals(keys.size, keys.toSet().size, "Duplicate keys for $type")
        }
    }

    @Test
    fun enumPropertiesDeclareValues() {
        NodeType.entries.flatMap(NodeTypeSchema::propertiesFor)
            .filter { it.valueType == PropertyValueType.ENUM }
            .forEach { assertTrue(it.allowedValues.isNotEmpty(), "Enum ${it.key} has no values") }
    }

    @Test
    fun looksUpSpecByKey() {
        val sessionNumber = assertNotNull(NodeTypeSchema.specFor(NodeType.SESSION, "session_number"))
        assertEquals(PropertyValueType.INTEGER, sessionNumber.valueType)
        assertTrue(sessionNumber.required)
        assertNull(NodeTypeSchema.specFor(NodeType.SESSION, "free_form_key"))
    }
}
