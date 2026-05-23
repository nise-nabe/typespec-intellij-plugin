# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added

- Added TypeSpec `package.json` inspections for likely TypeSpec extension packages, reporting missing recommended metadata:
  - `"type": "module"` (TPKG001)
  - Node.js entry point in `"main"` (TPKG002)
  - TypeSpec entry point in `exports["."].typespec` (TPKG003)
  - `@typespec/compiler` declared in `peerDependencies` instead of `dependencies` or `devDependencies` (TPKG004)
  - Informational hint to prefer `exports["."].typespec` over fallback entry points such as `"tspMain"` or `"main"` (TPKG005)
- Added quick fixes to apply recommended TypeSpec package metadata and to move `@typespec/compiler` into `peerDependencies`

## [0.1.1] - 2026-05-05

### Added

- Added a bundled spelling dictionary entry for `typespec`

## [0.1.0] - 2026-03-29

### Added

- Initial release of TypeSpec support for IntelliJ Platform IDEs
- Added support for `.tsp` files
- Added TypeSpec Language Server-based code completion, diagnostics, and navigation
