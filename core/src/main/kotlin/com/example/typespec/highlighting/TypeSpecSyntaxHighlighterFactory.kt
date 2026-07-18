package com.example.typespec.highlighting

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType

class TypeSpecSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter =
        TypeSpecSyntaxHighlighter()
}

internal class TypeSpecSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer() = TypeSpecLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> =
        when (tokenType) {
            TypeSpecTokenTypes.KEYWORD -> KEYWORD_KEYS
            TypeSpecTokenTypes.DECORATOR -> DECORATOR_KEYS
            TypeSpecTokenTypes.STRING -> STRING_KEYS
            TypeSpecTokenTypes.LINE_COMMENT -> LINE_COMMENT_KEYS
            TypeSpecTokenTypes.BLOCK_COMMENT -> BLOCK_COMMENT_KEYS
            TypeSpecTokenTypes.NUMBER -> NUMBER_KEYS
            TypeSpecTokenTypes.OPERATION_SIGN -> OPERATION_SIGN_KEYS
            else -> TextAttributesKey.EMPTY_ARRAY
        }

    companion object {
        private val KEYWORD_KEYS = arrayOf(DefaultLanguageHighlighterColors.KEYWORD)
        private val DECORATOR_KEYS = arrayOf(DefaultLanguageHighlighterColors.METADATA)
        private val STRING_KEYS = arrayOf(DefaultLanguageHighlighterColors.STRING)
        private val LINE_COMMENT_KEYS = arrayOf(DefaultLanguageHighlighterColors.LINE_COMMENT)
        private val BLOCK_COMMENT_KEYS = arrayOf(DefaultLanguageHighlighterColors.BLOCK_COMMENT)
        private val NUMBER_KEYS = arrayOf(DefaultLanguageHighlighterColors.NUMBER)
        private val OPERATION_SIGN_KEYS = arrayOf(DefaultLanguageHighlighterColors.OPERATION_SIGN)
    }
}
