package com.example.typespec

import com.intellij.openapi.project.Project

internal class TypeSpecLspServiceSettingsListener : TypeSpecServiceSettingsListener {
    override fun onServiceSettingsChanged(project: Project) {
        TypeSpecLspPackageResolutionCache.getInstance(project).invalidate()
        restartTypeSpecServerAsync(project)
    }
}
