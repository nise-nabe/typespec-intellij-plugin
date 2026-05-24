# TypeSpec LSP capabilities in IntelliJ

This plugin delegates editor intelligence to the TypeSpec Language Server (`tsp-server.js` from `@typespec/compiler`) via the IntelliJ Platform LSP integration (`com.intellij.modules.lsp`).

## Provided by LSP (no extra plugin code required)

When the language server is running and the project compiler package resolves, the platform typically wires:

| Capability | Notes |
|------------|--------|
| Code completion | `textDocument/completion` |
| Diagnostics | `textDocument/publishDiagnostics` |
| Go to definition | `textDocument/definition` |
| Find references | `textDocument/references` |
| Hover | `textDocument/hover` when supported by server |
| Rename | `textDocument/rename` when supported by server |
| Format document | `textDocument/formatting` when supported by server |
| LSP code actions | Compiler quick fixes via `textDocument/codeAction` |

No additional IntelliJ extension points are required for these features.

## Implemented as plugin actions (VS Code parity)

| VS Code command | IntelliJ action |
|-----------------|-----------------|
| Restart TypeSpec server | **Restart TypeSpec Server** |
| Show output channel | **Show TypeSpec Output** |
| Emit from TypeSpec | **Emit from TypeSpec** (+ run configuration) |
| Create TypeSpec project | **Create TypeSpec Project** |
| Import from OpenAPI 3 | **Import TypeSpec from OpenAPI 3** |
| Preview API documentation | **Preview API Documentation** |

Emit/import/preview use the resolved `@typespec/compiler` CLI (`cmd/tsp.js`) and npm tools where needed, matching the VS Code extension’s CLI-oriented workflows.

## Manual verification checklist

1. Open a `.tsp` file with `@typespec/compiler` installed and LSP enabled.
2. Confirm completion and diagnostics appear.
3. Use **Rename** (Shift+F6) on a symbol; use **Reformat Code** (Ctrl+Alt+L) for format-if-supported.
4. Trigger a compiler diagnostic and check for intention/light bulb quick fixes.
5. Use **Tools | TypeSpec** actions for restart, output, emit, init, import, and preview.
