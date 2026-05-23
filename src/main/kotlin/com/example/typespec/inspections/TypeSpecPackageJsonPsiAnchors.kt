package com.example.typespec.inspections

import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty

internal data class TypeSpecPackageJsonPsiAnchors(
    val rootObject: JsonObject,
    val typeProperty: JsonProperty?,
    val mainProperty: JsonProperty?,
    val tspMainProperty: JsonProperty?,
    val exportsProperty: JsonProperty?,
    val exportsDot: ExportsDotState,
    val dependenciesProperty: JsonProperty?,
    val devDependenciesProperty: JsonProperty?,
    val peerDependenciesProperty: JsonProperty?,
    val compilerDependencyProperty: JsonProperty?,
    val devCompilerDependencyProperty: JsonProperty?,
)
