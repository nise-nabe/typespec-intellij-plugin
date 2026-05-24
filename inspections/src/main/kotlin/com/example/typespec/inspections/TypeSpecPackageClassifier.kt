package com.example.typespec.inspections

/**
 * Decides whether a package.json likely describes a TypeSpec extension library.
 *
 * Classification policy:
 * 1. Explicit TypeSpec entry points (exports["."].typespec or tspMain) always qualify.
 * 2. TypeSpec-scoped peerDependencies qualify without requiring main.
 * 3. Otherwise, main must be present and at least one TypeSpec-scoped dependency must exist.
 */
internal object TypeSpecPackageClassifier {
    fun isLikelyTypeSpecExtensionPackage(
        typespecExport: String?,
        tspMain: String?,
        main: String?,
        dependencies: Map<String, String>,
        devDependencies: Map<String, String>,
        peerDependencies: Map<String, String>,
    ): Boolean {
        if (hasExplicitTypeSpecEntryPoint(typespecExport, tspMain)) {
            return true
        }
        if (hasTypespecScopedPeerDependencies(peerDependencies)) {
            return true
        }
        if (main.isNullOrBlank()) {
            return false
        }
        return hasTypespecScopedDependency(dependencies, devDependencies, peerDependencies)
    }

    private fun hasExplicitTypeSpecEntryPoint(
        typespecExport: String?,
        tspMain: String?,
    ): Boolean = !typespecExport.isNullOrBlank() || !tspMain.isNullOrBlank()

    private fun hasTypespecScopedPeerDependencies(peerDependencies: Map<String, String>): Boolean =
        peerDependencies.keys.any(::isTypespecScopedPackage)

    private fun hasTypespecScopedDependency(
        dependencies: Map<String, String>,
        devDependencies: Map<String, String>,
        peerDependencies: Map<String, String>,
    ): Boolean =
        (dependencies.keys + devDependencies.keys + peerDependencies.keys).any(::isTypespecScopedPackage)
}
