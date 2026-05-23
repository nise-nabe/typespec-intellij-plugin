package com.example.typespec.inspections

import com.example.typespec.TypeSpecBundle
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project

private enum class TypeSpecPackageJsonFixAction(
    val messageKey: String,
    val apply: (Project, TypeSpecPackageMetadata) -> Unit,
) {
    ADD_TYPE_MODULE("fix.addTypeModule", TypeSpecPackageJsonEditor::setTypeModule),
    ADD_MAIN_ENTRYPOINT("fix.addMainEntrypoint", TypeSpecPackageJsonEditor::addMainEntrypoint),
    ADD_TYPESPEC_EXPORT("fix.addTypespecExport", TypeSpecPackageJsonEditor::addTypespecExport),
    MOVE_COMPILER_TO_PEER_DEPENDENCIES(
        "fix.moveCompilerToPeerDependencies",
        TypeSpecPackageJsonEditor::moveCompilerToPeerDependencies,
    ),
    APPLY_RECOMMENDED_METADATA("fix.applyRecommendedMetadata", TypeSpecPackageJsonEditor::applyRecommendedMetadata),
}

private val RULE_FIX_ACTIONS = mapOf(
    TypeSpecPackageJsonRule.TPKG001 to TypeSpecPackageJsonFixAction.ADD_TYPE_MODULE,
    TypeSpecPackageJsonRule.TPKG002 to TypeSpecPackageJsonFixAction.ADD_MAIN_ENTRYPOINT,
    TypeSpecPackageJsonRule.TPKG003 to TypeSpecPackageJsonFixAction.ADD_TYPESPEC_EXPORT,
    TypeSpecPackageJsonRule.TPKG004 to TypeSpecPackageJsonFixAction.MOVE_COMPILER_TO_PEER_DEPENDENCIES,
    TypeSpecPackageJsonRule.TPKG005 to TypeSpecPackageJsonFixAction.ADD_TYPESPEC_EXPORT,
)

private class TypeSpecPackageJsonQuickFix(
    private val action: TypeSpecPackageJsonFixAction,
) : LocalQuickFix {
    override fun getFamilyName(): String = TypeSpecBundle.message(action.messageKey)

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val metadata = metadataFromElement(descriptor.psiElement) ?: return
        action.apply(project, metadata)
    }
}

internal fun quickFixesForRule(
    rule: TypeSpecPackageJsonRule,
    metadata: TypeSpecPackageMetadata,
): Array<LocalQuickFix> {
    val fixes = linkedSetOf<LocalQuickFix>()
    RULE_FIX_ACTIONS[rule]?.let { fixes += TypeSpecPackageJsonQuickFix(it) }

    if (rule != TypeSpecPackageJsonRule.TPKG004 && metadata.rules.needsRecommendedMetadataFix()) {
        fixes += TypeSpecPackageJsonQuickFix(TypeSpecPackageJsonFixAction.APPLY_RECOMMENDED_METADATA)
    }

    return fixes.toTypedArray()
}
