package com.example.typespec

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TypeSpecCompilerMissingNotificationLogicTest {
    @Test
    fun defersNotificationOnFirstUnresolvedSnapshot() {
        val snapshot = ResolutionSnapshot(
            isResolvable = false,
            packageKey = "/pkg",
            wasResolvable = null,
        )

        assertTrue(shouldDeferCompilerMissingNotification(snapshot))
        assertFalse(shouldShowCompilerMissingNotification(snapshot))
    }

    @Test
    fun showsNotificationAfterResolvableStateWasCached() {
        val snapshot = ResolutionSnapshot(
            isResolvable = false,
            packageKey = "/pkg",
            wasResolvable = false,
        )

        assertFalse(shouldDeferCompilerMissingNotification(snapshot))
        assertTrue(shouldShowCompilerMissingNotification(snapshot))
    }

    @Test
    fun showsNotificationWhenPackageBecomesUnresolved() {
        val snapshot = ResolutionSnapshot(
            isResolvable = false,
            packageKey = "/pkg",
            wasResolvable = true,
        )

        assertFalse(shouldDeferCompilerMissingNotification(snapshot))
        assertTrue(shouldShowCompilerMissingNotification(snapshot))
    }

    @Test
    fun doesNotDeferOrShowWhenPackageIsResolvable() {
        val snapshot = ResolutionSnapshot(
            isResolvable = true,
            packageKey = "/pkg",
            wasResolvable = false,
        )

        assertFalse(shouldDeferCompilerMissingNotification(snapshot))
        assertFalse(shouldShowCompilerMissingNotification(snapshot))
    }
}
