package com.example.typespec

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import java.util.concurrent.atomic.AtomicLong

@Service(Service.Level.PROJECT)
internal class TypeSpecLspPackageResolutionCache {
    private val resolutionUpdateGeneration = AtomicLong(0L)
    private var packageKey: String? = null
    private var resolvable: Boolean? = null
    private var checkedAtMillis: Long = 0L

    fun nextResolutionUpdateGeneration(): Long = resolutionUpdateGeneration.incrementAndGet()

    fun isLatestResolutionUpdate(generation: Long): Boolean =
        resolutionUpdateGeneration.get() == generation

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
    ): Boolean {
        val cachedResolvable = resolvable
        if (this.packageKey == packageKey &&
            cachedResolvable != null &&
            nowMillis - checkedAtMillis < RESOLUTION_CACHE_TTL_MILLIS
        ) {
            return cachedResolvable
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
