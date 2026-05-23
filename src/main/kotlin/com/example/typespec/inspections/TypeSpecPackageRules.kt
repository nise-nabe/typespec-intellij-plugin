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
    if (!input.isLikelyTypeSpecExtensionPackage) {
        return emptyList()
    }
    return TypeSpecPackageJsonRule.entries.filter { it.isViolated(input) }
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
        isLikelyTypeSpecExtensionPackage = TypeSpecPackageClassifier.isLikelyTypeSpecExtensionPackage(
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

private fun resolveRecommendedLayoutStatus(
    typespecExport: String?,
    tspMain: String?,
    main: String?,
): TypeSpecRecommendedLayoutStatus = when {
    !typespecExport.isNullOrBlank() -> TypeSpecRecommendedLayoutStatus.PREFERRED
    !tspMain.isNullOrBlank() || !main.isNullOrBlank() -> TypeSpecRecommendedLayoutStatus.VALID_FALLBACK
    else -> TypeSpecRecommendedLayoutStatus.MISSING
}
