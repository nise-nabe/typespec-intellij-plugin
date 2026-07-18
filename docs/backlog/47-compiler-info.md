# feat: add compiler info display and tsp info action

**Status:** backlog (archived from draft PR)
**Former PR:** [#47](https://github.com/nise-nabe/typespec-intellij-plugin/pull/47) (OPEN, draft)
**Branch:** `cursor/compiler-info-e553`
**Base:** `cursor/tsp-format-e553`

## Description

## Summary

- Settings page shows **compiler package version** and **CLI/LSP readiness**
- New **Show Compiler Info** action runs `tsp info` in the output window

**Base:** #46 (`cursor/tsp-format-e553`)

## Changed files

- `actions/src/main/kotlin/com/example/typespec/actions/TypeSpecShowCompilerInfoAction.kt` (+43/-0)
- `core/src/main/kotlin/com/example/typespec/TypeSpecCompilerVersionReader.kt` (+16/-0)
- `core/src/main/resources/messages/TypeSpecBundle.properties` (+11/-0)
- `core/src/test/kotlin/com/example/typespec/TypeSpecCompilerVersionReaderTest.kt` (+25/-0)
- `lsp/src/main/kotlin/com/example/typespec/TypeSpecSettingsConfigurable.kt` (+24/-0)
- `plugin/src/main/resources/META-INF/plugin.xml` (+1/-0)
- `plugin/src/test/kotlin/com/example/typespec/TypeSpecPluginDescriptorTest.kt` (+1/-0)

## Notes

- Closed without merge on 2026-07-18 to reset the stacked draft PR chain.
- Re-open as a fresh PR against current `main` when ready to implement.
