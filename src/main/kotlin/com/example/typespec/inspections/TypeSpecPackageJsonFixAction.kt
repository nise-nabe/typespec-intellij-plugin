package com.example.typespec.inspections

import com.example.typespec.TypeSpecBundle
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.Nls

internal enum class TypeSpecPackageJsonFixAction(
    val messageKey: String,
) {
    APPLY_RECOMMENDED_METADATA("fix.applyRecommendedMetadata"),
    MOVE_COMPILER_TO_PEER_DEPENDENCIES("fix.moveCompilerToPeerDependencies"),
    ;

    @Nls
    fun localizedFamilyName(): String = TypeSpecBundle.message(messageKey)

    fun apply(project: Project, metadata: TypeSpecPackageMetadata) {
        WriteCommandAction.runWriteCommandAction(project) {
            when (this) {
                APPLY_RECOMMENDED_METADATA ->
                    TypeSpecPackageJsonEditor.applyRecommendedMetadata(project, metadata)
                MOVE_COMPILER_TO_PEER_DEPENDENCIES ->
                    TypeSpecPackageJsonEditor.moveCompilerToPeerDependencies(project, metadata)
            }
        }
    }
}
