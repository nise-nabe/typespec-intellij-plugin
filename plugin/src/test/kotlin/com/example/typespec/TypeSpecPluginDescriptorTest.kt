package com.example.typespec

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TypeSpecPluginDescriptorTest {
    @Test
    fun pluginXmlDeclaresToolsMenuActions() {
        val xml = pluginXml()

        val actionIds = listOf(
            "TypeSpec.RestartServer",
            "TypeSpec.ShowOutput",
            "TypeSpec.EmitFromTypeSpec",
            "TypeSpec.CreateProject",
            "TypeSpec.InstallDependencies",
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
        val xml = pluginXml()

        assertTrue(xml.contains("class=\"com.example.typespec.actions.TypeSpecRestartServerAction\""))
        assertTrue(xml.contains("class=\"com.example.typespec.actions.TypeSpecShowOutputAction\""))
        assertTrue(xml.contains("class=\"com.example.typespec.actions.TypeSpecEmitFromTypeSpecAction\""))
    }

    @Test
    fun pluginXmlRegistersSyntaxHighlighterFactory() {
        val xml = pluginXml()

        assertTrue(
            xml.contains("<lang.syntaxHighlighterFactory"),
            "plugin.xml should declare lang.syntaxHighlighterFactory",
        )
        assertTrue(
            xml.contains("language=\"TypeSpec\""),
            "plugin.xml syntax highlighter should target TypeSpec language",
        )
        assertTrue(
            xml.contains(
                "implementationClass=\"com.example.typespec.highlighting.TypeSpecSyntaxHighlighterFactory\"",
            ),
            "plugin.xml should wire TypeSpecSyntaxHighlighterFactory",
        )
    }

    @Test
    fun pluginXmlRegistersTspConfigJsonSchemaProvider() {
        val xml = pluginXml()

        assertTrue(
            xml.contains("implementation=\"com.example.typespec.inspections.TypeSpecTspConfigSchemaProviderFactory\""),
            "plugin.xml should register tspconfig.yaml JSON Schema provider",
        )
        assertTrue(
            xml.contains("<ProviderFactory"),
            "plugin.xml should declare JsonSchema ProviderFactory extension",
        )
        assertTrue(
            xml.contains("defaultExtensionNs=\"JavaScript.JsonSchema\""),
            "plugin.xml should register JSON Schema provider under JavaScript.JsonSchema extension namespace",
        )
        assertTrue(
            xml.contains("<depends>org.jetbrains.plugins.yaml</depends>"),
            "plugin.xml should declare YAML plugin dependency for tspconfig.yaml validation",
        )
    }

    @Test
    fun pluginXmlDeclaresDynamicLoadingWithoutRestart() {
        val xml = pluginXml()

        assertTrue(
            xml.contains("<id>$TYPESPEC_PLUGIN_ID</id>"),
            "plugin.xml id should match TYPESPEC_PLUGIN_ID",
        )
        assertTrue(
            Regex("""<idea-plugin\b[^>]*\brequire-restart="false"""").containsMatchIn(xml),
            "plugin.xml should set require-restart=\"false\" so the IDE can load the plugin dynamically",
        )
        assertTrue(
            xml.contains("<applicationListeners>"),
            "plugin.xml should declare applicationListeners for dynamic plugin lifecycle",
        )
        assertTrue(
            xml.contains("class=\"com.example.typespec.TypeSpecDynamicPluginListener\""),
            "plugin.xml should wire TypeSpecDynamicPluginListener",
        )
        assertTrue(
            xml.contains("topic=\"com.intellij.ide.plugins.DynamicPluginListener\""),
            "plugin.xml should subscribe TypeSpecDynamicPluginListener to DynamicPluginListener",
        )
    }

    @Test
    fun pluginXmlRegistersProjectListenersAtPluginRoot() {
        val xml = pluginXml()
        val intellijExtensions = Regex(
            """<extensions defaultExtensionNs="com.intellij">[\s\S]*?</extensions>""",
        ).find(xml)?.value

        assertNotNull(intellijExtensions, "plugin.xml should declare com.intellij extensions")
        assertFalse(
            intellijExtensions!!.contains("<projectListeners>"),
            "projectListeners must be a direct child of idea-plugin, not nested under extensions",
        )
        assertTrue(xml.contains("<projectListeners>"), "plugin.xml should declare projectListeners")
        assertTrue(
            xml.contains("class=\"com.example.typespec.TypeSpecLspServiceSettingsListener\""),
            "plugin.xml should wire TypeSpecLspServiceSettingsListener",
        )
        assertTrue(
            xml.contains("topic=\"com.example.typespec.TypeSpecServiceSettingsListener\""),
            "plugin.xml should subscribe the settings listener to TypeSpecServiceSettingsListener",
        )
    }

    @Test
    fun pluginXmlActionGroupsDeclareIds() {
        val xml = pluginXml()
        val groups = Regex("""<group\b[^>]*>""").findAll(xml).map { it.value }.toList()

        assertTrue(groups.isNotEmpty(), "plugin.xml should declare action groups")
        for (group in groups) {
            assertTrue(
                group.contains("id=\""),
                "dynamic plugins require each action group to declare an id: $group",
            )
        }
    }

    private fun pluginXml(): String =
        requireNotNull(javaClass.classLoader.getResourceAsStream("META-INF/plugin.xml")) {
            "plugin.xml should be on the test classpath"
        }.bufferedReader().use { it.readText() }
}
