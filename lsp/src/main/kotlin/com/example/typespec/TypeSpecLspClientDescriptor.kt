package com.example.typespec

import com.intellij.execution.ExecutionException
import com.intellij.execution.process.KillableProcessHandler
import com.intellij.execution.process.OSProcessHandler
import com.intellij.lang.typescript.lsp.JSNodeLspClientDescriptor
import com.intellij.openapi.project.Project
import org.eclipse.lsp4j.ConfigurationItem

@Suppress("UnstableApiUsage")
class TypeSpecLspClientDescriptor(project: Project) :
    JSNodeLspClientDescriptor(project, TypeSpecLspServerActivationRule, "TypeSpec") {
    override fun getWorkspaceConfiguration(item: ConfigurationItem): Any? {
        val settings = TypeSpecServiceSettings.getInstance(project)
        return when (
            val response = TypeSpecWorkspaceConfiguration.responseFor(
                section = item.section,
                scopeUri = item.scopeUri,
                lspEmit = settings.lspEmit,
                entrypoints = settings.entrypoints,
            )
        ) {
            is TypeSpecWorkspaceConfiguration.Response.Value -> response.value
            TypeSpecWorkspaceConfiguration.Response.Unhandled -> super.getWorkspaceConfiguration(item)
        }
    }

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
