package com.example.typespec

import com.intellij.openapi.project.Project

internal object TypeSpecLspLifecycle {
    fun onConfigurationChanged(project: Project) {
        TypeSpecLspPackageResolutionCoordinator.onConfigurationChanged(project)
    }
}
