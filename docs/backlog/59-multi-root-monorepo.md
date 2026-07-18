# feat: improve monorepo package resolution

**Status:** backlog (archived from draft PR)
**Former PR:** [#59](https://github.com/nise-nabe/typespec-intellij-plugin/pull/59) (OPEN, draft)
**Branch:** `cursor/multi-root-monorepo-e553`
**Base:** `cursor/emitter-diff-monitoring-e553`

## Description

## Summary

Probes all project content roots for package resolution in monorepos.

**Base:** #57

## Changed files

- `actions/src/main/kotlin/com/example/typespec/workflow/TypeSpecProjectContextExtensions.kt` (+29/-0)
- `core/src/main/kotlin/com/example/typespec/TypeSpecContentRootResolver.kt` (+33/-0)
- `core/src/main/kotlin/com/example/typespec/TypeSpecPackageResolutionCache.kt` (+5/-3)
- `core/src/main/resources/messages/TypeSpecBundle.properties` (+1/-0)
- `lsp/src/main/kotlin/com/example/typespec/TypeSpecSettingsConfigurable.kt` (+4/-0)

## Notes

- Closed without merge on 2026-07-18 to reset the stacked draft PR chain.
- Re-open as a fresh PR against current `main` when ready to implement.
