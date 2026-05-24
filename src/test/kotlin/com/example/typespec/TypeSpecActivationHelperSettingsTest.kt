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

    fun testIsEligibleExceptPackageResolutionIsFalseWhenServiceDisabled() {
        val settings = TypeSpecServiceSettings.getInstance(project)
        val file = myFixture.configureByText("main.tsp", "namespace Demo {}").virtualFile

        settings.serviceMode = TypeSpecServiceMode.DISABLED
        assertFalse(TypeSpecLspServerActivationRule.isEligibleExceptPackageResolution(project, file))
    }

    fun testIsEligibleExceptPackageResolutionIsFalseForNonTypeSpecFile() {
        val settings = TypeSpecServiceSettings.getInstance(project)
        settings.serviceMode = TypeSpecServiceMode.ENABLED
        val file = myFixture.configureByText("main.json", "{}").virtualFile

        assertFalse(TypeSpecLspServerActivationRule.isEligibleExceptPackageResolution(project, file))
    }
}

class TypeSpecLspNotificationTrackerPlatformTest : BasePlatformTestCase() {
    fun testTrackerIsProjectScopedService() {
        val tracker = TypeSpecLspNotificationTracker.getInstance(project)

        assertTrue(tracker.shouldNotifyCompilerMissing("default"))
        assertFalse(tracker.shouldNotifyCompilerMissing("default"))
    }
}
