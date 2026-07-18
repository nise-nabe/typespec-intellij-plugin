package com.example.typespec.highlighting

import com.intellij.psi.tree.IElementType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TypeSpecLexerTest {
    @Test
    fun tokenizesKeywordsDecoratorsStringsCommentsAndNumbers() {
        val source = """
            |// line comment
            |/* block comment */
            |@service
            |namespace Demo {
            |  model Pet { id: 42, name: "Rex" }
            |}
        """.trimMargin()

        val tokens = tokenize(source)

        assertEquals(
            listOf(
                TypeSpecTokenTypes.LINE_COMMENT,
                TypeSpecTokenTypes.WHITESPACE,
                TypeSpecTokenTypes.BLOCK_COMMENT,
                TypeSpecTokenTypes.WHITESPACE,
                TypeSpecTokenTypes.DECORATOR,
                TypeSpecTokenTypes.WHITESPACE,
                TypeSpecTokenTypes.KEYWORD,
            ),
            tokens.take(7).map { it.type },
        )
        assertEquals("@service", tokens.first { it.type == TypeSpecTokenTypes.DECORATOR }.text)
        assertEquals("namespace", tokens.first { it.type == TypeSpecTokenTypes.KEYWORD }.text)
        assertEquals("42", tokens.first { it.type == TypeSpecTokenTypes.NUMBER }.text)
        assertEquals("\"Rex\"", tokens.first { it.type == TypeSpecTokenTypes.STRING }.text)
        assertTrue(tokens.any { it.type == TypeSpecTokenTypes.IDENTIFIER })
        assertTrue(tokens.any { it.type == TypeSpecTokenTypes.OPERATION_SIGN })
    }

    @Test
    fun tokenizesEscapedQuotesInsideStrings() {
        val tokens = tokenize("""name: "say \"hi\"" """)
        val stringToken = tokens.first { it.type == TypeSpecTokenTypes.STRING }
        assertEquals("\"say \\\"hi\\\"\"", stringToken.text)
    }

    @Test
    fun tokenizesTripleQuotedStringsWithEmbeddedQuotes() {
        val triple = "\"\"\""
        val source = """
            |alias S = $triple
            |  hello "world"
            |  $triple;
        """.trimMargin()

        val tokens = tokenize(source)
        val stringToken = tokens.first { it.type == TypeSpecTokenTypes.STRING }
        assertTrue(stringToken.text.startsWith(triple), stringToken.text)
        assertTrue(stringToken.text.endsWith(triple), stringToken.text)
        assertTrue(stringToken.text.contains("\"world\""), stringToken.text)
        // Embedded quotes stay inside the triple-quoted string token.
        assertTrue(tokens.none { it.type == TypeSpecTokenTypes.IDENTIFIER && it.text == "world" })
        assertEquals(";", tokens.last { it.type == TypeSpecTokenTypes.OPERATION_SIGN }.text)
    }

    @Test
    fun resumesBlockCommentAcrossSegments() {
        val lexer = TypeSpecLexer()
        lexer.start("/* open", 0, 7, TypeSpecLexer.STATE_DEFAULT)
        assertEquals(TypeSpecTokenTypes.BLOCK_COMMENT, lexer.tokenType)
        assertEquals(TypeSpecLexer.STATE_BLOCK_COMMENT, lexer.state)

        lexer.start(" still */ after", 0, 15, TypeSpecLexer.STATE_BLOCK_COMMENT)
        assertEquals(TypeSpecTokenTypes.BLOCK_COMMENT, lexer.tokenType)
        assertEquals(" still */", lexer.tokenText)
        assertEquals(TypeSpecLexer.STATE_DEFAULT, lexer.state)
        lexer.advance()
        assertEquals(TypeSpecTokenTypes.WHITESPACE, lexer.tokenType)
        lexer.advance()
        assertEquals(TypeSpecTokenTypes.IDENTIFIER, lexer.tokenType)
        assertEquals("after", lexer.tokenText)
    }

    @Test
    fun resumesBlockCommentWhenStarSlashSplitAcrossSegments() {
        val lexer = TypeSpecLexer()
        lexer.start("/* body*", 0, 8, TypeSpecLexer.STATE_DEFAULT)
        assertEquals(TypeSpecTokenTypes.BLOCK_COMMENT, lexer.tokenType)
        assertEquals(TypeSpecLexer.STATE_BLOCK_COMMENT_AFTER_STAR, lexer.state)

        lexer.start("/ after", 0, 7, TypeSpecLexer.STATE_BLOCK_COMMENT_AFTER_STAR)
        assertEquals(TypeSpecTokenTypes.BLOCK_COMMENT, lexer.tokenType)
        assertEquals("/", lexer.tokenText)
        assertEquals(TypeSpecLexer.STATE_DEFAULT, lexer.state)
        lexer.advance()
        assertEquals(TypeSpecTokenTypes.WHITESPACE, lexer.tokenType)
        lexer.advance()
        assertEquals("after", lexer.tokenText)
    }

    @Test
    fun resumesStringAcrossSegments() {
        val lexer = TypeSpecLexer()
        lexer.start("\"hello", 0, 6, TypeSpecLexer.STATE_DEFAULT)
        assertEquals(TypeSpecTokenTypes.STRING, lexer.tokenType)
        assertEquals(TypeSpecLexer.STATE_STRING, lexer.state)

        lexer.start(" world\";", 0, 8, TypeSpecLexer.STATE_STRING)
        assertEquals(TypeSpecTokenTypes.STRING, lexer.tokenType)
        assertEquals(" world\"", lexer.tokenText)
        assertEquals(TypeSpecLexer.STATE_DEFAULT, lexer.state)
        lexer.advance()
        assertEquals(";", lexer.tokenText)
    }

    @Test
    fun resumesStringWhenEscapeSplitAcrossSegments() {
        val lexer = TypeSpecLexer()
        lexer.start("\"say \\", 0, 6, TypeSpecLexer.STATE_DEFAULT)
        assertEquals(TypeSpecTokenTypes.STRING, lexer.tokenType)
        assertEquals(TypeSpecLexer.STATE_STRING_AFTER_ESCAPE, lexer.state)

        lexer.start("\"hi\";", 0, 5, TypeSpecLexer.STATE_STRING_AFTER_ESCAPE)
        assertEquals(TypeSpecTokenTypes.STRING, lexer.tokenType)
        assertEquals("\"hi\"", lexer.tokenText)
        assertEquals(TypeSpecLexer.STATE_DEFAULT, lexer.state)
        lexer.advance()
        assertEquals(";", lexer.tokenText)
    }

    @Test
    fun resumesTripleQuotedStringAcrossSegments() {
        val lexer = TypeSpecLexer()
        lexer.start("\"\"\"hello", 0, 8, TypeSpecLexer.STATE_DEFAULT)
        assertEquals(TypeSpecTokenTypes.STRING, lexer.tokenType)
        assertEquals(TypeSpecLexer.STATE_TRIPLE_STRING, lexer.state)

        lexer.start(" \"world\" \"\"\";", 0, 13, TypeSpecLexer.STATE_TRIPLE_STRING)
        assertEquals(TypeSpecTokenTypes.STRING, lexer.tokenType)
        assertEquals(" \"world\" \"\"\"", lexer.tokenText)
        assertEquals(TypeSpecLexer.STATE_DEFAULT, lexer.state)
        lexer.advance()
        assertEquals(";", lexer.tokenText)
    }

    @Test
    fun resumesTripleQuotedStringWhenClosingQuotesSplitAcrossSegments() {
        val lexer = TypeSpecLexer()
        lexer.start("\"\"\"body\"\"", 0, 9, TypeSpecLexer.STATE_DEFAULT)
        assertEquals(TypeSpecTokenTypes.STRING, lexer.tokenType)
        assertEquals(TypeSpecLexer.STATE_TRIPLE_STRING_QUOTE2, lexer.state)

        lexer.start("\";", 0, 2, TypeSpecLexer.STATE_TRIPLE_STRING_QUOTE2)
        assertEquals(TypeSpecTokenTypes.STRING, lexer.tokenType)
        assertEquals("\"", lexer.tokenText)
        assertEquals(TypeSpecLexer.STATE_DEFAULT, lexer.state)
        lexer.advance()
        assertEquals(";", lexer.tokenText)
    }

    @Test
    fun unterminatedDoubleQuotedStringStopsAtNewline() {
        val tokens = tokenize("alias x = \"oops\nmodel Foo {}")
        val stringToken = tokens.first { it.type == TypeSpecTokenTypes.STRING }
        assertEquals("\"oops", stringToken.text)
        assertEquals("model", tokens.first { it.text == "model" }.text)
        assertEquals(TypeSpecTokenTypes.KEYWORD, tokens.first { it.text == "model" }.type)
        assertEquals("Foo", tokens.first { it.type == TypeSpecTokenTypes.IDENTIFIER && it.text == "Foo" }.text)
    }

    @Test
    fun stringTemplateWithNestedQuotesResumesAfterInterpolation() {
        // Source is: alias s = "a${"b"}c"
        val source = "alias s = \"a\${\"b\"}c\""
        val tokens = tokenize(source)
        assertEquals("\"a\${", tokens.first { it.type == TypeSpecTokenTypes.STRING }.text)
        assertEquals("\"b\"", tokens.filter { it.type == TypeSpecTokenTypes.STRING }[1].text)
        assertEquals("}", tokens.first { it.type == TypeSpecTokenTypes.OPERATION_SIGN && it.text == "}" }.text)
        assertEquals("c\"", tokens.filter { it.type == TypeSpecTokenTypes.STRING }[2].text)
        assertTrue(tokens.none { it.type == TypeSpecTokenTypes.IDENTIFIER && it.text == "b" })
    }

    @Test
    fun nestedStringTemplatesThreeDeepResumeCorrectly() {
        // Source is: alias s = "a${"b${"c${d}e"}f"}g"
        val source = "alias s = \"a\${\"b\${\"c\${d}e\"}f\"}g\""
        val tokens = tokenize(source)
        val strings = tokens.filter { it.type == TypeSpecTokenTypes.STRING }.map { it.text }
        assertEquals(listOf("\"a\${", "\"b\${", "\"c\${", "e\"", "f\"", "g\""), strings)
        assertEquals("d", tokens.first { it.type == TypeSpecTokenTypes.IDENTIFIER && it.text == "d" }.text)
        assertTrue(tokens.none { it.type == TypeSpecTokenTypes.IDENTIFIER && it.text == "g" })
    }

    @Test
    fun nestedStringTemplatesFiveDeepResumeCorrectly() {
        // Source is: alias s = "a${"b${"c${"d${"e${x}f"}g"}h"}i"}j"
        val source = "alias s = \"a\${\"b\${\"c\${\"d\${\"e\${x}f\"}g\"}h\"}i\"}j\""
        val tokens = tokenize(source)
        val strings = tokens.filter { it.type == TypeSpecTokenTypes.STRING }.map { it.text }
        assertEquals(
            listOf("\"a\${", "\"b\${", "\"c\${", "\"d\${", "\"e\${", "f\"", "g\"", "h\"", "i\"", "j\""),
            strings,
        )
        assertEquals("x", tokens.first { it.type == TypeSpecTokenTypes.IDENTIFIER && it.text == "x" }.text)
        assertTrue(tokens.none { it.type == TypeSpecTokenTypes.IDENTIFIER && it.text == "j" })
    }

    @Test
    fun nestedStringTemplatesSixDeepResumeCorrectlyInSingleBuffer() {
        // Typed LexState keeps full nesting in one buffer (beyond Int continue capacity).
        // Source: alias s = "a${"b${"c${"d${"e${"f${x}g"}h"}i"}j"}k"}l"
        val source = "alias s = \"a\${\"b\${\"c\${\"d\${\"e\${\"f\${x}g\"}h\"}i\"}j\"}k\"}l\""
        val tokens = tokenize(source)
        val strings = tokens.filter { it.type == TypeSpecTokenTypes.STRING }.map { it.text }
        assertEquals(
            listOf(
                "\"a\${", "\"b\${", "\"c\${", "\"d\${", "\"e\${", "\"f\${",
                "g\"", "h\"", "i\"", "j\"", "k\"", "l\"",
            ),
            strings,
        )
        assertEquals("x", tokens.first { it.type == TypeSpecTokenTypes.IDENTIFIER && it.text == "x" }.text)
        assertTrue(tokens.none { it.type == TypeSpecTokenTypes.IDENTIFIER && it.text == "l" })
    }

    @Test
    fun deepTemplateBracesWithNestedStringResumeInSingleBuffer() {
        // Live template depth can exceed packed frame depth (5); same-buffer scan stays exact.
        // `${` + five `{` → depth 6; six `}` return to the outer string.
        val source = "alias s = \"a\${{{{{{ \"inner\" }}}}}}b\""
        val tokens = tokenize(source)
        val strings = tokens.filter { it.type == TypeSpecTokenTypes.STRING }.map { it.text }
        assertEquals(listOf("\"a\${", "\"inner\"", "b\""), strings)
        assertTrue(tokens.none { it.type == TypeSpecTokenTypes.IDENTIFIER && it.text == "b" })
    }

    @Test
    fun resumesThreeDeepNestedTemplatesAcrossSegments() {
        // Split after opening the innermost template: "a${"b${"c${
        val lexer = TypeSpecLexer()
        lexer.start("\"a\${\"b\${\"c\${", 0, 12, TypeSpecLexer.STATE_DEFAULT)
        while (lexer.tokenType != null) {
            lexer.advance()
        }
        assertTrue(TypeSpecLexer.isTemplateState(lexer.state), "expected template state, got ${lexer.state}")
        val templateState = lexer.state

        lexer.start("d}e\"}f\"}g\"", 0, 10, templateState)
        val texts = mutableListOf<String>()
        while (lexer.tokenType != null) {
            texts += lexer.tokenText
            lexer.advance()
        }
        assertEquals(listOf("d", "}", "e\"", "}", "f\"", "}", "g\""), texts)
    }

    @Test
    fun lineCommentStopsAtCarriageReturn() {
        val tokens = tokenize("// oops\rmodel Foo")
        assertEquals("// oops", tokens.first { it.type == TypeSpecTokenTypes.LINE_COMMENT }.text)
        assertEquals(TypeSpecTokenTypes.KEYWORD, tokens.first { it.text == "model" }.type)
    }

    @Test
    fun nestedStringTemplatesResumeOuterInterpolation() {
        // Source is: alias s = "a${"b${c}d"}e"
        val source = "alias s = \"a\${\"b\${c}d\"}e\""
        val tokens = tokenize(source)
        val strings = tokens.filter { it.type == TypeSpecTokenTypes.STRING }.map { it.text }
        assertEquals(listOf("\"a\${", "\"b\${", "d\"", "e\""), strings)
        assertEquals("c", tokens.first { it.type == TypeSpecTokenTypes.IDENTIFIER && it.text == "c" }.text)
        // Outer `}` after the inner string must stay an operation (template), not get absorbed.
        assertTrue(tokens.any { it.type == TypeSpecTokenTypes.OPERATION_SIGN && it.text == "}" })
        assertTrue(tokens.none { it.type == TypeSpecTokenTypes.IDENTIFIER && it.text == "e" })
    }

    @Test
    fun tripleQuotedStringRespectsEscapedQuotes() {
        // Compiler: backslash skips the next char, so \""" does not close the literal.
        val source = "alias s = \"\"\"foo\\\"\"\"bar\"\"\";"
        val tokens = tokenize(source)
        val stringToken = tokens.first { it.type == TypeSpecTokenTypes.STRING }
        assertEquals("\"\"\"foo\\\"\"\"bar\"\"\"", stringToken.text)
        assertTrue(tokens.none { it.type == TypeSpecTokenTypes.IDENTIFIER && it.text == "bar" })
        assertEquals(";", tokens.last { it.type == TypeSpecTokenTypes.OPERATION_SIGN }.text)
    }

    @Test
    fun resumesStringWhenDollarThenNewlineAcrossSegmentsWithoutEmptyToken() {
        val lexer = TypeSpecLexer()
        lexer.start("\"a$", 0, 3, TypeSpecLexer.STATE_DEFAULT)
        assertEquals(TypeSpecTokenTypes.STRING, lexer.tokenType)
        assertEquals(TypeSpecLexer.STATE_STRING_AFTER_DOLLAR, lexer.state)

        lexer.start("\nmodel", 0, 6, TypeSpecLexer.STATE_STRING_AFTER_DOLLAR)
        assertEquals(TypeSpecTokenTypes.WHITESPACE, lexer.tokenType)
        assertEquals("\n", lexer.tokenText)
        assertTrue(lexer.tokenEnd > lexer.tokenStart, "must not emit a zero-length token")
        assertEquals(TypeSpecLexer.STATE_DEFAULT, lexer.state)
        lexer.advance()
        assertEquals(TypeSpecTokenTypes.KEYWORD, lexer.tokenType)
        assertEquals("model", lexer.tokenText)
    }

    @Test
    fun tripleQuotedStringTemplateResumesAfterInterpolation() {
        val triple = "\"\"\""
        val source = "alias s = $triple" + "a\${x}b$triple"
        val tokens = tokenize(source)
        val strings = tokens.filter { it.type == TypeSpecTokenTypes.STRING }.map { it.text }
        assertEquals(listOf("${triple}a\${", "b$triple"), strings)
        assertEquals("x", tokens.first { it.type == TypeSpecTokenTypes.IDENTIFIER && it.text == "x" }.text)
    }

    @Test
    fun resumesStringTemplateWhenDollarSplitAcrossSegments() {
        val lexer = TypeSpecLexer()
        lexer.start("\"a$", 0, 3, TypeSpecLexer.STATE_DEFAULT)
        assertEquals(TypeSpecTokenTypes.STRING, lexer.tokenType)
        assertEquals(TypeSpecLexer.STATE_STRING_AFTER_DOLLAR, lexer.state)

        lexer.start("{x}y\";", 0, 6, TypeSpecLexer.STATE_STRING_AFTER_DOLLAR)
        assertEquals(TypeSpecTokenTypes.OPERATION_SIGN, lexer.tokenType)
        assertEquals("{", lexer.tokenText)
        assertEquals(TypeSpecLexer.templateState(1, TypeSpecLexer.STATE_STRING), lexer.state)
        lexer.advance()
        assertEquals("x", lexer.tokenText)
        lexer.advance()
        assertEquals("}", lexer.tokenText)
        assertEquals(TypeSpecLexer.STATE_STRING, lexer.state)
        lexer.advance()
        assertEquals("y\"", lexer.tokenText)
        assertEquals(TypeSpecLexer.STATE_DEFAULT, lexer.state)
    }

    @Test
    fun tokenizesDollarPrefixedIdentifier() {
        val tokens = tokenize("model \$foo { }")
        assertEquals("\$foo", tokens.first { it.type == TypeSpecTokenTypes.IDENTIFIER }.text)
    }

    @Test
    fun tokenizesIdentifierWithInternalDollar() {
        val tokens = tokenize("model \$money\$ { }")
        assertEquals("\$money\$", tokens.first { it.type == TypeSpecTokenTypes.IDENTIFIER }.text)
    }

    @Test
    fun tokenizesAugmentDecoratorAndDottedMemberAccess() {
        val tokens = tokenize("@@TypeSpec.service model Pet { id: int32 }")
        assertEquals("@@TypeSpec", tokens.first { it.type == TypeSpecTokenTypes.DECORATOR }.text)
        assertEquals(".", tokens.first { it.type == TypeSpecTokenTypes.OPERATION_SIGN }.text)
        assertEquals("service", tokens.first { it.text == "service" }.text)
        assertEquals(TypeSpecTokenTypes.IDENTIFIER, tokens.first { it.text == "service" }.type)
    }

    @Test
    fun tokenizesBacktickEscapedKeywordAsIdentifier() {
        val tokens = tokenize("model `enum` { }")
        assertEquals("`enum`", tokens.first { it.type == TypeSpecTokenTypes.IDENTIFIER }.text)
        assertTrue(tokens.none { it.type == TypeSpecTokenTypes.KEYWORD && it.text == "enum" })
    }

    @Test
    fun tokenizesBacktickEscapedIdentWithEscapeAndSpaces() {
        val tokens = tokenize("model `foo\\`bar baz` { }")
        assertEquals("`foo\\`bar baz`", tokens.first { it.type == TypeSpecTokenTypes.IDENTIFIER }.text)
    }

    @Test
    fun tokenizesDecoratorWithBacktickEscapedName() {
        val tokens = tokenize("@`service` model Pet { }")
        assertEquals("@`service`", tokens.first { it.type == TypeSpecTokenTypes.DECORATOR }.text)
        assertTrue(tokens.none { it.type == TypeSpecTokenTypes.KEYWORD && it.text == "service" })
    }

    @Test
    fun resumesEscapedIdentAcrossSegments() {
        val lexer = TypeSpecLexer()
        lexer.start("`enu", 0, 4, TypeSpecLexer.STATE_DEFAULT)
        assertEquals(TypeSpecTokenTypes.IDENTIFIER, lexer.tokenType)
        assertEquals(TypeSpecLexer.STATE_ESCAPED_IDENT, lexer.state)

        lexer.start("m` model", 0, 8, TypeSpecLexer.STATE_ESCAPED_IDENT)
        assertEquals(TypeSpecTokenTypes.IDENTIFIER, lexer.tokenType)
        assertEquals("m`", lexer.tokenText)
        assertEquals(TypeSpecLexer.STATE_DEFAULT, lexer.state)
        lexer.advance()
        assertEquals(TypeSpecTokenTypes.WHITESPACE, lexer.tokenType)
        lexer.advance()
        assertEquals(TypeSpecTokenTypes.KEYWORD, lexer.tokenType)
        assertEquals("model", lexer.tokenText)
    }

    @Test
    fun resumesDecoratorEscapedIdentAcrossSegments() {
        val lexer = TypeSpecLexer()
        lexer.start("@`serv", 0, 6, TypeSpecLexer.STATE_DEFAULT)
        assertEquals(TypeSpecTokenTypes.DECORATOR, lexer.tokenType)
        assertEquals(TypeSpecLexer.STATE_DECORATOR_ESCAPED_IDENT, lexer.state)

        lexer.start("ice` model", 0, 10, TypeSpecLexer.STATE_DECORATOR_ESCAPED_IDENT)
        assertEquals(TypeSpecTokenTypes.DECORATOR, lexer.tokenType)
        assertEquals("ice`", lexer.tokenText)
        assertEquals(TypeSpecLexer.STATE_DEFAULT, lexer.state)
    }

    @Test
    fun highlightsCompilerKeywordsNotSpuriousOnes() {
        val tokens = tokenize("const x = valueof int32; projection P { }; public struct S { }")
        assertEquals(
            listOf("const", "valueof", "projection", "public", "struct"),
            tokens.filter { it.type == TypeSpecTokenTypes.KEYWORD }.map { it.text },
        )
        assertTrue("projection" in TypeSpecKeywords.KEYWORDS)
        assertTrue("struct" in TypeSpecKeywords.KEYWORDS)
        assertTrue("async" in TypeSpecKeywords.KEYWORDS)
        assertTrue("function" !in TypeSpecKeywords.KEYWORDS)
        assertTrue("project" !in TypeSpecKeywords.KEYWORDS)
    }

    @Test
    fun packUnpackRoundTripsSimpleResumeStates() {
        val simples = listOf(
            TypeSpecLexer.STATE_DEFAULT,
            TypeSpecLexer.STATE_BLOCK_COMMENT,
            TypeSpecLexer.STATE_BLOCK_COMMENT_AFTER_STAR,
            TypeSpecLexer.STATE_STRING,
            TypeSpecLexer.STATE_STRING_AFTER_ESCAPE,
            TypeSpecLexer.STATE_TRIPLE_STRING,
            TypeSpecLexer.STATE_TRIPLE_STRING_QUOTE1,
            TypeSpecLexer.STATE_TRIPLE_STRING_QUOTE2,
            TypeSpecLexer.STATE_STRING_AFTER_DOLLAR,
            TypeSpecLexer.STATE_TRIPLE_STRING_AFTER_DOLLAR,
            TypeSpecLexer.STATE_TRIPLE_STRING_AFTER_ESCAPE,
            TypeSpecLexer.STATE_ESCAPED_IDENT,
            TypeSpecLexer.STATE_ESCAPED_IDENT_AFTER_ESCAPE,
            TypeSpecLexer.STATE_DECORATOR_ESCAPED_IDENT,
            TypeSpecLexer.STATE_DECORATOR_ESCAPED_IDENT_AFTER_ESCAPE,
        )
        for (packed in simples) {
            assertEquals(packed, LexStatePacking.pack(LexStatePacking.unpack(packed)), "state $packed")
        }
        val template = TypeSpecLexer.templateState(1, TypeSpecLexer.STATE_STRING)
        assertTrue(TypeSpecLexer.isTemplateState(template))
        assertEquals(template, LexStatePacking.pack(LexStatePacking.unpack(template)))
    }

    @Test
    fun packUnpackPreservesDeepTemplateContinueDepth() {
        // Template-as-continue used to clamp depth to 5; depths 5–15 must round-trip.
        for (depth in listOf(5, 6, 15)) {
            val nested = LexState.InString(
                triple = true,
                continueState = LexState.Template(depth, LexState.InString(triple = false)),
            )
            val roundTripped = LexStatePacking.unpack(LexStatePacking.pack(nested)) as LexState.InString
            val cont = roundTripped.continueState as LexState.Template
            assertEquals(depth, cont.depth, "continue Template depth $depth")
            assertTrue(cont.resume is LexState.InString)
        }
    }

    @Test
    fun deepTemplateBraceContinuePreservesDepthAcrossSegments() {
        // String opened inside brace-depth 6 must resume with full depth after a segment split.
        // Source: "a${{{{{{ """hi""" }}}}}}b"
        val lexer = TypeSpecLexer()
        val part1 = "\"a\${{{{{{ \"\"\"hi"
        lexer.start(part1, 0, part1.length, TypeSpecLexer.STATE_DEFAULT)
        while (lexer.tokenType != null) {
            lexer.advance()
        }
        val packed = lexer.state

        val part2 = "\"\"\" }}}}}}b\""
        lexer.start(part2, 0, part2.length, packed)
        val texts = mutableListOf<String>()
        while (lexer.tokenType != null) {
            texts += lexer.tokenText
            lexer.advance()
        }
        assertEquals(6, texts.count { it == "}" }, texts.toString())
        assertTrue(texts.contains("b\""), texts.toString())
        assertTrue(texts.none { it == "}b\"" }, texts.toString())
    }

    @Test
    fun continueStackKeepsDeepestFramesOnOverflow() {
        // Live Template + alternating InString/Template continues: nest(5) → 9 shallow frames;
        // nest(6) → 11 and must drop the outermost two (keep deepest).
        val capacity = LexStatePacking.CONTINUE_FRAME_CAPACITY
        assertEquals(9, capacity)

        fun nest(layers: Int): LexState {
            require(layers >= 1)
            var state: LexState = LexState.InString(triple = false)
            repeat(layers - 1) {
                state = LexState.InString(
                    triple = false,
                    continueState = LexState.Template(1, state),
                )
            }
            return LexState.Template(1, state)
        }

        val overflow = LexStatePacking.unpack(LexStatePacking.pack(nest(6))) as LexState.Template
        val exact = LexStatePacking.unpack(LexStatePacking.pack(nest(5))) as LexState.Template
        assertEquals(exact.resume, overflow.resume, "overflow must keep the same deepest resume chain")
        assertEquals(1, overflow.depth)
    }

    @Test
    fun sixDeepNestingSurvivesSegmentBoundaryViaPackedState() {
        // Split after opening six nested templates; packed Int truncates outermost frames,
        // so resume is approximate — assert deepest unwind still produces tokens without crash.
        val lexer = TypeSpecLexer()
        lexer.start("\"a\${\"b\${\"c\${\"d\${\"e\${\"f\${", 0, 24, TypeSpecLexer.STATE_DEFAULT)
        while (lexer.tokenType != null) {
            lexer.advance()
        }
        assertTrue(TypeSpecLexer.isTemplateState(lexer.state), "expected template state, got ${lexer.state}")
        val packed = lexer.state

        lexer.start("x}g\"}h\"}i\"}j\"}k\"}l\"", 0, 19, packed)
        val texts = mutableListOf<String>()
        while (lexer.tokenType != null) {
            texts += lexer.tokenText
            lexer.advance()
        }
        assertTrue(texts.contains("x"), texts.toString())
        assertTrue(texts.any { it.contains("g\"") }, texts.toString())
    }

    private data class Token(val type: IElementType, val text: String)

    private fun tokenize(source: String): List<Token> {
        val lexer = TypeSpecLexer()
        lexer.start(source)
        val tokens = mutableListOf<Token>()
        while (lexer.tokenType != null) {
            tokens += Token(lexer.tokenType!!, lexer.tokenText)
            lexer.advance()
        }
        return tokens
    }
}
