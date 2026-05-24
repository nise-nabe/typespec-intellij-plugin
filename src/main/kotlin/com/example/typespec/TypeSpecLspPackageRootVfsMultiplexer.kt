package com.example.typespec

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.APP)
internal class TypeSpecLspPackageRootVfsMultiplexer : Disposable {
    private val watchedPackageRoots = ConcurrentHashMap<Project, String>()

    init {
        VirtualFileManager.getInstance().addAsyncFileListenerBackgroundable(
            object : AsyncFileListener {
                override fun prepareChange(events: List<VFileEvent>): AsyncFileListener.ChangeApplier? {
                    val affectedProjects = findAffectedProjects(events)
                    if (affectedProjects.isEmpty()) {
                        return null
                    }
                    return object : AsyncFileListener.ChangeApplier {
                        override fun afterVfsChange() {
                            for (project in affectedProjects) {
                                if (project.isDisposed) {
                                    unwatch(project)
                                    continue
                                }
                                TypeSpecLspPackageResolutionCacheWatcher.getInstance(project)
                                    .onPackageRootAffected()
                            }
                        }
                    }
                }
            },
            this,
        )
    }

    fun watchPackageRoot(project: Project, packageRootPath: String) {
        val normalizedRoot = normalizePackageRoot(packageRootPath)
        if (normalizedRoot == null) {
            unwatch(project)
            return
        }
        watchedPackageRoots[project] = normalizedRoot
    }

    fun unwatch(project: Project) {
        watchedPackageRoots.remove(project)
    }

    private fun findAffectedProjects(events: List<VFileEvent>): Set<Project> {
        val affectedProjects = linkedSetOf<Project>()
        for ((project, packageRoot) in watchedPackageRoots) {
            if (project.isDisposed) {
                unwatch(project)
                continue
            }
            if (events.any { event -> vfsEventAffectsPackageRoot(event, packageRoot) }) {
                affectedProjects.add(project)
            }
        }
        return affectedProjects
    }

    override fun dispose() {
        watchedPackageRoots.clear()
    }

    companion object {
        fun getInstance(): TypeSpecLspPackageRootVfsMultiplexer = ApplicationManager.getApplication().service()
    }
}
