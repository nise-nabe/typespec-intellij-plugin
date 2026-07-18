# feat: expand project scaffolding templates

**Status:** backlog (archived from draft PR)
**Former PR:** [#55](https://github.com/nise-nabe/typespec-intellij-plugin/pull/55) (OPEN, draft)
**Branch:** `cursor/template-expansion-e553`
**Base:** `cursor/run-config-options-e553`

## Description

## Summary

Expands Create TypeSpec Project with `generic-rest-api` template, external template URL, and `-y` auto-accept.

**Base:** #53

## Changed files

- `actions/src/main/kotlin/com/example/typespec/actions/TypeSpecCreateProjectAction.kt` (+1/-7)
- `actions/src/main/kotlin/com/example/typespec/actions/TypeSpecCreateProjectDialog.kt` (+35/-1)
- `actions/src/main/kotlin/com/example/typespec/run/TypeSpecCompileSettings.kt` (+5/-0)
- `actions/src/main/kotlin/com/example/typespec/run/TypeSpecCompileSettingsEditor.kt` (+29/-0)
- `core/src/main/resources/messages/TypeSpecBundle.properties` (+2/-0)

## Notes

- Closed without merge on 2026-07-18 to reset the stacked draft PR chain.
- Re-open as a fresh PR against current `main` when ready to implement.
