package com.example.typespec.workflow

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TypeSpecTraceRoutingTest {
    @Test
    fun bracketTracePrefixIsTrace() {
        assertTrue(isTraceLine("[trace] compiler: resolved import"))
    }

    @Test
    fun bracketTracePrefixIsCaseInsensitive() {
        assertTrue(isTraceLine("[TRACE] compiler: resolved import"))
    }

    @Test
    fun bracketTracePrefixWithLeadingWhitespaceIsTrace() {
        assertTrue(isTraceLine("   [trace] compiler: resolved import"))
    }

    @Test
    fun tracePrefixIsTrace() {
        assertTrue(isTraceLine("trace: compiler: emit finished"))
    }

    @Test
    fun tracePrefixWithLeadingWhitespaceIsTrace() {
        assertTrue(isTraceLine("   trace: http: GET /pets"))
    }

    @Test
    fun tracePrefixIsCaseInsensitive() {
        assertTrue(isTraceLine("TRACE: compiler: emit finished"))
    }

    @Test
    fun stacktraceIsNotTrace() {
        assertFalse(isTraceLine("stacktrace: at Foo.bar(Foo.kt:1)"))
    }

    @Test
    fun traceInTheMiddleIsNotTrace() {
        assertFalse(isTraceLine("error: trace: unexpected token"))
    }

    @Test
    fun plainOutputIsNotTrace() {
        assertFalse(isTraceLine("Compilation completed."))
    }
}
