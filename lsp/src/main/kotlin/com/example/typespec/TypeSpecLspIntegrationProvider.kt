package com.example.typespec

import com.intellij.icons.AllIcons
import com.intellij.lang.typescript.compiler.languageService.TypeScriptLanguageServiceUtil
import com.intellij.lang.typescript.lsp.JSNodeLspClientDescriptor
import com.intellij.lang.typescript.lsp.LspServerActivationRule
import com.intellij.lang.typescript.lsp.ServiceActivationHelper
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspClient
import com.intellij.platform.lsp.api.LspClientManager
import com.intellij.platform.lsp.api.LspIntegrationProvider
import com.intellij.platform.lsp.api.lsWidget.LspClientWidgetItem

@Suppress("UnstableApiUsage")
class TypeSpecLspIntegrationProvider : LspIntegrationProvider {
    override fun fileOpened(
        project: Project,
        file: VirtualFile,
        clientStarter: LspIntegrationProvider.LspClientStarter,
    ) {
        TypeSpecLspNotifications.onTypeSpecFileOpened(project, file)
        if (TypeSpecLspServerActivationRule.isEnabledAndAvailable(project, file)) {
            clientStarter.ensureClientStarted(TypeSpecLspClientDescriptor(project))
        }
    }

    override fun createWidgetItem(
        lspClient: LspClient,
        currentFile: VirtualFile?,
    ): LspClientWidgetItem = LspClientWidgetItem(
        lspClient,
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
class TypeSpecLspClientDescriptor(project: Project) : JSNodeLspClientDescriptor(project, TypeSpecLspServerActivationRule, "TypeSpec")

fun restartTypeSpecServerAsync(project: Project) {
    if (ApplicationManager.getApplication().isUnitTestMode) {
        return
    }
    ApplicationManager.getApplication().invokeLater({
        LspClientManager.getInstance(project).stopAndRestartClientsIfNeeded(TypeSpecLspIntegrationProvider::class.java)
    }, project.disposed)
}
