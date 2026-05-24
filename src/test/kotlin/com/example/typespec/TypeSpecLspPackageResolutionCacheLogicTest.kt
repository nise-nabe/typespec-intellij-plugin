package com.example.typespec

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class TypeSpecLspPackageResolutionCacheLogicTest {
    @Test
    fun returnsCachedResultWithinTtl() {
        val cache = TypeSpecLspPackageResolutionCache()
        var computeCount = 0

        val first = cache.getOrCompute("default", nowMillis = 1_000L) {
            computeCount++
            false
        }
        val second = cache.getOrCompute("default", nowMillis = 20_000L) {
            computeCount++
            true
        }

        assertFalse(first)
        assertFalse(second)
        assertEquals(1, computeCount)
    }

    @Test
    fun recomputesAfterTtlExpires() {
        val cache = TypeSpecLspPackageResolutionCache()
        var computeCount = 0

        cache.getOrCompute("default", nowMillis = 0L) {
            computeCount++
            false
        }
        cache.getOrCompute("default", nowMillis = TypeSpecLspPackageResolutionCache.RESOLUTION_CACHE_TTL_MILLIS) {
            computeCount++
            true
        }

        assertEquals(2, computeCount)
    }

    @Test
    fun invalidateForcesRecomputation() {
        val cache = TypeSpecLspPackageResolutionCache()
        var computeCount = 0

        cache.getOrCompute("default", nowMillis = 1_000L) {
            computeCount++
            false
        }
        cache.invalidate()
        cache.getOrCompute("default", nowMillis = 2_000L) {
            computeCount++
            true
        }

        assertEquals(2, computeCount)
    }

    @Test
    fun recomputesWhenPackageKeyChanges() {
        val cache = TypeSpecLspPackageResolutionCache()
        var computeCount = 0

        cache.getOrCompute("default", nowMillis = 1_000L) {
            computeCount++
            false
        }
        cache.getOrCompute("custom-path", nowMillis = 2_000L) {
            computeCount++
            true
        }

        assertEquals(2, computeCount)
    }
}
