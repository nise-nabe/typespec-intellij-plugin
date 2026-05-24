package com.example.typespec

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread

private const val NOTIFICATION_GROUP_ID = "TypeSpec Notifications"

internal object TypeSpecLspPackageResolutionCoordinator {
    fun onConfigurationChanged(project: Project) {
        TypeSpecLspPackageResolutionCacheWatcher.getInstance(project).updateWatchedPackageRoot()
        val cache = TypeSpecLspPackageResolutionCache.getInstance(project)
        cache.invalidate()
        val isResolvable = TypeSpecPackageResolution.isSelectedPackageResolvable(project)
        syncCompilerMissingNotification(project, isResolvable)
        restartTypeSpecServerAsync(project)
    }

    @RequiresBackgroundThread
    fun onPackageRootAffected(project: Project) {
        val cache = TypeSpecLspPackageResolutionCache.getInstance(project)
        val wasResolvable = cache.peekResolvable()
        cache.invalidate()
        val isResolvable = TypeSpecPackageResolution.isSelectedPackageResolvable(project)
        syncCompilerMissingNotification(project, isResolvable)
        if (wasResolvable != null && wasResolvable != isResolvable) {
            restartTypeSpecServerAsync(project)
        }
    }

    fun onTypeSpecFileOpened(project: Project, file: VirtualFile) {
        if (!TypeSpecLspServerActivationRule.isEligibleExceptPackageResolution(project, file)) {
            return
        }
        if (ApplicationManager.getApplication().isUnitTestMode) {
            return
        }

        AppExecutorUtil.getAppExecutorService().execute {
            if (project.isDisposed) {
                return@execute
            }
            val isResolvable = TypeSpecPackageResolution.isSelectedPackageResolvable(project)
            ApplicationManager.getApplication().invokeLater(
                {
                    if (project.isDisposed) {
                        return@invokeLater
                    }
                    syncCompilerMissingNotification(project, isResolvable)
                },
                ModalityState.nonModal(),
                project.disposed,
            )
        }
    }

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
