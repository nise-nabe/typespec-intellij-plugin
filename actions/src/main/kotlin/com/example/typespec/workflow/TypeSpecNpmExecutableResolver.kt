package com.example.typespec.workflow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import java.nio.file.Files
import java.nio.file.Path

internal object TypeSpecNpmExecutableResolver {
    private const val NPM_ON_PATH = "npm"

    fun resolveExecutable(project: Project): String {
        if (!ApplicationManager.getApplication().isUnitTestMode) {
            resolveFromNodeJsInterpreter(project)?.let { return it }
        }
        return NPM_ON_PATH
    }

    private fun resolveFromNodeJsInterpreter(project: Project): String? {
        val nodeExecutable = TypeSpecNodeExecutableResolver.resolveLocalInterpreterPath(project)
            ?: return null
        return siblingNpmCandidates(nodeExecutable)
            .firstOrNull { Files.isRegularFile(it) }
            ?.toString()
    }

    private fun siblingNpmCandidates(nodeExecutable: Path): List<Path> {
        val names = if (SystemInfo.isWindows) listOf("npm.cmd", "npm.bat", "npm") else listOf("npm")
        return names.map { nodeExecutable.parent.resolve(it) }
    }
}
