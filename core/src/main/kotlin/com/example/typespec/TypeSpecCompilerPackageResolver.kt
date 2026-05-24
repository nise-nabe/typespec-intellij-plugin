package com.example.typespec

import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.openapi.project.Project
import java.nio.file.Files
import java.nio.file.Path

object TypeSpecCompilerPackageResolver {
    fun getSelectedPackage(project: Project): NodePackage =
        TypeSpecServiceSettings.getInstance(project).lspServerPackage

    fun isCompilerCliResolvable(project: Project): Boolean =
        TypeSpecPackageResolutionCache.getInstance(project).getOrCompute(project).compilerCliResolvable

    fun hasCompilerCli(packageDirectory: Path): Boolean {
        if (!Files.isDirectory(packageDirectory)) {
            return false
        }
        return Files.isRegularFile(packageDirectory.resolve(TYPESPEC_COMPILER_CLI_SCRIPT))
    }

    fun hasLspServerScript(packageDirectory: Path): Boolean {
        if (!Files.isDirectory(packageDirectory)) {
            return false
        }
        return Files.isRegularFile(packageDirectory.resolve(TYPESPEC_LSP_SERVER_SCRIPT))
    }
}
