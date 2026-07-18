package com.example.typespec.highlighting

import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType

/**
 * Highlighting lexer for TypeSpec. Tracks a small integer state so incremental
 * re-lex can resume inside block comments and (triple-quoted) strings, including
 * when a delimiter or escape is split across a buffer boundary.
 */
internal class TypeSpecLexer : LexerBase() {
    private var buffer: CharSequence = ""
    private var endOffset: Int = 0
    private var tokenStart: Int = 0
    private var tokenEnd: Int = 0
    private var currentType: IElementType? = null
    private var state: Int = STATE_DEFAULT

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.endOffset = endOffset
        tokenStart = startOffset
        tokenEnd = startOffset
        state = initialState
        advance()
    }

    override fun getState(): Int = state

    override fun getTokenType(): IElementType? = currentType

    override fun getTokenStart(): Int = tokenStart

    override fun getTokenEnd(): Int = tokenEnd

    override fun advance() {
        tokenStart = tokenEnd
        if (tokenStart >= endOffset) {
            currentType = null
            return
        }

        when (state) {
            STATE_BLOCK_COMMENT -> emit(TypeSpecTokenTypes.BLOCK_COMMENT, scanBlockComment(tokenStart, afterStar = false))
            STATE_BLOCK_COMMENT_AFTER_STAR -> emit(TypeSpecTokenTypes.BLOCK_COMMENT, scanBlockComment(tokenStart, afterStar = true))
            STATE_STRING -> emit(TypeSpecTokenTypes.STRING, scanString(tokenStart, afterEscape = false))
            STATE_STRING_AFTER_ESCAPE -> emit(TypeSpecTokenTypes.STRING, scanString(tokenStart, afterEscape = true))
            STATE_TRIPLE_STRING -> emit(TypeSpecTokenTypes.STRING, scanTripleQuotedString(tokenStart, pendingQuotes = 0))
            STATE_TRIPLE_STRING_QUOTE1 -> emit(TypeSpecTokenTypes.STRING, scanTripleQuotedString(tokenStart, pendingQuotes = 1))
            STATE_TRIPLE_STRING_QUOTE2 -> emit(TypeSpecTokenTypes.STRING, scanTripleQuotedString(tokenStart, pendingQuotes = 2))
            STATE_DEFAULT -> advanceDefault()
            else -> {
                // Unknown resume state: recover rather than mis-color forever.
                state = STATE_DEFAULT
                advanceDefault()
            }
        }
    }

    override fun getBufferSequence(): CharSequence = buffer

    override fun getBufferEnd(): Int = endOffset

    private fun advanceDefault() {
        val current = buffer[tokenStart]
        when {
            current == '/' && tokenStart + 1 < endOffset && buffer[tokenStart + 1] == '/' -> {
                emit(TypeSpecTokenTypes.LINE_COMMENT, Scan(findLineEnd(tokenStart + 2), STATE_DEFAULT))
            }
            current == '/' && tokenStart + 1 < endOffset && buffer[tokenStart + 1] == '*' -> {
                emit(TypeSpecTokenTypes.BLOCK_COMMENT, scanBlockComment(tokenStart + 2, afterStar = false))
            }
            current == '"' && isTripleQuoteAt(tokenStart) -> {
                emit(TypeSpecTokenTypes.STRING, scanTripleQuotedString(tokenStart + 3, pendingQuotes = 0))
            }
            current == '"' -> {
                emit(TypeSpecTokenTypes.STRING, scanString(tokenStart + 1, afterEscape = false))
            }
            current.isWhitespace() -> {
                var end = tokenStart + 1
                while (end < endOffset && buffer[end].isWhitespace()) {
                    end++
                }
                emit(TypeSpecTokenTypes.WHITESPACE, Scan(end, STATE_DEFAULT))
            }
            current == '@' -> {
                // Highlight `@name` / `@@name` as one decorator token for editor UX.
                var end = tokenStart + 1
                if (end < endOffset && buffer[end] == '@') {
                    end++
                }
                while (end < endOffset && isIdentifierPart(buffer[end])) {
                    end++
                }
                emit(TypeSpecTokenTypes.DECORATOR, Scan(end, STATE_DEFAULT))
            }
            current.isLetter() || current == '_' -> {
                var end = tokenStart + 1
                while (end < endOffset && isIdentifierPart(buffer[end])) {
                    end++
                }
                val word = buffer.subSequence(tokenStart, end).toString()
                val type = if (word in TypeSpecKeywords.KEYWORDS) {
                    TypeSpecTokenTypes.KEYWORD
                } else {
                    TypeSpecTokenTypes.IDENTIFIER
                }
                emit(type, Scan(end, STATE_DEFAULT))
            }
            current.isDigit() -> {
                var end = tokenStart + 1
                while (end < endOffset && (buffer[end].isDigit() || buffer[end] == '.')) {
                    end++
                }
                emit(TypeSpecTokenTypes.NUMBER, Scan(end, STATE_DEFAULT))
            }
            else -> emit(TypeSpecTokenTypes.OPERATION_SIGN, Scan(tokenStart + 1, STATE_DEFAULT))
        }
    }

    private fun emit(type: IElementType, scan: Scan) {
        tokenEnd = scan.end
        currentType = type
        state = scan.nextState
    }

    private fun isIdentifierPart(ch: Char): Boolean =
        ch.isLetterOrDigit() || ch == '_'

    private fun isTripleQuoteAt(offset: Int): Boolean =
        offset + 2 < endOffset &&
            buffer[offset] == '"' &&
            buffer[offset + 1] == '"' &&
            buffer[offset + 2] == '"'

    private fun findLineEnd(start: Int): Int {
        var index = start
        while (index < endOffset && buffer[index] != '\n') {
            index++
        }
        return index
    }

    private fun scanBlockComment(start: Int, afterStar: Boolean): Scan {
        var index = start
        var sawStar = afterStar
        while (index < endOffset) {
            val ch = buffer[index]
            when {
                sawStar && ch == '/' -> return Scan(index + 1, STATE_DEFAULT)
                ch == '*' -> {
                    sawStar = true
                    index++
                }
                else -> {
                    sawStar = false
                    index++
                }
            }
        }
        return Scan(endOffset, if (sawStar) STATE_BLOCK_COMMENT_AFTER_STAR else STATE_BLOCK_COMMENT)
    }

    private fun scanString(start: Int, afterEscape: Boolean): Scan {
        var index = start
        var escape = afterEscape
        while (index < endOffset) {
            val ch = buffer[index]
            when {
                escape -> {
                    escape = false
                    index++
                }
                ch == '\\' -> {
                    escape = true
                    index++
                }
                ch == '"' -> return Scan(index + 1, STATE_DEFAULT)
                else -> index++
            }
        }
        return Scan(endOffset, if (escape) STATE_STRING_AFTER_ESCAPE else STATE_STRING)
    }

    private fun scanTripleQuotedString(start: Int, pendingQuotes: Int): Scan {
        var index = start
        var quotes = pendingQuotes
        while (index < endOffset) {
            if (buffer[index] == '"') {
                quotes++
                index++
                if (quotes == 3) {
                    return Scan(index, STATE_DEFAULT)
                }
            } else {
                quotes = 0
                index++
            }
        }
        return Scan(
            endOffset,
            when (quotes) {
                1 -> STATE_TRIPLE_STRING_QUOTE1
                2 -> STATE_TRIPLE_STRING_QUOTE2
                else -> STATE_TRIPLE_STRING
            },
        )
    }

    private data class Scan(val end: Int, val nextState: Int)

    companion object {
        const val STATE_DEFAULT = 0
        const val STATE_BLOCK_COMMENT = 1
        const val STATE_BLOCK_COMMENT_AFTER_STAR = 2
        const val STATE_STRING = 3
        const val STATE_STRING_AFTER_ESCAPE = 4
        const val STATE_TRIPLE_STRING = 5
        const val STATE_TRIPLE_STRING_QUOTE1 = 6
        const val STATE_TRIPLE_STRING_QUOTE2 = 7
    }
}
