package com.example.typespec

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TypeSpecUltimateTestErrorSupportTest {
    @Test
    fun detectsUltimateConstructorFailureFromCauseChain() {
        val cause =
            RuntimeException(
                "Cannot find suitable constructor for class B.B.B.B.s, expected (), " +
                    "(CoroutineScope), (Application), or (Application, CoroutineScope)",
            )
        val exception =
            RuntimeException(
                "Cannot create extension (class=B.B.B.B.s) [Plugin: com.intellij.modules.ultimate]",
                cause,
            )

        assertTrue(isUltimatePostStartupConstructorError(exception.message, exception))
    }

    @Test
    fun ignoresUltimateExtensionMessageWithoutConstructorFailure() {
        assertFalse(
            isUltimatePostStartupConstructorError(
                "Cannot create extension (class=B.B.B.B.s) [Plugin: com.intellij.modules.ultimate]",
                null,
            ),
        )
    }

    @Test
    fun ignoresUnrelatedPluginErrors() {
        val exception =
            RuntimeException("Cannot create extension (class=Foo) [Plugin: com.example.typespec]")
        assertFalse(isUltimatePostStartupConstructorError(exception.message, exception))
    }
}
