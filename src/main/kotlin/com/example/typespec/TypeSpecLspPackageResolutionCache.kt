package com.example.typespec

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
internal class TypeSpecLspPackageResolutionCache {
    private val lock = Any()
    private var resolutionUpdateGeneration: Long = 0L
    private var packageKey: String? = null
    private var resolvable: Boolean? = null
    private var checkedAtMillis: Long = 0L

    fun nextResolutionUpdateGeneration(): Long = synchronized(lock) {
        resolutionUpdateGeneration += 1
        resolutionUpdateGeneration
    }

    fun isLatestResolutionUpdate(generation: Long): Boolean = synchronized(lock) {
        resolutionUpdateGeneration == generation
    }

    fun getOrCompute(project: Project, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val selectedPackage = TypeSpecPackageResolution.getSelectedPackage(project)
        return getOrCompute(
            packageKey = selectedPackage.systemDependentPath,
            nowMillis = nowMillis,
        ) {
            TypeSpecPackageResolution.isPackageWithServerScript(selectedPackage)
        }
    }

    internal fun getOrCompute(
        packageKey: String,
        nowMillis: Long,
        compute: () -> Boolean,
    ): Boolean = synchronized(lock) {
        val cachedResolvable = resolvable
        if (this.packageKey == packageKey &&
            cachedResolvable != null &&
            nowMillis - checkedAtMillis < RESOLUTION_CACHE_TTL_MILLIS
        ) {
            return@synchronized cachedResolvable
        }

        val result = compute()
        this.packageKey = packageKey
        resolvable = result
        checkedAtMillis = nowMillis
        result
    }

    fun invalidate() = synchronized(lock) {
        packageKey = null
        resolvable = null
        checkedAtMillis = 0L
    }

    internal fun peekResolvable(): Boolean? = synchronized(lock) {
        resolvable
    }

    companion object {
        internal const val RESOLUTION_CACHE_TTL_MILLIS = 30_000L

        fun getInstance(project: Project): TypeSpecLspPackageResolutionCache = project.service()
    }
}
