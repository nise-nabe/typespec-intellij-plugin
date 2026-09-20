package com.example.typespec

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

internal object TypeSpecStandaloneTspResolver {
    private const val TSP_ON_PATH = "tsp"
    private const val AVAILABILITY_TTL_MILLIS = 60_000L

    @Volatile
    private var cachedAvailability: Boolean? = null

    @Volatile
    private var checkedAtMillis: Long = 0L

    fun isStandaloneTspAvailable(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val cached = cachedAvailability
        if (cached != null && nowMillis - checkedAtMillis < AVAILABILITY_TTL_MILLIS) {
            return cached
        }
        val result = probeTspOnPath()
        cachedAvailability = result
        checkedAtMillis = nowMillis
        return result
    }

    private fun probeTspOnPath(): Boolean =
        try {
            val process = ProcessBuilder(TSP_ON_PATH, "--version")
                .redirectErrorStream(true)
                .start()
            val finished = process.waitFor(5, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
            }
            finished && process.exitValue() == 0
        } catch (_: Exception) {
            false
        }

    fun buildStandaloneServerCommandLine(
        serverScript: Path,
        isTspAvailable: () -> Boolean = { isStandaloneTspAvailable() },
    ): GeneralCommandLine? {
        if (!Files.isRegularFile(serverScript) || !isTspAvailable()) {
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
