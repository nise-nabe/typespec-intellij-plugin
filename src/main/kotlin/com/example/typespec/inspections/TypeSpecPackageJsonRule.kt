package com.example.typespec.inspections

import com.example.typespec.TypeSpecBundle
import com.intellij.json.psi.JsonElementGenerator
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.Nls

internal enum class TypeSpecPackageJsonRule(
    internal val defaultMessageKey: String,
    internal val defaultSeverity: TypeSpecFindingSeverity,
    val fixAction: TypeSpecPackageJsonFixAction,
    internal val anchorSelector: (TypeSpecPackageJsonPsiAnchors) -> PsiElement,
) {
    TPKG001(
        "inspection.tpkg001",
        TypeSpecFindingSeverity.WARNING,
        TypeSpecPackageJsonFixAction.APPLY_RECOMMENDED_METADATA,
        { it.typeProperty ?: it.rootObject },
    ) {
        override fun isViolated(input: TypeSpecPackageRulesInput): Boolean =
            input.type != RECOMMENDED_TYPE_MODULE

        override fun applyFix(
            metadata: TypeSpecPackageMetadata,
            generator: JsonElementGenerator,
        ) {
            val psi = metadata.psi
            upsertStringProperty(
                container = psi.rootObject,
                existing = psi.typeProperty,
                name = "type",
                value = RECOMMENDED_TYPE_MODULE,
                generator = generator,
            )
        }
    },
    TPKG002(
        "inspection.tpkg002",
        TypeSpecFindingSeverity.WARNING,
        TypeSpecPackageJsonFixAction.APPLY_RECOMMENDED_METADATA,
        { it.mainProperty ?: it.rootObject },
    ) {
        override fun isViolated(input: TypeSpecPackageRulesInput): Boolean =
            input.main.isNullOrBlank()

        override fun applyFix(
            metadata: TypeSpecPackageMetadata,
            generator: JsonElementGenerator,
        ) {
            val psi = metadata.psi
            upsertStringProperty(
                container = psi.rootObject,
                existing = psi.mainProperty,
                name = "main",
                value = RECOMMENDED_MAIN,
                generator = generator,
            )
        }
    },
    TPKG003(
        "inspection.tpkg003",
        TypeSpecFindingSeverity.WARNING,
        TypeSpecPackageJsonFixAction.APPLY_RECOMMENDED_METADATA,
        { it.exportsLayout.missingLayoutInspectionAnchor(it.rootObject) },
    ) {
        override fun isViolated(input: TypeSpecPackageRulesInput): Boolean =
            input.recommendedLayoutStatus != TypeSpecRecommendedLayoutStatus.PREFERRED

        override fun severity(input: TypeSpecPackageRulesInput): TypeSpecFindingSeverity =
            when (input.recommendedLayoutStatus) {
                TypeSpecRecommendedLayoutStatus.MISSING -> TypeSpecFindingSeverity.WARNING
                TypeSpecRecommendedLayoutStatus.VALID_FALLBACK -> TypeSpecFindingSeverity.INFORMATION
                TypeSpecRecommendedLayoutStatus.PREFERRED -> defaultSeverity
            }

        override fun messageKey(input: TypeSpecPackageRulesInput): String =
            when (input.recommendedLayoutStatus) {
                TypeSpecRecommendedLayoutStatus.MISSING -> "inspection.tpkg003"
                TypeSpecRecommendedLayoutStatus.VALID_FALLBACK -> "inspection.tpkg005"
                TypeSpecRecommendedLayoutStatus.PREFERRED -> defaultMessageKey
            }

        override fun anchor(
            psi: TypeSpecPackageJsonPsiAnchors,
            input: TypeSpecPackageRulesInput,
        ): PsiElement =
            when (input.recommendedLayoutStatus) {
                TypeSpecRecommendedLayoutStatus.MISSING ->
                    psi.exportsLayout.missingLayoutInspectionAnchor(psi.rootObject)
                TypeSpecRecommendedLayoutStatus.VALID_FALLBACK ->
                    psi.exportsLayout.fallbackLayoutInspectionAnchor(
                        psi.tspMainProperty,
                        psi.mainProperty,
                        psi.rootObject,
                    )
                TypeSpecRecommendedLayoutStatus.PREFERRED ->
                    anchorSelector(psi)
            }

        override fun applyFix(
            metadata: TypeSpecPackageMetadata,
            generator: JsonElementGenerator,
        ) {
            applyRecommendedTypespecExportFix(metadata, generator)
        }
    },
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
    ) {
        override fun isViolated(input: TypeSpecPackageRulesInput): Boolean {
            val hasCompilerOutsidePeerDependencies = input.dependencies.containsKey(TYPESPEC_COMPILER_PACKAGE) ||
                input.devDependencies.containsKey(TYPESPEC_COMPILER_PACKAGE)
            return hasCompilerOutsidePeerDependencies &&
                !input.peerDependencies.containsKey(TYPESPEC_COMPILER_PACKAGE)
        }

        override fun applyFix(
            metadata: TypeSpecPackageMetadata,
            generator: JsonElementGenerator,
        ) {
            moveCompilerToPeerDependencies(metadata, generator)
        }
    },
    ;

    open fun severity(input: TypeSpecPackageRulesInput): TypeSpecFindingSeverity = defaultSeverity

    open fun messageKey(input: TypeSpecPackageRulesInput): String = defaultMessageKey

    open fun anchor(
        psi: TypeSpecPackageJsonPsiAnchors,
        input: TypeSpecPackageRulesInput,
    ): PsiElement = anchorSelector(psi)

    @Nls
    fun localizedMessage(input: TypeSpecPackageRulesInput): String =
        TypeSpecBundle.message(messageKey(input))

    abstract fun isViolated(input: TypeSpecPackageRulesInput): Boolean

    abstract fun applyFix(
        metadata: TypeSpecPackageMetadata,
        generator: JsonElementGenerator,
    )
}

internal data class TypeSpecInspectionFinding(
    val rule: TypeSpecPackageJsonRule,
    val anchor: PsiElement,
)

private fun applyRecommendedTypespecExportFix(
    metadata: TypeSpecPackageMetadata,
    generator: JsonElementGenerator,
) {
    val psi = metadata.psi
    psi.exportsLayout.applyRecommendedTypespecExport(psi.rootObject, generator)
}

internal fun applyViolatedRules(
    metadata: TypeSpecPackageMetadata,
    fixAction: TypeSpecPackageJsonFixAction,
    generator: JsonElementGenerator,
) {
    var currentMetadata = metadata
    while (true) {
        val rule = evaluateRules(currentMetadata.input)
            .firstOrNull { it.fixAction == fixAction }
            ?: break
        rule.applyFix(currentMetadata, generator)
        currentMetadata = currentMetadata.refresh() ?: return
    }
}
