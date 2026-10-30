package com.example.typespec

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.platform.lsp.api.LspClientManager
import com.intellij.platform.lsp.api.LspServerState
import org.eclipse.lsp4j.DidChangeConfigurationParams

internal class TypeSpecLspServiceSettingsListener : TypeSpecServiceSettingsListener {
    override fun onServiceSettingsChanged(project: Project) {
        restartTypeSpecServerAsync(project)
    }

    override fun onWorkspaceConfigurationChanged(project: Project) {
        // tsp-server 1.17 copies workspace/didChangeConfiguration into its cached config and does not
        // recompile. Push the notification, then restart so open files are type-checked again.
        pushTypeSpecWorkspaceConfiguration(project)
        restartTypeSpecServerAsync(project)
    }
}

internal fun pushTypeSpecWorkspaceConfiguration(project: Project) {
    if (project.isDisposed || ApplicationManager.getApplication().isUnitTestMode) {
        return
    }
    val settings = TypeSpecServiceSettings.getInstance(project)
    val params = DidChangeConfigurationParams(
        TypeSpecWorkspaceConfiguration.changeNotificationSettings(settings.lspEmit, settings.entrypoints),
    )
    val clients = LspClientManager.getInstance(project)
        .getClients(TypeSpecLspIntegrationProvider::class.java)
        .toList()
    for (client in clients) {
        if (client.state != LspServerState.Running) {
            continue
        }
        client.sendNotification { server ->
            server.workspaceService.didChangeConfiguration(params)
        }
    }
}
