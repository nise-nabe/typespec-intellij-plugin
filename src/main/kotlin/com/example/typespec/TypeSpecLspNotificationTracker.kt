package com.example.typespec

import com.intellij.notification.Notification
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresEdt

@Service(Service.Level.PROJECT)
internal class TypeSpecLspNotificationTracker {
    private var compilerMissingNotified = false
    private var lastNotifiedPackageKey: String? = null
    private var compilerMissingNotification: Notification? = null

    @RequiresEdt
    fun tryAcquireCompilerMissingNotification(packageKey: String): Boolean =
        !compilerMissingNotified || lastNotifiedPackageKey != packageKey

    @RequiresEdt
    fun rememberCompilerMissingNotification(notification: Notification, packageKey: String) {
        compilerMissingNotified = true
        lastNotifiedPackageKey = packageKey
        expireActiveCompilerMissingNotification()
        compilerMissingNotification = notification
    }

    @RequiresEdt
    fun clearCompilerMissingNotification() {
        compilerMissingNotified = false
        lastNotifiedPackageKey = null
        expireActiveCompilerMissingNotification()
        compilerMissingNotification = null
    }

    private fun expireActiveCompilerMissingNotification() {
        val notification = compilerMissingNotification ?: return
        val application = ApplicationManager.getApplication() ?: return
        if (application.isDisposed) {
            return
        }
        notification.expire()
    }

    companion object {
        fun getInstance(project: Project): TypeSpecLspNotificationTracker = project.service()
    }
}
