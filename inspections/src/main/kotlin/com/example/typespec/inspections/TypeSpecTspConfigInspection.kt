package com.example.typespec.inspections

import com.example.typespec.TypeSpecBundle
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile

class TypeSpecTspConfigInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : PsiElementVisitor() {
            override fun visitFile(file: PsiFile) {
                if (file.name != TSP_CONFIG_FILE_NAME) {
                    return
                }
                for (finding in TypeSpecTspConfigRules.evaluate(file.text)) {
                    holder.registerProblem(
                        file,
                        TypeSpecBundle.message(finding.messageKey),
                        highlightTypeForSeverity(finding.severity),
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

    companion object {
        private const val TSP_CONFIG_FILE_NAME = "tspconfig.yaml"
    }
}
