package com.example.typespec

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.util.concurrency.AppExecutorUtil
import java.util.concurrent.atomic.AtomicBoolean

@Service(Service.Level.PROJECT)
internal class TypeSpecLspPackageResolutionCacheWatcher(
    private val project: Project,
) : Disposable {
    private val recheckScheduled = AtomicBoolean()

    init {
        updateWatchedPackageRoot()
    }

    fun updateWatchedPackageRoot() {
        if (ApplicationManager.getApplication().isUnitTestMode) {
            return
        }
        val packageRootPath = TypeSpecPackageResolution.getSelectedPackage(project).systemDependentPath
        TypeSpecLspPackageRootVfsMultiplexer.getInstance().watchPackageRoot(project, packageRootPath)
    }

    internal fun onPackageRootAffected() {
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
            TypeSpecLspPackageResolutionCoordinator.onPackageRootAffected(project)
        }
    }

    override fun dispose() {
        TypeSpecLspPackageRootVfsMultiplexer.getInstance().unwatch(project)
    }

    companion object {
        fun getInstance(project: Project): TypeSpecLspPackageResolutionCacheWatcher = project.service()
    }
}

internal class TypeSpecLspPackageResolutionCacheWatcherInitializer : ProjectActivity {
    override suspend fun execute(project: Project) {
        TypeSpecLspPackageResolutionCacheWatcher.getInstance(project)
    }
}
