# feat: add standalone tsp --server LSP fallback

**Status:** backlog (archived from draft PR)
**Former PR:** [#52](https://github.com/nise-nabe/typespec-intellij-plugin/pull/52) (OPEN, draft)
**Branch:** `cursor/standalone-tsp-fallback-e553`
**Base:** `cursor/structure-view-e553`

## Description

## Summary

Falls back to `tsp --server <tsp-server.js> --stdio` when the default Node-based LSP command line cannot be created (VS Code parity).

**Base:** #51 (`cursor/structure-view-e553`)

## Changed files

- `core/src/main/resources/messages/TypeSpecBundle.properties` (+1/-0)
- `lsp/src/main/kotlin/com/example/typespec/TypeSpecLspClientDescriptor.kt` (+17/-0)
- `lsp/src/main/kotlin/com/example/typespec/TypeSpecLspIntegrationProvider.kt` (+0/-4)
- `lsp/src/main/kotlin/com/example/typespec/TypeSpecStandaloneTspResolver.kt` (+39/-0)
- `lsp/src/test/kotlin/com/example/typespec/TypeSpecStandaloneTspResolverTest.kt` (+33/-0)

## Notes

- Closed without merge on 2026-07-18 to reset the stacked draft PR chain.
- Re-open as a fresh PR against current `main` when ready to implement.
