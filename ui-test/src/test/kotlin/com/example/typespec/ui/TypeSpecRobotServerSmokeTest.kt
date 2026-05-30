package com.example.typespec.ui

import com.intellij.remoterobot.RemoteRobot
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URI

/**
 * Smoke test for the IntelliJ UI Test Robot server.
 * Requires a running IDE started with `:plugin:runIdeForUiTests` (see docs/cloud-verification.md).
 */
class TypeSpecRobotServerSmokeTest {
    @Test
    fun robotServerIsReachable() {
        val baseUrl = System.getProperty("robot.server.url", "http://127.0.0.1:8082")
        assumeTrue("Robot server not running at $baseUrl", isRobotServerUp(baseUrl))

        val robot = RemoteRobot(baseUrl)
        assertNotNull(robot)
    }

    private fun isRobotServerUp(baseUrl: String): Boolean {
        return try {
            val connection = URI(baseUrl).toURL().openConnection() as HttpURLConnection
            connection.connectTimeout = 3_000
            connection.readTimeout = 3_000
            connection.requestMethod = "GET"
            connection.connect()
            connection.responseCode in 200..499
        } catch (_: Exception) {
            false
        }
    }
}
