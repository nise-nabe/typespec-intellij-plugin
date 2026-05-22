package com.example.typespec

import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.lang.typescript.lsp.LspServerLoader
import com.intellij.lang.typescript.lsp.LspServerPackageDescriptor
import com.intellij.lang.typescript.lsp.PackageVersion
import com.intellij.lang.typescript.lsp.createPackage
import com.intellij.lang.typescript.lsp.defaultPackageKey
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Suppress("UnstableApiUsage")
object TypeSpecLspServerLoader : LspServerLoader(TypeSpecLspServerPackageDescriptor) {
    override fun getSelectedPackage(project: Project): NodePackage {
        return TypeSpecServiceSettings.getInstance(project).lspServerPackage
    }
}

private const val TYPESPEC_COMPILER_PACKAGE_NAME = "@typespec/compiler"
private const val TYPESPEC_SERVER_SCRIPT_PATH = "/cmd/tsp-server.js"

@Suppress("UnstableApiUsage")
private object TypeSpecLspServerPackageDescriptor : LspServerPackageDescriptor(
    TYPESPEC_COMPILER_PACKAGE_NAME,
    PackageVersion.downloadable("1.10.0"),
    TYPESPEC_SERVER_SCRIPT_PATH,
) {
    override val registryVersion: String
        get() = ""
}

@Service(Service.Level.PROJECT)
class TypeSpecServiceSettings: SimplePersistentStateComponent<TypeSpecServiceState>(TypeSpecServiceState()) {
    var lspServerPackage: NodePackage
        get() = createPackage(state.lspServerPackageName, TypeSpecLspServerLoader.packageDescriptor.serverPackage)
        set(value) {
            state.lspServerPackageName = value.systemDependentPath
        }
    companion object {
        fun getInstance(project: Project): TypeSpecServiceSettings = project.service()
    }
}

class TypeSpecServiceState: BaseState() {
    var lspServerPackageName by string(defaultPackageKey)
}

