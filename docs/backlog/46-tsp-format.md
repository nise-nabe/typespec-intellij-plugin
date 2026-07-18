# feat: add tsp format actions

**Status:** backlog (archived from draft PR)
**Former PR:** [#46](https://github.com/nise-nabe/typespec-intellij-plugin/pull/46) (OPEN, draft)
**Branch:** `cursor/tsp-format-e553`
**Base:** `cursor/install-global-cli-e553`

## Description

## Summary

Adds `tsp format` integration:

- **Format TypeSpec File** — editor context menu; formats current `.tsp` or project when `tspconfig.yaml` is selected
- **Format TypeSpec Project** — Tools menu; runs `tsp format .`

**Base:** #45 (`cursor/install-global-cli-e553`)

## Changed files

- `actions/src/main/kotlin/com/example/typespec/actions/TypeSpecFormatAction.kt` (+54/-0)
- `actions/src/main/kotlin/com/example/typespec/actions/TypeSpecFormatProjectAction.kt` (+47/-0)
- `core/src/main/resources/messages/TypeSpecBundle.properties` (+12/-0)
- `plugin/src/main/resources/META-INF/plugin.xml` (+3/-0)
- `plugin/src/test/kotlin/com/example/typespec/TypeSpecPluginDescriptorTest.kt` (+2/-0)

## Notes

- Closed without merge on 2026-07-18 to reset the stacked draft PR chain.
- Re-open as a fresh PR against current `main` when ready to implement.
