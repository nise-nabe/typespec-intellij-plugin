package com.example.typespec

import com.intellij.execution.ExecutionException
import com.intellij.execution.process.KillableProcessHandler
import com.intellij.execution.process.OSProcessHandler
import com.intellij.lang.typescript.lsp.JSNodeLspClientDescriptor
import com.intellij.openapi.project.Project

@Suppress("UnstableApiUsage")
class TypeSpecLspClientDescriptor(project: Project) :
    JSNodeLspClientDescriptor(project, TypeSpecLspServerActivationRule, "TypeSpec") {
    override fun startServerProcess(): OSProcessHandler {
        try {
            return super.startServerProcess()
        } catch (e: ExecutionException) {
            val serverScript = TypeSpecStandaloneTspResolver.resolveServerScriptPath(project)
            val commandLine = serverScript?.let(TypeSpecStandaloneTspResolver::buildStandaloneServerCommandLine)
                ?: throw e
            return KillableProcessHandler(commandLine)
        }
    }
}
