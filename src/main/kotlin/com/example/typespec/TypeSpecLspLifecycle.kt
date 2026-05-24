package com.example.typespec

import com.intellij.openapi.project.Project

internal object TypeSpecLspLifecycle {
    fun onConfigurationChanged(project: Project) {
        TypeSpecLspPackageResolutionCache.getInstance(project).invalidate()
        restartTypeSpecServerAsync(project)
    }
}
