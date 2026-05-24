package com.example.typespec

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import java.nio.file.Path
import java.nio.file.Paths

@Service(Service.Level.PROJECT)
class TypeSpecPackageResolutionCache {
    private var packageKey: String? = null
    private var snapshot: Snapshot? = null
    private var checkedAtMillis: Long = 0L

    data class Snapshot(
        val compilerCliResolvable: Boolean,
        val lspServerResolvable: Boolean,
    )

    fun getOrCompute(project: Project, nowMillis: Long = System.currentTimeMillis()): Snapshot {
        val selectedPackage = TypeSpecServiceSettings.getInstance(project).lspServerPackage
        return getOrCompute(
            packageKey = selectedPackage.systemDependentPath,
            nowMillis = nowMillis,
        ) {
            computePackageResolutionSnapshot(Paths.get(selectedPackage.systemDependentPath))
        }
    }

    internal fun getOrCompute(
        packageKey: String,
        nowMillis: Long,
        compute: () -> Snapshot,
    ): Snapshot {
        if (this.packageKey == packageKey &&
            snapshot != null &&
            nowMillis - checkedAtMillis < RESOLUTION_CACHE_TTL_MILLIS
        ) {
            return snapshot!!
        }

        val result = compute()
        this.packageKey = packageKey
        snapshot = result
        checkedAtMillis = nowMillis
        return result
    }

    fun invalidate() {
        packageKey = null
        snapshot = null
        checkedAtMillis = 0L
    }

    fun peekSnapshot(): Snapshot? = snapshot

    companion object {
        internal const val RESOLUTION_CACHE_TTL_MILLIS = 30_000L

        fun getInstance(project: Project): TypeSpecPackageResolutionCache = project.service()
    }
}

internal fun computePackageResolutionSnapshot(packageDirectory: Path): TypeSpecPackageResolutionCache.Snapshot =
    TypeSpecPackageResolutionCache.Snapshot(
        compilerCliResolvable = TypeSpecCompilerPackageResolver.hasCompilerCli(packageDirectory),
        lspServerResolvable = TypeSpecCompilerPackageResolver.hasLspServerScript(packageDirectory),
    )
