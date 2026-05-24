package com.example.typespec

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/**
 * Project-scoped state for LSP package resolution orchestration.
 *
 * - [nextResolutionUpdateGeneration] / [isLatestResolutionUpdate]: stale async update suppression
 * - [peekResolvable] / [recordResolvable]: last applied resolvable flag (committed on EDT apply only)
 *
 * Fresh disk checks use [TypeSpecPackageResolution.isPackageWithServerScript] directly; this service
 * does not TTL-cache filesystem probes.
 */
@Service(Service.Level.PROJECT)
internal class TypeSpecLspPackageResolutionCache {
    private val lock = Any()
    private var resolutionUpdateGeneration: Long = 0L
    private var lastResolvable: Boolean? = null

    fun nextResolutionUpdateGeneration(): Long = synchronized(lock) {
        resolutionUpdateGeneration += 1
        resolutionUpdateGeneration
    }

    fun isLatestResolutionUpdate(generation: Long): Boolean = synchronized(lock) {
        resolutionUpdateGeneration == generation
    }

    fun peekResolvable(): Boolean? = synchronized(lock) {
        lastResolvable
    }

    fun recordResolvable(resolvable: Boolean) = synchronized(lock) {
        lastResolvable = resolvable
    }

    companion object {
        fun getInstance(project: Project): TypeSpecLspPackageResolutionCache = project.service()
    }
}
