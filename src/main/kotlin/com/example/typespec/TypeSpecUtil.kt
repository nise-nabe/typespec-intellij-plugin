package com.example.typespec

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager

object TypeSpecUtil {
    fun findTspExecutable(project: Project, file: VirtualFile? = null): String {
        val projectRoot = project.basePath?.let { LocalFileSystem.getInstance().findFileByPath(it) }
        var current: VirtualFile? = file?.parent ?: projectRoot
        
        while (current != null) {
            val binDir = current.findChild("node_modules")?.findChild(".bin")
            if (binDir != null) {
                val tsp = binDir.findChild("tsp") ?: binDir.findChild("tsp.cmd")
                if (tsp != null) return tsp.path
            }
            if (current == projectRoot) break
            current = current.parent
        }
        return "tsp"
    }

    fun isNodeJsConfigured(project: Project): Boolean {
        return NodeJsInterpreterManager.getInstance(project).interpreter != null
    }
}
