package com.example.typespec

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

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
