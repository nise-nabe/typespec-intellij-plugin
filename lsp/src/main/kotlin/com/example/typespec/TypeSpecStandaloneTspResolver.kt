package com.example.typespec

import com.intellij.execution.configurations.GeneralCommandLine
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

internal object TypeSpecStandaloneTspResolver {
    private const val TSP_ON_PATH = "tsp"

    fun isStandaloneTspAvailable(): Boolean =
        try {
            val process = ProcessBuilder(TSP_ON_PATH, "--version")
                .redirectErrorStream(true)
                .start()
            val finished = process.waitFor(5, TimeUnit.SECONDS)
            finished && process.exitValue() == 0
        } catch (_: Exception) {
            false
        }

    fun buildStandaloneServerCommandLine(serverScript: Path): GeneralCommandLine? {
        if (!Files.isRegularFile(serverScript) || !isStandaloneTspAvailable()) {
            return null
        }
        return GeneralCommandLine(TSP_ON_PATH)
            .withParameters("--server", serverScript.toString(), "--stdio")
            .apply {
                environment["TYPESPEC_SKIP_COMPILER_RESOLVE"] = "1"
            }
    }

    fun resolveServerScriptPath(project: Project): Path? {
        val packageDirectory = Paths.get(TypeSpecCompilerPackageResolver.getSelectedPackage(project).systemDependentPath)
        val script = packageDirectory.resolve(TYPESPEC_LSP_SERVER_SCRIPT)
        return script.takeIf { Files.isRegularFile(it) }
    }
}
