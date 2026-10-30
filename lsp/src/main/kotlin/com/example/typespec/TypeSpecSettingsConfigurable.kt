package com.example.typespec

import com.intellij.lang.typescript.lsp.bindPackage
import com.intellij.lang.typescript.lsp.createNodePackageField
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.UiDslUnnamedConfigurable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bind
import com.intellij.ui.dsl.builder.bindText
import java.nio.file.Paths

class TypeSpecSettingsConfigurable(
    private val project: Project,
) : UiDslUnnamedConfigurable.Simple(), Configurable {
    private val settings = TypeSpecServiceSettings.getInstance(project)

    private var lspEmitText: String
        get() = TypeSpecWorkspaceConfiguration.formatList(settings.lspEmit)
        set(value) {
            settings.lspEmit = TypeSpecWorkspaceConfiguration.parseList(value)
        }

    private var entrypointText: String
        get() = TypeSpecWorkspaceConfiguration.formatList(settings.entrypoints)
        set(value) {
            settings.entrypoints = TypeSpecWorkspaceConfiguration.parseList(value)
        }

    override fun Panel.createContent() {
        group(TypeSpecBundle.message("settings.typespec.service.group")) {
            row(TypeSpecBundle.message("settings.typespec.service.languageServerPackage")) {
                cell(createNodePackageField(project, TypeSpecLspServerLoader.packageDescriptor))
                    .align(AlignX.FILL)
                    .bindPackage(settings::lspServerPackage)
            }

            row(TypeSpecBundle.message("settings.typespec.compilerVersion")) {
                text(compilerVersionLabel())
            }

            row(TypeSpecBundle.message("settings.typespec.compilerStatus")) {
                text(compilerStatusLabel())
            }

            row(TypeSpecBundle.message("settings.typespec.lsp.emit")) {
                textField()
                    .align(AlignX.FILL)
                    .bindText(::lspEmitText)
                    .comment(
                        TypeSpecBundle.message(
                            "settings.typespec.lsp.emit.help",
                            TypeSpecWorkspaceConfiguration.CONFIG_DEFAULTS,
                        ),
                    )
            }

            row(TypeSpecBundle.message("settings.typespec.entrypoint")) {
                textField()
                    .align(AlignX.FILL)
                    .bindText(::entrypointText)
                    .comment(TypeSpecBundle.message("settings.typespec.entrypoint.help"))
            }

            buttonsGroup {
                row {
                    radioButton(
                        TypeSpecBundle.message("settings.typespec.service.disabled"),
                        TypeSpecServiceMode.DISABLED,
                    ).comment(TypeSpecBundle.message("settings.typespec.service.disabled.help"))
                }
                row {
                    radioButton(
                        TypeSpecBundle.message("settings.typespec.service.enabled"),
                        TypeSpecServiceMode.ENABLED,
                    ).comment(TypeSpecBundle.message("settings.typespec.service.enabled.help"))
                }
            }.apply {
                bind(settings::serviceMode)
            }

            row {
                comment(TypeSpecBundle.message("settings.typespec.standaloneTsp.help"))
            }
            row {
                comment(TypeSpecBundle.message("settings.typespec.contentRoots.help"))
            }
        }
    }

    override fun getDisplayName(): String = TypeSpecBundle.message("settings.typespec.title")

    private fun compilerVersionLabel(): String {
        val version = runCatching {
            TypeSpecCompilerVersionReader.readPackageVersion(
                Paths.get(settings.lspServerPackage.systemDependentPath),
            )
        }.getOrNull()
        return version ?: TypeSpecBundle.message("settings.typespec.compilerVersion.unknown")
    }

    private fun compilerStatusLabel(): String {
        val ready = runCatching {
            val snapshot = TypeSpecPackageResolutionCache.getInstance(project).getOrCompute(project)
            snapshot.compilerCliResolvable && snapshot.lspServerResolvable
        }.getOrDefault(false)
        return if (ready) {
            TypeSpecBundle.message("settings.typespec.compilerStatus.ready")
        } else {
            TypeSpecBundle.message("settings.typespec.compilerStatus.missing")
        }
    }
}
