package com.example.typespec

import com.intellij.openapi.project.Project

interface TypeSpecLanguageServerCommandExecutor {
    fun restartServerAsync(project: Project)

    companion object {
        fun getInstance(project: Project): TypeSpecLanguageServerCommandExecutor? =
            project.getService(TypeSpecLanguageServerCommandExecutor::class.java)
    }
}
