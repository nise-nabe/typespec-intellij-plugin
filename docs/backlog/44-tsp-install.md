# feat: add tsp install dependencies action

**Status:** backlog (archived from draft PR)
**Former PR:** [#44](https://github.com/nise-nabe/typespec-intellij-plugin/pull/44) (CLOSED, draft)
**Branch:** `cursor/tsp-install-e553`
**Base:** `main`

## Description

## Summary

Adds **Install TypeSpec Dependencies** (`TypeSpec.InstallDependencies`) — runs `tsp install` in the resolved TypeSpec project root.

## Changes

- New `TypeSpecInstallDependenciesAction` in the actions module
- Registered in **Tools | TypeSpec** and **Project View** context menu
- Bundle messages for progress, success notification, and failure handling
- Descriptor test updated for the new action ID

## Motivation

Completes the common onboarding flow after `tsp init`: users can install dependencies directly from the IDE instead of switching to a terminal.

Part 1 of a chained feature series.

## Changed files

- `actions/src/main/kotlin/com/example/typespec/actions/TypeSpecActionSupport.kt` (+1/-0)
- `actions/src/main/kotlin/com/example/typespec/actions/TypeSpecInstallDependenciesAction.kt` (+57/-0)
- `core/src/main/resources/messages/TypeSpecBundle.properties` (+8/-0)
- `plugin/src/main/resources/META-INF/plugin.xml` (+2/-0)
- `plugin/src/test/kotlin/com/example/typespec/TypeSpecPluginDescriptorTest.kt` (+1/-0)

## Notes

- Closed without merge on 2026-07-18 to reset the stacked draft PR chain.
- Re-open as a fresh PR against current `main` when ready to implement.
