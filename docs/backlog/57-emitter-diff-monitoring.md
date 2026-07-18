# feat: show diff when emitter output changes

**Status:** backlog (archived from draft PR)
**Former PR:** [#57](https://github.com/nise-nabe/typespec-intellij-plugin/pull/57) (OPEN, draft)
**Branch:** `cursor/emitter-diff-monitoring-e553`
**Base:** `cursor/http-client-integration-e553`

## Description

## Summary

Shows a diff view when primary emitter output changes after a successful compile.

**Base:** http-client-integration branch

## Changed files

- `actions/src/main/kotlin/com/example/typespec/actions/TypeSpecEmitFromTypeSpecAction.kt` (+11/-0)
- `actions/src/main/kotlin/com/example/typespec/workflow/TypeSpecEmitterDiffMonitor.kt` (+43/-0)
- `plugin/src/main/resources/META-INF/plugin.xml` (+1/-0)

## Notes

- Closed without merge on 2026-07-18 to reset the stacked draft PR chain.
- Re-open as a fresh PR against current `main` when ready to implement.
