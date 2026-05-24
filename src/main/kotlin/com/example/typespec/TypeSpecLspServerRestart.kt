package com.example.typespec

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.platform.lsp.api.LspServerManager

internal fun restartTypeSpecServerAsync(project: Project) {
    if (!TypeSpecActivationHelper.isEnabledInSettings(project)) {
        return
    }
    if (ApplicationManager.getApplication().isUnitTestMode) {
        return
    }
    ApplicationManager.getApplication().invokeLater({
        LspServerManager.getInstance(project).stopAndRestartIfNeeded(TypeSpecLspServerSupportProvider::class.java)
    }, project.disposed)
}
