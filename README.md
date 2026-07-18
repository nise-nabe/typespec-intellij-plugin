# TypeSpec Support for IntelliJ Platform

IntelliJ Platform plugin for [TypeSpec](https://typespec.io/) (`.tsp`) development. Targets **IntelliJ IDEA 2026.2** (`262.x`). Build with **JDK 25**.

## Features

### Language Server (LSP)

When `@typespec/compiler` is installed in the project, the plugin starts `tsp-server.js` and delegates to the Platform LSP integration:

- Code completion, diagnostics, hover, go to definition, and find references
- Rename, format, and compiler quick fixes (when supported by the server)

### Workflow actions (Tools | TypeSpec)

| Action | Description |
|--------|-------------|
| Restart TypeSpec Server | Restart the language server |
| Show TypeSpec Output | Open the output tool window |
| Emit from TypeSpec | Compile / emit via the TypeSpec CLI |
| Create TypeSpec Project | Scaffold with `tsp init` |
| Install TypeSpec Dependencies | Run `tsp install` |
| Import TypeSpec from OpenAPI 3 | Generate TypeSpec from OpenAPI |
| Preview API Documentation | Open Swagger UI preview in the browser |

Editor and project view context menus expose the same actions where relevant.

### Run configuration

**TypeSpec Compile** run configuration with a context-menu producer for `.tsp` files and `tspconfig.yaml`.

### Inspections and configuration

- `package.json` metadata for TypeSpec extension packages (TPKG001–TPKG005) with quick fixes
- JSON Schema validation and completion for `tspconfig.yaml`
- Project settings for the compiler package path and enabling/disabling the language server

## Prerequisites

1. **Node.js** and npm (configure in IntelliJ: Settings | Languages & Frameworks | Node.js)
2. **`@typespec/compiler`** in your TypeSpec project (`npm install @typespec/compiler`)

## Setup

1. Install the plugin (build from source or install the distribution ZIP).
2. Open a TypeSpec project with `tspconfig.yaml` and `.tsp` files.
3. Optionally set the compiler package path under **Settings | Languages & Frameworks | TypeSpec** (the language server is enabled by default; disable it there if needed).

## Build from source

```bash
./gradlew --non-interactive build
```

Distribution ZIP:

```bash
./gradlew --non-interactive :plugin:buildPlugin
```

Output: `plugin/build/distributions/TypeSpecPlugin-*.zip`

## Verification

See [docs/lsp-capabilities.md](docs/lsp-capabilities.md) and [docs/cloud-verification.md](docs/cloud-verification.md).

## Project structure

| Module | Purpose |
|--------|---------|
| `core` | File type, settings state, package resolution |
| `lsp` | Language server integration and settings UI |
| `actions` | Tools actions, CLI workflows, run configurations |
| `inspections` | `package.json` inspections and `tspconfig.yaml` JSON Schema |
| `plugin` | `plugin.xml` wiring and distribution |
| `ui-test` | Remote Robot UI smoke tests |
| `build-logic` | Shared Gradle convention plugins |
