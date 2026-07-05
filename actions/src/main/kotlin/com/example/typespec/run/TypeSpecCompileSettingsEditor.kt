package com.example.typespec.run

import com.example.typespec.TypeSpecBundle
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class TypeSpecCompileSettingsEditor(
    private val project: Project,
) : SettingsEditor<TypeSpecCompileRunConfiguration>() {
    private var entrypointPath: String = ""
    private var projectRootPath: String = ""
    private var emitters: String = ""
    private var extraArgs: String = ""
    private var watch: Boolean = false
    private var dryRun: Boolean = false
    private var noEmit: Boolean = false
    private var stats: Boolean = false
    private var trace: String = ""
    private var warnAsError: Boolean = false

    override fun createEditor(): JComponent = panel {
        row(TypeSpecBundle.message("runConfiguration.typespecCompile.projectRoot")) {
            textField().bindText(::projectRootPath)
        }
        row(TypeSpecBundle.message("runConfiguration.typespecCompile.entrypoint")) {
            textField().bindText(::entrypointPath)
        }
        row(TypeSpecBundle.message("runConfiguration.typespecCompile.emitters")) {
            textField().bindText(::emitters)
        }
        row(TypeSpecBundle.message("runConfiguration.typespecCompile.extraArgs")) {
            textField().bindText(::extraArgs)
        }
        row {
            checkBox(TypeSpecBundle.message("runConfiguration.typespecCompile.watch"))
                .bindSelected(::watch)
        }
        row {
            checkBox(TypeSpecBundle.message("runConfiguration.typespecCompile.dryRun"))
                .bindSelected(::dryRun)
        }
        row {
            checkBox(TypeSpecBundle.message("runConfiguration.typespecCompile.noEmit"))
                .bindSelected(::noEmit)
        }
        row {
            checkBox(TypeSpecBundle.message("runConfiguration.typespecCompile.stats"))
                .bindSelected(::stats)
        }
        row(TypeSpecBundle.message("runConfiguration.typespecCompile.trace")) {
            textField().bindText(::trace)
        }
        row {
            checkBox(TypeSpecBundle.message("runConfiguration.typespecCompile.warnAsError"))
                .bindSelected(::warnAsError)
        }
    }

    override fun resetEditorFrom(configuration: TypeSpecCompileRunConfiguration) {
        val settings = configuration.settings()
        entrypointPath = settings.entrypointPath
        projectRootPath = settings.projectRootPath
        emitters = settings.emitters
        extraArgs = settings.extraArgs
        watch = settings.watch
        dryRun = settings.dryRun
        noEmit = settings.noEmit
        stats = settings.stats
        trace = settings.trace
        warnAsError = settings.warnAsError
    }

    override fun applyEditorTo(configuration: TypeSpecCompileRunConfiguration) {
        val settings = configuration.settings()
        settings.entrypointPath = entrypointPath
        settings.projectRootPath = projectRootPath
        settings.emitters = emitters
        settings.extraArgs = extraArgs
        settings.watch = watch
        settings.dryRun = dryRun
        settings.noEmit = noEmit
        settings.stats = stats
        settings.trace = trace
        settings.warnAsError = warnAsError
    }
}
