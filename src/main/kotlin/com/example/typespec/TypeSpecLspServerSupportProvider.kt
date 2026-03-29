package com.example.typespec

import com.intellij.lang.typescript.compiler.languageService.TypeScriptLanguageServiceUtil
import com.intellij.lang.typescript.lsp.JSNodeLspServerDescriptor
import com.intellij.lang.typescript.lsp.LspServerActivationRule
import com.intellij.lang.typescript.lsp.LspServerLoader
import com.intellij.lang.typescript.lsp.LspServerPackageDescriptor
import com.intellij.lang.typescript.lsp.PackageVersion
import com.intellij.lang.typescript.lsp.ServiceActivationHelper
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerSupportProvider
import org.jetbrains.annotations.ApiStatus

class TypeSpecLspServerSupportProvider : LspServerSupportProvider {
    override fun fileOpened(project: Project, file: VirtualFile, serverStarter: LspServerSupportProvider.LspServerStarter) {
        if (TypeSpecLspServerActivationRule.isEnabledAndAvailable(project, file)) {
            serverStarter.ensureServerStarted(TypeSpecLspServerDescriptor(project))
        }
    }
}

private const val TYPESPEC_COMPILER_PACKAGE_NAME = "@typespec/compiler"
private const val TYPESPEC_SERVER_SCRIPT_PATH = "/cmd/tsp-server.js"

@OptIn(ApiStatus.Experimental::class)
private object TypeSpecLspServerPackageDescriptor : LspServerPackageDescriptor(
    TYPESPEC_COMPILER_PACKAGE_NAME,
    PackageVersion.downloadable("1.10.0"),
    TYPESPEC_SERVER_SCRIPT_PATH,
) {
    override val registryVersion: String
        get() = ""
}

@OptIn(ApiStatus.Experimental::class)
object TypeSpecLspServerLoader : LspServerLoader(TypeSpecLspServerPackageDescriptor)

@OptIn(ApiStatus.Experimental::class)
object TypeSpecLspServerActivationRule : LspServerActivationRule(TypeSpecLspServerLoader, TypeSpecActivationHelper) {
    override fun isFileAcceptable(file: VirtualFile): Boolean {
        if (!TypeScriptLanguageServiceUtil.IS_VALID_FILE_FOR_SERVICE.value(file)) {
            return false
        }

        return file.fileType == TypeSpecFileType
    }
}

@OptIn(ApiStatus.Experimental::class)
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

@OptIn(ApiStatus.Experimental::class)
class TypeSpecLspServerDescriptor(project: Project) : JSNodeLspServerDescriptor(project, TypeSpecLspServerActivationRule, "TypeSpec")
