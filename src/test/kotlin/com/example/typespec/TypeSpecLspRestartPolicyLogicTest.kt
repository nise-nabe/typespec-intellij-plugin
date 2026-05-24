package com.example.typespec

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TypeSpecLspRestartPolicyLogicTest {
    @Test
    fun doesNotRestartWhenPreviousResolvableStateWasUncached() {
        assertFalse(shouldRestartForResolvableChange(wasResolvable = null, isResolvable = false))
        assertFalse(shouldRestartForResolvableChange(wasResolvable = null, isResolvable = true))
    }

    @Test
    fun restartsWhenResolvableStateChanges() {
        assertTrue(shouldRestartForResolvableChange(wasResolvable = false, isResolvable = true))
        assertTrue(shouldRestartForResolvableChange(wasResolvable = true, isResolvable = false))
    }

    @Test
    fun doesNotRestartWhenResolvableStateIsUnchanged() {
        assertFalse(shouldRestartForResolvableChange(wasResolvable = false, isResolvable = false))
        assertFalse(shouldRestartForResolvableChange(wasResolvable = true, isResolvable = true))
    }
}
