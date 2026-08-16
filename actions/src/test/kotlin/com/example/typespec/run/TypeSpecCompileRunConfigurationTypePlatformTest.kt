package com.example.typespec.run

import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TypeSpecCompileRunConfigurationTypePlatformTest : BasePlatformTestCase() {
    fun testGetInstanceReturnsRegisteredExtension() {
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
