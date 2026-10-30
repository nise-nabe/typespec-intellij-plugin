package com.example.typespec

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

internal object TypeSpecStandaloneTspResolver {
    private const val TSP_SERVER_ON_PATH = "tsp-server"
    private const val NODE_ON_PATH = "node"
    private const val AVAILABILITY_TTL_MILLIS = 60_000L

    private data class CachedExecutable(val path: String?, val checkedAtMillis: Long)

    @Volatile
    private var cachedExecutable: CachedExecutable? = null

    fun isStandaloneTspServerAvailable(nowMillis: Long = System.currentTimeMillis()): Boolean =
        resolveTspServerExecutable(nowMillis) != null

    fun resolveTspServerExecutable(nowMillis: Long = System.currentTimeMillis()): String? {
        val cached = cachedExecutable
        if (cached != null && nowMillis - cached.checkedAtMillis < AVAILABILITY_TTL_MILLIS) {
            return cached.path
        }
        val resolved = findTspServerOnPath()
        cachedExecutable = CachedExecutable(resolved, nowMillis)
        return resolved
    }

    /**
     * npm's Windows shims are `tsp-server.cmd` and `tsp-server.bat`, the same names used for `tsp`.
     * A standalone binary may instead be `tsp-server.exe`. The first PATH entry that contains a
     * candidate wins, and `.cmd` is preferred inside that directory.
     */
    internal fun resolveExecutableOnPath(
        baseName: String,
        windows: Boolean,
        pathEntries: List<String>,
        findInDirectory: (directory: String, fileName: String) -> String?,
    ): String? {
        val names = if (windows) {
            listOf("$baseName.cmd", "$baseName.bat", "$baseName.exe", baseName)
        } else {
            listOf(baseName)
        }
        for (directory in pathEntries) {
            for (name in names) {
                findInDirectory(directory, name)?.let { return it }
            }
        }
        return null
    }

    private fun findTspServerOnPath(): String? {
        val pathValue = PathEnvironmentVariableUtil.getPathVariableValue() ?: return null
        return resolveExecutableOnPath(
            TSP_SERVER_ON_PATH,
            SystemInfo.isWindows,
            PathEnvironmentVariableUtil.getPathDirs(pathValue),
        ) { directory, name ->
            val candidate = File(directory, name)
            if (candidate.isFile && candidate.canExecute()) candidate.absolutePath else null
        }
    }

    /**
     * Prefer `tsp-server --stdio` from PATH. Otherwise run `node <package>/cmd/tsp-server.js --stdio`
     * when [serverScript] exists. `@typespec/compiler` does not accept `tsp --server`.
     */
    fun buildStandaloneServerCommandLine(
        serverScript: Path,
        isTspServerAvailable: () -> Boolean = { isStandaloneTspServerAvailable() },
        tspServerExecutable: () -> String = {
            resolveTspServerExecutable() ?: TSP_SERVER_ON_PATH
        },
        nodeExecutable: String = NODE_ON_PATH,
    ): GeneralCommandLine? {
        if (isTspServerAvailable()) {
            return languageServerCommand(tspServerExecutable(), listOf("--stdio"))
        }
        if (!Files.isRegularFile(serverScript)) {
            return null
        }
        return languageServerCommand(nodeExecutable, listOf(serverScript.toString(), "--stdio"))
    }

    fun resolveServerScriptPath(project: Project): Path? {
        val packageDirectory = Paths.get(TypeSpecCompilerPackageResolver.getSelectedPackage(project).systemDependentPath)
        val script = packageDirectory.resolve(TYPESPEC_LSP_SERVER_SCRIPT)
        return script.takeIf { Files.isRegularFile(it) }
    }

    private fun languageServerCommand(executable: String, parameters: List<String>): GeneralCommandLine {
        val commandLine = GeneralCommandLine(executable)
        parameters.forEach(commandLine::addParameter)
        commandLine.environment["TYPESPEC_SKIP_COMPILER_RESOLVE"] = "1"
        return commandLine
    }
}
