package com.example.typespec

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TypeSpecLspNotificationTrackerLogicTest {
    @Test
    fun tryAcquireCompilerMissingNotificationOnlyOnceForSamePackageKey() {
        val tracker = TypeSpecLspNotificationTracker()

        assertTrue(tracker.tryAcquireCompilerMissingNotification("default"))
        assertFalse(tracker.tryAcquireCompilerMissingNotification("default"))
    }

    @Test
    fun tryAcquireCompilerMissingNotificationAgainAfterPackageKeyChanges() {
        val tracker = TypeSpecLspNotificationTracker()

        assertTrue(tracker.tryAcquireCompilerMissingNotification("default"))
        assertTrue(tracker.tryAcquireCompilerMissingNotification("custom-path"))
    }

    @Test
    fun clearCompilerMissingNotificationAllowsSubsequentNotification() {
        val tracker = TypeSpecLspNotificationTracker()

        assertTrue(tracker.tryAcquireCompilerMissingNotification("default"))
        tracker.clearCompilerMissingNotification()
        assertTrue(tracker.tryAcquireCompilerMissingNotification("default"))
    }
}
