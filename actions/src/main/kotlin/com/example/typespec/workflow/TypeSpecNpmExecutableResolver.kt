package com.example.typespec.workflow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
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
        val npmCandidate = siblingNpmExecutable(nodeExecutable)
        return if (Files.isRegularFile(npmCandidate)) npmCandidate.toString() else null
    }

    private fun siblingNpmExecutable(nodeExecutable: Path): Path {
        val npmName = if (nodeExecutable.fileName.toString() == "node.exe") "npm.cmd" else "npm"
        return nodeExecutable.parent.resolve(npmName)
    }
}
