# Cloud verification

This document describes how to verify the TypeSpec IntelliJ plugin in cloud agent environments (**Cursor Cloud**, **Devin Cloud**) and other headless Linux environments.

Environment setup is tool-specific: Cursor Cloud runs `.cursor/install.sh` via `.cursor/environment.json`; Devin Cloud builds its snapshot from `.devin/blueprint.yaml`. Everything below applies regardless of the tool.

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

## What cloud agents can and cannot verify

| Layer | Command / approach | Cloud-friendly |
|-------|------------------|----------------|
| Compile + unit tests | `./gradlew --non-interactive build` | Yes |
| Headless Platform tests | Included in `build` | Yes |
| Sandbox IDE startup | `scripts/run-ide-smoke.sh` | Partially — IDE starts, but the plugin cannot load without a license (see below) |
| UI automation | `:plugin:runIdeForUiTests` + `./gradlew :ui-test:test` (Remote Robot) | No — requires a licensed sandbox IDE (local only) |
| Interactive LSP / browser preview | Local `:plugin:runIde` | No (needs desktop IDE + Node) |

### Sandbox licensing (local only)

Since the 2025.3 unified IntelliJ IDEA distribution, an unlicensed sandbox disables
`com.intellij.modules.ultimate`. NodeJS/JavaScript then fail to load, and this plugin
(which `<depends>` on them) is excluded as well — so `runIde`/`runIdeForUiTests`
verify nothing about the plugin until the sandbox is licensed once:

1. `./gradlew :plugin:runIde` (or `:plugin:runIdeForUiTests` for UI tests)
2. In the sandbox IDE: **Help | Manage Licenses** (or the startup dialog) → Activate.
   Ultimate plugins then load dynamically; no restart is required.
3. The key persists in `.intellijPlatform/sandbox/plugin/<ver>/config_<task>/idea.key`,
   so later runs stay licensed. Each `runIde*` variant has its own config dir.
   Alternatively, point IPGP's `subscriptionKey` property at an existing `idea.key`.

CI and cloud agents have no license, so plugin-load and UI verification are
local-only. `run-ide-smoke.sh` still passes on CI but only proves the IDE starts.

See [lsp-capabilities.md](lsp-capabilities.md) for a per-feature verification matrix.

## Optional: IDE startup smoke

Confirms the sandbox IDE starts and prints a warning if the plugin did not load
(unlicensed sandbox — see "Sandbox licensing"):

```bash
./scripts/run-ide-smoke.sh
```

Or trigger the GitHub Actions workflow **Run IDE smoke** (`.github/workflows/run-ide-smoke.yml`).

First run downloads IntelliJ IDEA (~several GB) and may take more than 10 minutes.

## Optional: UI tests (Remote Robot) — local only

UI tests live in the `ui-test` module. They require a running **licensed** sandbox
IDE with the robot-server plugin (see "Sandbox licensing" above). Feature checks
skip when the robot server is unreachable or the sandbox is unlicensed.

```bash
# Local (real display; auto-starts runIdeForUiTests if needed):
./scripts/run-ui-tests-local.sh

# Or manually:
./gradlew --non-interactive :plugin:runIdeForUiTests &   # activate once if first run
./gradlew --non-interactive :ui-test:test -Drobot.server.url=http://127.0.0.1:8082
```

The CI workflow (`.github/workflows/run-ui-tests.yml`, via `scripts/run-ui-tests-ci.sh`)
still runs, but on an unlicensed runner only robot-server reachability is verified.

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
