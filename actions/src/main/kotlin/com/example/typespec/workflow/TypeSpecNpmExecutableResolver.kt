package com.example.typespec.workflow

import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.javascript.nodejs.interpreter.local.NodeJsLocalInterpreter
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

internal object TypeSpecNpmExecutableResolver {
    private const val NPM_ON_PATH = "npm"

    fun resolveExecutable(project: Project): String {
        if (!ApplicationManager.getApplication().isUnitTestMode) {
            resolveFromNodeJsInterpreter(project)?.let { return it }
        }
        return NPM_ON_PATH
    }

    private fun resolveFromNodeJsInterpreter(project: Project): String? {
        val interpreter = NodeJsInterpreterManager.getInstance(project).interpreter as? NodeJsLocalInterpreter
            ?: return null
        val nodePath = interpreter.interpreterSystemIndependentPath
        if (nodePath.isBlank()) {
            return null
        }
        val nodeExecutable = Paths.get(nodePath)
        if (!Files.isRegularFile(nodeExecutable)) {
            return null
        }
        val npmCandidate = siblingNpmExecutable(nodeExecutable)
        return if (Files.isRegularFile(npmCandidate)) npmCandidate.toString() else null
    }

    private fun siblingNpmExecutable(nodeExecutable: Path): Path {
        val fileName = nodeExecutable.fileName.toString()
        val npmName = if (fileName == "node.exe") "npm.cmd" else "npm"
        return nodeExecutable.parent.resolve(npmName)
    }
}
