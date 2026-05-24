package com.example.typespec

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TypeSpecLspNotificationTrackerLogicTest {
    @Test
    fun shouldNotifyCompilerMissingOnlyOnceForSamePackageKey() {
        val tracker = TypeSpecLspNotificationTracker()

        assertTrue(tracker.shouldNotifyCompilerMissing("default"))
        assertFalse(tracker.shouldNotifyCompilerMissing("default"))
    }

    @Test
    fun shouldNotifyCompilerMissingAgainAfterPackageKeyChanges() {
        val tracker = TypeSpecLspNotificationTracker()

        assertTrue(tracker.shouldNotifyCompilerMissing("default"))
        assertTrue(tracker.shouldNotifyCompilerMissing("custom-path"))
    }

    @Test
    fun clearCompilerMissingNotificationAllowsSubsequentNotification() {
        val tracker = TypeSpecLspNotificationTracker()

        assertTrue(tracker.shouldNotifyCompilerMissing("default"))
        tracker.clearCompilerMissingNotification()
        assertTrue(tracker.shouldNotifyCompilerMissing("default"))
    }
}
