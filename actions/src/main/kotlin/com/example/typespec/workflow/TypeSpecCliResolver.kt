package com.example.typespec.workflow

import com.example.typespec.TypeSpecLspServerLoader
import com.intellij.openapi.project.Project
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

internal object TypeSpecCliResolver {
    private const val TSP_JS_RELATIVE = "cmd/tsp.js"

    fun resolveTspCli(project: Project, contextDirectory: Path): TypeSpecCliCommand? {
        val compilerPackage = TypeSpecLspServerLoader.getSelectedPackage(project)
        val packageDirectory = Paths.get(compilerPackage.systemDependentPath)
        if (!Files.isDirectory(packageDirectory)) {
            return null
        }
        val tspJs = packageDirectory.resolve(TSP_JS_RELATIVE)
        if (!Files.isRegularFile(tspJs)) {
            return null
        }
        val node = resolveNodeExecutable() ?: return null
        return TypeSpecCliCommand(
            executable = node,
            args = listOf(tspJs.toString()),
            workingDirectory = contextDirectory,
            displayCommand = "$node ${tspJs.fileName}",
        )
    }

    fun resolveNpxCli(workingDirectory: Path): TypeSpecCliCommand? {
        val npx = resolveNpxExecutable() ?: return null
        return TypeSpecCliCommand(
            executable = npx,
            args = emptyList(),
            workingDirectory = workingDirectory,
            displayCommand = npx,
        )
    }

    private fun resolveNodeExecutable(): String? =
        sequenceOf("node", "node.exe")
            .firstOrNull { isOnPath(it) }

    private fun resolveNpxExecutable(): String? =
        sequenceOf("npx", "npx.cmd")
            .firstOrNull { isOnPath(it) }

    private fun isOnPath(command: String): Boolean {
        val path = System.getenv("PATH") ?: return false
        val extensions = if (System.getProperty("os.name").lowercase().contains("win")) {
            System.getenv("PATHEXT")?.split(";")?.map { it.lowercase() } ?: listOf(".exe", ".cmd", ".bat")
        } else {
            listOf("")
        }
        return path.split(java.io.File.pathSeparator).any { directory ->
            extensions.any { extension ->
                Files.isRegularFile(Paths.get(directory, command + extension))
            }
        }
    }
}
