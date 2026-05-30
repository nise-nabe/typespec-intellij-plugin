package com.example.typespec

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TypeSpecPluginDescriptorTest {
    @Test
    fun pluginXmlDeclaresToolsMenuActions() {
        val xml = requireNotNull(javaClass.classLoader.getResourceAsStream("META-INF/plugin.xml")) {
            "plugin.xml should be on the test classpath"
        }.bufferedReader().use { it.readText() }

        val actionIds = listOf(
            "TypeSpec.RestartServer",
            "TypeSpec.ShowOutput",
            "TypeSpec.EmitFromTypeSpec",
            "TypeSpec.CreateProject",
            "TypeSpec.ImportFromOpenApi",
            "TypeSpec.PreviewOpenApi",
        )
        for (actionId in actionIds) {
            assertTrue(xml.contains("id=\"$actionId\""), "plugin.xml should declare action $actionId")
        }
        assertTrue(xml.contains("id=\"TypeSpec.Tools\""), "plugin.xml should declare Tools menu group")
    }

    @Test
    fun pluginXmlWiresActionImplementations() {
        val xml = requireNotNull(javaClass.classLoader.getResourceAsStream("META-INF/plugin.xml")) {
            "plugin.xml should be on the test classpath"
        }.bufferedReader().use { it.readText() }

        assertTrue(xml.contains("class=\"com.example.typespec.actions.TypeSpecRestartServerAction\""))
        assertTrue(xml.contains("class=\"com.example.typespec.actions.TypeSpecShowOutputAction\""))
        assertTrue(xml.contains("class=\"com.example.typespec.actions.TypeSpecEmitFromTypeSpecAction\""))
    }
}
