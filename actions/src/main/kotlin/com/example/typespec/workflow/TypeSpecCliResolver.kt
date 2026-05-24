package com.example.typespec.workflow

import com.example.typespec.TYPESPEC_COMPILER_CLI_SCRIPT
import com.example.typespec.TYPESPEC_OPENAPI3_CLI_SCRIPT
import com.example.typespec.TYPESPEC_OPENAPI3_PACKAGE_NAME
import com.example.typespec.TypeSpecCompilerPackageResolver
import com.intellij.openapi.project.Project
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

internal object TypeSpecCliResolver {
    fun resolveTspCli(project: Project, contextDirectory: Path): TypeSpecCliCommand? {
        val compilerPackage = TypeSpecCompilerPackageResolver.getSelectedPackage(project)
        val packageDirectory = Paths.get(compilerPackage.systemDependentPath)
        return resolveNodeScriptCli(
            script = packageDirectory.resolve(TYPESPEC_COMPILER_CLI_SCRIPT),
            workingDirectory = contextDirectory,
            displayName = packageDirectory.resolve(TYPESPEC_COMPILER_CLI_SCRIPT).fileName.toString(),
        )
    }

    fun resolveOpenApi3Cli(project: Project, workingDirectory: Path): TypeSpecCliCommand? {
        val packageDirectories = linkedSetOf(
            workingDirectory.resolve("node_modules").resolve(TYPESPEC_OPENAPI3_PACKAGE_NAME),
            Paths.get(TypeSpecCompilerPackageResolver.getSelectedPackage(project).systemDependentPath)
                .parent
                ?.resolve(TYPESPEC_OPENAPI3_PACKAGE_NAME),
        ).filterNotNull()

        for (packageDirectory in packageDirectories) {
            val command = resolveNodeScriptCli(
                script = packageDirectory.resolve(TYPESPEC_OPENAPI3_CLI_SCRIPT),
                workingDirectory = workingDirectory,
                displayName = "tsp-openapi3",
            )
            if (command != null) {
                return command
            }
        }
        return null
    }

    private fun resolveNodeScriptCli(
        script: Path,
        workingDirectory: Path,
        displayName: String,
    ): TypeSpecCliCommand? {
        if (!Files.isRegularFile(script)) {
            return null
        }
        val node = resolveNodeExecutable() ?: return null
        return TypeSpecCliCommand(
            executable = node,
            args = listOf(script.toString()),
            workingDirectory = workingDirectory,
            displayCommand = "$node $displayName",
        )
    }

    private fun resolveNodeExecutable(): String? =
        sequenceOf("node", "node.exe")
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
