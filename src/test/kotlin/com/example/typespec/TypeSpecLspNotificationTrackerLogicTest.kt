package com.example.typespec

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TypeSpecLspNotificationTrackerLogicTest {
    @Test
    fun tryAcquireCompilerMissingNotificationOnlyOnceForSamePackageKey() {
        val tracker = TypeSpecLspNotificationTracker()

        assertTrue(tracker.tryAcquireCompilerMissingNotification("default"))
        tracker.rememberCompilerMissingNotification(stubNotification(), "default")
        assertFalse(tracker.tryAcquireCompilerMissingNotification("default"))
    }

    @Test
    fun tryAcquireCompilerMissingNotificationAgainAfterPackageKeyChanges() {
        val tracker = TypeSpecLspNotificationTracker()

        assertTrue(tracker.tryAcquireCompilerMissingNotification("default"))
        tracker.rememberCompilerMissingNotification(stubNotification(), "default")
        assertTrue(tracker.tryAcquireCompilerMissingNotification("custom-path"))
    }

    @Test
    fun clearCompilerMissingNotificationAllowsSubsequentNotification() {
        val tracker = TypeSpecLspNotificationTracker()

        assertTrue(tracker.tryAcquireCompilerMissingNotification("default"))
        tracker.rememberCompilerMissingNotification(stubNotification(), "default")
        tracker.clearCompilerMissingNotification()
        assertTrue(tracker.tryAcquireCompilerMissingNotification("default"))
    }

    private fun stubNotification(): Notification = Notification("TypeSpec Notifications", "", "", NotificationType.WARNING)
}
