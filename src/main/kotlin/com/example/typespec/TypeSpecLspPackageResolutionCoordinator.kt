package com.example.typespec

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.concurrency.ThreadingAssertions
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresEdt
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.atomic.AtomicInteger

@TestOnly
internal val typeSpecLspRestartRequestCountForTests = AtomicInteger(0)

@TestOnly
internal fun resetTypeSpecLspRestartRequestCountForTests() {
    typeSpecLspRestartRequestCountForTests.set(0)
}

internal fun shouldRestartForResolvableChange(wasResolvable: Boolean?, isResolvable: Boolean): Boolean =
    wasResolvable != null && wasResolvable != isResolvable

internal fun shouldRequestServiceRestart(
    project: Project,
    restartPolicy: RestartPolicy,
    snapshot: ResolutionSnapshot,
): Boolean {
    if (!TypeSpecActivationHelper.isEnabledInSettings(project)) {
        return false
    }
    return when (restartPolicy) {
        RestartPolicy.Always -> true
        RestartPolicy.OnResolvableChange ->
            shouldRestartForResolvableChange(snapshot.wasResolvable, snapshot.isResolvable)
        RestartPolicy.Never -> false
    }
}

internal data class ResolutionSnapshot(
    val isResolvable: Boolean,
    val packageKey: String,
    val wasResolvable: Boolean? = null,
)

internal fun shouldDeferCompilerMissingNotification(snapshot: ResolutionSnapshot): Boolean =
    !snapshot.isResolvable && snapshot.wasResolvable == null

internal fun shouldShowCompilerMissingNotification(snapshot: ResolutionSnapshot): Boolean =
    !snapshot.isResolvable && snapshot.wasResolvable != null

internal enum class RestartPolicy {
    Always,
    OnResolvableChange,
    Never,
}

internal object TypeSpecLspPackageResolutionCoordinator {
    fun onConfigurationChanged(project: Project) {
        scheduleResolutionUpdate(project, RestartPolicy.Always) { currentProject ->
            TypeSpecLspPackageResolutionCacheWatcher.getInstance(currentProject).updateWatchedPackageRoot()
            computeResolutionSnapshot(currentProject, captureWasResolvable = false)
        }
    }

    fun onPackageRootAffected(project: Project) {
        scheduleResolutionUpdate(project, RestartPolicy.OnResolvableChange) { currentProject ->
            computeResolutionSnapshot(currentProject, captureWasResolvable = true)
        }
    }

    fun scheduleDeferredCompilerMissingRecheck(project: Project) {
        scheduleResolutionUpdate(project, RestartPolicy.Never) { currentProject ->
            computeResolutionSnapshot(currentProject, captureWasResolvable = true)
        }
    }

    fun onTypeSpecFileOpened(
        project: Project,
        file: VirtualFile,
        serverStarter: LspServerSupportProvider.LspServerStarter,
    ) {
        // Activation off (settings, environment, or file type): no server start and no resolution pipeline.
        if (!TypeSpecLspServerActivationRule.isEnabled(project, file)) {
            return
        }

        if (TypeSpecLspServerActivationRule.isEnabledAndAvailable(project, file)) {
            serverStarter.ensureServerStarted(TypeSpecLspServerDescriptor(project))
        }

        scheduleResolutionUpdate(project, RestartPolicy.Never) { currentProject ->
            computeResolutionSnapshot(currentProject, captureWasResolvable = false)
        }
    }

    @TestOnly
    internal fun applyResolutionSnapshotForTests(
        project: Project,
        snapshot: ResolutionSnapshot,
        restartPolicy: RestartPolicy,
    ) {
        applyResolutionSnapshot(project, snapshot, restartPolicy)
    }

    private fun scheduleResolutionUpdate(
        project: Project,
        restartPolicy: RestartPolicy,
        prepareOnBackground: (Project) -> ResolutionSnapshot,
    ) {
        if (project.isDisposed) {
            return
        }
        val generation = TypeSpecLspPackageResolutionCache.getInstance(project).nextResolutionUpdateGeneration()
        submitResolutionUpdate(project, generation, restartPolicy, prepareOnBackground)
    }

    private fun submitResolutionUpdate(
        project: Project,
        generation: Long,
        restartPolicy: RestartPolicy,
        prepareOnBackground: (Project) -> ResolutionSnapshot,
    ) {
        val runUpdate = Runnable {
            runResolutionUpdate(project, generation, restartPolicy, prepareOnBackground)
        }
        if (ApplicationManager.getApplication().isDispatchThread) {
            AppExecutorUtil.getAppExecutorService().execute(runUpdate)
        } else {
            runUpdate.run()
        }
    }

    @RequiresBackgroundThread
    private fun runResolutionUpdate(
        project: Project,
        generation: Long,
        restartPolicy: RestartPolicy,
        prepareOnBackground: (Project) -> ResolutionSnapshot,
    ) {
        if (project.isDisposed) {
            return
        }
        val snapshot = prepareOnBackground(project)
        ApplicationManager.getApplication().invokeLater(
            {
                if (project.isDisposed) {
                    return@invokeLater
                }
                tryApplyResolutionSnapshot(project, generation, snapshot, restartPolicy)
            },
            ModalityState.nonModal(),
            project.disposed,
        )
    }

    @RequiresBackgroundThread
    private fun computeResolutionSnapshot(
        project: Project,
        captureWasResolvable: Boolean,
    ): ResolutionSnapshot {
        val cache = TypeSpecLspPackageResolutionCache.getInstance(project)
        val wasResolvable = if (captureWasResolvable) cache.peekResolvable() else null
        val selectedPackage = TypeSpecPackageResolution.getSelectedPackage(project)
        val isResolvable = TypeSpecPackageResolution.isPackageWithServerScript(selectedPackage)
        val packageKey = normalizePackageRoot(selectedPackage.systemDependentPath)
            ?: selectedPackage.systemDependentPath
        return ResolutionSnapshot(
            isResolvable = isResolvable,
            packageKey = packageKey,
            wasResolvable = wasResolvable,
        )
    }

    @RequiresEdt
    private fun tryApplyResolutionSnapshot(
        project: Project,
        generation: Long,
        snapshot: ResolutionSnapshot,
        restartPolicy: RestartPolicy,
    ): Boolean {
        ThreadingAssertions.assertEventDispatchThread()
        val cache = TypeSpecLspPackageResolutionCache.getInstance(project)
        if (!cache.isLatestResolutionUpdate(generation)) {
            return false
        }
        applyResolutionSnapshot(project, snapshot, restartPolicy)
        return true
    }

    @TestOnly
    internal fun tryApplyResolutionSnapshotForTests(
        project: Project,
        generation: Long,
        snapshot: ResolutionSnapshot,
        restartPolicy: RestartPolicy,
    ): Boolean = tryApplyResolutionSnapshot(project, generation, snapshot, restartPolicy)

    @RequiresEdt
    private fun applyResolutionSnapshot(
        project: Project,
        snapshot: ResolutionSnapshot,
        restartPolicy: RestartPolicy,
    ) {
        ThreadingAssertions.assertEventDispatchThread()
        TypeSpecLspPackageResolutionCache.getInstance(project).recordResolvable(snapshot.isResolvable)
        TypeSpecCompilerMissingNotification.sync(project, snapshot)
        if (shouldRequestServiceRestart(project, restartPolicy, snapshot)) {
            requestServiceRestart(project)
        }
    }

    @RequiresEdt
    private fun requestServiceRestart(project: Project) {
        typeSpecLspRestartRequestCountForTests.incrementAndGet()
        TypeSpecLspServerActivationRule.restartService(project)
    }

}
