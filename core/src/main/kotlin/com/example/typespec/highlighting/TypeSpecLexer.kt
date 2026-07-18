package com.example.typespec.highlighting

import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType

/**
 * Highlighting lexer for TypeSpec. Tracks a small integer state so incremental
 * re-lex can resume inside block comments and (triple-quoted) strings, including
 * when a delimiter or escape is split across a buffer boundary.
 *
 * Double-quoted (non-triple) strings end at a newline, matching the compiler.
 * `${` in a string opens a brace-tracked interpolation so nested quotes inside
 * the expression highlight correctly; the string resumes after the matching `}`.
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

        when (val current = state) {
            STATE_BLOCK_COMMENT -> emit(TypeSpecTokenTypes.BLOCK_COMMENT, scanBlockComment(tokenStart, afterStar = false))
            STATE_BLOCK_COMMENT_AFTER_STAR -> emit(TypeSpecTokenTypes.BLOCK_COMMENT, scanBlockComment(tokenStart, afterStar = true))
            STATE_STRING -> emit(TypeSpecTokenTypes.STRING, scanString(tokenStart, afterEscape = false))
            STATE_STRING_AFTER_ESCAPE -> emit(TypeSpecTokenTypes.STRING, scanString(tokenStart, afterEscape = true))
            STATE_STRING_AFTER_DOLLAR -> advanceAfterDollar(resumeStringState = STATE_STRING)
            STATE_TRIPLE_STRING -> emit(TypeSpecTokenTypes.STRING, scanTripleQuotedString(tokenStart, pendingQuotes = 0))
            STATE_TRIPLE_STRING_QUOTE1 -> emit(TypeSpecTokenTypes.STRING, scanTripleQuotedString(tokenStart, pendingQuotes = 1))
            STATE_TRIPLE_STRING_QUOTE2 -> emit(TypeSpecTokenTypes.STRING, scanTripleQuotedString(tokenStart, pendingQuotes = 2))
            STATE_TRIPLE_STRING_AFTER_DOLLAR -> advanceAfterDollar(resumeStringState = STATE_TRIPLE_STRING)
            STATE_DEFAULT -> advanceDefault()
            else -> {
                val templateDepth = templateDepthOrZero(current)
                if (templateDepth > 0) {
                    advanceTemplate(templateDepth, resumeStringState(current))
                } else {
                    // Unknown resume state: recover rather than mis-color forever.
                    state = STATE_DEFAULT
                    advanceDefault()
                }
            }
        }
    }

    override fun getBufferSequence(): CharSequence = buffer

    override fun getBufferEnd(): Int = endOffset

    private fun advanceDefault(continueState: Int = STATE_DEFAULT) {
        val current = buffer[tokenStart]
        when {
            current == '/' && tokenStart + 1 < endOffset && buffer[tokenStart + 1] == '/' -> {
                emit(TypeSpecTokenTypes.LINE_COMMENT, Scan(findLineEnd(tokenStart + 2), continueState))
            }
            current == '/' && tokenStart + 1 < endOffset && buffer[tokenStart + 1] == '*' -> {
                emit(
                    TypeSpecTokenTypes.BLOCK_COMMENT,
                    scanBlockComment(tokenStart + 2, afterStar = false, continueState = continueState),
                )
            }
            current == '"' && isTripleQuoteAt(tokenStart) -> {
                emit(
                    TypeSpecTokenTypes.STRING,
                    scanTripleQuotedString(tokenStart + 3, pendingQuotes = 0, continueState = continueState),
                )
            }
            current == '"' -> {
                emit(
                    TypeSpecTokenTypes.STRING,
                    scanString(tokenStart + 1, afterEscape = false, continueState = continueState),
                )
            }
            current.isWhitespace() -> {
                var end = tokenStart + 1
                while (end < endOffset && buffer[end].isWhitespace()) {
                    end++
                }
                emit(TypeSpecTokenTypes.WHITESPACE, Scan(end, continueState))
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
                emit(TypeSpecTokenTypes.DECORATOR, Scan(end, continueState))
            }
            current == '$' || current.isLetter() || current == '_' -> {
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
                emit(type, Scan(end, continueState))
            }
            current.isDigit() -> {
                var end = tokenStart + 1
                while (end < endOffset && (buffer[end].isDigit() || buffer[end] == '.')) {
                    end++
                }
                emit(TypeSpecTokenTypes.NUMBER, Scan(end, continueState))
            }
            else -> emit(TypeSpecTokenTypes.OPERATION_SIGN, Scan(tokenStart + 1, continueState))
        }
    }

    private fun advanceAfterDollar(resumeStringState: Int) {
        if (buffer[tokenStart] == '{') {
            emit(
                TypeSpecTokenTypes.OPERATION_SIGN,
                Scan(tokenStart + 1, templateState(1, resumeStringState)),
            )
        } else if (resumeStringState == STATE_TRIPLE_STRING) {
            emit(TypeSpecTokenTypes.STRING, scanTripleQuotedString(tokenStart, pendingQuotes = 0))
        } else {
            emit(TypeSpecTokenTypes.STRING, scanString(tokenStart, afterEscape = false))
        }
    }

    private fun advanceTemplate(depth: Int, resumeStringState: Int) {
        val current = buffer[tokenStart]
        when (current) {
            '{' -> emit(
                TypeSpecTokenTypes.OPERATION_SIGN,
                Scan(tokenStart + 1, templateState(depth + 1, resumeStringState)),
            )
            '}' -> {
                val next = if (depth <= 1) {
                    resumeStringState
                } else {
                    templateState(depth - 1, resumeStringState)
                }
                emit(TypeSpecTokenTypes.OPERATION_SIGN, Scan(tokenStart + 1, next))
            }
            else -> advanceDefault(continueState = templateState(depth, resumeStringState))
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

    private fun scanBlockComment(start: Int, afterStar: Boolean, continueState: Int = STATE_DEFAULT): Scan {
        var index = start
        var sawStar = afterStar
        while (index < endOffset) {
            val ch = buffer[index]
            when {
                sawStar && ch == '/' -> return Scan(index + 1, continueState)
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

    private fun scanString(
        start: Int,
        afterEscape: Boolean,
        continueState: Int = STATE_DEFAULT,
    ): Scan {
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
                ch == '$' -> {
                    if (index + 1 < endOffset && buffer[index + 1] == '{') {
                        // Include `${` in the string span (template head), then lex the expression.
                        return Scan(index + 2, templateState(1, STATE_STRING))
                    }
                    if (index + 1 >= endOffset) {
                        return Scan(endOffset, STATE_STRING_AFTER_DOLLAR)
                    }
                    index++
                }
                ch == '"' -> return Scan(index + 1, continueState)
                ch == '\n' || ch == '\r' -> return Scan(index, continueState)
                else -> index++
            }
        }
        return Scan(endOffset, if (escape) STATE_STRING_AFTER_ESCAPE else STATE_STRING)
    }

    private fun scanTripleQuotedString(
        start: Int,
        pendingQuotes: Int,
        continueState: Int = STATE_DEFAULT,
    ): Scan {
        var index = start
        var quotes = pendingQuotes
        while (index < endOffset) {
            val ch = buffer[index]
            when {
                ch == '$' -> {
                    quotes = 0
                    if (index + 1 < endOffset && buffer[index + 1] == '{') {
                        return Scan(index + 2, templateState(1, STATE_TRIPLE_STRING))
                    }
                    if (index + 1 >= endOffset) {
                        return Scan(endOffset, STATE_TRIPLE_STRING_AFTER_DOLLAR)
                    }
                    index++
                }
                ch == '"' -> {
                    quotes++
                    index++
                    if (quotes == 3) {
                        return Scan(index, continueState)
                    }
                }
                else -> {
                    quotes = 0
                    index++
                }
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
        const val STATE_STRING_AFTER_DOLLAR = 8
        const val STATE_TRIPLE_STRING_AFTER_DOLLAR = 9

        /** Template interpolation states: 10–17 resume double-quoted, 18–25 resume triple-quoted. */
        private const val TEMPLATE_STRING_BASE = 10
        private const val TEMPLATE_TRIPLE_BASE = 18
        private const val TEMPLATE_MAX_DEPTH = 8

        fun templateState(depth: Int, resumeStringState: Int): Int {
            val clamped = depth.coerceIn(1, TEMPLATE_MAX_DEPTH)
            return when (resumeStringState) {
                STATE_TRIPLE_STRING, STATE_TRIPLE_STRING_QUOTE1, STATE_TRIPLE_STRING_QUOTE2,
                STATE_TRIPLE_STRING_AFTER_DOLLAR,
                -> TEMPLATE_TRIPLE_BASE + clamped - 1
                else -> TEMPLATE_STRING_BASE + clamped - 1
            }
        }

        fun templateDepthOrZero(state: Int): Int = when (state) {
            in TEMPLATE_STRING_BASE until TEMPLATE_STRING_BASE + TEMPLATE_MAX_DEPTH ->
                state - TEMPLATE_STRING_BASE + 1
            in TEMPLATE_TRIPLE_BASE until TEMPLATE_TRIPLE_BASE + TEMPLATE_MAX_DEPTH ->
                state - TEMPLATE_TRIPLE_BASE + 1
            else -> 0
        }

        fun resumeStringState(templateState: Int): Int = when (templateState) {
            in TEMPLATE_TRIPLE_BASE until TEMPLATE_TRIPLE_BASE + TEMPLATE_MAX_DEPTH -> STATE_TRIPLE_STRING
            else -> STATE_STRING
        }
    }
}
