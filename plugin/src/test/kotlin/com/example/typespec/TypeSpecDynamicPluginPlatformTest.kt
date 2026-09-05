package com.example.typespec

import com.example.typespec.run.TypeSpecCompileRunConfigurationProducer
import com.example.typespec.run.TypeSpecCompileRunConfigurationType
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId

class TypeSpecDynamicPluginPlatformTest : TypeSpecBasePlatformTestCase() {
    fun testPluginDescriptorIsLoaded() {
        val descriptor = PluginManagerCore.getPlugin(PluginId.getId(TYPESPEC_PLUGIN_ID))

        assertNotNull(descriptor)
        assertTrue(TypeSpecPluginLifecycle.isTypeSpecPlugin(descriptor!!))
    }

    fun testGetInstanceReturnsRegisteredConfigurationType() {
        val registered = ConfigurationTypeUtil.findConfigurationType(TypeSpecCompileRunConfigurationType::class.java)

        assertSame(registered, TypeSpecCompileRunConfigurationType.getInstance())
        assertEquals("TypeSpecCompileRunConfigurationType", registered.id)
    }

    fun testProducerUsesRegisteredFactory() {
        val producer = TypeSpecCompileRunConfigurationProducer()

        assertSame(
            TypeSpecCompileRunConfigurationType.getInstance().configurationFactories[0],
            producer.configurationFactory,
        )
    }
}
