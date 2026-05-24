package com.example.typespec.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.json.psi.JsonFile
import com.intellij.psi.PsiElementVisitor

class TypeSpecPackageJsonInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : com.intellij.json.psi.JsonElementVisitor() {
            override fun visitFile(file: com.intellij.psi.PsiFile) {
                if (file !is JsonFile || file.name != "package.json") {
                    return
                }

                val metadata = TypeSpecPackageMetadata.fromJsonFile(file) ?: return
                for (finding in metadata.evaluateFindings()) {
                    val highlightType = highlightTypeForSeverity(finding.rule.severity(metadata.input))
                    val message = finding.rule.localizedMessage(metadata.input)
                    holder.registerProblem(
                        finding.anchor,
                        message,
                        highlightType,
                        quickFixFor(finding.rule.fixAction),
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
}
