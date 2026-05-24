package com.example.typespec.workflow

internal data class TypeSpecCliJobSpec(
    val progressMessageKey: String,
    val titleKey: String,
    val cliUnavailableMessageKey: String = "workflow.compilerMissing",
    val failureMessageKey: String? = null,
)
