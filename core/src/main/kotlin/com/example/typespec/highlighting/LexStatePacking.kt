package com.example.typespec.highlighting

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

    /** Maximum continue frames that fit in the Int payload (for tests). */
    const val CONTINUE_FRAME_CAPACITY = PAYLOAD_BITS / FRAME_BITS

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
    private const val MODE_ESCAPED_IDENT = 11
    private const val MODE_ESCAPED_IDENT_AFTER_ESCAPE = 12
    private const val MODE_DECORATOR_ESCAPED_IDENT = 13
    private const val MODE_DECORATOR_ESCAPED_IDENT_AFTER_ESCAPE = 14

    fun pack(state: LexState): Int = when (state) {
        LexState.Default -> MODE_DEFAULT
        is LexState.BlockComment -> withContinue(
            if (state.afterStar) MODE_BLOCK_COMMENT_AFTER_STAR else MODE_BLOCK_COMMENT,
            state.continueState,
        )
        is LexState.InString -> withContinue(stringMode(state), state.continueState)
        is LexState.EscapedIdent -> withContinue(escapedIdentMode(state), state.continueState)
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
            MODE_ESCAPED_IDENT -> LexState.EscapedIdent(continueState = cont)
            MODE_ESCAPED_IDENT_AFTER_ESCAPE -> LexState.EscapedIdent(afterEscape = true, continueState = cont)
            MODE_DECORATOR_ESCAPED_IDENT -> LexState.EscapedIdent(asDecorator = true, continueState = cont)
            MODE_DECORATOR_ESCAPED_IDENT_AFTER_ESCAPE ->
                LexState.EscapedIdent(afterEscape = true, asDecorator = true, continueState = cont)
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

    private fun escapedIdentMode(state: LexState.EscapedIdent): Int = when {
        state.asDecorator && state.afterEscape -> MODE_DECORATOR_ESCAPED_IDENT_AFTER_ESCAPE
        state.asDecorator -> MODE_DECORATOR_ESCAPED_IDENT
        state.afterEscape -> MODE_ESCAPED_IDENT_AFTER_ESCAPE
        else -> MODE_ESCAPED_IDENT
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
            is LexState.EscapedIdent -> {
                // Escaped identifiers are live-mode only; recover to their continue target.
                return encodeContinue(continueState.continueState)
            }
            LexState.Default -> return 0
        }
        val rest = encodeContinue(restState)
        // Capacity is finite: drop outermost frames so the deepest resume targets survive.
        if ((rest ushr (PAYLOAD_BITS - FRAME_BITS)) != 0) {
            return rest
        }
        return frame or (rest shl FRAME_BITS)
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
