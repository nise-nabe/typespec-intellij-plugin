package com.example.typespec

import com.intellij.lang.typescript.compiler.languageService.TypeScriptLanguageServiceUtil
import com.intellij.lang.typescript.lsp.JSNodeLspServerDescriptor
import com.intellij.lang.typescript.lsp.LspServerActivationRule
import com.intellij.lang.typescript.lsp.ServiceActivationHelper
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

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
