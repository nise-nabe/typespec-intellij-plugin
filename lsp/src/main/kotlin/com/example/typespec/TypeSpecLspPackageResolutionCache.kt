package com.example.typespec

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
internal class TypeSpecLspPackageResolutionCache {
    private var packageKey: String? = null
    private var resolvable: Boolean? = null
    private var checkedAtMillis: Long = 0L

    fun getOrCompute(project: Project, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val selectedPackage = TypeSpecCompilerPackageResolver.getSelectedPackage(project)
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
