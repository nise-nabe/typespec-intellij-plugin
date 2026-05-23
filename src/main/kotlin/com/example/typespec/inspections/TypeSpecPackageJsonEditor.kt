package com.example.typespec.inspections

import com.intellij.json.psi.JsonElementGenerator
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

internal object TypeSpecPackageJsonEditor {
    fun applyRecommendedMetadata(project: Project, metadata: TypeSpecPackageMetadata) {
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

    fun moveCompilerToPeerDependencies(project: Project, metadata: TypeSpecPackageMetadata) {
        val rules = metadata.rules
        val compilerVersion = rules.dependencies[TYPESPEC_COMPILER_PACKAGE]
            ?: rules.devDependencies[TYPESPEC_COMPILER_PACKAGE]
            ?: return
        if (rules.peerDependencies.containsKey(TYPESPEC_COMPILER_PACKAGE)) {
            return
        }

        val psi = metadata.psi
        val generator = JsonElementGenerator(project)
        val compilerPeerProperty = generator.createProperty(
            TYPESPEC_COMPILER_PACKAGE,
            jsonString(compilerVersion),
        )
        val peerDependenciesSnippet =
            """{ ${jsonString(TYPESPEC_COMPILER_PACKAGE)}: ${jsonString(compilerVersion)} }"""

        when (val peerDependenciesProperty = psi.peerDependenciesProperty) {
            null -> psi.rootObject.add(
                generator.createProperty("peerDependencies", peerDependenciesSnippet),
            )
            else -> {
                val peerDependenciesObject = peerDependenciesProperty.value as? JsonObject
                if (peerDependenciesObject == null) {
                    peerDependenciesProperty.replace(
                        generator.createProperty("peerDependencies", peerDependenciesSnippet),
                    )
                } else {
                    val existingCompilerProperty = peerDependenciesObject.findProperty(TYPESPEC_COMPILER_PACKAGE)
                    if (existingCompilerProperty != null) {
                        existingCompilerProperty.replace(compilerPeerProperty)
                    } else {
                        peerDependenciesObject.add(compilerPeerProperty)
                    }
                }
            }
        }

        psi.compilerDependencyProperty?.delete()
        psi.devCompilerDependencyProperty?.delete()
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
        val psi = metadata.psi
        val generator = JsonElementGenerator(project)
        val newProperty = generator.createProperty("main", jsonString(RECOMMENDED_MAIN))
        if (psi.mainProperty != null) {
            psi.mainProperty.replace(newProperty)
        } else {
            psi.rootObject.add(newProperty)
        }
    }

    private fun addTypespecExportChanges(project: Project, metadata: TypeSpecPackageMetadata) {
        val psi = metadata.psi
        val generator = JsonElementGenerator(project)
        psi.exportsDot.applyRecommendedTypespecExport(psi.rootObject, generator)
    }

}

internal fun metadataFromElement(element: PsiElement): TypeSpecPackageMetadata? {
    val file = element.containingFile as? JsonFile ?: return null
    return TypeSpecPackageMetadata.fromJsonFile(file)
}
