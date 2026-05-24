package com.example.typespec

import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic

interface TypeSpecServiceSettingsListener {
    fun onServiceSettingsChanged(project: Project)

    companion object {
        @Topic.ProjectLevel
        val TOPIC: Topic<TypeSpecServiceSettingsListener> = Topic.create(
            "TypeSpecServiceSettings",
            TypeSpecServiceSettingsListener::class.java,
        )
    }
}
