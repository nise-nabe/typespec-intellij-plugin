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
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import java.nio.file.Files
import java.nio.file.Paths

internal const val TYPESPEC_COMPILER_PACKAGE_NAME = "@typespec/compiler"
private const val TYPESPEC_SERVER_SCRIPT_PATH = "/cmd/tsp-server.js"

@Suppress("UnstableApiUsage")
object TypeSpecLspServerLoader : LspServerLoader(TypeSpecLspServerPackageDescriptor) {
    override fun getSelectedPackage(project: Project): NodePackage =
        TypeSpecServiceSettings.getInstance(project).lspServerPackage

    internal fun isSelectedPackageResolvable(project: Project): Boolean =
        TypeSpecLspPackageResolutionCache.getInstance(project).getOrCompute(project)

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
                TypeSpecLspPackageResolutionCache.getInstance(project).invalidate()
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
                TypeSpecLspPackageResolutionCache.getInstance(project).invalidate()
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

@Service(Service.Level.PROJECT)
internal class TypeSpecLspPackageResolutionCache {
    private var packageKey: String? = null
    private var resolvable: Boolean? = null
    private var checkedAtMillis: Long = 0L

    fun getOrCompute(project: Project, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val selectedPackage = TypeSpecLspServerLoader.getSelectedPackage(project)
        return getOrCompute(
            packageKey = selectedPackage.systemDependentPath,
            nowMillis = nowMillis,
        ) {
            TypeSpecLspServerLoader.isPackageWithServerScript(selectedPackage)
        }
    }

    internal fun getOrCompute(
        packageKey: String,
        nowMillis: Long,
        compute: () -> Boolean,
    ): Boolean {
        if (this.packageKey == packageKey &&
            resolvable != null &&
            nowMillis - checkedAtMillis < RESOLUTION_CACHE_TTL_MILLIS
        ) {
            return resolvable!!
        }

        val result = compute()
        this.packageKey = packageKey
        resolvable = result
        checkedAtMillis = nowMillis
        return result
    }

    fun invalidate() {
        packageKey = null
        resolvable = null
        checkedAtMillis = 0L
    }

    internal fun peekResolvable(): Boolean? = resolvable

    companion object {
        internal const val RESOLUTION_CACHE_TTL_MILLIS = 30_000L

        fun getInstance(project: Project): TypeSpecLspPackageResolutionCache = project.service()
    }
}
