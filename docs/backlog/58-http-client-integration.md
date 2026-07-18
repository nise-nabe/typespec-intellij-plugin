# feat: generate HTTP Client requests from OpenAPI

**Status:** backlog (archived from draft PR)
**Former PR:** [#58](https://github.com/nise-nabe/typespec-intellij-plugin/pull/58) (OPEN, draft)
**Branch:** `cursor/http-client-integration-e553`
**Base:** `cursor/live-templates-e553`

## Description

## Summary

Generates `typespec-generated.http` from OpenAPI output for IntelliJ HTTP Client.

**Base:** live-templates branch

## Changed files

- `actions/src/main/kotlin/com/example/typespec/actions/TypeSpecGenerateHttpClientAction.kt` (+38/-0)
- `actions/src/main/kotlin/com/example/typespec/workflow/TypeSpecHttpClientGenerator.kt` (+27/-0)
- `core/src/main/resources/messages/TypeSpecBundle.properties` (+4/-0)
- `plugin/src/main/resources/META-INF/plugin.xml` (+1/-0)
- `plugin/src/test/kotlin/com/example/typespec/TypeSpecPluginDescriptorTest.kt` (+1/-0)

## Notes

- Closed without merge on 2026-07-18 to reset the stacked draft PR chain.
- Re-open as a fresh PR against current `main` when ready to implement.
