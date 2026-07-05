package com.example.typespec

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.lang.typescript.lsp.JSNodeLspClientDescriptor
import com.intellij.openapi.project.Project

@Suppress("UnstableApiUsage")
class TypeSpecLspClientDescriptor(project: Project) : JSNodeLspClientDescriptor(project, TypeSpecLspServerActivationRule, "TypeSpec") {
    override fun createCommandLine(): GeneralCommandLine? {
        val defaultCommandLine = super.createCommandLine()
        if (defaultCommandLine != null) {
            return defaultCommandLine
        }
        val serverScript = TypeSpecStandaloneTspResolver.resolveServerScriptPath(project) ?: return null
        return TypeSpecStandaloneTspResolver.buildStandaloneServerCommandLine(serverScript)
    }
}
