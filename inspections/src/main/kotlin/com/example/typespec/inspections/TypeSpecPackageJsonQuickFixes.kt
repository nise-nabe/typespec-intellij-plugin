package com.example.typespec.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project

private class TypeSpecPackageJsonQuickFix(
    private val action: TypeSpecPackageJsonFixAction,
) : LocalQuickFix {
    override fun getFamilyName(): String = action.localizedFamilyName()

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val metadata = metadataFromElement(descriptor.psiElement) ?: return
        action.apply(project, metadata)
    }
}

private val quickFixByAction = TypeSpecPackageJsonFixAction.entries.associateWith { action ->
    TypeSpecPackageJsonQuickFix(action)
}

internal fun quickFixFor(action: TypeSpecPackageJsonFixAction): LocalQuickFix =
    quickFixByAction.getValue(action)
