package com.example.typespec

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TypeSpecActivationHelperTest {
    @Test
    fun downloadableCompilerShouldNotRequireProjectLocalPackage() {
        assertTrue(
            TypeSpecActivationHelper.isEnvironmentSupported(
                isUnitTestMode = false,
                hasProjectLocalCompilerPackage = false,
            ),
        )
    }

    @Test
    fun unitTestModeDisablesEnvironmentSupport() {
        assertFalse(
            TypeSpecActivationHelper.isEnvironmentSupported(
                isUnitTestMode = true,
                hasProjectLocalCompilerPackage = true,
            ),
        )
    }
}