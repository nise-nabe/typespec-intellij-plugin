package com.example.typespec

import com.intellij.lang.typescript.lsp.LspServerPackageDescriptor
import com.intellij.lang.typescript.lsp.PackageVersion

@Suppress("UnstableApiUsage")
object TypeSpecPackageDescriptors {
    val compilerCli: LspServerPackageDescriptor = object : LspServerPackageDescriptor(
        TYPESPEC_COMPILER_PACKAGE_NAME,
        PackageVersion.downloadable("1.10.0"),
        "/$TYPESPEC_COMPILER_CLI_SCRIPT",
    ) {
        override val registryVersion: String
            get() = ""
    }

    val lspServer: LspServerPackageDescriptor = object : LspServerPackageDescriptor(
        TYPESPEC_COMPILER_PACKAGE_NAME,
        PackageVersion.downloadable("1.10.0"),
        "/$TYPESPEC_LSP_SERVER_SCRIPT",
    ) {
        override val registryVersion: String
            get() = ""
    }
}
