package com.example.typespec

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileAdapter
import com.intellij.openapi.vfs.VirtualFileEvent
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import java.util.concurrent.atomic.AtomicBoolean

@Service(Service.Level.PROJECT)
internal class TypeSpecLspPackageResolutionCacheWatcher(
    private val project: Project,
) : Disposable {
    private val recheckScheduled = AtomicBoolean()
    private val vfsListener = object : VirtualFileAdapter() {
        override fun fileCreated(event: VirtualFileEvent) = onVfsEvent(event)
        override fun fileDeleted(event: VirtualFileEvent) = onVfsEvent(event)
        override fun contentsChanged(event: VirtualFileEvent) = onVfsEvent(event)
    }

    init {
        @Suppress("DEPRECATION")
        VirtualFileManager.getInstance().addVirtualFileListener(vfsListener, this)
    }

    private fun onVfsEvent(event: VirtualFileEvent) {
        if (project.isDisposed) {
            return
        }
        val packageRoot = TypeSpecLspServerLoader.getSelectedPackage(project).systemDependentPath
        if (!vfsEventAffectsPackageRoot(event, packageRoot)) {
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
        val cache = TypeSpecLspPackageResolutionCache.getInstance(project)
        val wasResolvable = cache.peekResolvable()
        cache.invalidate()

        val isResolvable = TypeSpecLspServerLoader.isSelectedPackageResolvable(project)
        if (isResolvable) {
            TypeSpecLspNotificationTracker.getInstance(project).clearCompilerMissingNotification()
        }
        if (wasResolvable != isResolvable) {
            restartTypeSpecServerAsync(project)
        }
    }

    override fun dispose() {
        // Listener is removed automatically via parent disposable.
    }

    companion object {
        fun getInstance(project: Project): TypeSpecLspPackageResolutionCacheWatcher = project.service()
    }
}

internal fun vfsEventAffectsPackageRoot(event: VirtualFileEvent, packageRoot: String): Boolean {
    val normalizedRoot = normalizePackageRoot(packageRoot) ?: return false
    return vfsPathIsUnderPackageRoot(event.path, normalizedRoot) ||
        vfsFileIsUnderPackageRoot(event.file, normalizedRoot)
}

internal fun vfsFileIsUnderPackageRoot(file: VirtualFile, normalizedPackageRoot: String): Boolean =
    vfsPathIsUnderPackageRoot(file.path, normalizedPackageRoot)

internal fun vfsPathIsUnderPackageRoot(path: String, normalizedPackageRoot: String): Boolean {
    val normalizedPath = path.replace('\\', '/').trimEnd('/')
    return normalizedPath == normalizedPackageRoot || normalizedPath.startsWith("$normalizedPackageRoot/")
}

internal fun normalizePackageRoot(packageRoot: String): String? {
    val normalized = packageRoot.replace('\\', '/').trim().trimEnd('/')
    return normalized.takeIf { it.isNotEmpty() }
}

internal class TypeSpecLspPackageResolutionCacheWatcherInitializer : ProjectActivity {
    override suspend fun execute(project: Project) {
        TypeSpecLspPackageResolutionCacheWatcher.getInstance(project)
    }
}
