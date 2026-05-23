package com.example.typespec.inspections

import com.intellij.json.psi.JsonElementGenerator
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

internal object TypeSpecPackageJsonEditor {
    fun applyRecommendedMetadata(project: Project, metadata: TypeSpecPackageMetadata) {
        runWrite(project) {
            applyRecommendedMetadataChanges(project, metadata)
        }
    }

    fun setTypeModule(project: Project, metadata: TypeSpecPackageMetadata) {
        runWrite(project) {
            setTypeModuleChanges(project, metadata)
        }
    }

    fun addMainEntrypoint(project: Project, metadata: TypeSpecPackageMetadata) {
        if (!metadata.rules.main.isNullOrBlank()) {
            return
        }
        runWrite(project) {
            addMainEntrypointChanges(project, metadata)
        }
    }

    fun addTypespecExport(project: Project, metadata: TypeSpecPackageMetadata) {
        if (!metadata.rules.typespecExport.isNullOrBlank()) {
            return
        }
        runWrite(project) {
            addTypespecExportChanges(project, metadata)
        }
    }

    fun moveCompilerToPeerDependencies(project: Project, metadata: TypeSpecPackageMetadata) {
        val rules = metadata.rules
        val compilerVersion = rules.dependencies[TYPESPEC_COMPILER_PACKAGE]
            ?: rules.devDependencies[TYPESPEC_COMPILER_PACKAGE]
            ?: return
        if (rules.peerDependencies.containsKey(TYPESPEC_COMPILER_PACKAGE)) {
            return
        }
        runWrite(project) {
            moveCompilerToPeerDependenciesChanges(project, metadata, compilerVersion)
        }
    }

    private fun applyRecommendedMetadataChanges(project: Project, metadata: TypeSpecPackageMetadata) {
        val rules = metadata.rules
        if (rules.type != RECOMMENDED_TYPE_MODULE) {
            setTypeModuleChanges(project, metadata)
        }
        if (rules.main.isNullOrBlank()) {
            addMainEntrypointChanges(project, metadata)
        }
        if (rules.typespecExport.isNullOrBlank()) {
            addTypespecExportChanges(project, metadata)
        }
    }

    private fun setTypeModuleChanges(project: Project, metadata: TypeSpecPackageMetadata) {
        val psi = metadata.psi
        val generator = JsonElementGenerator(project)
        val newProperty = generator.createProperty("type", jsonString(RECOMMENDED_TYPE_MODULE))
        if (psi.typeProperty != null) {
            psi.typeProperty.replace(newProperty)
        } else {
            psi.rootObject.add(newProperty)
        }
    }

    private fun addMainEntrypointChanges(project: Project, metadata: TypeSpecPackageMetadata) {
        val generator = JsonElementGenerator(project)
        metadata.psi.rootObject.add(generator.createProperty("main", jsonString(RECOMMENDED_MAIN)))
    }

    private fun addTypespecExportChanges(project: Project, metadata: TypeSpecPackageMetadata) {
        val psi = metadata.psi
        val generator = JsonElementGenerator(project)
        when {
            psi.exportsProperty == null -> {
                psi.rootObject.add(
                    generator.createProperty(
                        "exports",
                        """{ ".": { "typespec": ${jsonString(RECOMMENDED_TYPESPEC_EXPORT)} } }""",
                    ),
                )
            }
            psi.exportsDotProperty == null -> {
                val recommendedExports =
                    """{ ".": { "typespec": ${jsonString(RECOMMENDED_TYPESPEC_EXPORT)} } }"""
                val exportsObject = psi.exportsProperty.value as? JsonObject
                if (exportsObject == null) {
                    psi.exportsProperty.replace(generator.createProperty("exports", recommendedExports))
                } else {
                    exportsObject.add(
                        generator.createProperty(
                            ".",
                            """{ "typespec": ${jsonString(RECOMMENDED_TYPESPEC_EXPORT)} }""",
                        ),
                    )
                }
            }
            psi.exportsDotKind == TypeSpecExportsDotKind.STRING -> {
                val existingJsEntry = psi.defaultExport ?: readStringValue(psi.exportsDotProperty.value)
                val exportObject = buildString {
                    append("{ ")
                    if (!existingJsEntry.isNullOrBlank()) {
                        append(""""default": ${jsonString(existingJsEntry)}, """)
                    }
                    append(""""typespec": ${jsonString(RECOMMENDED_TYPESPEC_EXPORT)} }""")
                }
                psi.exportsDotProperty.replace(generator.createProperty(".", exportObject))
            }
            else -> {
                val typespecProperty = generator.createProperty(
                    "typespec",
                    jsonString(RECOMMENDED_TYPESPEC_EXPORT),
                )
                val dotObject = psi.exportsDotProperty.value as? JsonObject
                when {
                    dotObject != null && psi.typespecExportProperty == null ->
                        dotObject.add(typespecProperty)
                    psi.typespecExportProperty != null ->
                        psi.typespecExportProperty.replace(typespecProperty)
                    else ->
                        psi.exportsDotProperty.replace(
                            generator.createProperty(
                                ".",
                                """{ "typespec": ${jsonString(RECOMMENDED_TYPESPEC_EXPORT)} }""",
                            ),
                        )
                }
            }
        }
    }

    private fun moveCompilerToPeerDependenciesChanges(
        project: Project,
        metadata: TypeSpecPackageMetadata,
        compilerVersion: String,
    ) {
        val psi = metadata.psi
        val generator = JsonElementGenerator(project)
        if (psi.peerDependenciesProperty == null) {
            psi.rootObject.add(
                generator.createProperty(
                    "peerDependencies",
                    """{ ${jsonString(TYPESPEC_COMPILER_PACKAGE)}: ${jsonString(compilerVersion)} }""",
                ),
            )
        } else {
            val peerDependenciesObject = psi.peerDependenciesProperty.value as JsonObject
            peerDependenciesObject.add(
                generator.createProperty(TYPESPEC_COMPILER_PACKAGE, jsonString(compilerVersion)),
            )
        }

        psi.compilerDependencyProperty?.delete()
        psi.devCompilerDependencyProperty?.delete()
    }

    private fun runWrite(project: Project, action: () -> Unit) {
        WriteCommandAction.runWriteCommandAction(project, Runnable { action() })
    }

    private fun jsonString(value: String): String = "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

    private fun readStringValue(value: com.intellij.json.psi.JsonValue?): String? =
        (value as? JsonStringLiteral)?.value
}

internal fun metadataFromElement(element: PsiElement): TypeSpecPackageMetadata? {
    val file = element.containingFile as? JsonFile ?: return null
    return TypeSpecPackageMetadata.fromJsonFile(file)
}
