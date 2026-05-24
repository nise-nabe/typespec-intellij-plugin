package com.example.typespec

import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Path
import java.nio.file.Paths

fun nioPathsEqual(left: Path, right: Path): Boolean =
    left.toAbsolutePath().normalize() == right.toAbsolutePath().normalize()

fun virtualFilePathEquals(file: VirtualFile, path: Path): Boolean =
    nioPathsEqual(Paths.get(file.path), path)

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
