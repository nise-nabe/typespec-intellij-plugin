package com.example.typespec.run

import com.example.typespec.TypeSpecBundle
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.RuntimeConfigurationException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.process.ColoredProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import org.jdom.Element

class TypeSpecCompileRunConfiguration(
    project: Project,
    factory: TypeSpecCompileRunConfigurationFactory,
    name: String,
) : RunConfigurationBase<TypeSpecCompileSettings>(project, factory, name) {
    private val settings = TypeSpecCompileSettings()

    override fun getConfigurationEditor(): SettingsEditor<TypeSpecCompileRunConfiguration> =
        TypeSpecCompileSettingsEditor(project)

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        settings.readExternal(element)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        settings.readExternal(element)
    }

    override fun checkConfiguration() {
        settings.buildCommandLine(project)
            ?: throw RuntimeConfigurationException(
                TypeSpecBundle.message("runConfiguration.typespecCompile.compilerMissing"),
            )
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        val commandLine = settings.buildCommandLine(project)
            ?: throw RuntimeConfigurationException(
                TypeSpecBundle.message("runConfiguration.typespecCompile.compilerMissing"),
            )
        return object : CommandLineState(environment) {
            override fun startProcess() = ColoredProcessHandler(commandLine)
        }
    }

    fun settings(): TypeSpecCompileSettings = settings
}
