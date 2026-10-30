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
            // Null when the package script is absent. tsp-server on PATH can still start;
            // the node fallback runs only when that script exists.
            val commandLine = TypeSpecStandaloneTspResolver.buildStandaloneServerCommandLine(
                TypeSpecStandaloneTspResolver.resolveServerScriptPath(project),
            ) ?: throw e
            return KillableProcessHandler(commandLine)
        }
    }
}
