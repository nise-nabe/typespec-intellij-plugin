# feat: add TypeSpec syntax highlighting

**Status:** backlog (archived from draft PR)
**Former PR:** [#56](https://github.com/nise-nabe/typespec-intellij-plugin/pull/56) (OPEN, draft)
**Branch:** `cursor/syntax-highlighting-e553`
**Base:** `cursor/template-expansion-e553`

## Description

## Summary

Adds lexer-based syntax highlighting for TypeSpec keywords, decorators, strings, and comments.

**Base:** template-expansion branch

## Changed files

- `core/src/main/kotlin/com/example/typespec/highlighting/TypeSpecKeywords.kt` (+10/-0)
- `core/src/main/kotlin/com/example/typespec/highlighting/TypeSpecLexer.kt` (+106/-0)
- `core/src/main/kotlin/com/example/typespec/highlighting/TypeSpecSyntaxHighlighterFactory.kt` (+40/-0)
- `core/src/main/kotlin/com/example/typespec/highlighting/TypeSpecTokenTypes.kt` (+15/-0)
- `plugin/src/main/resources/META-INF/plugin.xml` (+3/-0)

## Notes

- Closed without merge on 2026-07-18 to reset the stacked draft PR chain.
- Re-open as a fresh PR against current `main` when ready to implement.
