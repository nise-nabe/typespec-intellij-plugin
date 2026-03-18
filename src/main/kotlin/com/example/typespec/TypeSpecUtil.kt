package com.example.typespec

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem

object TypeSpecUtil {
    fun findTspExecutable(project: Project): String {
        val projectRoot = project.basePath?.let { LocalFileSystem.getInstance().findFileByPath(it) }
        val binDir = projectRoot?.findChild("node_modules")?.findChild(".bin")
        if (binDir != null) {
            val tsp = if (SystemInfo.isWindows) {
                binDir.findChild("tsp-server.cmd") ?: binDir.findChild("tsp.cmd") ?: binDir.findChild("tsp-server") ?: binDir.findChild("tsp")
            } else {
                binDir.findChild("tsp-server") ?: binDir.findChild("tsp") ?: binDir.findChild("tsp-server.cmd") ?: binDir.findChild("tsp.cmd")
            }
            if (tsp != null) return FileUtil.toSystemDependentName(tsp.path)
        }
        return if (SystemInfo.isWindows) "tsp.cmd" else "tsp"
    }

    fun findTspCliJs(project: Project): String? {
        val projectRoot = project.basePath?.let { LocalFileSystem.getInstance().findFileByPath(it) }
        val nodeModules = projectRoot?.findChild("node_modules")
        if (nodeModules != null) {
            val compilerDir = nodeModules.findChild("@typespec")?.findChild("compiler")
            if (compilerDir != null) {
                val cliJs = compilerDir.findChild("dist")?.findChild("src")?.findChild("cli")?.findChild("cli.js")
                if (cliJs != null) return FileUtil.toSystemDependentName(cliJs.path)
            }
        }
        return null
    }
}
