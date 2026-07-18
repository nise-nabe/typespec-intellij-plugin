# feat: navigate to generated artifacts after emit

**Status:** backlog (archived from draft PR)
**Former PR:** [#48](https://github.com/nise-nabe/typespec-intellij-plugin/pull/48) (OPEN, draft)
**Branch:** `cursor/artifact-navigation-e553`
**Base:** `cursor/compiler-info-e553`

## Description

## Summary

- **Show Generated Output** action opens the primary artifact (e.g. `openapi.yaml`) or output directory
- **Emit from TypeSpec** auto-reveals output on success
- `TypeSpecTspConfigReader` parses `output-dir` from `tspconfig.yaml`

**Base:** #47 (`cursor/compiler-info-e553`)

## Changed files

- `actions/src/main/kotlin/com/example/typespec/actions/TypeSpecEmitFromTypeSpecAction.kt` (+4/-0)
- `actions/src/main/kotlin/com/example/typespec/actions/TypeSpecShowGeneratedOutputAction.kt` (+29/-0)
- `actions/src/main/kotlin/com/example/typespec/workflow/TypeSpecArtifactNavigator.kt` (+51/-0)
- `actions/src/main/kotlin/com/example/typespec/workflow/TypeSpecTspConfigReader.kt` (+20/-0)
- `actions/src/test/kotlin/com/example/typespec/workflow/TypeSpecTspConfigOutputDirTest.kt` (+23/-0)
- `core/src/main/resources/messages/TypeSpecBundle.properties` (+4/-0)
- `plugin/src/main/resources/META-INF/plugin.xml` (+2/-0)
- `plugin/src/test/kotlin/com/example/typespec/TypeSpecPluginDescriptorTest.kt` (+1/-0)

## Notes

- Closed without merge on 2026-07-18 to reset the stacked draft PR chain.
- Re-open as a fresh PR against current `main` when ready to implement.
