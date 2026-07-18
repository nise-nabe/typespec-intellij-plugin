# feat: add tspconfig.yaml inspection

**Status:** backlog (archived from draft PR)
**Former PR:** [#50](https://github.com/nise-nabe/typespec-intellij-plugin/pull/50) (OPEN, draft)
**Branch:** `cursor/tspconfig-inspection-e553`
**Base:** `cursor/jcef-preview-e553`

## Description

## Summary

Adds `tspconfig.yaml` inspection (TCFG001–TCFG003) for project kind/entrypoint, feature flags, and missing emit section.

**Base:** #49 (`cursor/jcef-preview-e553`)

## Changed files

- `core/src/main/resources/messages/TypeSpecBundle.properties` (+6/-0)
- `inspections/src/main/kotlin/com/example/typespec/inspections/TypeSpecTspConfigInspection.kt` (+38/-0)
- `inspections/src/main/kotlin/com/example/typespec/inspections/TypeSpecTspConfigRules.kt` (+41/-0)
- `inspections/src/main/resources/inspectionDescriptions/TypeSpecTspConfigInspection.html` (+6/-0)
- `inspections/src/test/kotlin/com/example/typespec/inspections/TypeSpecTspConfigRulesTest.kt` (+41/-0)
- `plugin/src/main/resources/META-INF/plugin.xml` (+9/-0)

## Notes

- Closed without merge on 2026-07-18 to reset the stacked draft PR chain.
- Re-open as a fresh PR against current `main` when ready to implement.
