package com.example.typespec

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class TypeSpecOpenApi3PackageResolverTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun isResolvableWhenCliScriptExistsUnderProjectNodeModules() {
        val projectRoot = tempDir.resolve("project")
        val compilerPackage = tempDir.resolve("compiler")
        val openApiPackage = projectRoot.resolve("node_modules").resolve(TYPESPEC_OPENAPI3_PACKAGE_NAME)
        Files.createDirectories(openApiPackage.resolve("cmd"))
        Files.writeString(openApiPackage.resolve(TYPESPEC_OPENAPI3_CLI_SCRIPT), "// stub")

        assertTrue(TypeSpecOpenApi3PackageResolver.isResolvable(projectRoot, compilerPackage))
    }

    @Test
    fun isResolvableWhenCliScriptExistsNextToCompilerPackage() {
        val projectRoot = tempDir.resolve("empty-project")
        Files.createDirectories(projectRoot)
        val compilerPackage = tempDir.resolve("node_modules").resolve(TYPESPEC_COMPILER_PACKAGE_NAME)
        val openApiPackage = compilerPackage.parent.resolve(TYPESPEC_OPENAPI3_PACKAGE_NAME)
        Files.createDirectories(openApiPackage.resolve("cmd"))
        Files.writeString(openApiPackage.resolve(TYPESPEC_OPENAPI3_CLI_SCRIPT), "// stub")

        assertTrue(TypeSpecOpenApi3PackageResolver.isResolvable(projectRoot, compilerPackage))
    }

    @Test
    fun vfsEventAffectsOpenApi3PackageMatchesProjectNodeModules() {
        val projectRoot = "C:/project"
        val openApiRoot = "$projectRoot/node_modules/$TYPESPEC_OPENAPI3_PACKAGE_NAME"

        assertTrue(vfsEventAffectsOpenApi3Package(openApiRoot, projectRoot))
        assertTrue(vfsEventAffectsOpenApi3Package("$openApiRoot/cmd/tsp-openapi3.js", projectRoot))
        assertFalse(vfsEventAffectsOpenApi3Package("C:/project/src/main.tsp", projectRoot))
        assertFalse(vfsEventAffectsOpenApi3Package("C:/other/node_modules/$TYPESPEC_OPENAPI3_PACKAGE_NAME", projectRoot))
    }
}
