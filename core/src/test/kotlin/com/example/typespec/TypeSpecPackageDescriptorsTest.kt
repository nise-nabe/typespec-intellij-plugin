package com.example.typespec

import com.intellij.lang.typescript.lsp.LspServerPackageDescriptor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@Suppress("UnstableApiUsage")
class TypeSpecPackageDescriptorsTest {
    @Test
    fun cliAndLspServerShareTheDownloadableCompilerVersion() {
        assertEquals("1.17.0", TypeSpecPackageDescriptors.DOWNLOADABLE_COMPILER_VERSION)
        assertEquals("1.17.0", rawVersion(TypeSpecPackageDescriptors.compilerCli))
        assertEquals("1.17.0", rawVersion(TypeSpecPackageDescriptors.lspServer))
    }

    private fun rawVersion(descriptor: LspServerPackageDescriptor): String =
        descriptor.defaultVersion.version.rawVersion
}
