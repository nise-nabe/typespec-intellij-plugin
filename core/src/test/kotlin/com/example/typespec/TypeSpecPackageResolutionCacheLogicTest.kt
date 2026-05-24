package com.example.typespec

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class TypeSpecPackageResolutionCacheLogicTest {
    @Test
    fun returnsCachedResultWithinTtl() {
        val cache = TypeSpecPackageResolutionCache()
        var computeCount = 0
        val snapshot = unresolvedSnapshot()

        val first = cache.getOrCompute("default", nowMillis = 1_000L) {
            computeCount++
            snapshot
        }
        val second = cache.getOrCompute("default", nowMillis = 20_000L) {
            computeCount++
            resolvedSnapshot()
        }

        assertFalse(first.compilerCliResolvable)
        assertFalse(second.compilerCliResolvable)
        assertEquals(1, computeCount)
    }

    @Test
    fun recomputesAfterTtlExpires() {
        val cache = TypeSpecPackageResolutionCache()
        var computeCount = 0
        val snapshot = unresolvedSnapshot()

        cache.getOrCompute("default", nowMillis = 0L) {
            computeCount++
            snapshot
        }
        cache.getOrCompute("default", nowMillis = TypeSpecPackageResolutionCache.RESOLUTION_CACHE_TTL_MILLIS) {
            computeCount++
            resolvedSnapshot()
        }

        assertEquals(2, computeCount)
    }

    @Test
    fun invalidateForcesRecomputation() {
        val cache = TypeSpecPackageResolutionCache()
        var computeCount = 0
        val snapshot = unresolvedSnapshot()

        cache.getOrCompute("default", nowMillis = 1_000L) {
            computeCount++
            snapshot
        }
        cache.invalidate()
        cache.getOrCompute("default", nowMillis = 2_000L) {
            computeCount++
            resolvedSnapshot()
        }

        assertEquals(2, computeCount)
    }

    @Test
    fun recomputesWhenPackageKeyChanges() {
        val cache = TypeSpecPackageResolutionCache()
        var computeCount = 0
        val snapshot = unresolvedSnapshot()

        cache.getOrCompute("default", nowMillis = 1_000L) {
            computeCount++
            snapshot
        }
        cache.getOrCompute("custom-path", nowMillis = 2_000L) {
            computeCount++
            resolvedSnapshot()
        }

        assertEquals(2, computeCount)
    }

    private fun unresolvedSnapshot() = TypeSpecPackageResolutionCache.Snapshot(
        compilerCliResolvable = false,
        lspServerResolvable = false,
        openApi3CliResolvable = false,
    )

    private fun resolvedSnapshot() = TypeSpecPackageResolutionCache.Snapshot(
        compilerCliResolvable = true,
        lspServerResolvable = true,
        openApi3CliResolvable = true,
    )
}
