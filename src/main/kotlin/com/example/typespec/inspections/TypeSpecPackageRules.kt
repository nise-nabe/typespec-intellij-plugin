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

internal fun evaluateRules(input: TypeSpecPackageRulesInput): List<TypeSpecPackageJsonRule> {
    val findings = mutableListOf<TypeSpecPackageJsonRule>()

    if (input.isLikelyTypeSpecExtensionPackage) {
        if (input.type != RECOMMENDED_TYPE_MODULE) {
            findings += TypeSpecPackageJsonRule.TPKG001
        }
        if (input.main.isNullOrBlank()) {
            findings += TypeSpecPackageJsonRule.TPKG002
        }
        when (input.recommendedLayoutStatus) {
            TypeSpecRecommendedLayoutStatus.MISSING -> findings += TypeSpecPackageJsonRule.TPKG003
            TypeSpecRecommendedLayoutStatus.VALID_FALLBACK -> findings += TypeSpecPackageJsonRule.TPKG005
            TypeSpecRecommendedLayoutStatus.PREFERRED -> Unit
        }

        val hasCompilerOutsidePeerDependencies = input.dependencies.containsKey(TYPESPEC_COMPILER_PACKAGE) ||
            input.devDependencies.containsKey(TYPESPEC_COMPILER_PACKAGE)
        if (hasCompilerOutsidePeerDependencies &&
            !input.peerDependencies.containsKey(TYPESPEC_COMPILER_PACKAGE)
        ) {
            findings += TypeSpecPackageJsonRule.TPKG004
        }
    }

    return findings
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
