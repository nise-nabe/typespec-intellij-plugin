package com.example.typespec.inspections

internal const val TYPESPEC_COMPILER_PACKAGE = "@typespec/compiler"
internal const val RECOMMENDED_MAIN = "dist/index.js"
internal const val RECOMMENDED_TYPESPEC_EXPORT = "./lib/main.tsp"
internal const val RECOMMENDED_TYPE_MODULE = "module"

internal enum class TypeSpecRecommendedLayoutStatus {
    PREFERRED,
    VALID_FALLBACK,
    MISSING,
}

internal enum class TypeSpecFindingSeverity {
    WARNING,
    INFORMATION,
}

internal enum class TypeSpecPackageJsonFixAction {
    APPLY_RECOMMENDED_METADATA,
    MOVE_COMPILER_TO_PEER_DEPENDENCIES,
}
