package com.example.typespec.inspections

internal data class TypeSpecPackageRulesInput(
    val type: String?,
    val main: String?,
    val tspMain: String?,
    val typespecExport: String?,
    val dependencies: Map<String, String>,
    val devDependencies: Map<String, String>,
    val peerDependencies: Map<String, String>,
    val supportingSignals: Set<TypeSpecSupportingSignal>,
    val packageShapeSignals: Set<TypeSpecPackageShapeSignal>,
    val isLikelyTypeSpecExtensionPackage: Boolean,
    val recommendedLayoutStatus: TypeSpecRecommendedLayoutStatus,
)

internal fun evaluateRules(input: TypeSpecPackageRulesInput): List<TypeSpecInspectionFindingDescriptor> {
    val findings = mutableListOf<TypeSpecInspectionFindingDescriptor>()

    if (input.isLikelyTypeSpecExtensionPackage) {
        if (input.type != RECOMMENDED_TYPE_MODULE) {
            findings += TypeSpecInspectionFindingDescriptor(
                rule = TypeSpecPackageJsonRule.TPKG001,
                severity = TypeSpecFindingSeverity.WARNING,
            )
        }
        if (input.main.isNullOrBlank()) {
            findings += TypeSpecInspectionFindingDescriptor(
                rule = TypeSpecPackageJsonRule.TPKG002,
                severity = TypeSpecFindingSeverity.WARNING,
            )
        }
        when (input.recommendedLayoutStatus) {
            TypeSpecRecommendedLayoutStatus.MISSING -> findings += TypeSpecInspectionFindingDescriptor(
                rule = TypeSpecPackageJsonRule.TPKG003,
                severity = TypeSpecFindingSeverity.WARNING,
            )
            TypeSpecRecommendedLayoutStatus.VALID_FALLBACK -> findings += TypeSpecInspectionFindingDescriptor(
                rule = TypeSpecPackageJsonRule.TPKG005,
                severity = TypeSpecFindingSeverity.INFORMATION,
            )
            TypeSpecRecommendedLayoutStatus.PREFERRED -> Unit
        }
    }

    val hasCompilerOutsidePeerDependencies = input.dependencies.containsKey(TYPESPEC_COMPILER_PACKAGE) ||
        input.devDependencies.containsKey(TYPESPEC_COMPILER_PACKAGE)
    if (hasCompilerOutsidePeerDependencies &&
        !input.peerDependencies.containsKey(TYPESPEC_COMPILER_PACKAGE)
    ) {
        findings += TypeSpecInspectionFindingDescriptor(
            rule = TypeSpecPackageJsonRule.TPKG004,
            severity = TypeSpecFindingSeverity.WARNING,
        )
    }

    return findings
}

internal data class TypeSpecInspectionFindingDescriptor(
    val rule: TypeSpecPackageJsonRule,
    val severity: TypeSpecFindingSeverity,
)

internal fun buildRulesInput(
    type: String? = null,
    main: String? = null,
    tspMain: String? = null,
    typespecExport: String? = null,
    dependencies: Map<String, String> = emptyMap(),
    devDependencies: Map<String, String> = emptyMap(),
    peerDependencies: Map<String, String> = emptyMap(),
): TypeSpecPackageRulesInput {
    val supportingSignals = collectSupportingSignals(dependencies, devDependencies, peerDependencies)
    val packageShapeSignals = collectPackageShapeSignals(
        typespecExport = typespecExport,
        tspMain = tspMain,
        main = main,
        supportingSignals = supportingSignals,
    )
    return TypeSpecPackageRulesInput(
        type = type,
        main = main,
        tspMain = tspMain,
        typespecExport = typespecExport,
        dependencies = dependencies,
        devDependencies = devDependencies,
        peerDependencies = peerDependencies,
        supportingSignals = supportingSignals,
        packageShapeSignals = packageShapeSignals,
        isLikelyTypeSpecExtensionPackage = packageShapeSignals.isNotEmpty(),
        recommendedLayoutStatus = resolveRecommendedLayoutStatus(
            typespecExport = typespecExport,
            tspMain = tspMain,
            main = main,
        ),
    )
}

private fun collectSupportingSignals(
    dependencies: Map<String, String>,
    devDependencies: Map<String, String>,
    peerDependencies: Map<String, String>,
): Set<TypeSpecSupportingSignal> {
    val allDependencies = dependencies.keys + devDependencies.keys + peerDependencies.keys
    val signals = linkedSetOf<TypeSpecSupportingSignal>()
    if (allDependencies.contains(TYPESPEC_COMPILER_PACKAGE)) {
        signals += TypeSpecSupportingSignal.TYPESPEC_COMPILER_DEPENDENCY
    }
    if (allDependencies.any { it.startsWith(TYPESPEC_SCOPE_PREFIX) }) {
        signals += TypeSpecSupportingSignal.TYPESPEC_SCOPED_DEPENDENCY
    }
    return signals
}

private fun collectPackageShapeSignals(
    typespecExport: String?,
    tspMain: String?,
    main: String?,
    supportingSignals: Set<TypeSpecSupportingSignal>,
): Set<TypeSpecPackageShapeSignal> {
    val signals = linkedSetOf<TypeSpecPackageShapeSignal>()
    if (!typespecExport.isNullOrBlank()) {
        signals += TypeSpecPackageShapeSignal.TYPESPEC_EXPORT
    }
    if (!tspMain.isNullOrBlank()) {
        signals += TypeSpecPackageShapeSignal.TSP_MAIN
    }
    if (!main.isNullOrBlank() && supportingSignals.isNotEmpty()) {
        signals += TypeSpecPackageShapeSignal.MAIN
    }
    return signals
}

private fun resolveRecommendedLayoutStatus(
    typespecExport: String?,
    tspMain: String?,
    main: String?,
): TypeSpecRecommendedLayoutStatus = when {
    !typespecExport.isNullOrBlank() -> TypeSpecRecommendedLayoutStatus.PREFERRED
    !tspMain.isNullOrBlank() || !main.isNullOrBlank() -> TypeSpecRecommendedLayoutStatus.VALID_FALLBACK
    else -> TypeSpecRecommendedLayoutStatus.MISSING
}

private const val TYPESPEC_SCOPE_PREFIX = "@typespec/"
