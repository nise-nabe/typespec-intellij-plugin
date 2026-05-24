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
    fun sync(project: Project, isResolvable: Boolean) {
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

        val tracker = TypeSpecLspNotificationTracker.getInstance(project)
        val packageKey = TypeSpecPackageResolution.getSelectedPackage(project).systemDependentPath
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
