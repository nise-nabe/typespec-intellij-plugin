package com.example.typespec

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TypeSpecActivationHelperTest {
    @Test
    fun isEnvironmentSupportedReturnsFalseInUnitTestMode() {
        assertFalse(TypeSpecActivationHelper.isEnvironmentSupported(isUnitTestMode = true))
    }

    @Test
    fun isEnvironmentSupportedReturnsTrueOutsideUnitTestMode() {
        assertTrue(TypeSpecActivationHelper.isEnvironmentSupported(isUnitTestMode = false))
    }
}
