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

## Cursor Cloud specific instructions

### System prerequisites (not in the Gradle update script)

- **JDK 25** on `PATH` (`java -version` → 25). CI uses Eclipse Temurin; install via [Adoptium apt repo](https://adoptium.net/installation/linux/) (`temurin-25-jdk`) if the VM image only has JDK 21.
- **Xvfb** for sandbox IDE / UI tests: `sudo apt-get install -y xvfb`. Export `XDG_RUNTIME_DIR` (e.g. `/tmp/runtime-cursor`, mode `700`) before Platform tests or `runIde` to avoid `XDG_RUNTIME_DIR is invalid` noise in test output.
- **JetBrains consent** (headless IDE): run `scripts/prepare-jetbrains-consent.sh` before `runIde` or IDE smoke (same as `scripts/run-ide-smoke.sh`).

### Standard commands

| Goal | Command |
|------|---------|
| Lint / compile / unit + headless Platform tests | `./gradlew build` |
| Distributable plugin ZIP | `./gradlew :plugin:buildPlugin` → `plugin/build/distributions/TypeSpecPlugin-*.zip` |
| Sandbox IDE with plugin | `./gradlew :plugin:runIde` (needs `DISPLAY` + Xvfb on Linux) |
| IDE startup smoke | `scripts/run-ide-smoke.sh` (default 900s timeout; first IDEA download can exceed this—raise `STARTUP_TIMEOUT_SECONDS` or confirm via log; see below) |

### Sandbox IDE logs (IntelliJ Platform Gradle Plugin 2.x)

Pinned IDE build is **`262.6653.22`** (`gradle/libs.versions.toml` → `intellij-idea`). IPGP 2.x sandboxes use a **product-version directory** (e.g. `.intellijPlatform/sandbox/plugin/IU-2026.2/log/idea.log`), not the build number and not always `plugin/build/idea-sandbox/.../system/log/idea.log` (Platform Gradle Plugin 1.x layout).

Discover the newest `idea.log` and confirm the plugin loaded:

```bash
IDEA_LOG="$(find .intellijPlatform/sandbox/plugin plugin/build/idea-sandbox build/idea-sandbox \
  \( -path '*/log/idea.log' -o -path '*/system/log/idea.log' \) \
  -print -quit 2>/dev/null)"
test -n "$IDEA_LOG" && grep -F "Loaded custom plugins: TypeSpec Support" "$IDEA_LOG"
```

`scripts/run-ide-smoke.sh` only searches `plugin/build/idea-sandbox/**/system/log/idea.log` and greps `Startup completed` / `IDE started`; on IPGP 2.x or slow EAP startups it may time out while the IDE is healthy—prefer the `find` + grep above.

### What Cloud does not run by default

Full TypeSpec LSP/CLI E2E needs **Node.js**, `npm install` in a sample project, and `@typespec/compiler`—see [docs/cloud-verification.md](docs/cloud-verification.md).
