package com.example.typespec

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
internal class TypeSpecLanguageServerCommandExecutorImpl : TypeSpecLanguageServerCommandExecutor {
    override fun restartServerAsync(project: Project) {
        restartTypeSpecServerAsync(project)
    }
}
