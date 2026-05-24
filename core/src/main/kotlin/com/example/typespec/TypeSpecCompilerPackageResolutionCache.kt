package com.example.typespec

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import java.nio.file.Files
import java.nio.file.Paths

@Service(Service.Level.PROJECT)
class TypeSpecCompilerPackageResolutionCache {
    private var packageKey: String? = null
    private var resolvable: Boolean? = null
    private var checkedAtMillis: Long = 0L

    fun getOrCompute(project: Project, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val selectedPackage = TypeSpecServiceSettings.getInstance(project).lspServerPackage
        return getOrCompute(
            packageKey = selectedPackage.systemDependentPath,
            nowMillis = nowMillis,
        ) {
            TypeSpecCompilerPackageResolver.hasCompilerCli(Paths.get(selectedPackage.systemDependentPath))
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

    companion object {
        internal const val RESOLUTION_CACHE_TTL_MILLIS = 30_000L

        fun getInstance(project: Project): TypeSpecCompilerPackageResolutionCache = project.service()
    }
}
