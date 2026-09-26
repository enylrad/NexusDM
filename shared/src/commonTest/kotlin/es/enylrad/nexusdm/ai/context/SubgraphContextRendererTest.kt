package es.enylrad.nexusdm.ai.context

import es.enylrad.nexusdm.testing.Fixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class SubgraphContextRendererTest {

    private val rendered = SubgraphContextRenderer().render(Fixtures.campaign, Fixtures.subgraph)
    private val document = Json.parseToJsonElement(rendered).jsonObject
    private val nodes = document["nodes"]!!.jsonArray.associateBy { it.jsonObject["id"]!!.jsonPrimitive.content }

    @Test
    fun neverLeaksFoundryLinksOrAssets() {
        assertFalse("Actor.secretUuid123" in rendered)
        assertFalse("varis.webp" in rendered)
        assertFalse("createdAt" in rendered)
        assertFalse("version" in rendered)
    }

    @Test
    fun sendsDescriptionsOnlyForSeedNodes() {
        val varis = nodes.getValue("npc_varis").jsonObject
        val ironCircle = nodes.getValue("faction_iron_circle").jsonObject

        assertEquals(Fixtures.varis.description, varis["description"]!!.jsonPrimitive.content)
        assertNull(ironCircle["description"])
    }

    @Test
    fun marksLockedNodesAndFocus() {
        assertEquals("true", nodes.getValue("pc_aria").jsonObject["locked"]!!.jsonPrimitive.content)
        assertNull(nodes.getValue("npc_varis").jsonObject["locked"])
        assertEquals(listOf("npc_varis"), document["focusNodeIds"]!!.jsonArray.map { it.jsonPrimitive.content })
    }

    @Test
    fun includesEdgesAndCampaign() {
        val edge = document["edges"]!!.jsonArray.single().jsonObject
        assertEquals("LEADS", edge["type"]!!.jsonPrimitive.content)
        assertEquals("npc_varis", edge["source"]!!.jsonPrimitive.content)
        assertTrue(document["campaign"]!!.jsonObject["name"]!!.jsonPrimitive.content == "The Sunken Crown")
    }
}
