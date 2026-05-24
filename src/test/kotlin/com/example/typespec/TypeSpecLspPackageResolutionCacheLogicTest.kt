package com.example.typespec

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TypeSpecLspPackageResolutionCacheLogicTest {
    @Test
    fun peekResolvableReturnsNullUntilRecorded() {
        val cache = TypeSpecLspPackageResolutionCache()

        assertNull(cache.peekResolvable())
    }

    @Test
    fun recordResolvableUpdatesPeek() {
        val cache = TypeSpecLspPackageResolutionCache()

        cache.recordResolvable(false)
        assertFalse(cache.peekResolvable()!!)

        cache.recordResolvable(true)
        assertTrue(cache.peekResolvable()!!)
    }

    @Test
    fun clearLastResolvableRemovesPeekSnapshot() {
        val cache = TypeSpecLspPackageResolutionCache()

        cache.recordResolvable(false)
        cache.clearLastResolvable()

        assertNull(cache.peekResolvable())
    }
}
