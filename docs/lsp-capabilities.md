# TypeSpec LSP capabilities in IntelliJ

This plugin delegates editor intelligence to the TypeSpec Language Server (`tsp-server.js` from `@typespec/compiler`) via the IntelliJ Platform LSP integration (`com.intellij.modules.lsp`).

**Verification legend**

| Status | Meaning |
|--------|---------|
| **Automated** | Covered by `./gradlew build` (unit or headless Platform tests) |
| **Manual** | Requires local IDE (`:plugin:runIde`) or real `@typespec/compiler` + Node |
| **Planned** | Not yet automated; candidate for Platform or UI tests |

## Provided by LSP (no extra plugin code required)

When the language server is running and the project compiler package resolves, the platform typically wires:

| Capability | Notes | Verification |
|------------|--------|--------------|
| Code completion | `textDocument/completion` | Manual |
| Diagnostics | `textDocument/publishDiagnostics` | Manual |
| Go to definition | `textDocument/definition` | Manual |
| Find references | `textDocument/references` | Manual |
| Hover | `textDocument/hover` when supported by server | Manual |
| Rename | `textDocument/rename` when supported by server | Manual |
| Format document | `textDocument/formatting` when supported by server | Manual |
| LSP code actions | Compiler quick fixes via `textDocument/codeAction` | Manual |

No additional IntelliJ extension points are required for these features.

**Related automated coverage (not full LSP E2E):** LSP activation rules and package resolution — `TypeSpecLspServerActivationRuleTest`, `TypeSpecLspPackageResolutionPlatformTest`, `TypeSpecLspServerLoaderTest`.

## Implemented as plugin actions (VS Code parity)

| VS Code command | IntelliJ action | Action ID | Verification |
|-----------------|-----------------|-----------|--------------|
| Restart TypeSpec server | **Restart TypeSpec Server** | `TypeSpec.RestartServer` | Automated (registration + visibility) |
| Show output channel | **Show TypeSpec Output** | `TypeSpec.ShowOutput` | Automated (registration + visibility) |
| Emit from TypeSpec | **Emit from TypeSpec** (+ run configuration) | `TypeSpec.EmitFromTypeSpec` | Automated (registration); CLI args — unit tests |
| Create TypeSpec project | **Create TypeSpec Project** | `TypeSpec.CreateProject` | Automated (registration) |
| Import from OpenAPI 3 | **Import TypeSpec from OpenAPI 3** | `TypeSpec.ImportFromOpenApi` | Automated (registration) |
| Preview API documentation | **Preview API Documentation** | `TypeSpec.PreviewOpenApi` | Automated (registration); browser — Manual |

Emit/import/preview use the resolved `@typespec/compiler` CLI (`cmd/tsp.js`) and npm tools where needed, matching the VS Code extension’s CLI-oriented workflows.

## Inspections and project metadata

| Feature | Verification |
|---------|--------------|
| `package.json` inspection and quick fixes | Automated — `TypeSpecPackageJsonFixTest`, `TypeSpecPackageRulesTest` |
| TypeSpec project settings service | Automated — `TypeSpecActivationHelperSettingsTest` |

## UI and tool window

| Feature | Verification |
|---------|--------------|
| TypeSpec Output tool window factory | Planned (UI test) |
| Tools \| TypeSpec menu group | Automated (action IDs); UI smoke — `ui-test` / Manual |

## Manual verification checklist

Use after `./gradlew build` passes and before release. Run locally with `:plugin:runIde` and a project that has `@typespec/compiler` installed.

1. Open a `.tsp` file with `@typespec/compiler` installed and LSP enabled.
2. Confirm completion and diagnostics appear.
3. Use **Rename** (Shift+F6) on a symbol; use **Reformat Code** (Ctrl+Alt+L) for format-if-supported.
4. Trigger a compiler diagnostic and check for intention/light bulb quick fixes.
5. Use **Tools \| TypeSpec** actions for restart, output, emit, init, import, and preview.

See [cloud-verification.md](cloud-verification.md) for Cursor Cloud and CI workflows.
