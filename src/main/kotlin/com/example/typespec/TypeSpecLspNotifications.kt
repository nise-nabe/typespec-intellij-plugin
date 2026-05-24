package com.example.typespec

import com.intellij.lang.typescript.lsp.defaultPackageKey
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

private const val NOTIFICATION_GROUP_ID = "TypeSpec Notifications"

internal object TypeSpecLspNotifications {
    fun onTypeSpecFileOpened(project: Project, file: VirtualFile) {
        if (!TypeSpecLspServerActivationRule.wouldActivateIfPackageResolvable(project, file)) {
            return
        }
        if (TypeSpecCompilerPackageResolver.isCompilerPackageResolvable(project)) {
            TypeSpecLspNotificationTracker.getInstance(project).clearCompilerMissingNotification()
            return
        }

        val packageKey = TypeSpecServiceSettings.getInstance(project).state.lspServerPackageName ?: defaultPackageKey
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

@Service(Service.Level.PROJECT)
internal class TypeSpecLspNotificationTracker {
    private var compilerMissingNotified = false
    private var lastNotifiedPackageKey: String? = null

    fun shouldNotifyCompilerMissing(packageKey: String): Boolean {
        if (compilerMissingNotified && lastNotifiedPackageKey == packageKey) {
            return false
        }
        compilerMissingNotified = true
        lastNotifiedPackageKey = packageKey
        return true
    }

    fun clearCompilerMissingNotification() {
        compilerMissingNotified = false
        lastNotifiedPackageKey = null
    }

    companion object {
        fun getInstance(project: Project): TypeSpecLspNotificationTracker = project.service()
    }
}
