package com.example.typespec

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresEdt

internal const val TYPESPEC_NOTIFICATION_GROUP_ID = "TypeSpec Notifications"

internal object TypeSpecCompilerMissingNotification {
    @RequiresEdt
    fun sync(project: Project, snapshot: ResolutionSnapshot) {
        val debounce = TypeSpecCompilerMissingNotificationDebounce.getInstance(project)
        if (!TypeSpecActivationHelper.isEnabledInSettings(project)) {
            debounce.cancel()
            TypeSpecLspNotificationTracker.getInstance(project).clearCompilerMissingNotification()
            return
        }
        if (snapshot.isResolvable) {
            debounce.cancel()
            TypeSpecLspNotificationTracker.getInstance(project).clearCompilerMissingNotification()
            return
        }
        if (ApplicationManager.getApplication().isUnitTestMode) {
            return
        }
        if (shouldDeferCompilerMissingNotification(snapshot)) {
            debounce.scheduleRecheck()
            return
        }
        if (!shouldShowCompilerMissingNotification(snapshot)) {
            return
        }

        debounce.cancel()
        showCompilerMissingNotification(project, snapshot.packageKey)
    }

    @RequiresEdt
    private fun showCompilerMissingNotification(project: Project, packageKey: String) {
        val tracker = TypeSpecLspNotificationTracker.getInstance(project)
        if (!tracker.tryAcquireCompilerMissingNotification(packageKey)) {
            return
        }

        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup(TYPESPEC_NOTIFICATION_GROUP_ID)
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
        notification.notify(project)
        tracker.rememberCompilerMissingNotification(notification, packageKey)
    }
}
