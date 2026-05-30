package com.example.typespec

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import java.util.concurrent.atomic.AtomicBoolean

@Service(Service.Level.PROJECT)
internal class TypeSpecLspPackageResolutionCacheWatcher(
    private val project: Project,
) : Disposable {
    private val recheckScheduled = AtomicBoolean()
    private val vfsListener = object : BulkFileListener {
        override fun after(events: List<VFileEvent>) {
            for (event in events) {
                when (event) {
                    is VFileCreateEvent, is VFileDeleteEvent, is VFileContentChangeEvent -> onVfsEvent(event)
                }
            }
        }
    }

    init {
        project.messageBus.connect(this).subscribe(VirtualFileManager.VFS_CHANGES, vfsListener)
    }

    private fun onVfsEvent(event: VFileEvent) {
        if (project.isDisposed) {
            return
        }
        val packageRoot = TypeSpecLspServerLoader.getSelectedPackage(project).systemDependentPath
        if (!vfsEventAffectsPackageRoot(event, packageRoot) &&
            !vfsEventAffectsOpenApi3Package(event.path, project.basePath)
        ) {
            return
        }
        scheduleRecheck()
    }

    private fun scheduleRecheck() {
        if (project.isDisposed || ApplicationManager.getApplication().isUnitTestMode) {
            return
        }
        if (!recheckScheduled.compareAndSet(false, true)) {
            return
        }
        AppExecutorUtil.getAppExecutorService().execute {
            recheckScheduled.set(false)
            if (project.isDisposed) {
                return@execute
            }
            recheckPackageResolution()
        }
    }

    @RequiresBackgroundThread
    private fun recheckPackageResolution() {
        val cache = TypeSpecPackageResolutionCache.getInstance(project)
        val wasLspResolvable = cache.peekSnapshot()?.lspServerResolvable
        cache.invalidate()

        val isResolvable = TypeSpecLspServerLoader.isSelectedPackageResolvable(project)
        if (isResolvable) {
            TypeSpecLspNotificationTracker.getInstance(project).clearCompilerMissingNotification()
        }
        if (wasLspResolvable != isResolvable) {
            restartTypeSpecServerAsync(project)
        }
    }

    override fun dispose() {
        // Message bus connection is disposed automatically via parent disposable.
    }

    companion object {
        fun getInstance(project: Project): TypeSpecLspPackageResolutionCacheWatcher = project.service()
    }
}

internal fun vfsEventAffectsPackageRoot(event: VFileEvent, packageRoot: String): Boolean {
    val normalizedRoot = normalizePackageRoot(packageRoot) ?: return false
    val file = event.file
    return vfsPathIsUnderPackageRoot(event.path, normalizedRoot) ||
        (file != null && vfsFileIsUnderPackageRoot(file, normalizedRoot))
}

internal class TypeSpecLspPackageResolutionCacheWatcherInitializer : ProjectActivity {
    override suspend fun execute(project: Project) {
        TypeSpecLspPackageResolutionCacheWatcher.getInstance(project)
    }
}
