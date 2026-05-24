package com.example.typespec

import com.intellij.notification.Notification
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
internal class TypeSpecLspNotificationTracker {
    private var compilerMissingNotified = false
    private var lastNotifiedPackageKey: String? = null
    private var compilerMissingNotification: Notification? = null

    fun tryAcquireCompilerMissingNotification(packageKey: String): Boolean {
        if (compilerMissingNotified && lastNotifiedPackageKey == packageKey) {
            return false
        }
        compilerMissingNotified = true
        lastNotifiedPackageKey = packageKey
        return true
    }

    fun rememberCompilerMissingNotification(notification: Notification) {
        compilerMissingNotification?.expire()
        compilerMissingNotification = notification
    }

    fun clearCompilerMissingNotification() {
        compilerMissingNotified = false
        lastNotifiedPackageKey = null
        compilerMissingNotification?.expire()
        compilerMissingNotification = null
    }

    companion object {
        fun getInstance(project: Project): TypeSpecLspNotificationTracker = project.service()
    }
}
