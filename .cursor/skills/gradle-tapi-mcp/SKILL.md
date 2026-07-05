---
name: gradle-tapi-mcp
description: >-
  Use the gradle MCP server for token-efficient build verification in this repo.
  Prefer lightweight Tooling API queries before running tasks.
---

# Gradle Tooling API MCP

This repository configures [nise-nabe/gradle-tapi-mcp-server](https://github.com/nise-nabe/gradle-tapi-mcp-server) v0.3.3 in `.cursor/mcp.json`. The JAR is installed by `.cursor/install.sh` to `~/.local/share/gradle-tapi-mcp-server/gradle-tapi-mcp-server.jar`. At MCP server launch, `.cursor/mcp.json` sets `GRADLE_PROJECT_DIR=${workspaceFolder}`.

The MCP server may report `loading` for a few seconds on first use; call `gradle_connection_status` before other tools.

## Workflow (token-efficient)

1. `gradle_connection_status` — confirm connected (`connectedAny: true`)
2. `gradle_get_build_environment` — resolved Gradle/Java versions
3. `gradle_get_project_overview` — module hierarchy (`build-logic`, `core`, `lsp`, `actions`, `inspections`, `plugin`, `ui-test`)
4. `gradle_run_tasks` with `[":plugin:compileKotlin"]` when verification is needed

Avoid `includeTasks=true` and heavy model queries unless necessary. `gradle_run_tasks` omits stdout/stderr by default (`includeOutput=false`). On failure, check `failedTasks` and `buildSummary.failureSummary` before setting `includeOutput=true`.

### Inquiry tools (read-only, fast)

| Tool | Use |
|------|-----|
| `gradle_get_gradle_build` | Composite builds (`build-logic` included build) |
| `gradle_get_java_runtimes` | Daemon Java + toolchain JDKs under `~/.gradle/jdks/` |
| `gradle_get_build_cache_status` | Build cache / parallel / config-cache settings |
| `gradle_get_build_invocations` | Task discovery with `taskNamePrefix` / `taskGroup` filters |
| `gradle_get_project_model` | Task lists with `taskGroup` / `taskNamePrefix` / `maxTasks` |
| `gradle_get_project_publications` | Published artifacts |
| `gradle_list_builds` | Recent MCP builds (recovery when a call times out) |

### Task discovery (root vs submodules)

The workspace root is a thin aggregator; most runnable tasks live on submodules or in the `build-logic` composite build.

| Goal | Prefer |
|------|--------|
| Module tree / composite builds | `gradle_get_project_overview` or `gradle_get_gradle_build` |
| List verification tasks on `:plugin` | `gradle_get_project_model` with `taskGroup: "verification"` and `includeTasks: true` |
| Find compile/test tasks | `gradle_get_build_invocations` with `taskNamePrefix: "compile"` and `includeTasks: true`, or use known paths (`:plugin:compileKotlin`, `:lsp:test`) |

There is no `projectPath` filter on model tools — they return the project tree; apply `taskGroup` / `taskNamePrefix` and read tasks from the relevant child node.

## When to use MCP vs shell

| Scenario | Prefer |
|----------|--------|
| Compile check (`:plugin:compileKotlin`), daemon warm | MCP `gradle_run_tasks` foreground — sub-second to ~2s, clean JSON |
| Compile check, cold start (first MCP build in session) | MCP `gradle_run_tasks` with `background: true` + poll, or shell — foreground often hits MCP client timeout (~60s) while Gradle still runs |
| Full `build` or `:plugin:test` in Cloud | **Shell** `./gradlew --non-interactive build` — IntelliJ tests are long-running and MCP clients often time out (~60s) |
| Scoped module tests (`:lsp:test`, `:core:test`) | Shell `./gradlew --non-interactive :lsp:test` or MCP background + polling |
| MCP server unresponsive / all tools timeout | **Shell** `./gradlew --non-interactive`; Gradle daemon may still be IDLE while MCP is stuck |
| PR / CI parity check | `./gradlew --non-interactive build` |

Do **not** start a second MCP `gradle_run_tasks` or `gradle_run_tests` while one is still running. Overlapping runs cause `InterruptedException`, can leave the IntelliJ test sandbox corrupted, and may wedge the MCP server until it is restarted.

## Running builds and tests

### Default: background + poll for anything that may exceed ~30s

MCP clients commonly time out around 60s. Gradle may finish successfully even when the initiating call returns `Request timed out`.

1. Start with `background: true`:

```json
{
  "tasks": [":plugin:compileKotlin"],
  "background": true
}
```

Or for a single test class:

```json
{
  "testClasses": ["com.example.typespec.TypeSpecPluginDescriptorTest"],
  "background": true
}
```

2. Poll `gradle_get_build_status` with the returned `buildId` until `status` is `succeeded`, `failed`, or `cancelled`.

3. Read `outcome` and `buildSummary` from the poll response. Use `includeProgress: true` for task/test events; set `includeOutput: true` only if you need truncated logs.

**If the start call times out:** run `gradle_list_builds` (if MCP responds) or `ls .gradle/mcp-builds/` and poll the most recent `buildId` with `gradle_get_build_status`.

**Foreground is fine when the daemon and outputs are warm** — e.g. repeated `:plugin:compileKotlin` after a recent build often completes in under 1s.

### Shell fallback (reliable in Cloud)

```bash
./gradlew --non-interactive :plugin:compileKotlin
./gradlew --non-interactive :lsp:test
./gradlew --non-interactive :plugin:test
./gradlew --non-interactive build
```

### Disk recovery when MCP is stuck

Build records persist under `.gradle/mcp-builds/<buildId>/` even when the MCP server stops responding.

| File | Use |
|------|-----|
| `mcp-result.json` | Terminal MCP outcome when Gradle finished but MCP cannot answer |
| `gradle-result.json` | Authoritative Gradle init-script result when present |
| `stdout.log` / `stderr.log` | Full captured output after the build ends |
| `events.ndjson` | Task/test progress events |

If every MCP call times out but `./gradlew` still works:

1. List recent builds: `gradle_list_builds` (if MCP responds) or `ls .gradle/mcp-builds/`
2. Read `.gradle/mcp-builds/<buildId>/mcp-result.json` for `status`, `outcome`, and `buildSummary`
3. Avoid starting new MCP test runs until the environment recovers (restart the MCP server / agent session if needed)

## Repo-specific tips

### Verification commands

| Goal | MCP tool | Example |
|------|----------|---------|
| Full verify | shell (preferred) | `./gradlew --non-interactive build` |
| All plugin tests | shell (preferred) or MCP background | `./gradlew --non-interactive :plugin:test` |
| LSP module tests | shell | `./gradlew --non-interactive :lsp:test` |
| Single test class | `gradle_run_tests` + background, or shell | See running builds section |
| Fast compile gate | `gradle_run_tasks` | `{ "tasks": [":plugin:compileKotlin"] }` — use `background: true` on cold start |

Prefer shell for `:plugin:test` and `build` in Cursor Cloud. Use MCP for lightweight queries and compile checks.

### JDK / toolchain

This repo pins **JDK 25** (JetBrains vendor) in `build-logic/src/main/kotlin/typespec.kotlin-conventions.gradle.kts` and `gradle/libs.versions.toml`. `gradle_get_build_environment` reports the running daemon's Java; use `gradle_get_java_runtimes` for all detected JDKs including toolchain downloads under `~/.gradle/jdks/`.

### IntelliJ test sandbox corruption

If `:plugin:test` fails with many unrelated test errors and a stack trace mentioning `PersistentEnumerator storage corrupted` under `.intellijPlatform/sandbox/plugin/`, the test sandbox index is stale — not an MCP or code regression. This often follows an interrupted MCP test run.

```bash
.cursor/clean-test-sandbox.sh
```

Then rerun `:plugin:test` or `build` via shell or MCP (background + polling).

## Upstream documentation

Full tool reference and advanced workflows live in the upstream repository:

- [README (v0.3.3)](https://github.com/nise-nabe/gradle-tapi-mcp-server/blob/v0.3.3/README.md)
- [gradle-tapi-mcp skill](https://github.com/nise-nabe/gradle-tapi-mcp-server/tree/v0.3.3/skills/gradle-tapi-mcp)
