# feat: add structure view for TypeSpec files

**Status:** backlog (archived from draft PR)
**Former PR:** [#51](https://github.com/nise-nabe/typespec-intellij-plugin/pull/51) (OPEN, draft)
**Branch:** `cursor/structure-view-e553`
**Base:** `cursor/tspconfig-inspection-e553`

## Description

## Summary

Adds Structure tool window support for `.tsp` files by parsing top-level declarations (namespace, model, interface, op, etc.).

**Base:** #50 (`cursor/tspconfig-inspection-e553`)

## Changed files

- `core/src/main/kotlin/com/example/typespec/TypeSpecStructureParser.kt` (+28/-0)
- `core/src/main/kotlin/com/example/typespec/TypeSpecStructureViewFactory.kt` (+75/-0)
- `core/src/test/kotlin/com/example/typespec/TypeSpecStructureParserTest.kt` (+19/-0)
- `plugin/src/main/resources/META-INF/plugin.xml` (+3/-0)

## Notes

- Closed without merge on 2026-07-18 to reset the stacked draft PR chain.
- Re-open as a fresh PR against current `main` when ready to implement.
