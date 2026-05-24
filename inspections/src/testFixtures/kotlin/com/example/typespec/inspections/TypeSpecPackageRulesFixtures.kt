package com.example.typespec.inspections

/**
 * Reusable [TypeSpecPackageRulesInput] presets for package.json rule evaluation tests.
 */
object TypeSpecPackageRulesFixtures {
    internal fun typespecDependenciesOnly(): TypeSpecPackageRulesInput =
        buildRulesInput(
            typespecExport = null,
            main = null,
            tspMain = null,
            dependencies = mapOf(TYPESPEC_COMPILER_PACKAGE to TypeSpecPackageJsonFixtures.COMPILER_VERSION),
        )

    internal fun exportsTypeSpecMissingMetadata(): TypeSpecPackageRulesInput =
        buildRulesInput(
            typespecExport = TypeSpecPackageJsonFixtures.TSP_MAIN,
            main = null,
            tspMain = null,
        )

    internal fun peerDependenciesMissingExport(): TypeSpecPackageRulesInput =
        buildRulesInput(
            typespecExport = null,
            main = null,
            tspMain = null,
            peerDependencies = mapOf("@typespec/http" to TypeSpecPackageJsonFixtures.HTTP_PEER_VERSION),
        )

    internal fun tspMainFallback(): TypeSpecPackageRulesInput =
        buildRulesInput(
            typespecExport = null,
            main = null,
            tspMain = TypeSpecPackageJsonFixtures.TSP_MAIN,
        )

    internal fun mainWithTypespecDependency(): TypeSpecPackageRulesInput =
        buildRulesInput(
            typespecExport = null,
            main = TypeSpecPackageJsonFixtures.CUSTOM_JS_EXPORT,
            tspMain = null,
            peerDependencies = mapOf("@typespec/http" to TypeSpecPackageJsonFixtures.HTTP_PEER_VERSION),
        )

    internal fun devDependencyCompiler(): TypeSpecPackageRulesInput =
        buildRulesInput(
            typespecExport = TypeSpecPackageJsonFixtures.TSP_MAIN,
            main = null,
            tspMain = null,
            devDependencies = mapOf(TYPESPEC_COMPILER_PACKAGE to TypeSpecPackageJsonFixtures.COMPILER_VERSION),
        )

    internal fun existingPeerDependencySuppressesCompilerWarning(): TypeSpecPackageRulesInput =
        buildRulesInput(
            typespecExport = TypeSpecPackageJsonFixtures.TSP_MAIN,
            main = null,
            tspMain = null,
            dependencies = mapOf(TYPESPEC_COMPILER_PACKAGE to TypeSpecPackageJsonFixtures.COMPILER_VERSION),
            peerDependencies = mapOf(TYPESPEC_COMPILER_PACKAGE to "~2.0.0"),
        )

    internal fun preferredLayout(): TypeSpecPackageRulesInput =
        buildRulesInput(
            type = RECOMMENDED_TYPE_MODULE,
            main = RECOMMENDED_MAIN,
            tspMain = null,
            typespecExport = RECOMMENDED_TYPESPEC_EXPORT,
        )
}
