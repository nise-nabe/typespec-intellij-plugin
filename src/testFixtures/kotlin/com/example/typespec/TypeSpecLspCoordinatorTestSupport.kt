package com.example.typespec

import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.UIUtil
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Drains background and EDT work scheduled by [TypeSpecLspPackageResolutionCoordinator].
 *
 * Coordinator updates use `AppExecutorUtil` then `invokeLater`; platform tests must flush both
 * queues before asserting on tracker state or restart counters.
 */
object TypeSpecLspCoordinatorTestSupport {
    private const val MAX_DRAIN_ROUNDS = 32
    private const val DRAIN_TIMEOUT_SECONDS = 10L

    fun drainResolutionCoordinatorQueues() {
        repeat(MAX_DRAIN_ROUNDS) { round ->
            val backgroundQueueDrained = CountDownLatch(1)
            AppExecutorUtil.getAppExecutorService().execute { backgroundQueueDrained.countDown() }
            check(backgroundQueueDrained.await(DRAIN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                "Timed out waiting for AppExecutor queue (round ${round + 1}/$MAX_DRAIN_ROUNDS)"
            }
            UIUtil.dispatchAllInvocationEvents()
            if (!ApplicationManager.getApplication().isDispatchThread) {
                Thread.yield()
            }
        }
    }
}
