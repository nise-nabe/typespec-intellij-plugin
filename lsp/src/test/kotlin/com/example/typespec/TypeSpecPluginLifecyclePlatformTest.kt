package com.example.typespec

import com.intellij.openapi.extensions.PluginId

class TypeSpecPluginLifecyclePlatformTest : TypeSpecBasePlatformTestCase() {
    fun testIsTypeSpecPluginMatchesPluginId() {
        assertTrue(TypeSpecPluginLifecycle.isTypeSpecPlugin(PluginId.getId(TYPESPEC_PLUGIN_ID)))
        assertFalse(TypeSpecPluginLifecycle.isTypeSpecPlugin(PluginId.getId("com.intellij")))
    }

    fun testBeforeUnloadInvalidatesPackageResolutionCache() {
        val cache = TypeSpecPackageResolutionCache.getInstance(project)
        cache.getOrCompute(project)

        assertNotNull(cache.peekSnapshot())
        TypeSpecPluginLifecycle.beforeUnload(arrayOf(project))
        assertNull(cache.peekSnapshot())
    }

    fun testOnBeforeUnloadInvalidatesCacheForThisPlugin() {
        val cache = TypeSpecPackageResolutionCache.getInstance(project)
        cache.getOrCompute(project)

        TypeSpecPluginLifecycle.onBeforeUnload(PluginId.getId(TYPESPEC_PLUGIN_ID), arrayOf(project))

        assertNull(cache.peekSnapshot())
    }

    fun testOnBeforeUnloadIgnoresOtherPluginIds() {
        val cache = TypeSpecPackageResolutionCache.getInstance(project)
        cache.getOrCompute(project)

        TypeSpecPluginLifecycle.onBeforeUnload(PluginId.getId("com.intellij"), arrayOf(project))

        assertNotNull(cache.peekSnapshot())
    }

    fun testAfterLoadOnOpenProjectDoesNotThrow() {
        TypeSpecPluginLifecycle.onAfterLoad(PluginId.getId(TYPESPEC_PLUGIN_ID), arrayOf(project))
        TypeSpecPluginLifecycle.onAfterLoad(PluginId.getId("com.intellij"), arrayOf(project))
        TypeSpecPluginLifecycle.afterLoad(emptyArray())
        TypeSpecPluginLifecycle.beforeUnload(emptyArray())
    }
}
