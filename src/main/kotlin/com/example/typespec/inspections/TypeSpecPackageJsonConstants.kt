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
