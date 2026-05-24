package com.example.typespec

import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.lang.typescript.lsp.LspServerPackageDescriptor
import com.intellij.lang.typescript.lsp.PackageVersion
import com.intellij.lang.typescript.lsp.createPackage
import com.intellij.lang.typescript.lsp.defaultPackageKey
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

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
                notifySettingsChanged()
            }
        }

    var lspServerPackage: NodePackage
        get() = createPackage(state.lspServerPackageName, TypeSpecCompilerPackageDescriptor.serverPackage)
        set(value) {
            val path = value.systemDependentPath
            val changed = state.lspServerPackageName != path
            state.lspServerPackageName = path
            if (changed) {
                notifySettingsChanged()
            }
        }

    private fun notifySettingsChanged() {
        TypeSpecCompilerPackageResolutionCache.getInstance(project).invalidate()
        project.messageBus.syncPublisher(TypeSpecServiceSettingsListener.TOPIC).onServiceSettingsChanged(project)
    }

    companion object {
        fun getInstance(project: Project): TypeSpecServiceSettings = project.service()
    }
}

class TypeSpecServiceState : BaseState() {
    var serviceModeName by string(TypeSpecServiceMode.ENABLED.name)
    var lspServerPackageName by string(defaultPackageKey)
}

@Suppress("UnstableApiUsage")
private object TypeSpecCompilerPackageDescriptor : LspServerPackageDescriptor(
    TYPESPEC_COMPILER_PACKAGE_NAME,
    PackageVersion.downloadable("1.10.0"),
    "/cmd/tsp.js",
) {
    override val registryVersion: String
        get() = ""
}
