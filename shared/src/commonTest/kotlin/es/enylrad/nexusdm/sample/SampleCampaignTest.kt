package es.enylrad.nexusdm.sample

import es.enylrad.nexusdm.domain.schema.NodeTypeSchema
import es.enylrad.nexusdm.domain.schema.PropertyValueType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SampleCampaignTest {

    private val nodesById = SampleCampaign.nodes.associateBy { it.id }

    @Test
    fun idsAreUnique() {
        assertEquals(SampleCampaign.nodes.size, nodesById.size)
        assertEquals(SampleCampaign.edges.size, SampleCampaign.edges.map { it.id }.toSet().size)
    }

    @Test
    fun edgesConnectExistingNodesWithValidTypes() {
        SampleCampaign.edges.forEach { edge ->
            val source = assertNotNull(nodesById[edge.sourceId], "Unknown source in ${edge.id}")
            val target = assertNotNull(nodesById[edge.targetId], "Unknown target in ${edge.id}")
            assertTrue(edge.type.accepts(source.type, target.type), "${edge.type} cannot link ${source.type} -> ${target.type}")
        }
    }

    @Test
    fun wellKnownPropertiesHaveValidValues() {
        SampleCampaign.nodes.forEach { node ->
            node.properties.forEach { (key, value) ->
                val spec = NodeTypeSchema.specFor(node.type, key) ?: return@forEach
                val valid = when (spec.valueType) {
                    PropertyValueType.TEXT -> true
                    PropertyValueType.INTEGER -> value.toIntOrNull() != null
                    PropertyValueType.BOOLEAN -> value == "true" || value == "false"
                    PropertyValueType.DATE -> Regex("""\d{4}-\d{2}-\d{2}""").matches(value)
                    PropertyValueType.ENUM -> value in spec.allowedValues
                }
                assertTrue(valid, "Invalid value '$value' for ${node.id.value}.$key")
            }
            NodeTypeSchema.propertiesFor(node.type).filter { it.required }.forEach { spec ->
                assertTrue(spec.key in node.properties, "Missing required ${spec.key} in ${node.id.value}")
            }
        }
    }

    @Test
    fun playerCharactersAreLocked() {
        SampleCampaign.nodes.filter { it.type.name == "PLAYER_CHARACTER" }.forEach { assertTrue(it.isLocked) }
    }
}
