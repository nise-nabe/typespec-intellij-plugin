# Cursor Cloud verification

This document describes how to verify the TypeSpec IntelliJ plugin in **Cursor Cloud** and other headless Linux environments.

## Standard gate (every change)

Use the same command as [`.github/workflows/main.yml`](../.github/workflows/main.yml):

```bash
# Requires JDK 25 on PATH (Temurin or JetBrains Runtime)
java -version   # should report 25

# Gradle 9.6+: avoid console prompts in headless / agent / CI runs
./gradlew --non-interactive build
```

This compiles all modules, runs unit and headless Platform tests (`BasePlatformTestCase`), and builds the distributable plugin.

### If the build fails

Run module-scoped tests to narrow the failure:

```bash
./gradlew --non-interactive :core:test
./gradlew --non-interactive :lsp:test
./gradlew --non-interactive :actions:test
./gradlew --non-interactive :inspections:test
./gradlew --non-interactive :plugin:test
```

## What Cloud can and cannot verify

| Layer | Command / approach | Cloud-friendly |
|-------|------------------|----------------|
| Compile + unit tests | `./gradlew --non-interactive build` | Yes |
| Headless Platform tests | Included in `build` | Yes |
| Sandbox IDE startup | `scripts/run-ide-smoke.sh` | Yes (xvfb, slow first run) |
| UI automation | `:plugin:runIdeForUiTests` + `./gradlew :ui-test:test` (Remote Robot) | Yes (xvfb + `runIdeForUiTests`, manual/CI workflow) |
| Interactive LSP / browser preview | Local `:plugin:runIde` | No (needs desktop IDE + Node) |

See [lsp-capabilities.md](lsp-capabilities.md) for a per-feature verification matrix.

## Optional: IDE startup smoke

Confirms the plugin loads in a sandbox IDE (no UI interaction):

```bash
./scripts/run-ide-smoke.sh
```

Or trigger the GitHub Actions workflow **Run IDE smoke** (`.github/workflows/run-ide-smoke.yml`).

First run downloads IntelliJ IDEA (~several GB) and may take more than 10 minutes.

## Optional: UI tests (Remote Robot)

UI tests live in the `ui-test` module. They require a running IDE with the robot-server plugin:

```bash
# Terminal 1 (Linux): virtual display + IDE for UI tests
export DISPLAY=:99
Xvfb :99 -screen 0 1920x1080x24 &
./scripts/prepare-jetbrains-consent.sh
./gradlew --non-interactive :plugin:runIdeForUiTests &

# Wait until http://127.0.0.1:8082 responds, then:
./gradlew --non-interactive :ui-test:test -Drobot.server.url=http://127.0.0.1:8082
```

Or run `./scripts/run-ui-tests-ci.sh` locally, or use the **Run UI tests** workflow (`.github/workflows/run-ui-tests.yml`).

## Local full manual check

On a developer machine with IntelliJ IDEA 2026.2+:

```bash
./gradlew :plugin:runIde
```

Follow the checklist in [lsp-capabilities.md](lsp-capabilities.md) (items marked **Manual**).

## Environment notes

1. **JDK 25** — CI uses Eclipse Temurin 25; local conventions prefer JetBrains Runtime via Gradle toolchain.
2. **No display** — `runIde` / UI tests need `xvfb` on Linux.
3. **JetBrains consent** — Pre-create `~/.local/share/JetBrains/consentOptions/accepted` (see `scripts/prepare-jetbrains-consent.sh`) so headless IDE is not blocked by dialogs.
4. **Real TypeSpec LSP** — Automated tests use fixtures; end-to-end LSP with `@typespec/compiler` requires Node/npm and a sample project (local manual).
