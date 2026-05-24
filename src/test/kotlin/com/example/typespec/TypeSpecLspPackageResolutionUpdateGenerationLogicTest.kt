package com.example.typespec

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TypeSpecLspPackageResolutionUpdateGenerationLogicTest {
    @Test
    fun onlyLatestGenerationIsAccepted() {
        val cache = TypeSpecLspPackageResolutionCache()

        val first = cache.nextResolutionUpdateGeneration()
        val second = cache.nextResolutionUpdateGeneration()

        assertTrue(cache.isLatestResolutionUpdate(second))
        assertFalse(cache.isLatestResolutionUpdate(first))
    }
}
