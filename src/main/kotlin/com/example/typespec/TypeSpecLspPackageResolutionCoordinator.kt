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

private const val NOTIFICATION_GROUP_ID = "TypeSpec Notifications"

internal object TypeSpecLspPackageResolutionCoordinator {
    fun onConfigurationChanged(project: Project) {
        TypeSpecLspPackageResolutionCacheWatcher.getInstance(project).updateWatchedPackageRoot()
        applyResolutionOnEdt(project, RestartPolicy.Always) {
            val cache = TypeSpecLspPackageResolutionCache.getInstance(project)
            cache.invalidate()
            ResolutionSnapshot(
                isResolvable = TypeSpecPackageResolution.isSelectedPackageResolvable(project),
            )
        }
    }

    @RequiresBackgroundThread
    fun onPackageRootAffected(project: Project) {
        applyResolutionOnEdt(project, RestartPolicy.OnResolvableChange) {
            val cache = TypeSpecLspPackageResolutionCache.getInstance(project)
            val wasResolvable = cache.peekResolvable()
            cache.invalidate()
            ResolutionSnapshot(
                isResolvable = TypeSpecPackageResolution.isSelectedPackageResolvable(project),
                wasResolvable = wasResolvable,
            )
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

    private fun scheduleCompilerMissingNotificationSync(project: Project) {
        AppExecutorUtil.getAppExecutorService().execute {
            applyResolutionOnEdt(project, RestartPolicy.Never) {
                ResolutionSnapshot(
                    isResolvable = TypeSpecPackageResolution.isSelectedPackageResolvable(project),
                )
            }
        }
    }

    private fun applyResolutionOnEdt(
        project: Project,
        restartPolicy: RestartPolicy,
        prepare: () -> ResolutionSnapshot,
    ) {
        if (project.isDisposed) {
            return
        }
        val snapshot = prepare()
        ApplicationManager.getApplication().invokeLater(
            {
                if (project.isDisposed) {
                    return@invokeLater
                }
                applyResolutionSnapshot(project, snapshot, restartPolicy)
            },
            ModalityState.nonModal(),
            project.disposed,
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
            RestartPolicy.Always -> TypeSpecLspServerActivationRule.restartService(project)
            RestartPolicy.OnResolvableChange -> {
                val wasResolvable = snapshot.wasResolvable
                if (wasResolvable != null && wasResolvable != snapshot.isResolvable) {
                    TypeSpecLspServerActivationRule.restartService(project)
                }
            }
            RestartPolicy.Never -> Unit
        }
    }

    @RequiresEdt
    private fun syncCompilerMissingNotification(project: Project, isResolvable: Boolean) {
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

private data class ResolutionSnapshot(
    val isResolvable: Boolean,
    val wasResolvable: Boolean? = null,
)

private enum class RestartPolicy {
    Always,
    OnResolvableChange,
    Never,
}
