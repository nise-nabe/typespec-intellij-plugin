package com.example.typespec.inspections

import com.example.typespec.TypeSpecBundle
import com.intellij.json.psi.JsonElementGenerator
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
        { it.exportsDot.missingLayoutInspectionAnchor(it.rootObject) },
    ) {
        override fun isViolated(input: TypeSpecPackageRulesInput): Boolean =
            input.recommendedLayoutStatus == TypeSpecRecommendedLayoutStatus.MISSING

        override fun applyFix(
            metadata: TypeSpecPackageMetadata,
            generator: JsonElementGenerator,
        ) {
            metadata.psi.exportsDot.applyRecommendedTypespecExport(metadata.psi.rootObject, generator)
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
    TPKG005(
        "inspection.tpkg005",
        TypeSpecFindingSeverity.INFORMATION,
        TypeSpecPackageJsonFixAction.APPLY_RECOMMENDED_METADATA,
        { it.exportsDot.fallbackLayoutInspectionAnchor(it.tspMainProperty, it.mainProperty, it.rootObject) },
    ) {
        override fun isViolated(input: TypeSpecPackageRulesInput): Boolean =
            input.recommendedLayoutStatus == TypeSpecRecommendedLayoutStatus.VALID_FALLBACK

        override fun applyFix(
            metadata: TypeSpecPackageMetadata,
            generator: JsonElementGenerator,
        ) {
            metadata.psi.exportsDot.applyRecommendedTypespecExport(metadata.psi.rootObject, generator)
        }
    },
    ;

    fun anchor(psi: TypeSpecPackageJsonPsiAnchors): PsiElement = anchorSelector(psi)

    @Nls
    fun localizedMessage(): String = TypeSpecBundle.message(messageKey)

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

internal fun applyViolatedRules(
    metadata: TypeSpecPackageMetadata,
    fixAction: TypeSpecPackageJsonFixAction,
    generator: JsonElementGenerator,
) {
    var currentMetadata = metadata
    val rules = evaluateRules(currentMetadata.input).filter { it.fixAction == fixAction }
    for (rule in rules) {
        rule.applyFix(currentMetadata, generator)
        currentMetadata = currentMetadata.refresh() ?: return
    }
}
