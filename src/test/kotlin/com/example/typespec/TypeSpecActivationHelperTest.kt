package com.example.typespec

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import com.intellij.javascript.nodejs.util.NodePackage
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class TypeSpecActivationHelperTest {
    @Test
    fun isEnvironmentSupportedReturnsFalseInUnitTestMode() {
        assertFalse(TypeSpecActivationHelper.isEnvironmentSupported(isUnitTestMode = true))
    }

    @Test
    fun isEnvironmentSupportedReturnsTrueOutsideUnitTestMode() {
        assertTrue(TypeSpecActivationHelper.isEnvironmentSupported(isUnitTestMode = false))
    }
}

class TypeSpecLspServerLoaderTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun isPackageWithServerScriptReturnsTrueWhenScriptExists() {
        val packageDirectory = tempDir.resolve("compiler")
        Files.createDirectories(packageDirectory.resolve("cmd"))
        Files.writeString(packageDirectory.resolve("cmd/tsp-server.js"), "// server")
        val nodePackage = NodePackage(packageDirectory.toString())

        assertTrue(TypeSpecLspServerLoader.isPackageWithServerScript(nodePackage))
    }

    @Test
    fun isPackageWithServerScriptReturnsFalseWhenScriptMissing() {
        val packageDirectory = tempDir.resolve("compiler")
        Files.createDirectories(packageDirectory)
        val nodePackage = NodePackage(packageDirectory.toString())

        assertFalse(TypeSpecLspServerLoader.isPackageWithServerScript(nodePackage))
    }
}

class TypeSpecLspPackageResolutionVfsListenerTest {
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

class TypeSpecLspPackageResolutionCacheLogicTest {
    @Test
    fun returnsCachedResultWithinTtl() {
        val cache = TypeSpecLspPackageResolutionCache()
        var computeCount = 0

        val first = cache.getOrCompute("default", nowMillis = 1_000L) {
            computeCount++
            false
        }
        val second = cache.getOrCompute("default", nowMillis = 20_000L) {
            computeCount++
            true
        }

        assertFalse(first)
        assertFalse(second)
        assertEquals(1, computeCount)
    }

    @Test
    fun recomputesAfterTtlExpires() {
        val cache = TypeSpecLspPackageResolutionCache()
        var computeCount = 0

        cache.getOrCompute("default", nowMillis = 0L) {
            computeCount++
            false
        }
        cache.getOrCompute("default", nowMillis = TypeSpecLspPackageResolutionCache.RESOLUTION_CACHE_TTL_MILLIS) {
            computeCount++
            true
        }

        assertEquals(2, computeCount)
    }

    @Test
    fun invalidateForcesRecomputation() {
        val cache = TypeSpecLspPackageResolutionCache()
        var computeCount = 0

        cache.getOrCompute("default", nowMillis = 1_000L) {
            computeCount++
            false
        }
        cache.invalidate()
        cache.getOrCompute("default", nowMillis = 2_000L) {
            computeCount++
            true
        }

        assertEquals(2, computeCount)
    }

    @Test
    fun recomputesWhenPackageKeyChanges() {
        val cache = TypeSpecLspPackageResolutionCache()
        var computeCount = 0

        cache.getOrCompute("default", nowMillis = 1_000L) {
            computeCount++
            false
        }
        cache.getOrCompute("custom-path", nowMillis = 2_000L) {
            computeCount++
            true
        }

        assertEquals(2, computeCount)
    }
}

class TypeSpecLspNotificationTrackerLogicTest {
    @Test
    fun shouldNotifyCompilerMissingOnlyOnceForSamePackageKey() {
        val tracker = TypeSpecLspNotificationTracker()

        assertTrue(tracker.shouldNotifyCompilerMissing("default"))
        assertFalse(tracker.shouldNotifyCompilerMissing("default"))
    }

    @Test
    fun shouldNotifyCompilerMissingAgainAfterPackageKeyChanges() {
        val tracker = TypeSpecLspNotificationTracker()

        assertTrue(tracker.shouldNotifyCompilerMissing("default"))
        assertTrue(tracker.shouldNotifyCompilerMissing("custom-path"))
    }

    @Test
    fun clearCompilerMissingNotificationAllowsSubsequentNotification() {
        val tracker = TypeSpecLspNotificationTracker()

        assertTrue(tracker.shouldNotifyCompilerMissing("default"))
        tracker.clearCompilerMissingNotification()
        assertTrue(tracker.shouldNotifyCompilerMissing("default"))
    }
}
