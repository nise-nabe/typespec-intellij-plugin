package com.example.typespec.inspections

import com.intellij.json.psi.JsonFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager

internal fun entrypointHintsForConfigFile(file: PsiFile): TypeSpecTspConfigProjectHints {
    val directory = file.virtualFile?.parent ?: return TypeSpecTspConfigProjectHints()
    val mainTsp = directory.findChild("main.tsp")
    val mainTspExists = mainTsp != null && mainTsp.isValid && !mainTsp.isDirectory
    val packageJson = directory.findChild("package.json")
        ?: return TypeSpecTspConfigProjectHints(mainTspExists = mainTspExists)
    val jsonFile = PsiManager.getInstance(file.project).findFile(packageJson) as? JsonFile
        ?: return TypeSpecTspConfigProjectHints(mainTspExists = mainTspExists)
    val input = TypeSpecPackageMetadata.fromJsonFile(jsonFile)?.input
        ?: return TypeSpecTspConfigProjectHints(mainTspExists = mainTspExists)
    return TypeSpecTspConfigProjectHints(
        typespecExport = input.typespecExport,
        tspMain = input.tspMain,
        mainTspExists = mainTspExists,
    )
}
