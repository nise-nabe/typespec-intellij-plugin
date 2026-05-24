package com.example.typespec.workflow

import com.example.typespec.TYPESPEC_COMPILER_CLI_SCRIPT
import com.example.typespec.TYPESPEC_OPENAPI3_CLI_SCRIPT
import com.example.typespec.TypeSpecCompilerPackageResolver
import com.example.typespec.TypeSpecOpenApi3PackageResolver
import com.intellij.openapi.project.Project
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

internal object TypeSpecCliResolver {
    fun resolveTspCli(project: Project, contextDirectory: Path): TypeSpecCliCommand? {
        val compilerPackage = TypeSpecCompilerPackageResolver.getSelectedPackage(project)
        val packageDirectory = Paths.get(compilerPackage.systemDependentPath)
        return resolveNodeScriptCli(
            project = project,
            script = packageDirectory.resolve(TYPESPEC_COMPILER_CLI_SCRIPT),
            workingDirectory = contextDirectory,
            displayName = TYPESPEC_COMPILER_CLI_SCRIPT.substringAfterLast('/'),
        )
    }

    fun isOpenApi3CliResolvable(project: Project): Boolean =
        TypeSpecCompilerPackageResolver.isOpenApi3CliResolvable(project)

    fun resolveOpenApi3Cli(project: Project, workingDirectory: Path): TypeSpecCliCommand? {
        val compilerPackageDirectory = Paths.get(
            TypeSpecCompilerPackageResolver.getSelectedPackage(project).systemDependentPath,
        )
        val packageDirectory = TypeSpecOpenApi3PackageResolver.findPackageDirectory(
            probeDirectory = workingDirectory,
            compilerPackageDirectory = compilerPackageDirectory,
        ) ?: return null
        return resolveNodeScriptCli(
            project = project,
            script = packageDirectory.resolve(TYPESPEC_OPENAPI3_CLI_SCRIPT),
            workingDirectory = workingDirectory,
            displayName = "tsp-openapi3",
        )
    }

    private fun resolveNodeScriptCli(
        project: Project,
        script: Path,
        workingDirectory: Path,
        displayName: String,
    ): TypeSpecCliCommand? {
        if (!Files.isRegularFile(script)) {
            return null
        }
        val nodeExecutable = TypeSpecNodeExecutableResolver.resolveExecutable(project)
        return TypeSpecCliCommand(
            executable = nodeExecutable,
            args = listOf(script.toString()),
            workingDirectory = workingDirectory,
            displayCommand = "$nodeExecutable $displayName",
        )
    }
}
