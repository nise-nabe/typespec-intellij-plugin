package com.example.typespec

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServer
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.platform.lsp.api.lsWidget.LspServerWidgetItem

@Suppress("UnstableApiUsage")
class TypeSpecLspServerSupportProvider : LspServerSupportProvider {
    override fun fileOpened(
        project: Project,
        file: VirtualFile,
        serverStarter: LspServerSupportProvider.LspServerStarter,
    ) {
        TypeSpecLspPackageResolutionCoordinator.onTypeSpecFileOpened(project, file, serverStarter)
    }

    override fun createLspServerWidgetItem(
        lspServer: LspServer,
        currentFile: VirtualFile?,
    ): LspServerWidgetItem = LspServerWidgetItem(
        lspServer,
        currentFile,
        AllIcons.FileTypes.Any_type,
        settingsPageClass = TypeSpecSettingsConfigurable::class.java,
    )
}
