package com.example.typespec

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TypeSpecLspNotificationTrackerPlatformTest : BasePlatformTestCase() {
    fun testTrackerIsProjectScopedService() {
        val tracker = TypeSpecLspNotificationTracker.getInstance(project)

        assertTrue(tracker.tryAcquireCompilerMissingNotification("default"))
        assertFalse(tracker.tryAcquireCompilerMissingNotification("default"))
    }
}
