package com.example.typespec

import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.lang.typescript.lsp.LspServerLoader
import com.intellij.lang.typescript.lsp.LspServerPackageDescriptor
import com.intellij.lang.typescript.lsp.PackageVersion
import com.intellij.lang.typescript.lsp.createPackage
import com.intellij.lang.typescript.lsp.defaultPackageKey
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.lsp.api.LspServerManager
import java.nio.file.Files
import java.nio.file.Paths

internal const val TYPESPEC_COMPILER_PACKAGE_NAME = "@typespec/compiler"
private const val TYPESPEC_SERVER_SCRIPT_PATH = "/cmd/tsp-server.js"

@Suppress("UnstableApiUsage")
object TypeSpecLspServerLoader : LspServerLoader(TypeSpecLspServerPackageDescriptor) {
    override fun getSelectedPackage(project: Project): NodePackage =
        TypeSpecServiceSettings.getInstance(project).lspServerPackage

    internal fun isSelectedPackageResolvable(project: Project): Boolean =
        isPackageWithServerScript(getSelectedPackage(project))

    internal fun isPackageWithServerScript(nodePackage: NodePackage): Boolean {
        val packageDirectory = Paths.get(nodePackage.systemDependentPath)
        if (!Files.isDirectory(packageDirectory)) {
            return false
        }
        return Files.exists(packageDirectory.resolve(TYPESPEC_SERVER_SCRIPT_PATH.removePrefix("/")))
    }
}

@Suppress("UnstableApiUsage")
internal object TypeSpecLspServerPackageDescriptor : LspServerPackageDescriptor(
    TYPESPEC_COMPILER_PACKAGE_NAME,
    PackageVersion.downloadable("1.10.0"),
    TYPESPEC_SERVER_SCRIPT_PATH,
) {
    override val registryVersion: String
        get() = ""
}

enum class TypeSpecServiceMode {
    ENABLED,
    DISABLED,
}

@Service(Service.Level.PROJECT)
@State(name = "TypeSpecServiceSettings", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class TypeSpecServiceSettings(
    private val project: Project,
) : SimplePersistentStateComponent<TypeSpecServiceState>(TypeSpecServiceState()) {
    var serviceMode: TypeSpecServiceMode
        get() = TypeSpecServiceMode.entries.find { it.name == state.serviceModeName } ?: TypeSpecServiceMode.ENABLED
        set(value) {
            val changed = state.serviceModeName != value.name
            state.serviceModeName = value.name
            if (changed) {
                restartTypeSpecServerAsync(project)
            }
        }

    var lspServerPackage: NodePackage
        get() = createPackage(state.lspServerPackageName, TypeSpecLspServerLoader.packageDescriptor.serverPackage)
        set(value) {
            val path = value.systemDependentPath
            val changed = state.lspServerPackageName != path
            state.lspServerPackageName = path
            if (changed) {
                restartTypeSpecServerAsync(project)
            }
        }

    companion object {
        fun getInstance(project: Project): TypeSpecServiceSettings = project.service()
    }
}

class TypeSpecServiceState : BaseState() {
    var serviceModeName by string(TypeSpecServiceMode.ENABLED.name)
    var lspServerPackageName by string(defaultPackageKey)
}

internal fun restartTypeSpecServerAsync(project: Project) {
    if (ApplicationManager.getApplication().isUnitTestMode) {
        return
    }
    ApplicationManager.getApplication().invokeLater({
        LspServerManager.getInstance(project).stopAndRestartIfNeeded(TypeSpecLspServerSupportProvider::class.java)
    }, project.disposed)
}
