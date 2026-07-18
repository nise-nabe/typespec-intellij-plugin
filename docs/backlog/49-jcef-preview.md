# feat: add in-IDE OpenAPI preview via JCEF

**Status:** backlog (archived from draft PR)
**Former PR:** [#49](https://github.com/nise-nabe/typespec-intellij-plugin/pull/49) (OPEN, draft)
**Branch:** `cursor/jcef-preview-e553`
**Base:** `cursor/artifact-navigation-e553`

## Description

## Summary

OpenAPI preview now renders in an IDE **TypeSpec API Preview** tool window via JCEF (Swagger UI). Falls back to external browser when JCEF is unavailable.

**Base:** #48 (`cursor/artifact-navigation-e553`)

## Changed files

- `actions/src/main/kotlin/com/example/typespec/workflow/TypeSpecApiPreviewToolWindowFactory.kt` (+20/-0)
- `actions/src/main/kotlin/com/example/typespec/workflow/TypeSpecJcefPreviewSupport.kt` (+49/-0)
- `actions/src/main/kotlin/com/example/typespec/workflow/TypeSpecOpenApiPreviewWorkflow.kt` (+6/-6)
- `core/src/main/resources/messages/TypeSpecBundle.properties` (+1/-0)
- `plugin/src/main/resources/META-INF/plugin.xml` (+7/-0)

## Notes

- Closed without merge on 2026-07-18 to reset the stacked draft PR chain.
- Re-open as a fresh PR against current `main` when ready to implement.
