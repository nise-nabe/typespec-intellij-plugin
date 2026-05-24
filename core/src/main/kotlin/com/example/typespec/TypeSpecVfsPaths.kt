package com.example.typespec

import com.intellij.openapi.vfs.VirtualFile

fun vfsFileIsUnderPackageRoot(file: VirtualFile, normalizedPackageRoot: String): Boolean =
    vfsPathIsUnderPackageRoot(file.path, normalizedPackageRoot)

fun vfsPathIsUnderPackageRoot(path: String, normalizedPackageRoot: String): Boolean {
    val normalizedPath = path.replace('\\', '/').trimEnd('/')
    return normalizedPath == normalizedPackageRoot || normalizedPath.startsWith("$normalizedPackageRoot/")
}

fun normalizePackageRoot(packageRoot: String): String? {
    val normalized = packageRoot.replace('\\', '/').trim().trimEnd('/')
    return normalized.takeIf { it.isNotEmpty() }
}
