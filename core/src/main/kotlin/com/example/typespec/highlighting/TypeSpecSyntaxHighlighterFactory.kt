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
        val KEYWORD = TextAttributesKey.createTextAttributesKey(
            "TYPESPEC_KEYWORD",
            DefaultLanguageHighlighterColors.KEYWORD,
        )
        val DECORATOR = TextAttributesKey.createTextAttributesKey(
            "TYPESPEC_DECORATOR",
            DefaultLanguageHighlighterColors.METADATA,
        )
        val STRING = TextAttributesKey.createTextAttributesKey(
            "TYPESPEC_STRING",
            DefaultLanguageHighlighterColors.STRING,
        )
        val LINE_COMMENT = TextAttributesKey.createTextAttributesKey(
            "TYPESPEC_LINE_COMMENT",
            DefaultLanguageHighlighterColors.LINE_COMMENT,
        )
        val BLOCK_COMMENT = TextAttributesKey.createTextAttributesKey(
            "TYPESPEC_BLOCK_COMMENT",
            DefaultLanguageHighlighterColors.BLOCK_COMMENT,
        )
        val NUMBER = TextAttributesKey.createTextAttributesKey(
            "TYPESPEC_NUMBER",
            DefaultLanguageHighlighterColors.NUMBER,
        )
        val OPERATION_SIGN = TextAttributesKey.createTextAttributesKey(
            "TYPESPEC_OPERATION_SIGN",
            DefaultLanguageHighlighterColors.OPERATION_SIGN,
        )

        private val KEYWORD_KEYS = arrayOf(KEYWORD)
        private val DECORATOR_KEYS = arrayOf(DECORATOR)
        private val STRING_KEYS = arrayOf(STRING)
        private val LINE_COMMENT_KEYS = arrayOf(LINE_COMMENT)
        private val BLOCK_COMMENT_KEYS = arrayOf(BLOCK_COMMENT)
        private val NUMBER_KEYS = arrayOf(NUMBER)
        private val OPERATION_SIGN_KEYS = arrayOf(OPERATION_SIGN)
    }
}
