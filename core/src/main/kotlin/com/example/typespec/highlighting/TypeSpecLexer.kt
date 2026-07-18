package com.example.typespec.highlighting

import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType

/**
 * Highlighting lexer for TypeSpec. Scanning uses a typed [LexState] tree so nested
 * string templates keep full fidelity inside a buffer. [getState]/[start] pack that
 * tree into an Int for IntelliJ incremental re-lex (with a finite continue capacity).
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
    private var lexState: LexState = LexState.Default

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.endOffset = endOffset
        tokenStart = startOffset
        tokenEnd = startOffset
        lexState = LexStatePacking.unpack(initialState)
        advance()
    }

    override fun getState(): Int = LexStatePacking.pack(lexState)

    override fun getTokenType(): IElementType? = currentType

    override fun getTokenStart(): Int = tokenStart

    override fun getTokenEnd(): Int = tokenEnd

    override fun advance() {
        tokenStart = tokenEnd
        if (tokenStart >= endOffset) {
            currentType = null
            return
        }

        when (val state = lexState) {
            LexState.Default -> advanceDefault(LexState.Default)
            is LexState.BlockComment -> emit(
                TypeSpecTokenTypes.BLOCK_COMMENT,
                scanBlockComment(tokenStart, afterStar = state.afterStar, continueState = state.continueState),
            )
            is LexState.InString -> {
                if (state.afterDollar) {
                    advanceAfterDollar(state)
                } else {
                    emit(
                        TypeSpecTokenTypes.STRING,
                        scanQuoted(
                            tokenStart,
                            triple = state.triple,
                            pendingQuotes = state.pendingQuotes,
                            afterEscape = state.afterEscape,
                            continueState = state.continueState,
                        ),
                    )
                }
            }
            is LexState.Template -> advanceTemplate(state)
        }
    }

    override fun getBufferSequence(): CharSequence = buffer

    override fun getBufferEnd(): Int = endOffset

    private fun advanceDefault(continueState: LexState) {
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
                    scanQuoted(
                        tokenStart + 3,
                        triple = true,
                        pendingQuotes = 0,
                        afterEscape = false,
                        continueState = continueState,
                    ),
                )
            }
            current == '"' -> {
                emit(
                    TypeSpecTokenTypes.STRING,
                    scanQuoted(
                        tokenStart + 1,
                        triple = false,
                        pendingQuotes = 0,
                        afterEscape = false,
                        continueState = continueState,
                    ),
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

    private fun advanceAfterDollar(state: LexState.InString) {
        if (buffer[tokenStart] == '{') {
            emit(
                TypeSpecTokenTypes.OPERATION_SIGN,
                Scan(
                    tokenStart + 1,
                    LexState.Template(
                        depth = 1,
                        resume = state.copy(afterDollar = false),
                    ),
                ),
            )
            return
        }
        emit(
            TypeSpecTokenTypes.STRING,
            scanQuoted(
                tokenStart,
                triple = state.triple,
                pendingQuotes = 0,
                afterEscape = false,
                continueState = state.continueState,
            ),
        )
    }

    private fun advanceTemplate(state: LexState.Template) {
        when (buffer[tokenStart]) {
            '{' -> emit(
                TypeSpecTokenTypes.OPERATION_SIGN,
                Scan(tokenStart + 1, LexState.Template(state.depth + 1, state.resume)),
            )
            '}' -> {
                val next = if (state.depth <= 1) {
                    state.resume
                } else {
                    LexState.Template(state.depth - 1, state.resume)
                }
                emit(TypeSpecTokenTypes.OPERATION_SIGN, Scan(tokenStart + 1, next))
            }
            else -> advanceDefault(continueState = state)
        }
    }

    private fun emit(type: IElementType, scan: Scan) {
        // LexerBase requires positive-length tokens. A resume state can land on a
        // terminator that must be re-lexed in the continue state (e.g. `$` split
        // across buffers, then a newline ending a double-quoted string).
        if (scan.end == tokenStart) {
            lexState = scan.nextState
            advance()
            return
        }
        tokenEnd = scan.end
        currentType = type
        lexState = scan.nextState
    }

    private fun isIdentifierPart(ch: Char): Boolean =
        ch.isLetterOrDigit() || ch == '_' || ch == '$'

    private fun isTripleQuoteAt(offset: Int): Boolean =
        offset + 2 < endOffset &&
            buffer[offset] == '"' &&
            buffer[offset + 1] == '"' &&
            buffer[offset + 2] == '"'

    private fun findLineEnd(start: Int): Int {
        var index = start
        while (index < endOffset && buffer[index] != '\n' && buffer[index] != '\r') {
            index++
        }
        return index
    }

    private fun scanBlockComment(start: Int, afterStar: Boolean, continueState: LexState): Scan {
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
        return Scan(endOffset, LexState.BlockComment(afterStar = sawStar, continueState = continueState))
    }

    private fun scanQuoted(
        start: Int,
        triple: Boolean,
        pendingQuotes: Int,
        afterEscape: Boolean,
        continueState: LexState,
    ): Scan {
        var index = start
        var quotes = pendingQuotes
        var escape = afterEscape
        while (index < endOffset) {
            val ch = buffer[index]
            when {
                escape -> {
                    escape = false
                    quotes = 0
                    index++
                }
                ch == '\\' -> {
                    escape = true
                    quotes = 0
                    index++
                }
                ch == '$' -> {
                    quotes = 0
                    val dollar = scanDollarInString(index, triple = triple, continueState = continueState)
                    if (dollar != null) return dollar
                    index++
                }
                ch == '"' -> {
                    if (!triple) return Scan(index + 1, continueState)
                    quotes++
                    index++
                    if (quotes == 3) return Scan(index, continueState)
                }
                !triple && (ch == '\n' || ch == '\r') -> return Scan(index, continueState)
                else -> {
                    quotes = 0
                    index++
                }
            }
        }
        return Scan(
            endOffset,
            LexState.InString(
                triple = triple,
                pendingQuotes = if (triple) quotes else 0,
                afterEscape = escape,
                afterDollar = false,
                continueState = continueState,
            ),
        )
    }

    /**
     * Handles `$` / `${` / `$` at buffer end inside a string. Returns null when `$`
     * is ordinary string content (caller should advance past it).
     */
    private fun scanDollarInString(index: Int, triple: Boolean, continueState: LexState): Scan? {
        if (index + 1 < endOffset && buffer[index + 1] == '{') {
            return Scan(
                index + 2,
                LexState.Template(
                    depth = 1,
                    resume = LexState.InString(triple = triple, continueState = continueState),
                ),
            )
        }
        if (index + 1 >= endOffset) {
            return Scan(
                endOffset,
                LexState.InString(triple = triple, afterDollar = true, continueState = continueState),
            )
        }
        return null
    }

    private data class Scan(val end: Int, val nextState: LexState)

    companion object {
        /** Packed Int for [LexState.Default] — used by tests and incremental resume. */
        const val STATE_DEFAULT = 0
        val STATE_BLOCK_COMMENT: Int = LexStatePacking.pack(LexState.BlockComment(afterStar = false))
        val STATE_BLOCK_COMMENT_AFTER_STAR: Int = LexStatePacking.pack(LexState.BlockComment(afterStar = true))
        val STATE_STRING: Int = LexStatePacking.pack(LexState.InString(triple = false))
        val STATE_STRING_AFTER_ESCAPE: Int =
            LexStatePacking.pack(LexState.InString(triple = false, afterEscape = true))
        val STATE_TRIPLE_STRING: Int = LexStatePacking.pack(LexState.InString(triple = true))
        val STATE_TRIPLE_STRING_QUOTE1: Int =
            LexStatePacking.pack(LexState.InString(triple = true, pendingQuotes = 1))
        val STATE_TRIPLE_STRING_QUOTE2: Int =
            LexStatePacking.pack(LexState.InString(triple = true, pendingQuotes = 2))
        val STATE_STRING_AFTER_DOLLAR: Int =
            LexStatePacking.pack(LexState.InString(triple = false, afterDollar = true))
        val STATE_TRIPLE_STRING_AFTER_DOLLAR: Int =
            LexStatePacking.pack(LexState.InString(triple = true, afterDollar = true))
        val STATE_TRIPLE_STRING_AFTER_ESCAPE: Int =
            LexStatePacking.pack(LexState.InString(triple = true, afterEscape = true))

        fun isTemplateState(state: Int): Boolean = LexStatePacking.unpack(state) is LexState.Template

        fun templateState(depth: Int, resumeStringState: Int): Int =
            LexStatePacking.pack(
                LexState.Template(depth, LexStatePacking.unpack(resumeStringState)),
            )
    }
}

/**
 * In-memory lexer state. Packed to Int only at the [TypeSpecLexer.getState] /
 * [TypeSpecLexer.start] boundary for incremental highlighting.
 */
internal sealed interface LexState {
    data object Default : LexState

    data class BlockComment(
        val afterStar: Boolean,
        val continueState: LexState = Default,
    ) : LexState

    data class InString(
        val triple: Boolean,
        val pendingQuotes: Int = 0,
        val afterEscape: Boolean = false,
        val afterDollar: Boolean = false,
        val continueState: LexState = Default,
    ) : LexState

    data class Template(
        val depth: Int,
        val resume: LexState,
    ) : LexState
}

/**
 * Lossy Int codec for [LexState]. Live scanning never goes through this path, so
 * deep nesting inside one buffer stays exact; only segment resume is capacity-limited.
 *
 * Packed layout:
 * - bits 0–3: mode (or template brace depth when [TEMPLATE_BIT] is set)
 * - bit 4: template interpolation marker
 * - bits 5–31: continue stack (compact frames)
 *
 * Each continue frame is 3 bits (LSB = next frame to apply):
 * - 0: end
 * - 1: resume double-quoted string
 * - 2: resume triple-quoted string
 * - 3–7: template with brace depth (frame - 2), depths 1–5
 *
 * 27 payload bits fit nine 3-bit frames. Deeper stacks keep the deepest frames.
 */
internal object LexStatePacking {
    private const val MODE_MASK = 0x0F
    private const val TEMPLATE_BIT = 1 shl 4
    private const val PAYLOAD_SHIFT = 5
    private const val PAYLOAD_BITS = Int.SIZE_BITS - PAYLOAD_SHIFT
    private const val PAYLOAD_MASK = (1 shl PAYLOAD_BITS) - 1
    private const val TEMPLATE_MAX_DEPTH = MODE_MASK
    private const val FRAME_BITS = 3
    private const val FRAME_MASK = (1 shl FRAME_BITS) - 1
    private const val FRAME_RESUME_STRING = 1
    private const val FRAME_RESUME_TRIPLE = 2
    private const val FRAME_TEMPLATE_BASE = 3
    private const val FRAME_TEMPLATE_MAX_DEPTH = FRAME_MASK - FRAME_TEMPLATE_BASE + 1

    private const val MODE_DEFAULT = 0
    private const val MODE_BLOCK_COMMENT = 1
    private const val MODE_BLOCK_COMMENT_AFTER_STAR = 2
    private const val MODE_STRING = 3
    private const val MODE_STRING_AFTER_ESCAPE = 4
    private const val MODE_TRIPLE_STRING = 5
    private const val MODE_TRIPLE_STRING_QUOTE1 = 6
    private const val MODE_TRIPLE_STRING_QUOTE2 = 7
    private const val MODE_STRING_AFTER_DOLLAR = 8
    private const val MODE_TRIPLE_STRING_AFTER_DOLLAR = 9
    private const val MODE_TRIPLE_STRING_AFTER_ESCAPE = 10

    fun pack(state: LexState): Int = when (state) {
        LexState.Default -> MODE_DEFAULT
        is LexState.BlockComment -> withContinue(
            if (state.afterStar) MODE_BLOCK_COMMENT_AFTER_STAR else MODE_BLOCK_COMMENT,
            state.continueState,
        )
        is LexState.InString -> withContinue(stringMode(state), state.continueState)
        is LexState.Template -> {
            val clamped = state.depth.coerceIn(1, TEMPLATE_MAX_DEPTH)
            TEMPLATE_BIT or clamped or (encodeContinue(state.resume) shl PAYLOAD_SHIFT)
        }
    }

    fun unpack(packed: Int): LexState {
        if ((packed and TEMPLATE_BIT) != 0) {
            val depth = packed and MODE_MASK
            if (depth == 0) return LexState.Default
            return LexState.Template(depth, decodeContinue(packed ushr PAYLOAD_SHIFT))
        }
        val mode = packed and MODE_MASK
        val cont = decodeContinue(packed ushr PAYLOAD_SHIFT)
        return when (mode) {
            MODE_DEFAULT -> LexState.Default
            MODE_BLOCK_COMMENT -> LexState.BlockComment(afterStar = false, continueState = cont)
            MODE_BLOCK_COMMENT_AFTER_STAR -> LexState.BlockComment(afterStar = true, continueState = cont)
            MODE_STRING -> LexState.InString(triple = false, continueState = cont)
            MODE_STRING_AFTER_ESCAPE -> LexState.InString(triple = false, afterEscape = true, continueState = cont)
            MODE_TRIPLE_STRING -> LexState.InString(triple = true, continueState = cont)
            MODE_TRIPLE_STRING_QUOTE1 -> LexState.InString(triple = true, pendingQuotes = 1, continueState = cont)
            MODE_TRIPLE_STRING_QUOTE2 -> LexState.InString(triple = true, pendingQuotes = 2, continueState = cont)
            MODE_STRING_AFTER_DOLLAR -> LexState.InString(triple = false, afterDollar = true, continueState = cont)
            MODE_TRIPLE_STRING_AFTER_DOLLAR -> LexState.InString(triple = true, afterDollar = true, continueState = cont)
            MODE_TRIPLE_STRING_AFTER_ESCAPE -> LexState.InString(triple = true, afterEscape = true, continueState = cont)
            else -> LexState.Default
        }
    }

    private fun stringMode(state: LexState.InString): Int = when {
        state.afterDollar && state.triple -> MODE_TRIPLE_STRING_AFTER_DOLLAR
        state.afterDollar -> MODE_STRING_AFTER_DOLLAR
        state.afterEscape && state.triple -> MODE_TRIPLE_STRING_AFTER_ESCAPE
        state.afterEscape -> MODE_STRING_AFTER_ESCAPE
        state.triple && state.pendingQuotes == 1 -> MODE_TRIPLE_STRING_QUOTE1
        state.triple && state.pendingQuotes == 2 -> MODE_TRIPLE_STRING_QUOTE2
        state.triple -> MODE_TRIPLE_STRING
        else -> MODE_STRING
    }

    private fun withContinue(mode: Int, continueState: LexState): Int {
        if (continueState == LexState.Default) return mode and MODE_MASK
        return (mode and MODE_MASK) or (encodeContinue(continueState) shl PAYLOAD_SHIFT)
    }

    private fun encodeContinue(continueState: LexState): Int {
        if (continueState == LexState.Default) return 0
        val frame: Int
        val restState: LexState
        when (continueState) {
            is LexState.Template -> {
                val depth = continueState.depth.coerceIn(1, FRAME_TEMPLATE_MAX_DEPTH)
                frame = FRAME_TEMPLATE_BASE + depth - 1
                restState = continueState.resume
            }
            is LexState.InString -> {
                frame = if (continueState.triple) FRAME_RESUME_TRIPLE else FRAME_RESUME_STRING
                // Fine-grained InString modes (quotes/escape/dollar) collapse when nested
                // in the continue stack — only the live mode nibble preserves them.
                restState = continueState.continueState
            }
            is LexState.BlockComment -> {
                // Block comments are not pushed as continue frames; recover to Default.
                return encodeContinue(continueState.continueState)
            }
            LexState.Default -> return 0
        }
        val rest = encodeContinue(restState)
        return (frame or (rest shl FRAME_BITS)) and PAYLOAD_MASK
    }

    private fun decodeContinue(enc: Int): LexState {
        if (enc == 0) return LexState.Default
        val frame = enc and FRAME_MASK
        val rest = decodeContinue(enc ushr FRAME_BITS)
        return when (frame) {
            FRAME_RESUME_STRING -> LexState.InString(triple = false, continueState = rest)
            FRAME_RESUME_TRIPLE -> LexState.InString(triple = true, continueState = rest)
            in FRAME_TEMPLATE_BASE..FRAME_MASK -> {
                val depth = frame - FRAME_TEMPLATE_BASE + 1
                LexState.Template(depth, rest)
            }
            else -> rest
        }
    }
}
