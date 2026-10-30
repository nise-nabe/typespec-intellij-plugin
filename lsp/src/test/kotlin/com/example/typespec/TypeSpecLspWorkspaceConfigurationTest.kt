package com.example.typespec

import com.intellij.openapi.project.Project
import org.eclipse.lsp4j.ConfigurationItem

class TypeSpecLspWorkspaceConfigurationTest : TypeSpecBasePlatformTestCase() {
    override fun setUp() {
        super.setUp()
        val settings = TypeSpecServiceSettings.getInstance(project)
        settings.serviceMode = TypeSpecServiceMode.ENABLED
        settings.lspEmit = emptyList()
        settings.entrypoints = emptyList()
    }

    fun testSettingsRoundTripEmittersAndEntrypoints() {
        val settings = TypeSpecServiceSettings.getInstance(project)

        assertEquals(emptyList<String>(), settings.lspEmit)
        assertEquals(emptyList<String>(), settings.entrypoints)

        settings.lspEmit = listOf(" <config:defaults> ", "@typespec/openapi3", "<config:defaults>")
        settings.entrypoints = listOf("client.tsp", " main.tsp ")

        assertEquals(listOf("<config:defaults>", "@typespec/openapi3"), settings.lspEmit)
        assertEquals(listOf("client.tsp", "main.tsp"), settings.entrypoints)
        assertEquals(TypeSpecServiceMode.ENABLED, settings.serviceMode)
    }

    fun testDescriptorReturnsTypespecSectionForNullAndOtherScopes() {
        val settings = TypeSpecServiceSettings.getInstance(project)
        settings.lspEmit = listOf("<config:defaults>", "@typespec/openapi3")
        settings.entrypoints = listOf("client.tsp", "main.tsp")
        val descriptor = TypeSpecLspClientDescriptor(project)
        val expected = mapOf(
            "lsp" to mapOf("emit" to listOf("<config:defaults>", "@typespec/openapi3")),
            "entrypoint" to listOf("client.tsp", "main.tsp"),
        )

        for (scopeUri in listOf(null, "", "file:///tmp/other/main.tsp", "not a uri")) {
            val item = ConfigurationItem()
            item.section = "typespec"
            item.scopeUri = scopeUri
            assertEquals(expected, descriptor.getWorkspaceConfiguration(item))
        }
    }

    fun testDescriptorLeavesEmptyTypespecSectionEmpty() {
        val descriptor = TypeSpecLspClientDescriptor(project)
        val item = ConfigurationItem()
        item.section = "typespec"
        item.scopeUri = null

        assertEquals(emptyMap<String, Any>(), descriptor.getWorkspaceConfiguration(item))
    }

    fun testDescriptorDelegatesUnknownAndNullSections() {
        val settings = TypeSpecServiceSettings.getInstance(project)
        settings.lspEmit = listOf("<config:defaults>")
        val descriptor = TypeSpecLspClientDescriptor(project)

        val unknown = ConfigurationItem()
        unknown.section = "typescript"
        unknown.scopeUri = "file:///tmp/main.tsp"
        assertNull(descriptor.getWorkspaceConfiguration(unknown))

        val missing = ConfigurationItem()
        missing.section = null
        missing.scopeUri = null
        assertNull(descriptor.getWorkspaceConfiguration(missing))
    }

    fun testWorkspaceConfigurationChangesNotifyOnlyWorkspaceListeners() {
        val settings = TypeSpecServiceSettings.getInstance(project)
        var serviceChanges = 0
        var workspaceChanges = 0
        project.messageBus.connect(testRootDisposable).subscribe(
            TypeSpecServiceSettingsListener.TOPIC,
            object : TypeSpecServiceSettingsListener {
                override fun onServiceSettingsChanged(project: Project) {
                    serviceChanges++
                }

                override fun onWorkspaceConfigurationChanged(project: Project) {
                    workspaceChanges++
                }
            },
        )

        settings.lspEmit = listOf("@typespec/openapi3")
        settings.lspEmit = listOf("@typespec/openapi3")
        settings.entrypoints = listOf("client.tsp")
        settings.serviceMode = TypeSpecServiceMode.DISABLED

        assertEquals(2, workspaceChanges)
        assertEquals(1, serviceChanges)
        assertEquals(listOf("@typespec/openapi3"), settings.lspEmit)
        assertEquals(listOf("client.tsp"), settings.entrypoints)
    }

    fun testSettingsPanelResetsWithoutModification() {
        val settings = TypeSpecServiceSettings.getInstance(project)
        settings.lspEmit = listOf("<config:defaults>")
        settings.entrypoints = listOf("main.tsp")
        val configurable = TypeSpecSettingsConfigurable(project)

        try {
            assertNotNull(configurable.createComponent())
            configurable.reset()
            assertFalse(configurable.isModified)
        } finally {
            configurable.disposeUIResources()
        }
    }
}
