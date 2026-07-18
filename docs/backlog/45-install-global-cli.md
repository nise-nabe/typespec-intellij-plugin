# feat: add install global TypeSpec compiler action

**Status:** backlog (archived from draft PR)
**Former PR:** [#45](https://github.com/nise-nabe/typespec-intellij-plugin/pull/45) (OPEN, draft)
**Branch:** `cursor/install-global-cli-e553`
**Base:** `cursor/tsp-install-e553`

## Description

## Summary

Adds **Install TypeSpec Compiler Globally** — VS Code parity command that runs `npm install -g @typespec/compiler`.

## Changes

- `TypeSpecInstallGlobalCompilerAction` and `TypeSpecNpmExecutableResolver`
- Registered in **Tools | TypeSpec**
- Success notification suggests restarting the language server

**Base:** #44 (`cursor/tsp-install-e553`)

## Changed files

- `actions/src/main/kotlin/com/example/typespec/actions/TypeSpecInstallGlobalCompilerAction.kt` (+62/-0)
- `actions/src/main/kotlin/com/example/typespec/workflow/TypeSpecNpmExecutableResolver.kt` (+41/-0)
- `core/src/main/resources/messages/TypeSpecBundle.properties` (+8/-0)
- `plugin/src/main/resources/META-INF/plugin.xml` (+1/-0)
- `plugin/src/test/kotlin/com/example/typespec/TypeSpecPluginDescriptorTest.kt` (+1/-0)

## Notes

- Closed without merge on 2026-07-18 to reset the stacked draft PR chain.
- Re-open as a fresh PR against current `main` when ready to implement.
