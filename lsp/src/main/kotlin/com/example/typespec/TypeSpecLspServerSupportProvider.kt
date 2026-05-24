package com.example.typespec

import com.intellij.icons.AllIcons
import com.intellij.lang.typescript.compiler.languageService.TypeScriptLanguageServiceUtil
import com.intellij.lang.typescript.lsp.JSNodeLspServerDescriptor
import com.intellij.lang.typescript.lsp.LspServerActivationRule
import com.intellij.lang.typescript.lsp.ServiceActivationHelper
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServer
import com.intellij.platform.lsp.api.LspServerManager
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.platform.lsp.api.lsWidget.LspServerWidgetItem

@Suppress("UnstableApiUsage")
class TypeSpecLspServerSupportProvider : LspServerSupportProvider {
    override fun fileOpened(
        project: Project,
        file: VirtualFile,
        serverStarter: LspServerSupportProvider.LspServerStarter,
    ) {
        TypeSpecLspNotifications.onTypeSpecFileOpened(project, file)
        if (TypeSpecLspServerActivationRule.isEnabledAndAvailable(project, file)) {
            serverStarter.ensureServerStarted(TypeSpecLspServerDescriptor(project))
        }
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

@Suppress("UnstableApiUsage")
object TypeSpecLspServerActivationRule : LspServerActivationRule(TypeSpecLspServerLoader, TypeSpecActivationHelper) {
    override fun isFileAcceptable(file: VirtualFile): Boolean {
        if (!TypeScriptLanguageServiceUtil.IS_VALID_FILE_FOR_SERVICE.value(file)) {
            return false
        }

        return file.fileType == TypeSpecFileType
    }

    override fun restartService(project: Project) {
        restartTypeSpecServerAsync(project)
    }

    internal fun isEligibleExceptPackageResolution(project: Project, file: VirtualFile): Boolean =
        isEnabled(project, file)
}

@Suppress("UnstableApiUsage")
object TypeSpecActivationHelper : ServiceActivationHelper {
    override fun isProjectContext(project: Project, context: VirtualFile): Boolean =
        context.fileType == TypeSpecFileType

    override fun isEnabledInSettings(project: Project): Boolean =
        TypeSpecServiceSettings.getInstance(project).serviceMode == TypeSpecServiceMode.ENABLED

    override fun isEnabledByEnvironment(project: Project, context: VirtualFile): Boolean =
        isEnvironmentSupported(ApplicationManager.getApplication().isUnitTestMode)

    internal fun isEnvironmentSupported(isUnitTestMode: Boolean): Boolean =
        !isUnitTestMode
}

@Suppress("UnstableApiUsage")
class TypeSpecLspServerDescriptor(project: Project) : JSNodeLspServerDescriptor(project, TypeSpecLspServerActivationRule, "TypeSpec")

internal fun restartTypeSpecServerAsync(project: Project) {
    if (ApplicationManager.getApplication().isUnitTestMode) {
        return
    }
    ApplicationManager.getApplication().invokeLater({
        LspServerManager.getInstance(project).stopAndRestartIfNeeded(TypeSpecLspServerSupportProvider::class.java)
    }, project.disposed)
}
