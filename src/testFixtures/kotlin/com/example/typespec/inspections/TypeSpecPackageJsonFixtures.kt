package com.example.typespec.inspections

/**
 * Reusable package.json payloads for inspection and quick-fix tests.
 */
object TypeSpecPackageJsonFixtures {
    const val TSP_MAIN = RECOMMENDED_TYPESPEC_EXPORT
    const val CUSTOM_JS_EXPORT = "./dist/custom.js"
    const val COMPILER_VERSION = "~1.0.0"
    const val COMPILER_VERSION_CARET = "^2.0.0"
    const val HTTP_PEER_VERSION = "~1.0.0"

    fun tspMainOnly(): String =
        """
        {
          "tspMain": "$TSP_MAIN"
        }
        """.trimIndent()

    fun peerDependenciesOnly(): String =
        """
        {
          "peerDependencies": {
            "@typespec/http": "$HTTP_PEER_VERSION"
          }
        }
        """.trimIndent()

    fun dotStringExportWithTspMain(defaultExport: String = CUSTOM_JS_EXPORT): String =
        """
        {
          "exports": {
            ".": "$defaultExport"
          },
          "tspMain": "$TSP_MAIN"
        }
        """.trimIndent()

    fun dotObjectExportWithTspMain(defaultExport: String = CUSTOM_JS_EXPORT): String =
        """
        {
          "exports": {
            ".": {
              "default": "$defaultExport"
            }
          },
          "tspMain": "$TSP_MAIN"
        }
        """.trimIndent()

    fun missingDotEntryWithTspMain(): String =
        """
        {
          "exports": {
            "./other": "./other.js"
          },
          "tspMain": "$TSP_MAIN"
        }
        """.trimIndent()

    fun invalidExportsWithTspMain(): String =
        """
        {
          "exports": "./invalid",
          "tspMain": "$TSP_MAIN"
        }
        """.trimIndent()

    fun compilerInDependenciesWithTspMain(version: String = COMPILER_VERSION): String =
        """
        {
          "tspMain": "$TSP_MAIN",
          "dependencies": {
            "$TYPESPEC_COMPILER_PACKAGE": "$version"
          }
        }
        """.trimIndent()

    fun compilerInDevDependenciesWithPeerBlock(
        compilerVersion: String = COMPILER_VERSION_CARET,
        httpPeerVersion: String = HTTP_PEER_VERSION,
    ): String =
        """
        {
          "tspMain": "$TSP_MAIN",
          "devDependencies": {
            "$TYPESPEC_COMPILER_PACKAGE": "$compilerVersion"
          },
          "peerDependencies": {
            "@typespec/http": "$httpPeerVersion"
          }
        }
        """.trimIndent()

    fun compilerInDevDependenciesWithInvalidPeerBlock(version: String = COMPILER_VERSION): String =
        """
        {
          "tspMain": "$TSP_MAIN",
          "devDependencies": {
            "$TYPESPEC_COMPILER_PACKAGE": "$version"
          },
          "peerDependencies": "invalid"
        }
        """.trimIndent()
}
