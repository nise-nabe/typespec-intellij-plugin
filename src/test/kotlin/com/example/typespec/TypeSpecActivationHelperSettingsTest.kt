package com.example.typespec

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TypeSpecActivationHelperSettingsTest : BasePlatformTestCase() {
    fun testIsEnabledInSettingsReflectsServiceMode() {
        val settings = TypeSpecServiceSettings.getInstance(project)

        settings.serviceMode = TypeSpecServiceMode.ENABLED
        assertTrue(TypeSpecActivationHelper.isEnabledInSettings(project))

        settings.serviceMode = TypeSpecServiceMode.DISABLED
        assertFalse(TypeSpecActivationHelper.isEnabledInSettings(project))
    }
}

class TypeSpecLspNotificationTrackerPlatformTest : BasePlatformTestCase() {
    fun testTrackerIsProjectScopedService() {
        val tracker = TypeSpecLspNotificationTracker.getInstance(project)

        assertTrue(tracker.shouldNotifyCompilerMissing("default"))
        assertFalse(tracker.shouldNotifyCompilerMissing("default"))
    }
}
