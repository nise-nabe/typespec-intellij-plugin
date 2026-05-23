package com.example.typespec.inspections

import com.example.typespec.TypeSpecBundle
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.json.psi.JsonFile
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.annotations.Nls

class TypeSpecPackageJsonInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : com.intellij.json.psi.JsonElementVisitor() {
            override fun visitFile(file: com.intellij.psi.PsiFile) {
                if (file !is JsonFile || file.name != "package.json") {
                    return
                }

                val metadata = TypeSpecPackageMetadata.fromJsonFile(file) ?: return
                for (finding in metadata.evaluateFindings()) {
                    holder.registerProblem(
                        finding.anchor,
                        messageForRule(finding.rule),
                        highlightTypeForSeverity(finding.severity),
                        *quickFixesForRule(finding.rule, metadata),
                    )
                }
            }
        }

    override fun isDumbAware(): Boolean = true

    private fun highlightTypeForSeverity(severity: TypeSpecFindingSeverity): ProblemHighlightType =
        when (severity) {
            TypeSpecFindingSeverity.WARNING -> ProblemHighlightType.GENERIC_ERROR_OR_WARNING
            TypeSpecFindingSeverity.INFORMATION -> ProblemHighlightType.INFORMATION
        }

    @Nls
    private fun messageForRule(rule: TypeSpecPackageJsonRule): String = when (rule) {
        TypeSpecPackageJsonRule.TPKG001 -> TypeSpecBundle.message("inspection.tpkg001")
        TypeSpecPackageJsonRule.TPKG002 -> TypeSpecBundle.message("inspection.tpkg002")
        TypeSpecPackageJsonRule.TPKG003 -> TypeSpecBundle.message("inspection.tpkg003")
        TypeSpecPackageJsonRule.TPKG004 -> TypeSpecBundle.message("inspection.tpkg004")
        TypeSpecPackageJsonRule.TPKG005 -> TypeSpecBundle.message("inspection.tpkg005")
    }
}
