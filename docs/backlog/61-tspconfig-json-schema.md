# feat: add JSON Schema for tspconfig.yaml

**Status:** backlog (archived from draft PR)
**Former PR:** [#61](https://github.com/nise-nabe/typespec-intellij-plugin/pull/61) (OPEN, draft)
**Branch:** `cursor/tspconfig-json-schema-e553`
**Base:** `cursor/multi-root-monorepo-e553`

## Description

## Summary

Registers embedded JSON Schema for `tspconfig.yaml` validation and completion.

**Base:** multi-root branch

## Changed files

- `inspections/src/main/kotlin/com/example/typespec/inspections/TypeSpecTspConfigSchemaProviderFactory.kt` (+31/-0)
- `inspections/src/main/resources/schemas/tspconfig.schema.json` (+35/-0)
- `plugin/src/main/resources/META-INF/plugin.xml` (+2/-0)

## Notes

- Closed without merge on 2026-07-18 to reset the stacked draft PR chain.
- Re-open as a fresh PR against current `main` when ready to implement.
