package com.example.typespec.run

import com.example.typespec.TypeSpecBundle
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
class TypeSpecCompileRunConfigurationFactory(
    type: ConfigurationType,
) : ConfigurationFactory(type) {
    override fun getId(): String = "TypeSpecCompileRunConfiguration"

    override fun getName(): String = TypeSpecBundle.message("runConfiguration.typespecCompile.factoryName")

    override fun createTemplateConfiguration(project: Project): RunConfiguration =
        TypeSpecCompileRunConfiguration(project, this, TypeSpecBundle.message("runConfiguration.typespecCompile.defaultName"))
}
