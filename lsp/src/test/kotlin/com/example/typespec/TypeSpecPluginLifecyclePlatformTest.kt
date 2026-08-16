package com.example.typespec

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TypeSpecPluginLifecyclePlatformTest : BasePlatformTestCase() {
    fun testPluginIsLoadedUnderExpectedPluginId() {
        val descriptor = PluginManagerCore.getPlugin(PluginId.getId(TYPESPEC_PLUGIN_ID))

        assertNotNull(descriptor)
        assertTrue(TypeSpecPluginLifecycle.isTypeSpecPlugin(descriptor!!))
    }

    fun testIsTypeSpecPluginRejectsOtherPlugins() {
        val other = PluginManagerCore.loadedPlugins.first { it.pluginId.idString != TYPESPEC_PLUGIN_ID }

        assertFalse(TypeSpecPluginLifecycle.isTypeSpecPlugin(other))
    }

    fun testBeforeUnloadInvalidatesPackageResolutionCache() {
        val cache = TypeSpecPackageResolutionCache.getInstance(project)
        cache.getOrCompute(project)

        assertNotNull(cache.peekSnapshot())
        TypeSpecPluginLifecycle.beforeUnload(arrayOf(project))
        assertNull(cache.peekSnapshot())
    }

    fun testListenerUnloadsTypeSpecPlugin() {
        val descriptor = PluginManagerCore.getPlugin(PluginId.getId(TYPESPEC_PLUGIN_ID))!!
        val cache = TypeSpecPackageResolutionCache.getInstance(project)
        cache.getOrCompute(project)

        TypeSpecDynamicPluginListener().beforePluginUnload(descriptor, false)

        assertNull(cache.peekSnapshot())
    }

    fun testListenerIgnoresOtherPlugins() {
        val other = PluginManagerCore.loadedPlugins.first { it.pluginId.idString != TYPESPEC_PLUGIN_ID }
        val cache = TypeSpecPackageResolutionCache.getInstance(project)
        cache.getOrCompute(project)

        TypeSpecDynamicPluginListener().beforePluginUnload(other, false)

        assertNotNull(cache.peekSnapshot())
    }

    fun testAfterLoadOnOpenProjectDoesNotThrow() {
        TypeSpecPluginLifecycle.afterLoad(arrayOf(project))
        TypeSpecPluginLifecycle.afterLoad(emptyArray())
        TypeSpecPluginLifecycle.beforeUnload(emptyArray())
    }
}
