package com.example.typespec

import com.intellij.lang.typescript.compiler.languageService.TypeScriptLanguageServiceUtil
import com.intellij.lang.typescript.lsp.JSNodeLspServerDescriptor
import com.intellij.lang.typescript.lsp.LspServerActivationRule
import com.intellij.lang.typescript.lsp.ServiceActivationHelper
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerSupportProvider

@Suppress("UnstableApiUsage")
class TypeSpecLspServerSupportProvider : LspServerSupportProvider {
    override fun fileOpened(project: Project, file: VirtualFile, serverStarter: LspServerSupportProvider.LspServerStarter) {
        if (TypeSpecLspServerActivationRule.isEnabledAndAvailable(project, file)) {
            serverStarter.ensureServerStarted(TypeSpecLspServerDescriptor(project))
        }
    }
}

@Suppress("UnstableApiUsage")
object TypeSpecLspServerActivationRule : LspServerActivationRule(TypeSpecLspServerLoader, TypeSpecActivationHelper) {
    override fun isFileAcceptable(file: VirtualFile): Boolean {
        if (!TypeScriptLanguageServiceUtil.IS_VALID_FILE_FOR_SERVICE.value(file)) {
            return false
        }

        return file.fileType == TypeSpecFileType
    }
}

@Suppress("UnstableApiUsage")
object TypeSpecActivationHelper : ServiceActivationHelper {
    override fun isProjectContext(project: Project, context: VirtualFile): Boolean =
        context.fileType == TypeSpecFileType

    override fun isEnabledInSettings(project: Project): Boolean = true

    override fun isEnabledByEnvironment(project: Project, context: VirtualFile): Boolean =
        isEnvironmentSupported(ApplicationManager.getApplication().isUnitTestMode)

    internal fun isEnvironmentSupported(isUnitTestMode: Boolean): Boolean =
        !isUnitTestMode

    internal fun isEnvironmentSupported(isUnitTestMode: Boolean, hasProjectLocalCompilerPackage: Boolean): Boolean =
        isEnvironmentSupported(isUnitTestMode)
}

@Suppress("UnstableApiUsage")
class TypeSpecLspServerDescriptor(project: Project) : JSNodeLspServerDescriptor(project, TypeSpecLspServerActivationRule, "TypeSpec")
