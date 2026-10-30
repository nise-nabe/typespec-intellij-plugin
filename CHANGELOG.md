# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Fixed

- Answer `workspace/configuration` for the `typespec` section so tsp-server receives `lsp.emit` and `entrypoint` during live checking
- Create Project always runs `tsp init` non-interactively (`-y` and `--project-name`, defaulting to the target folder name) and passes `--template` together with a template index URL, so URL init no longer errors and the CLI is not left waiting on prompts
- Start the standalone language server with `tsp-server --stdio`, or `node` and `cmd/tsp-server.js --stdio`, instead of the invalid `tsp --server` command

### Changed

- Bump the default downloadable `@typespec/compiler` from 1.13.0 to 1.17.0 when a project has no local compiler (CLI and language server)
- Create Project template ids match compiler 1.17 scaffolding: `empty`, `rest`, `library-ts`, and `emitter-ts`
- Bump pinned IDEA SDK to IntelliJ IDEA 2026.2.3 (`262.10968.63`)
- Suppress a 2026.2.2 Ultimate headless-test `PluginException` from `com.intellij.modules.ultimate` (`B.B.B.B.s` colliding with `product-backend.jar`)

### Added

- Project settings for the emitters used in live checking (`<config:defaults>` or emitter names) and for entrypoint file names
- Create Project can add emitters with repeated `--template-emitters` flags: checkboxes for the built-in `rest` template, and a comma-separated list when a template index URL is set
- Support installing, updating, enabling, and disabling the plugin without restarting the IDE (dynamic plugin loading)

## [0.2.0] - 2026-08-15

### Fixed

- Fixed plugin startup failure when opening `.tsp` files caused by duplicate `projectSettings` registration of `TypeSpecServiceSettings` (`configurationSchemaKey must be specified`)
- Fixed a crash (`IllegalAccessError`) when opening `.tsp` files that activated the TypeSpec Language Server

### Changed

- Target **IntelliJ IDEA 2026.2** (platform build 262.0 and later); the plugin build uses JDK 25 to match the JBR bundled with IDEA 2026.2
- Bump pinned IntelliJ Platform Gradle Plugin to 2.18.1 and IDEA SDK to 2026.2 stable (`262.8665.258`)

### Added

- Added lexer-based syntax highlighting for TypeSpec keywords, decorators, strings, comments, and numbers in `.tsp` files
- Added VS Code parity workflow actions: restart TypeSpec server, show output, emit from TypeSpec, create project (`tsp init`), install TypeSpec dependencies (`tsp install`), import from OpenAPI 3, and preview API documentation (Swagger UI in browser)
- Added TypeSpec compile run configuration with context-menu producer for `.tsp` files
- Added TypeSpec output tool window for CLI and workflow logs
- Documented LSP-provided editor capabilities in `docs/lsp-capabilities.md`
- Added project settings UI to configure the `@typespec/compiler` package path and enable or disable the TypeSpec Language Server
- Added Language Services status bar widget integration with a link to TypeSpec settings
- Added balloon notification when `@typespec/compiler` cannot be resolved while opening `.tsp` files
- Added TypeSpec `package.json` inspections for likely TypeSpec extension packages, reporting missing recommended metadata:
  - `"type": "module"` (TPKG001)
  - Node.js entry point in `"main"` (TPKG002)
  - TypeSpec entry point in `exports["."].typespec` (TPKG003)
  - `@typespec/compiler` declared in `peerDependencies` instead of `dependencies` or `devDependencies` (TPKG004)
  - Informational hint to prefer `exports["."].typespec` over fallback entry points such as `"tspMain"` or `"main"` (TPKG005)
- Added quick fixes to apply recommended TypeSpec package metadata and to move `@typespec/compiler` into `peerDependencies`
- Added embedded JSON Schema validation and completion for `tspconfig.yaml` project configuration keys

## [0.1.1] - 2026-05-05

### Added

- Added a bundled spelling dictionary entry for `typespec`

## [0.1.0] - 2026-03-29

### Added

- Initial release of TypeSpec support for IntelliJ Platform IDEs
- Added support for `.tsp` files
- Added TypeSpec Language Server-based code completion, diagnostics, and navigation
