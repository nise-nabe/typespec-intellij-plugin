package com.example.typespec

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.concurrency.ThreadingAssertions
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresEdt
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.atomic.AtomicInteger

private const val NOTIFICATION_GROUP_ID = "TypeSpec Notifications"

@TestOnly
internal val typeSpecLspRestartRequestCountForTests = AtomicInteger(0)

@TestOnly
internal fun resetTypeSpecLspRestartRequestCountForTests() {
    typeSpecLspRestartRequestCountForTests.set(0)
}

internal fun shouldRestartForResolvableChange(wasResolvable: Boolean?, isResolvable: Boolean): Boolean =
    wasResolvable != null && wasResolvable != isResolvable

internal data class ResolutionSnapshot(
    val isResolvable: Boolean,
    val wasResolvable: Boolean? = null,
)

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

    fun onTypeSpecFileOpened(
        project: Project,
        file: VirtualFile,
        serverStarter: LspServerSupportProvider.LspServerStarter,
    ) {
        if (!TypeSpecLspServerActivationRule.isEnabled(project, file)) {
            return
        }

        if (TypeSpecLspServerActivationRule.isEnabledAndAvailable(project, file)) {
            serverStarter.ensureServerStarted(TypeSpecLspServerDescriptor(project))
        }

        scheduleCompilerMissingNotificationSync(project)
    }

    @TestOnly
    internal fun applyResolutionSnapshotForTests(
        project: Project,
        snapshot: ResolutionSnapshot,
        restartPolicy: RestartPolicy,
    ) {
        applyResolutionSnapshot(project, snapshot, restartPolicy)
    }

    private fun scheduleCompilerMissingNotificationSync(project: Project) {
        scheduleResolutionUpdate(project, RestartPolicy.Never) { currentProject ->
            computeResolutionSnapshot(currentProject, captureWasResolvable = false)
        }
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
        AppExecutorUtil.getAppExecutorService().execute {
            if (project.isDisposed) {
                return@execute
            }
            val snapshot = prepareOnBackground(project)
            ApplicationManager.getApplication().invokeLater(
                {
                    if (project.isDisposed) {
                        return@invokeLater
                    }
                    val cache = TypeSpecLspPackageResolutionCache.getInstance(project)
                    if (!cache.isLatestResolutionUpdate(generation)) {
                        return@invokeLater
                    }
                    applyResolutionSnapshot(project, snapshot, restartPolicy)
                },
                ModalityState.nonModal(),
                project.disposed,
            )
        }
    }

    @RequiresBackgroundThread
    private fun computeResolutionSnapshot(
        project: Project,
        captureWasResolvable: Boolean,
    ): ResolutionSnapshot {
        val cache = TypeSpecLspPackageResolutionCache.getInstance(project)
        val wasResolvable = if (captureWasResolvable) cache.peekResolvable() else null
        cache.invalidate()
        return ResolutionSnapshot(
            isResolvable = TypeSpecPackageResolution.isSelectedPackageResolvable(project),
            wasResolvable = wasResolvable,
        )
    }

    @RequiresEdt
    private fun applyResolutionSnapshot(
        project: Project,
        snapshot: ResolutionSnapshot,
        restartPolicy: RestartPolicy,
    ) {
        ThreadingAssertions.assertEventDispatchThread()
        syncCompilerMissingNotification(project, snapshot.isResolvable)
        when (restartPolicy) {
            RestartPolicy.Always -> requestServiceRestart(project)
            RestartPolicy.OnResolvableChange -> {
                if (shouldRestartForResolvableChange(snapshot.wasResolvable, snapshot.isResolvable)) {
                    requestServiceRestart(project)
                }
            }
            RestartPolicy.Never -> Unit
        }
    }

    @RequiresEdt
    private fun requestServiceRestart(project: Project) {
        typeSpecLspRestartRequestCountForTests.incrementAndGet()
        TypeSpecLspServerActivationRule.restartService(project)
    }

    @RequiresEdt
    private fun syncCompilerMissingNotification(project: Project, isResolvable: Boolean) {
        if (!TypeSpecActivationHelper.isEnabledInSettings(project)) {
            TypeSpecLspNotificationTracker.getInstance(project).clearCompilerMissingNotification()
            return
        }
        if (isResolvable) {
            TypeSpecLspNotificationTracker.getInstance(project).clearCompilerMissingNotification()
            return
        }
        if (ApplicationManager.getApplication().isUnitTestMode) {
            return
        }

        val packageKey = TypeSpecPackageResolution.getSelectedPackage(project).systemDependentPath
        if (!TypeSpecLspNotificationTracker.getInstance(project).shouldNotifyCompilerMissing(packageKey)) {
            return
        }

        NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP_ID)
            .createNotification(
                TypeSpecBundle.message("notification.compilerMissing.title"),
                TypeSpecBundle.message("notification.compilerMissing.content"),
                NotificationType.WARNING,
            )
            .addAction(
                NotificationAction.createSimpleExpiring(
                    TypeSpecBundle.message("notification.compilerMissing.openSettings"),
                ) {
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, TypeSpecSettingsConfigurable::class.java)
                },
            )
            .notify(project)
    }
}
