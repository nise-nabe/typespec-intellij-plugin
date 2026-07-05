# TypeSpec Support for IntelliJ Platform

IntelliJ Platform plugin for [TypeSpec](https://typespec.io/) (`.tsp`) development. Targets **IntelliJ IDEA 2026.2** (`262.x`) with **JDK 25**.

## Features

### Language Server (LSP)

When `@typespec/compiler` is installed in the project, the plugin starts `tsp-server.js` and delegates to the Platform LSP integration:

- Code completion, diagnostics, hover, go to definition, find references
- Rename, format, and compiler quick fixes (when supported by the server)
- Standalone `tsp --server` fallback when Node-based startup is unavailable

### Workflow actions (Tools | TypeSpec)

| Action | Description |
|--------|-------------|
| Restart TypeSpec Server | Restart the language server |
| Show TypeSpec Output | Open the output tool window |
| Install TypeSpec Dependencies | Run `tsp install` |
| Install TypeSpec Compiler Globally | Run `npm install -g @typespec/compiler` |
| Format TypeSpec Project | Run `tsp format .` |
| Show Compiler Info | Run `tsp info` |
| Create TypeSpec Project | Scaffold with `tsp init` |
| Import from OpenAPI 3 | Generate TypeSpec from OpenAPI |
| Preview API Documentation | Swagger UI preview (JCEF or browser) |

Editor context menu also provides emit, format, show generated output, HTTP client generation, and preview.

### Run configuration

**TypeSpec Compile** supports watch mode, dry-run, no-emit, stats, trace, and warn-as-error flags.

### Inspections

- `package.json` metadata for TypeSpec extension packages (TPKG001–TPKG005)
- `tspconfig.yaml` project configuration (TCFG001–TCFG003)
- JSON Schema assistance for `tspconfig.yaml`

### IDE enhancements

- Syntax highlighting and live templates for `.tsp` files
- Structure view for top-level declarations
- Monorepo-aware package resolution across content roots
- Emitter output diff after compile
- Trace tab in the TypeSpec output tool window

## Prerequisites

1. **Node.js** and npm (configure in IntelliJ: Settings | Languages & Frameworks | Node.js)
2. **`@typespec/compiler`** in your TypeSpec project (`npm install @typespec/compiler`)

## Setup

1. Install the plugin (build from source or install the distribution ZIP).
2. Open a TypeSpec project with `tspconfig.yaml` and `.tsp` files.
3. Configure the compiler package path if needed: **Settings | Languages | TypeSpec**.
4. Ensure the TypeSpec Language Server is **enabled** in project settings.

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
| `core` | File type, settings, package resolution, syntax highlighting |
| `lsp` | Language server integration and settings UI |
| `actions` | Tools actions, CLI workflows, run configurations |
| `inspections` | `package.json` and `tspconfig.yaml` inspections |
| `plugin` | `plugin.xml` wiring and distribution |

## License

See repository license file.
