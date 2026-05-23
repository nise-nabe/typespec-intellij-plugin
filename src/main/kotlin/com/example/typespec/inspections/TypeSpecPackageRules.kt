package com.example.typespec.inspections

internal data class TypeSpecPackageRulesInput(
    val type: String?,
    val main: String?,
    val tspMain: String?,
    val typespecExport: String?,
    val dependencies: Map<String, String>,
    val devDependencies: Map<String, String>,
    val peerDependencies: Map<String, String>,
    val isLikelyTypeSpecExtensionPackage: Boolean,
    val recommendedLayoutStatus: TypeSpecRecommendedLayoutStatus,
)

internal fun evaluateRules(input: TypeSpecPackageRulesInput): List<TypeSpecInspectionFindingDescriptor> {
    val findings = mutableListOf<TypeSpecInspectionFindingDescriptor>()

    if (input.isLikelyTypeSpecExtensionPackage) {
        if (input.type != RECOMMENDED_TYPE_MODULE) {
            findings += descriptorFor(TypeSpecPackageJsonRule.TPKG001, TypeSpecFindingSeverity.WARNING)
        }
        if (input.main.isNullOrBlank()) {
            findings += descriptorFor(TypeSpecPackageJsonRule.TPKG002, TypeSpecFindingSeverity.WARNING)
        }
        when (input.recommendedLayoutStatus) {
            TypeSpecRecommendedLayoutStatus.MISSING -> findings += descriptorFor(
                TypeSpecPackageJsonRule.TPKG003,
                TypeSpecFindingSeverity.WARNING,
            )
            TypeSpecRecommendedLayoutStatus.VALID_FALLBACK -> findings += descriptorFor(
                TypeSpecPackageJsonRule.TPKG005,
                TypeSpecFindingSeverity.INFORMATION,
            )
            TypeSpecRecommendedLayoutStatus.PREFERRED -> Unit
        }

        val hasCompilerOutsidePeerDependencies = input.dependencies.containsKey(TYPESPEC_COMPILER_PACKAGE) ||
            input.devDependencies.containsKey(TYPESPEC_COMPILER_PACKAGE)
        if (hasCompilerOutsidePeerDependencies &&
            !input.peerDependencies.containsKey(TYPESPEC_COMPILER_PACKAGE)
        ) {
            findings += descriptorFor(TypeSpecPackageJsonRule.TPKG004, TypeSpecFindingSeverity.WARNING)
        }
    }

    return findings
}

internal data class TypeSpecInspectionFindingDescriptor(
    val rule: TypeSpecPackageJsonRule,
    val severity: TypeSpecFindingSeverity,
    val fixAction: TypeSpecPackageJsonFixAction,
)

private fun descriptorFor(
    rule: TypeSpecPackageJsonRule,
    severity: TypeSpecFindingSeverity,
): TypeSpecInspectionFindingDescriptor =
    TypeSpecInspectionFindingDescriptor(
        rule = rule,
        severity = severity,
        fixAction = fixActionForRule(rule),
    )

private fun fixActionForRule(rule: TypeSpecPackageJsonRule): TypeSpecPackageJsonFixAction = when (rule) {
    TypeSpecPackageJsonRule.TPKG004 -> TypeSpecPackageJsonFixAction.MOVE_COMPILER_TO_PEER_DEPENDENCIES
    TypeSpecPackageJsonRule.TPKG001,
    TypeSpecPackageJsonRule.TPKG002,
    TypeSpecPackageJsonRule.TPKG003,
    TypeSpecPackageJsonRule.TPKG005,
    -> TypeSpecPackageJsonFixAction.APPLY_RECOMMENDED_METADATA
}

internal fun buildRulesInput(
    type: String? = null,
    main: String? = null,
    tspMain: String? = null,
    typespecExport: String? = null,
    dependencies: Map<String, String> = emptyMap(),
    devDependencies: Map<String, String> = emptyMap(),
    peerDependencies: Map<String, String> = emptyMap(),
): TypeSpecPackageRulesInput =
    TypeSpecPackageRulesInput(
        type = type,
        main = main,
        tspMain = tspMain,
        typespecExport = typespecExport,
        dependencies = dependencies,
        devDependencies = devDependencies,
        peerDependencies = peerDependencies,
        isLikelyTypeSpecExtensionPackage = isLikelyTypeSpecExtensionPackage(
            typespecExport = typespecExport,
            tspMain = tspMain,
            main = main,
            dependencies = dependencies,
            devDependencies = devDependencies,
            peerDependencies = peerDependencies,
        ),
        recommendedLayoutStatus = resolveRecommendedLayoutStatus(
            typespecExport = typespecExport,
            tspMain = tspMain,
            main = main,
        ),
    )

private fun isLikelyTypeSpecExtensionPackage(
    typespecExport: String?,
    tspMain: String?,
    main: String?,
    dependencies: Map<String, String>,
    devDependencies: Map<String, String>,
    peerDependencies: Map<String, String>,
): Boolean {
    if (!typespecExport.isNullOrBlank() || !tspMain.isNullOrBlank()) {
        return true
    }
    if (main.isNullOrBlank()) {
        return false
    }
    return hasTypespecDependencySignals(dependencies, devDependencies, peerDependencies)
}

private fun hasTypespecDependencySignals(
    dependencies: Map<String, String>,
    devDependencies: Map<String, String>,
    peerDependencies: Map<String, String>,
): Boolean {
    val allDependencies = dependencies.keys + devDependencies.keys + peerDependencies.keys
    return allDependencies.contains(TYPESPEC_COMPILER_PACKAGE) ||
        allDependencies.any { it.startsWith(TYPESPEC_SCOPE_PREFIX) }
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
