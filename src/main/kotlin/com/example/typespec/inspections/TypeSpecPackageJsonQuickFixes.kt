package com.example.typespec.inspections

import com.example.typespec.TypeSpecBundle
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project

private val FIX_ACTION_MESSAGE_KEYS = mapOf(
    TypeSpecPackageJsonFixAction.APPLY_RECOMMENDED_METADATA to "fix.applyRecommendedMetadata",
    TypeSpecPackageJsonFixAction.MOVE_COMPILER_TO_PEER_DEPENDENCIES to "fix.moveCompilerToPeerDependencies",
)

private class TypeSpecPackageJsonQuickFix(
    private val action: TypeSpecPackageJsonFixAction,
) : LocalQuickFix {
    override fun getFamilyName(): String =
        TypeSpecBundle.message(FIX_ACTION_MESSAGE_KEYS.getValue(action))

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val metadata = metadataFromElement(descriptor.psiElement) ?: return
        TypeSpecPackageJsonEditor.applyFix(action, project, metadata)
    }
}

private val quickFixByAction = TypeSpecPackageJsonFixAction.entries.associateWith { action ->
    TypeSpecPackageJsonQuickFix(action)
}

internal fun quickFixFor(action: TypeSpecPackageJsonFixAction): LocalQuickFix =
    quickFixByAction.getValue(action)
