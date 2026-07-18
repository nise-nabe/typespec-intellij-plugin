# Archived draft PR backlog

These items were migrated from closed draft pull requests (#44–#63) on 2026-07-18.
The repository had GitHub Issues disabled at migration time, so work is tracked here until issues are enabled.

Re-implement against current `main` one item at a time; the old PR chain is obsolete.

## Suggested merge order (former chain)

| # | Title | Former PR | Branch | Base |
|---|-------|-----------|--------|------|
| 1 | feat: add tsp install dependencies action | [#44](https://github.com/nise-nabe/typespec-intellij-plugin/pull/44) | `cursor/tsp-install-e553` | `main` |
| 2 | feat: add install global TypeSpec compiler action | [#45](https://github.com/nise-nabe/typespec-intellij-plugin/pull/45) | `cursor/install-global-cli-e553` | `cursor/tsp-install-e553` |
| 3 | feat: add tsp format actions | [#46](https://github.com/nise-nabe/typespec-intellij-plugin/pull/46) | `cursor/tsp-format-e553` | `cursor/install-global-cli-e553` |
| 4 | feat: add compiler info display and tsp info action | [#47](https://github.com/nise-nabe/typespec-intellij-plugin/pull/47) | `cursor/compiler-info-e553` | `cursor/tsp-format-e553` |
| 5 | feat: navigate to generated artifacts after emit | [#48](https://github.com/nise-nabe/typespec-intellij-plugin/pull/48) | `cursor/artifact-navigation-e553` | `cursor/compiler-info-e553` |
| 6 | feat: add in-IDE OpenAPI preview via JCEF | [#49](https://github.com/nise-nabe/typespec-intellij-plugin/pull/49) | `cursor/jcef-preview-e553` | `cursor/artifact-navigation-e553` |
| 7 | feat: add tspconfig.yaml inspection | [#50](https://github.com/nise-nabe/typespec-intellij-plugin/pull/50) | `cursor/tspconfig-inspection-e553` | `cursor/jcef-preview-e553` |
| 8 | feat: add structure view for TypeSpec files | [#51](https://github.com/nise-nabe/typespec-intellij-plugin/pull/51) | `cursor/structure-view-e553` | `cursor/tspconfig-inspection-e553` |
| 9 | feat: add standalone tsp --server LSP fallback | [#52](https://github.com/nise-nabe/typespec-intellij-plugin/pull/52) | `cursor/standalone-tsp-fallback-e553` | `cursor/structure-view-e553` |
| 10 | feat: extend run configuration with compile CLI options | [#53](https://github.com/nise-nabe/typespec-intellij-plugin/pull/53) | `cursor/run-config-options-e553` | `cursor/standalone-tsp-fallback-e553` |
| 11 | feat: add TypeSpec live templates | [#54](https://github.com/nise-nabe/typespec-intellij-plugin/pull/54) | `cursor/live-templates-e553` | `cursor/syntax-highlighting-e553` |
| 12 | feat: expand project scaffolding templates | [#55](https://github.com/nise-nabe/typespec-intellij-plugin/pull/55) | `cursor/template-expansion-e553` | `cursor/run-config-options-e553` |
| 13 | feat: add TypeSpec syntax highlighting | [#56](https://github.com/nise-nabe/typespec-intellij-plugin/pull/56) | `cursor/syntax-highlighting-e553` | `cursor/template-expansion-e553` |
| 14 | feat: show diff when emitter output changes | [#57](https://github.com/nise-nabe/typespec-intellij-plugin/pull/57) | `cursor/emitter-diff-monitoring-e553` | `cursor/http-client-integration-e553` |
| 15 | feat: generate HTTP Client requests from OpenAPI | [#58](https://github.com/nise-nabe/typespec-intellij-plugin/pull/58) | `cursor/http-client-integration-e553` | `cursor/live-templates-e553` |
| 16 | feat: improve monorepo package resolution | [#59](https://github.com/nise-nabe/typespec-intellij-plugin/pull/59) | `cursor/multi-root-monorepo-e553` | `cursor/emitter-diff-monitoring-e553` |
| 17 | test: expand automated test coverage | [#60](https://github.com/nise-nabe/typespec-intellij-plugin/pull/60) | `cursor/test-infrastructure-e553` | `cursor/trace-output-tab-e553` |
| 18 | feat: add JSON Schema for tspconfig.yaml | [#61](https://github.com/nise-nabe/typespec-intellij-plugin/pull/61) | `cursor/tspconfig-json-schema-e553` | `cursor/multi-root-monorepo-e553` |
| 19 | feat: add trace tab to output tool window | [#62](https://github.com/nise-nabe/typespec-intellij-plugin/pull/62) | `cursor/trace-output-tab-e553` | `cursor/tspconfig-json-schema-e553` |
| 20 | docs: add root README | [#63](https://github.com/nise-nabe/typespec-intellij-plugin/pull/63) | `cursor/readme-e553` | `cursor/test-infrastructure-e553` |

## Individual backlog files

- [feat: add tsp install dependencies action](./44-tsp-install.md)
- [feat: add install global TypeSpec compiler action](./45-install-global-cli.md)
- [feat: add tsp format actions](./46-tsp-format.md)
- [feat: add compiler info display and tsp info action](./47-compiler-info.md)
- [feat: navigate to generated artifacts after emit](./48-artifact-navigation.md)
- [feat: add in-IDE OpenAPI preview via JCEF](./49-jcef-preview.md)
- [feat: add tspconfig.yaml inspection](./50-tspconfig-inspection.md)
- [feat: add structure view for TypeSpec files](./51-structure-view.md)
- [feat: add standalone tsp --server LSP fallback](./52-standalone-tsp-fallback.md)
- [feat: extend run configuration with compile CLI options](./53-run-config-options.md)
- [feat: add TypeSpec live templates](./54-live-templates.md)
- [feat: expand project scaffolding templates](./55-template-expansion.md)
- [feat: add TypeSpec syntax highlighting](./56-syntax-highlighting.md)
- [feat: show diff when emitter output changes](./57-emitter-diff-monitoring.md)
- [feat: generate HTTP Client requests from OpenAPI](./58-http-client-integration.md)
- [feat: improve monorepo package resolution](./59-multi-root-monorepo.md)
- [test: expand automated test coverage](./60-test-infrastructure.md)
- [feat: add JSON Schema for tspconfig.yaml](./61-tspconfig-json-schema.md)
- [feat: add trace tab to output tool window](./62-trace-output-tab.md)
- [docs: add root README](./63-readme.md)
