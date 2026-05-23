package com.example.typespec.inspections

import com.example.typespec.TypeSpecBundle
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.Nls

internal enum class TypeSpecPackageJsonRule(
    val messageKey: String,
    val severity: TypeSpecFindingSeverity,
    val fixAction: TypeSpecPackageJsonFixAction,
    private val anchorSelector: (TypeSpecPackageJsonPsiAnchors) -> PsiElement,
) {
    TPKG001(
        "inspection.tpkg001",
        TypeSpecFindingSeverity.WARNING,
        TypeSpecPackageJsonFixAction.APPLY_RECOMMENDED_METADATA,
        { it.typeProperty ?: it.rootObject },
    ),
    TPKG002(
        "inspection.tpkg002",
        TypeSpecFindingSeverity.WARNING,
        TypeSpecPackageJsonFixAction.APPLY_RECOMMENDED_METADATA,
        { it.mainProperty ?: it.rootObject },
    ),
    TPKG003(
        "inspection.tpkg003",
        TypeSpecFindingSeverity.WARNING,
        TypeSpecPackageJsonFixAction.APPLY_RECOMMENDED_METADATA,
        {
            it.exportsProperty
                ?: it.exportsDot.dotProperty
                ?: it.exportsDot.typespecExportProperty
                ?: it.rootObject
        },
    ),
    TPKG004(
        "inspection.tpkg004",
        TypeSpecFindingSeverity.WARNING,
        TypeSpecPackageJsonFixAction.MOVE_COMPILER_TO_PEER_DEPENDENCIES,
        {
            it.compilerDependencyProperty
                ?: it.devCompilerDependencyProperty
                ?: it.dependenciesProperty
                ?: it.devDependenciesProperty
                ?: it.rootObject
        },
    ),
    TPKG005(
        "inspection.tpkg005",
        TypeSpecFindingSeverity.INFORMATION,
        TypeSpecPackageJsonFixAction.APPLY_RECOMMENDED_METADATA,
        { it.exportsProperty ?: it.tspMainProperty ?: it.mainProperty ?: it.rootObject },
    ),
    ;

    fun anchor(psi: TypeSpecPackageJsonPsiAnchors): PsiElement = anchorSelector(psi)

    @Nls
    fun localizedMessage(): String = TypeSpecBundle.message(messageKey)
}

internal data class TypeSpecInspectionFinding(
    val rule: TypeSpecPackageJsonRule,
    val anchor: PsiElement,
)
