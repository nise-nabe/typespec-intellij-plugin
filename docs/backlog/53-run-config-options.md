# feat: extend run configuration with compile CLI options

**Status:** backlog (archived from draft PR)
**Former PR:** [#53](https://github.com/nise-nabe/typespec-intellij-plugin/pull/53) (OPEN, draft)
**Branch:** `cursor/run-config-options-e553`
**Base:** `cursor/standalone-tsp-fallback-e553`

## Description

## Summary

Extends **TypeSpec Compile** run configuration with `--dry-run`, `--no-emit`, `--stats`, `--trace`, and `--warn-as-error` options.

**Base:** #52 (`cursor/standalone-tsp-fallback-e553`)

## Changed files

- `actions/src/main/kotlin/com/example/typespec/run/TypeSpecCompileSettings.kt` (+15/-0)
- `actions/src/main/kotlin/com/example/typespec/run/TypeSpecCompileSettingsEditor.kt` (+5/-0)
- `actions/src/main/kotlin/com/example/typespec/workflow/TypeSpecCompileRequest.kt` (+21/-0)
- `actions/src/test/kotlin/com/example/typespec/workflow/TypeSpecCompileCommandBuilderTest.kt` (+27/-0)
- `core/src/main/resources/messages/TypeSpecBundle.properties` (+5/-0)

## Notes

- Closed without merge on 2026-07-18 to reset the stacked draft PR chain.
- Re-open as a fresh PR against current `main` when ready to implement.
