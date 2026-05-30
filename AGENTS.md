# Agent instructions (Cursor Cloud)

## Verify plugin changes

1. Ensure **JDK 25** is available (`java -version`).
2. Run the standard gate (same as CI):

   ```bash
   ./gradlew build
   ```

3. On failure, run scoped tests (`./gradlew :lsp:test`, etc.). See [docs/cloud-verification.md](docs/cloud-verification.md).

4. Summarize results against the verification matrix in [docs/lsp-capabilities.md](docs/lsp-capabilities.md).

## Do not use by default on Cloud

- `:plugin:runIde` — requires a display; use `scripts/run-ide-smoke.sh` or the **Run IDE smoke** workflow instead.
- Full LSP/browser manual checklist — local IDE only.

## Optional deeper checks

| Goal | Command / workflow |
|------|-------------------|
| IDE loads with plugin | `scripts/run-ide-smoke.sh` or `.github/workflows/run-ide-smoke.yml` |
| Tools menu UI smoke | `.github/workflows/run-ui-tests.yml` or steps in [docs/cloud-verification.md](docs/cloud-verification.md) |

Target platform: IntelliJ IDEA **2026.2** (`262.x`), JDK **25**.
