package com.example.typespec

import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager

const val TYPESPEC_PLUGIN_ID = "com.example.typespec"

internal class TypeSpecDynamicPluginListener : DynamicPluginListener {
    override fun beforePluginUnload(pluginDescriptor: IdeaPluginDescriptor, isUpdate: Boolean) {
        if (!TypeSpecPluginLifecycle.isTypeSpecPlugin(pluginDescriptor)) {
            return
        }
        TypeSpecPluginLifecycle.beforeUnload(ProjectManager.getInstance().openProjects)
    }

    override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
        if (!TypeSpecPluginLifecycle.isTypeSpecPlugin(pluginDescriptor)) {
            return
        }
        TypeSpecPluginLifecycle.afterLoad(ProjectManager.getInstance().openProjects)
    }
}

internal object TypeSpecPluginLifecycle {
    fun isTypeSpecPlugin(pluginDescriptor: IdeaPluginDescriptor): Boolean =
        pluginDescriptor.pluginId == PluginId.getId(TYPESPEC_PLUGIN_ID)

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
