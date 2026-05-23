package com.example.typespec.inspections

import com.example.typespec.TypeSpecBundle
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project

internal class AddTypeModuleFix : LocalQuickFix {
    override fun getFamilyName(): String = TypeSpecBundle.message("fix.addTypeModule")

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val metadata = metadataFromElement(descriptor.psiElement) ?: return
        TypeSpecPackageJsonEditor.setTypeModule(project, metadata)
    }
}

internal class AddMainEntrypointFix : LocalQuickFix {
    override fun getFamilyName(): String = TypeSpecBundle.message("fix.addMainEntrypoint")

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val metadata = metadataFromElement(descriptor.psiElement) ?: return
        TypeSpecPackageJsonEditor.addMainEntrypoint(project, metadata)
    }
}

internal class AddTypespecExportFix : LocalQuickFix {
    override fun getFamilyName(): String = TypeSpecBundle.message("fix.addTypespecExport")

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val metadata = metadataFromElement(descriptor.psiElement) ?: return
        TypeSpecPackageJsonEditor.addTypespecExport(project, metadata)
    }
}

internal class MoveCompilerDependencyToPeerDependenciesFix : LocalQuickFix {
    override fun getFamilyName(): String = TypeSpecBundle.message("fix.moveCompilerToPeerDependencies")

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val metadata = metadataFromElement(descriptor.psiElement) ?: return
        TypeSpecPackageJsonEditor.moveCompilerToPeerDependencies(project, metadata)
    }
}

internal class ApplyRecommendedTypeSpecPackageMetadataFix : LocalQuickFix {
    override fun getFamilyName(): String = TypeSpecBundle.message("fix.applyRecommendedMetadata")

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val metadata = metadataFromElement(descriptor.psiElement) ?: return
        TypeSpecPackageJsonEditor.applyRecommendedMetadata(project, metadata)
    }
}

internal fun quickFixesForRule(
    rule: TypeSpecPackageJsonRule,
    metadata: TypeSpecPackageMetadata,
): Array<LocalQuickFix> {
    val fixes = linkedSetOf<LocalQuickFix>()
    when (rule) {
        TypeSpecPackageJsonRule.TPKG001 -> fixes += AddTypeModuleFix()
        TypeSpecPackageJsonRule.TPKG002 -> fixes += AddMainEntrypointFix()
        TypeSpecPackageJsonRule.TPKG003 -> fixes += AddTypespecExportFix()
        TypeSpecPackageJsonRule.TPKG004 -> fixes += MoveCompilerDependencyToPeerDependenciesFix()
        TypeSpecPackageJsonRule.TPKG005 -> fixes += AddTypespecExportFix()
    }

    if (rule != TypeSpecPackageJsonRule.TPKG004 && hasRecommendedMetadataFix(metadata)) {
        fixes += ApplyRecommendedTypeSpecPackageMetadataFix()
    }

    return fixes.toTypedArray()
}

internal fun hasRecommendedMetadataFix(metadata: TypeSpecPackageMetadata): Boolean =
    metadata.rules.needsRecommendedMetadataFix()
