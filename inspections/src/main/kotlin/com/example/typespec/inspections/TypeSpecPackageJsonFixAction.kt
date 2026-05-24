package com.example.typespec.inspections

import com.example.typespec.TypeSpecBundle
import com.intellij.json.psi.JsonElementGenerator
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
        val file = metadata.psi.rootObject.containingFile
        WriteCommandAction.runWriteCommandAction(
            project,
            localizedFamilyName(),
            null,
            Runnable {
                applyViolatedRules(
                    metadata = metadata,
                    fixAction = this,
                    generator = JsonElementGenerator(project),
                )
            },
            file,
        )
    }
}
