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
        runWrite(project, metadata) {
            applyRecommendedMetadataChanges(project, metadata)
        }
    }

    fun setTypeModule(project: Project, metadata: TypeSpecPackageMetadata) {
        runWrite(project, metadata) {
            setTypeModuleChanges(project, metadata)
        }
    }

    fun addMainEntrypoint(project: Project, metadata: TypeSpecPackageMetadata) {
        if (!metadata.main.isNullOrBlank()) {
            return
        }
        runWrite(project, metadata) {
            addMainEntrypointChanges(project, metadata)
        }
    }

    fun addTypespecExport(project: Project, metadata: TypeSpecPackageMetadata) {
        if (!metadata.typespecExport.isNullOrBlank()) {
            return
        }
        runWrite(project, metadata) {
            addTypespecExportChanges(project, metadata)
        }
    }

    fun moveCompilerToPeerDependencies(project: Project, metadata: TypeSpecPackageMetadata) {
        val compilerVersion = metadata.dependencies[TYPESPEC_COMPILER_PACKAGE] ?: return
        if (metadata.peerDependencies.containsKey(TYPESPEC_COMPILER_PACKAGE)) {
            return
        }
        runWrite(project, metadata) {
            moveCompilerToPeerDependenciesChanges(project, metadata, compilerVersion)
        }
    }

    private fun applyRecommendedMetadataChanges(project: Project, metadata: TypeSpecPackageMetadata) {
        if (metadata.type != RECOMMENDED_TYPE_MODULE) {
            setTypeModuleChanges(project, metadata)
        }
        if (metadata.main.isNullOrBlank()) {
            addMainEntrypointChanges(project, metadata)
        }
        if (metadata.typespecExport.isNullOrBlank()) {
            addTypespecExportChanges(project, metadata)
        }
    }

    private fun setTypeModuleChanges(project: Project, metadata: TypeSpecPackageMetadata) {
        val generator = JsonElementGenerator(project)
        val newProperty = generator.createProperty("type", jsonString(RECOMMENDED_TYPE_MODULE))
        if (metadata.typeProperty != null) {
            metadata.typeProperty.replace(newProperty)
        } else {
            metadata.rootObject.add(newProperty)
        }
    }

    private fun addMainEntrypointChanges(project: Project, metadata: TypeSpecPackageMetadata) {
        val generator = JsonElementGenerator(project)
        metadata.rootObject.add(generator.createProperty("main", jsonString(RECOMMENDED_MAIN)))
    }

    private fun addTypespecExportChanges(project: Project, metadata: TypeSpecPackageMetadata) {
        val generator = JsonElementGenerator(project)
        when {
            metadata.exportsProperty == null -> {
                metadata.rootObject.add(
                    generator.createProperty(
                        "exports",
                        """{ ".": { "typespec": ${jsonString(RECOMMENDED_TYPESPEC_EXPORT)} } }""",
                    ),
                )
            }
            metadata.exportsDotProperty == null -> {
                val exportsObject = metadata.exportsProperty.value as JsonObject
                exportsObject.add(
                    generator.createProperty(
                        ".",
                        """{ "typespec": ${jsonString(RECOMMENDED_TYPESPEC_EXPORT)} }""",
                    ),
                )
            }
            metadata.exportsDotKind == TypeSpecExportsDotKind.STRING -> {
                val existingJsEntry = metadata.defaultExport ?: readStringValue(metadata.exportsDotProperty.value)
                val exportObject = buildString {
                    append("{ ")
                    if (!existingJsEntry.isNullOrBlank()) {
                        append(""""default": ${jsonString(existingJsEntry)}, """)
                    }
                    append(""""typespec": ${jsonString(RECOMMENDED_TYPESPEC_EXPORT)} }""")
                }
                metadata.exportsDotProperty.replace(generator.createProperty(".", exportObject))
            }
            metadata.typespecExportProperty == null -> {
                val dotObject = metadata.exportsDotProperty.value as JsonObject
                dotObject.add(generator.createProperty("typespec", jsonString(RECOMMENDED_TYPESPEC_EXPORT)))
            }
        }
    }

    private fun moveCompilerToPeerDependenciesChanges(
        project: Project,
        metadata: TypeSpecPackageMetadata,
        compilerVersion: String,
    ) {
        val generator = JsonElementGenerator(project)
        if (metadata.peerDependenciesProperty == null) {
            metadata.rootObject.add(
                generator.createProperty(
                    "peerDependencies",
                    """{ ${jsonString(TYPESPEC_COMPILER_PACKAGE)}: ${jsonString(compilerVersion)} }""",
                ),
            )
            return
        }

        val peerDependenciesObject = metadata.peerDependenciesProperty.value as JsonObject
        peerDependenciesObject.add(
            generator.createProperty(TYPESPEC_COMPILER_PACKAGE, jsonString(compilerVersion)),
        )
    }

    private fun runWrite(project: Project, metadata: TypeSpecPackageMetadata, action: () -> Unit) {
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
