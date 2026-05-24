package com.example.typespec.workflow

import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.javascript.nodejs.interpreter.local.NodeJsLocalInterpreter
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import java.nio.file.Files
import java.nio.file.Paths

internal object TypeSpecNodeExecutableResolver {
    private const val NODE_ON_PATH = "node"

    fun resolveExecutable(project: Project): String {
        if (!ApplicationManager.getApplication().isUnitTestMode) {
            resolveFromNodeJsInterpreter(project)?.let { return it }
        }
        return NODE_ON_PATH
    }

    private fun resolveFromNodeJsInterpreter(project: Project): String? {
        val interpreter = NodeJsInterpreterManager.getInstance(project).interpreter as? NodeJsLocalInterpreter
            ?: return null
        val path = interpreter.interpreterSystemIndependentPath
        if (path.isBlank()) {
            return null
        }
        val executable = Paths.get(path)
        return if (Files.isRegularFile(executable)) path else null
    }
}
