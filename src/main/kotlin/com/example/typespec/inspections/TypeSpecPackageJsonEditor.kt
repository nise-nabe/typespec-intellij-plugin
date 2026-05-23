package com.example.typespec.inspections

import com.intellij.json.psi.JsonElementGenerator
import com.intellij.json.psi.JsonFile
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

internal object TypeSpecPackageJsonEditor {
    fun applyFixAction(
        project: Project,
        metadata: TypeSpecPackageMetadata,
        fixAction: TypeSpecPackageJsonFixAction,
    ) {
        applyViolatedRules(
            metadata = metadata,
            fixAction = fixAction,
            generator = JsonElementGenerator(project),
        )
    }
}

internal fun metadataFromElement(element: PsiElement): TypeSpecPackageMetadata? {
    val file = element.containingFile as? JsonFile ?: return null
    return TypeSpecPackageMetadata.fromJsonFile(file)
}
