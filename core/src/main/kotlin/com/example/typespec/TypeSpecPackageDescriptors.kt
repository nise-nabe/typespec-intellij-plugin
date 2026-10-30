package com.example.typespec

import com.intellij.lang.typescript.lsp.LspServerPackageDescriptor
import com.intellij.lang.typescript.lsp.PackageVersion

@Suppress("UnstableApiUsage")
object TypeSpecPackageDescriptors {
    /**
     * `@typespec/compiler` version the IDE downloads when a project has no local install.
     * Shared by the CLI (`tsp`) and the language server (`tsp-server`).
     */
    const val DOWNLOADABLE_COMPILER_VERSION = "1.17.0"

    val compilerCli: LspServerPackageDescriptor = object : LspServerPackageDescriptor(
        TYPESPEC_COMPILER_PACKAGE_NAME,
        PackageVersion.downloadable(DOWNLOADABLE_COMPILER_VERSION),
        "/$TYPESPEC_COMPILER_CLI_SCRIPT",
    ) {
        override val registryVersion: String
            get() = ""
    }

    val lspServer: LspServerPackageDescriptor = object : LspServerPackageDescriptor(
        TYPESPEC_COMPILER_PACKAGE_NAME,
        PackageVersion.downloadable(DOWNLOADABLE_COMPILER_VERSION),
        "/$TYPESPEC_LSP_SERVER_SCRIPT",
    ) {
        override val registryVersion: String
            get() = ""
    }
}
