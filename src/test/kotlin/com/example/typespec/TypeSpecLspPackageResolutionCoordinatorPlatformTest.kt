package com.example.typespec

import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.platform.lsp.api.LspServerDescriptor
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.UIUtil
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class TypeSpecLspPackageResolutionCoordinatorPlatformTest : BasePlatformTestCase() {
    private lateinit var packageDirectory: Path

    override fun setUp() {
        super.setUp()
        packageDirectory = Files.createTempDirectory("typespec-coordinator")
    }

    override fun tearDown() {
        try {
            packageDirectory.toFile().deleteRecursively()
        } finally {
            super.tearDown()
        }
    }

    fun testOnConfigurationChangedClearsCompilerMissingTrackerWhenPackageIsResolvable() {
        val settings = TypeSpecServiceSettings.getInstance(project)
        val tracker = TypeSpecLspNotificationTracker.getInstance(project)
        val packageKey = packageDirectory.toString()

        Files.createDirectories(packageDirectory.resolve("cmd"))
        Files.writeString(packageDirectory.resolve("cmd/tsp-server.js"), "// server")
        settings.lspServerPackage = NodePackage(packageKey)
        assertTrue(tracker.shouldNotifyCompilerMissing(packageKey))
        assertFalse(tracker.shouldNotifyCompilerMissing(packageKey))

        TypeSpecLspPackageResolutionCoordinator.onConfigurationChanged(project)
        drainResolutionCoordinatorQueues()

        assertTrue(TypeSpecPackageResolution.isSelectedPackageResolvable(project))
        assertTrue(tracker.shouldNotifyCompilerMissing(packageKey))
    }

    fun testOnPackageRootAffectedClearsTrackerWhenPackageBecomesResolvable() {
        val settings = TypeSpecServiceSettings.getInstance(project)
        val tracker = TypeSpecLspNotificationTracker.getInstance(project)
        val packageKey = packageDirectory.toString()

        settings.lspServerPackage = NodePackage(packageKey)
        TypeSpecLspPackageResolutionCache.getInstance(project).invalidate()
        assertFalse(TypeSpecPackageResolution.isSelectedPackageResolvable(project))
        assertTrue(tracker.shouldNotifyCompilerMissing(packageKey))
        assertFalse(tracker.shouldNotifyCompilerMissing(packageKey))

        Files.createDirectories(packageDirectory.resolve("cmd"))
        Files.writeString(packageDirectory.resolve("cmd/tsp-server.js"), "// server")

        TypeSpecLspPackageResolutionCoordinator.onPackageRootAffected(project)
        drainResolutionCoordinatorQueues()

        assertTrue(TypeSpecPackageResolution.isSelectedPackageResolvable(project))
        assertTrue(tracker.shouldNotifyCompilerMissing(packageKey))
    }

    fun testOnPackageRootAffectedFromBackgroundThreadAppliesOnEdt() {
        val settings = TypeSpecServiceSettings.getInstance(project)
        val packageKey = packageDirectory.toString()

        settings.lspServerPackage = NodePackage(packageKey)
        TypeSpecLspPackageResolutionCache.getInstance(project).invalidate()
        Files.createDirectories(packageDirectory.resolve("cmd"))
        Files.writeString(packageDirectory.resolve("cmd/tsp-server.js"), "// server")

        val failure = AtomicReference<Throwable>()
        val task = AppExecutorUtil.getAppExecutorService().submit {
            try {
                TypeSpecLspPackageResolutionCoordinator.onPackageRootAffected(project)
            } catch (throwable: Throwable) {
                failure.set(throwable)
            }
        }
        task.get(10, TimeUnit.SECONDS)
        drainResolutionCoordinatorQueues()

        assertNull(failure.get())
        assertTrue(TypeSpecPackageResolution.isSelectedPackageResolvable(project))
    }

    fun testWatcherNotifiesCoordinatorWhenPackageRootChanges() {
        val settings = TypeSpecServiceSettings.getInstance(project)
        val tracker = TypeSpecLspNotificationTracker.getInstance(project)
        val packageKey = packageDirectory.toString()

        settings.lspServerPackage = NodePackage(packageKey)
        TypeSpecLspPackageResolutionCache.getInstance(project).invalidate()
        TypeSpecLspPackageRootVfsMultiplexer.getInstance().watchPackageRoot(project, packageKey)
        assertTrue(tracker.shouldNotifyCompilerMissing(packageKey))
        assertFalse(tracker.shouldNotifyCompilerMissing(packageKey))

        Files.createDirectories(packageDirectory.resolve("cmd"))
        Files.writeString(packageDirectory.resolve("cmd/tsp-server.js"), "// server")

        TypeSpecLspPackageResolutionCacheWatcher.getInstance(project).onPackageRootAffected()
        drainResolutionCoordinatorQueues()

        assertTrue(TypeSpecPackageResolution.isSelectedPackageResolvable(project))
        assertTrue(tracker.shouldNotifyCompilerMissing(packageKey))
    }

    fun testMultiplexerRoutesPackageRootEventsToWatcher() {
        val settings = TypeSpecServiceSettings.getInstance(project)
        val packageKey = packageDirectory.toString()

        settings.lspServerPackage = NodePackage(packageKey)
        TypeSpecLspPackageResolutionCache.getInstance(project).invalidate()
        TypeSpecLspPackageRootVfsMultiplexer.getInstance().watchPackageRoot(project, packageKey)

        val serverScriptPath = packageDirectory.resolve("cmd/tsp-server.js")
        Files.createDirectories(serverScriptPath.parent)
        Files.writeString(serverScriptPath, "// server")

        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(serverScriptPath)
            ?: error("Expected server script to be visible in VFS")
        val changeEvent = VFileContentChangeEvent(
            this,
            virtualFile,
            virtualFile.modificationStamp - 1,
            virtualFile.modificationStamp,
            false,
        )
        val multiplexer = TypeSpecLspPackageRootVfsMultiplexer.getInstance()
        val applier = multiplexer.prepareChangeForTest(listOf(changeEvent))
        assertNotNull(applier)
        applier!!.afterVfsChange()
        drainResolutionCoordinatorQueues()

        assertTrue(TypeSpecPackageResolution.isSelectedPackageResolvable(project))
    }

    fun testOnTypeSpecFileOpenedDoesNotStartServerWhenActivationIsDisabledInUnitTestMode() {
        val settings = TypeSpecServiceSettings.getInstance(project)
        settings.serviceMode = TypeSpecServiceMode.ENABLED
        val file = myFixture.configureByText("main.tsp", "namespace Demo {}").virtualFile
        var ensureServerStartedCalled = false
        val serverStarter = object : LspServerSupportProvider.LspServerStarter {
            override fun ensureServerStarted(descriptor: LspServerDescriptor) {
                ensureServerStartedCalled = true
            }
        }

        TypeSpecLspPackageResolutionCoordinator.onTypeSpecFileOpened(project, file, serverStarter)
        drainResolutionCoordinatorQueues()

        assertFalse(ensureServerStartedCalled)
    }

    fun testOnConfigurationChangedClearsCompilerMissingTrackerWhenServiceDisabled() {
        val settings = TypeSpecServiceSettings.getInstance(project)
        val tracker = TypeSpecLspNotificationTracker.getInstance(project)
        val packageKey = packageDirectory.toString()

        settings.serviceMode = TypeSpecServiceMode.ENABLED
        settings.lspServerPackage = NodePackage(packageKey)
        TypeSpecLspPackageResolutionCache.getInstance(project).invalidate()
        assertFalse(TypeSpecPackageResolution.isSelectedPackageResolvable(project))
        assertTrue(tracker.shouldNotifyCompilerMissing(packageKey))
        assertFalse(tracker.shouldNotifyCompilerMissing(packageKey))

        settings.serviceMode = TypeSpecServiceMode.DISABLED
        drainResolutionCoordinatorQueues()

        assertTrue(tracker.shouldNotifyCompilerMissing(packageKey))
    }

    fun testSettingsChangeUsesCoordinatorToClearTrackerWhenPackageBecomesResolvable() {
        val settings = TypeSpecServiceSettings.getInstance(project)
        val tracker = TypeSpecLspNotificationTracker.getInstance(project)
        val unresolvableDirectory = Files.createTempDirectory("typespec-unresolvable")
        try {
            settings.lspServerPackage = NodePackage(unresolvableDirectory.toString())
            val unresolvableKey = unresolvableDirectory.toString()
            assertTrue(tracker.shouldNotifyCompilerMissing(unresolvableKey))
            assertFalse(tracker.shouldNotifyCompilerMissing(unresolvableKey))

            Files.createDirectories(packageDirectory.resolve("cmd"))
            Files.writeString(packageDirectory.resolve("cmd/tsp-server.js"), "// server")
            settings.lspServerPackage = NodePackage(packageDirectory.toString())
            drainResolutionCoordinatorQueues()

            assertTrue(TypeSpecPackageResolution.isSelectedPackageResolvable(project))
            assertTrue(tracker.shouldNotifyCompilerMissing(packageDirectory.toString()))
        } finally {
            unresolvableDirectory.toFile().deleteRecursively()
        }
    }

    private fun drainResolutionCoordinatorQueues() {
        repeat(32) {
            val backgroundQueueDrained = CountDownLatch(1)
            AppExecutorUtil.getAppExecutorService().execute { backgroundQueueDrained.countDown() }
            assertTrue(backgroundQueueDrained.await(10, TimeUnit.SECONDS))
            UIUtil.dispatchAllInvocationEvents()
            if (!ApplicationManager.getApplication().isDispatchThread) {
                Thread.yield()
            }
        }
    }
}
