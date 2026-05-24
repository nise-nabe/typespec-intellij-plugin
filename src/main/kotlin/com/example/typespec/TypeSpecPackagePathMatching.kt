package com.example.typespec

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

internal fun vfsEventAffectsPackageRoot(event: VFileEvent, packageRoot: String): Boolean {
    val normalizedRoot = normalizePackageRoot(packageRoot) ?: return false
    return vfsPathIsUnderPackageRoot(event.path, normalizedRoot) ||
        (event.file?.let { vfsFileIsUnderPackageRoot(it, normalizedRoot) } == true)
}

internal fun vfsFileIsUnderPackageRoot(file: VirtualFile, normalizedPackageRoot: String): Boolean =
    vfsPathIsUnderPackageRoot(file.path, normalizedPackageRoot)

internal fun vfsPathIsUnderPackageRoot(path: String, normalizedPackageRoot: String): Boolean {
    val normalizedPath = path.replace('\\', '/').trimEnd('/')
    return normalizedPath == normalizedPackageRoot || normalizedPath.startsWith("$normalizedPackageRoot/")
}

internal fun normalizePackageRoot(packageRoot: String): String? {
    val normalized = packageRoot.replace('\\', '/').trim().trimEnd('/')
    return normalized.takeIf { it.isNotEmpty() }
}
