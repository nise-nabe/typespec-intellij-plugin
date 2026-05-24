package com.example.typespec

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TypeSpecPackagePathMatchingTest {
    @Test
    fun vfsPathIsUnderPackageRootMatchesPackageDirectoryAndChildren() {
        val packageRoot = "C:/project/node_modules/@typespec/compiler"

        assertTrue(vfsPathIsUnderPackageRoot(packageRoot, packageRoot))
        assertTrue(vfsPathIsUnderPackageRoot("$packageRoot/cmd/tsp-server.js", packageRoot))
        assertFalse(vfsPathIsUnderPackageRoot("C:/project/node_modules/other", packageRoot))
    }

    @Test
    fun normalizePackageRootReturnsNullForBlankPath() {
        assertNull(normalizePackageRoot(""))
        assertNull(normalizePackageRoot("   "))
    }
}
