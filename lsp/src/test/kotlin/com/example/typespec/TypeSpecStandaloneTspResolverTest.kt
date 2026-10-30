package com.example.typespec

import com.intellij.execution.configurations.GeneralCommandLine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class TypeSpecStandaloneTspResolverTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun buildStandaloneServerCommandLineReturnsNullWhenScriptMissingAndTspServerUnavailable() {
        assertNull(
            TypeSpecStandaloneTspResolver.buildStandaloneServerCommandLine(
                tempDir.resolve("missing.js"),
                isTspServerAvailable = { false },
            ),
        )
    }

    @Test
    fun buildStandaloneServerCommandLineUsesTspServerWhenScriptMissing() {
        val commandLine = TypeSpecStandaloneTspResolver.buildStandaloneServerCommandLine(
            tempDir.resolve("missing.js"),
            isTspServerAvailable = { true },
            tspServerExecutable = { "tsp-server" },
        )

        assertEquals("tsp-server", commandLine!!.exePath)
        assertEquals(listOf("--stdio"), commandLine.parametersList.parameters)
        assertSkipCompilerResolve(commandLine)
    }

    @Test
    fun buildStandaloneServerCommandLineFallsBackToNodeScript() {
        val script = writeServerScript()

        val commandLine = TypeSpecStandaloneTspResolver.buildStandaloneServerCommandLine(
            script,
            isTspServerAvailable = { false },
        )

        assertEquals("node", commandLine!!.exePath)
        assertEquals(listOf(script.toString(), "--stdio"), commandLine.parametersList.parameters)
        assertFalse(commandLine.parametersList.parameters.contains("--server"))
        assertSkipCompilerResolve(commandLine)
    }

    @Test
    fun buildStandaloneServerCommandLinePrefersTspServerOverNodeScript() {
        val script = writeServerScript()

        val commandLine = TypeSpecStandaloneTspResolver.buildStandaloneServerCommandLine(
            script,
            isTspServerAvailable = { true },
            tspServerExecutable = { "tsp-server" },
        )

        assertEquals("tsp-server", commandLine!!.exePath)
        assertEquals(listOf("--stdio"), commandLine.parametersList.parameters)
        assertFalse(commandLine.parametersList.parameters.contains(script.toString()))
        assertFalse(commandLine.parametersList.parameters.contains("--server"))
        assertSkipCompilerResolve(commandLine)
    }

    @Test
    fun buildStandaloneServerCommandLineUsesWindowsCmdShim() {
        val script = writeServerScript()

        val commandLine = TypeSpecStandaloneTspResolver.buildStandaloneServerCommandLine(
            script,
            isTspServerAvailable = { true },
            tspServerExecutable = { "C:/npm/tsp-server.cmd" },
        )

        assertEquals("C:/npm/tsp-server.cmd", commandLine!!.exePath)
        assertEquals(listOf("--stdio"), commandLine.parametersList.parameters)
        assertSkipCompilerResolve(commandLine)
    }

    @Test
    fun resolveExecutableOnPathPrefersCmdShimInTheFirstMatchingDirectory() {
        val queried = mutableListOf<String>()

        val resolved = TypeSpecStandaloneTspResolver.resolveExecutableOnPath(
            "tsp-server",
            windows = true,
            pathEntries = listOf("C:/TypeSpec", "C:/npm"),
        ) { directory, name ->
            queried += "$directory/$name"
            if (directory == "C:/npm" && name == "tsp-server.cmd") "C:/npm/tsp-server.cmd" else null
        }

        assertEquals("C:/npm/tsp-server.cmd", resolved)
        assertEquals(
            listOf(
                "C:/TypeSpec/tsp-server.cmd",
                "C:/TypeSpec/tsp-server.bat",
                "C:/TypeSpec/tsp-server.exe",
                "C:/TypeSpec/tsp-server",
                "C:/npm/tsp-server.cmd",
            ),
            queried,
        )
    }

    @Test
    fun resolveExecutableOnPathPrefersCmdShimOverExeInTheSameDirectory() {
        val resolved = TypeSpecStandaloneTspResolver.resolveExecutableOnPath(
            "tsp-server",
            windows = true,
            pathEntries = listOf("C:/npm"),
        ) { _, name ->
            when (name) {
                "tsp-server.cmd" -> "C:/npm/tsp-server.cmd"
                "tsp-server.exe" -> "C:/npm/tsp-server.exe"
                else -> null
            }
        }

        assertEquals("C:/npm/tsp-server.cmd", resolved)
    }

    @Test
    fun resolveExecutableOnPathUsesBareNameOffWindows() {
        val queried = mutableListOf<String>()

        val resolved = TypeSpecStandaloneTspResolver.resolveExecutableOnPath(
            "tsp-server",
            windows = false,
            pathEntries = listOf("/usr/bin", "/usr/local/bin"),
        ) { directory, name ->
            queried += "$directory/$name"
            if (directory == "/usr/local/bin" && name == "tsp-server") "/usr/local/bin/tsp-server" else null
        }

        assertEquals("/usr/local/bin/tsp-server", resolved)
        assertEquals(listOf("/usr/bin/tsp-server", "/usr/local/bin/tsp-server"), queried)
    }

    @Test
    fun resolveExecutableOnPathReturnsNullWhenNothingMatches() {
        assertNull(
            TypeSpecStandaloneTspResolver.resolveExecutableOnPath(
                "tsp-server",
                windows = true,
                pathEntries = listOf("C:/npm"),
            ) { _, _ -> null },
        )
        assertNull(
            TypeSpecStandaloneTspResolver.resolveExecutableOnPath(
                "tsp-server",
                windows = false,
                pathEntries = emptyList(),
            ) { _, _ -> null },
        )
    }

    private fun writeServerScript(): Path {
        val script = tempDir.resolve("cmd").resolve("tsp-server.js")
        Files.createDirectories(script.parent)
        Files.writeString(script, "// mock server")
        return script
    }

    private fun assertSkipCompilerResolve(commandLine: GeneralCommandLine) {
        assertEquals("1", commandLine.environment["TYPESPEC_SKIP_COMPILER_RESOLVE"])
    }
}
