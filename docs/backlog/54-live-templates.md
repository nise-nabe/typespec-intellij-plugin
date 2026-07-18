# feat: add TypeSpec live templates

**Status:** backlog (archived from draft PR)
**Former PR:** [#54](https://github.com/nise-nabe/typespec-intellij-plugin/pull/54) (OPEN, draft)
**Branch:** `cursor/live-templates-e553`
**Base:** `cursor/syntax-highlighting-e553`

## Description

## Summary

Adds live templates for model, namespace, and op declarations in `.tsp` files.

**Base:** syntax-highlighting branch

## Changed files

- `core/src/main/kotlin/com/example/typespec/TypeSpecTemplateContextType.kt` (+8/-0)
- `plugin/src/main/resources/META-INF/plugin.xml` (+4/-0)
- `plugin/src/main/resources/liveTemplates/TypeSpec.xml` (+22/-0)

## Notes

- Closed without merge on 2026-07-18 to reset the stacked draft PR chain.
- Re-open as a fresh PR against current `main` when ready to implement.
