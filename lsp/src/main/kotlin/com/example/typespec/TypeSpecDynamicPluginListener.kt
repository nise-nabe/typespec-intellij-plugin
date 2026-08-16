package com.example.typespec

import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager

const val TYPESPEC_PLUGIN_ID = "com.example.typespec"

internal class TypeSpecDynamicPluginListener : DynamicPluginListener {
    override fun beforePluginUnload(pluginDescriptor: IdeaPluginDescriptor, isUpdate: Boolean) {
        TypeSpecPluginLifecycle.onBeforeUnload(pluginDescriptor.pluginId)
    }

    override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
        TypeSpecPluginLifecycle.onAfterLoad(pluginDescriptor.pluginId)
    }
}

object TypeSpecPluginLifecycle {
    fun isTypeSpecPlugin(pluginId: PluginId): Boolean =
        pluginId == PluginId.getId(TYPESPEC_PLUGIN_ID)

    fun isTypeSpecPlugin(pluginDescriptor: IdeaPluginDescriptor): Boolean =
        isTypeSpecPlugin(pluginDescriptor.pluginId)

    fun onBeforeUnload(
        pluginId: PluginId,
        projects: Array<Project> = ProjectManager.getInstance().openProjects,
    ) {
        if (!isTypeSpecPlugin(pluginId)) {
            return
        }
        beforeUnload(projects)
    }

    fun onAfterLoad(
        pluginId: PluginId,
        projects: Array<Project> = ProjectManager.getInstance().openProjects,
    ) {
        if (!isTypeSpecPlugin(pluginId)) {
            return
        }
        afterLoad(projects)
    }

    fun beforeUnload(projects: Array<Project>) {
        for (project in projects) {
            stopTypeSpecServer(project)
            if (!project.isDisposed) {
                TypeSpecPackageResolutionCache.getInstance(project).invalidate()
            }
        }
    }

    fun afterLoad(projects: Array<Project>) {
        for (project in projects) {
            startTypeSpecServerIfNeeded(project)
        }
    }
}
