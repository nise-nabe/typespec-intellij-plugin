---
name: gradle-tapi-mcp
description: >-
  Use the gradle MCP server for token-efficient build verification in this repo.
  Prefer lightweight Tooling API queries before running tasks.
---

# Gradle Tooling API MCP

This repository configures [nise-nabe/gradle-tapi-mcp-server](https://github.com/nise-nabe/gradle-tapi-mcp-server) v0.13.0 in `.cursor/mcp.json`. The JAR is installed by `.cursor/install.sh` to `~/.local/share/gradle-tapi-mcp-server/gradle-tapi-mcp-server.jar`. At MCP server launch, `.cursor/mcp.json` sets `GRADLE_PROJECT_DIR=${workspaceFolder}`.

The MCP server may report `loading` for a few seconds on first use; call `gradle_connection_status` before other tools.

## Workflow (token-efficient)

1. `gradle_connection_status` — confirm connected. `runtimeStackAvailable=true` already shows `gradleVersion` / `javaHome`; when false, call with `refresh: true` or use `gradle_get_build_environment`
2. `gradle_get_build_environment` — resolved Gradle/Java versions
3. `gradle_get_project_overview` — module hierarchy (`build-logic`, `core`, `lsp`, `actions`, `inspections`, `plugin`, `ui-test`)
4. Optional: `gradle_get_dependency_resolution` — omit `configuration` to list resolvable/consumable names, or pass a resolvable `configuration` for the resolved graph without running report tasks
5. `gradle_run_tasks` with `[":plugin:compileKotlin"]` when verification is needed

Avoid `includeTasks=true` and heavy model queries unless necessary. `gradle_run_tasks` omits stdout/stderr by default (`includeOutput=false`). On failure, check `failedTasks`, `testFailures`, `buildSummary.failureSummary`, and `problems` before setting `includeOutput=true`.

### Inquiry tools (read-only, fast)

| Tool | Use |
|------|-----|
| `gradle_get_gradle_build` | Composite builds (`build-logic` included build); discover `buildTreePath` identity paths |
| `gradle_get_java_runtimes` | Daemon Java + toolchain JDKs (`includeToolchains: false` for daemon only) |
| `gradle_get_help` | Gradle CLI help text (`gradle --help` equivalent; Gradle 9.4+) |
| `gradle_get_build_cache_status` | Build cache / parallel / config-cache settings (`probeConfigurationCache: true` to probe) |
| `gradle_get_build_invocations` | Task discovery with `taskNamePrefix` / `taskGroup` filters |
| `gradle_get_project_model` | Task lists with `taskGroup` / `taskNamePrefix` / `maxTasks` |
| `gradle_get_project_publications` | Published artifacts |
| `gradle_get_dependency_resolution` | Resolved dependency graph via `ResolutionResult` (no task run) |
| `gradle_list_builds` | Recent MCP builds (recovery when a call times out) |

### Task discovery (root vs submodules)

The workspace root is a thin aggregator; most runnable tasks live on submodules or in the `build-logic` composite build.

| Goal | Prefer |
|------|--------|
| Module tree / composite builds | `gradle_get_project_overview` or `gradle_get_gradle_build` |
| List verification tasks on `:plugin` | `gradle_get_project_model` with `taskGroup: "verification"` and `includeTasks: true` |
| Find compile/test tasks | `gradle_get_build_invocations` with `taskNamePrefix: "compile"` and `includeTasks: true`, or use known paths (`:plugin:compileKotlin`, `:lsp:test`) |

Scope model queries with `projectPath` (e.g. `:plugin`) for a subproject subtree, or `buildTreePath` (e.g. `:buildSrc`) for included builds — never both. Cap large trees with `maxDepth` / `maxChildren`. On Gradle 9.4+ a partially failing build returns `partial: true` + capped `failures[]` instead of an error.

## Multiple projects

One MCP process can hold several Gradle project connections.

1. `gradle_connect` per project root — existing connections stay open; rejected while a build runs for the same `projectDirectory`
2. Pass `projectDirectory` on query/build tools; omitted falls back to the default connected project / `GRADLE_PROJECT_DIR`
3. `gradle_connection_status` with no args returns `connections[]` + `defaultProjectDirectory`
4. `gradle_disconnect` with `projectDirectory` closes one project (cancelling its builds); omit to close all

## When to use MCP vs shell

| Scenario | Prefer |
|----------|--------|
| Compile check (`:plugin:compileKotlin`), daemon warm | MCP `gradle_run_tasks` foreground — sub-second to ~2s, clean JSON |
| Compile check, cold start (first MCP build in session) | MCP `gradle_run_tasks` with `background: true` + poll, or shell — foreground often hits MCP client timeout (~60s) while Gradle still runs |
| Full `build` or `:plugin:test` in Cloud | **Shell** `./gradlew --non-interactive build` — IntelliJ tests are long-running and MCP clients often time out (~60s) |
| Scoped module tests (`:lsp:test`, `:core:test`) | Shell `./gradlew --non-interactive :lsp:test` or MCP background + polling |
| MCP server unresponsive / all tools timeout | **Shell** `./gradlew --non-interactive`; Gradle daemon may still be IDLE while MCP is stuck |
| PR / CI parity check | `./gradlew --non-interactive build` |

Do **not** run shell `./gradlew` on the same checkout while an MCP build is active — IntelliJ Platform `:plugin:test` runs compete for the same test sandbox and can hang or corrupt state.

## Running builds and tests

### Default: background + poll for anything that may exceed ~30s

MCP clients commonly time out around 60s. Foreground `gradle_run_tasks` / `gradle_run_tests` auto-detach after ~45s (`detached: true` + `buildId`) when the run outlives the client request. For known-slow builds, start with `background: true`:

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

Poll `gradle_get_build_status` with the returned `buildId` until `status` is `succeeded`, `failed`, or `cancelled`. Read `outcome` and `buildSummary`; use `includeProgress: true` for task/test events and `includeOutput: true` only for truncated logs.

**Concurrency and queueing:** only one MCP build **runs** per `projectDirectory` at a time. A second `background: true` call enqueues (`status: queued`, max 3 per project; `queueIfBusy` defaults true for background). Foreground overlap or `queueIfBusy: false` returns `BUILD_ALREADY_RUNNING` with `activeBuildId` (or `activeBuildIds` on global pool saturation); a saturated queue returns `BUILD_QUEUE_FULL`. Use `gradle_cancel_build` with the `buildId` to stop an unwanted run, then poll until terminal.

**If the start call times out:** run `gradle_list_builds` (if MCP responds) or `ls .gradle/mcp-builds/` and poll the most recent `buildId` with `gradle_get_build_status`.

**Polling options:** while `status` is `running`, poll without `includeOutput`. For live logs use `sinceStdoutOffset` / `sinceStderrOffset` to get `stdoutDelta` / `stderrDelta` instead of re-reading the full tail. `waitUntilComplete: true` adds a short server-side wait (`waitTimeoutMs` default 30s, max 60s) independent of the MCP client timeout — treat `waitTimedOut` as "still running, poll again", not server death.

**On terminal failure:** read `failureCategory` (`TEST`, `GRADLE_TASK`, `TOOLING_CONNECTION`, `CANCELLED`), `testFailures`, `buildSummary.failureSummary`, and `failedTasks`. Failed `GRADLE_TASK` builds include a capped `problems` array by default (with `originLocations` path/line); re-poll with `includeProblems: true` if missing — do not re-run the task via CLI for compiler output (`includeOutput` tails often miss Kotlin `Compilation error. See log for more details`).

### Test selectors

`gradle_run_tests` requires exactly one of `testClasses`, `testMethods`, or `includePattern(s)` (patterns also require `tasks`). A selector-less call is `INVALID_ARGUMENT` — run a whole suite with `gradle_run_tasks`.

| Goal | Arguments |
|------|-----------|
| One Test task + class list | `taskPath` + `testClasses` |
| One Test task + method map | `taskPath` + `testMethods` (`{"com.example.FooTest": ["shouldWork"]}`) |
| Custom `JvmTestSuite` (e.g. `fastTest`) | `taskPath: ":mod:fastTest"`, or `tasks: [":mod:fastTest"]` + `includePatterns` |
| Several Test tasks in one MCP build | `tasks: [":mod:test", ":mod:fastTest"]` + `includePatterns` |
| Whole Test task / suite | `gradle_run_tasks` with `tasks: [":mod:test"]` |

Batch multiple classes/methods in **one** `gradle_run_tests` call — parallel same-project calls just enqueue. Unscoped `testClasses` / `testMethods` on multi-project builds infer `taskPath` when unambiguous (`taskPathInferred: true`); otherwise `INVALID_ARGUMENT` with `suggestedTaskPaths` (full list via `gradle_get_project_model` + `includeTasks=true`).

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

## Dependency sources (index → search → read)

Locate types/symbols inside dependency `*-sources.jar` without browsing `~/.gradle`.

1. **Index** — `gradle_index_dependency_sources` (required before searching that `tokenMode`). Idea keep-set by default; scope with `projectPath`, or pass explicit `artifacts[]` / `sourcePaths[]`. `tokenMode`: `all` (default) or `idents` (faster on large first indexes). `background: true` returns `indexId`; foreground auto-detaches after ~45s. Poll `gradle_get_dependency_sources_index_status`
2. **Search** — `gradle_search_dependency_sources` (`query`) or `gradle_search_dependency_sources_multi` (`queries`). Exact simple-name only; `tokenMode` must match the index (no silent reindex). `limit` / `perQueryLimit` omit = unlimited
3. **Read** — `gradle_read_dependency_source` with a hit's `gav` + `path` (optional `line` + `contextLines`, default 10). Pass `sourceRoot` explicitly only when the index/cache cannot resolve it

`artifacts[]` accepts GAVs not in the project graph (e.g. another Kotlin/plugin version); add `downloadSources: true` to fetch missing `*-sources.jar` (Maven Central default; `sourcesRepositories` for mirrors). Never list `gradleUserHome` / `~/.gradle/caches` / `wrapper/dists` / `jdks` to find jars — the server resolves them.

## MCP resources (optional)

`gradle-tapi://{project-root}/…` resources wrap the same handlers (`connection/status`, `environment`, `overview`, `builds/{buildId}/status`, `builds/recent`). They are host-side context — keep calling tools; do not skip `tools/call` because templates exist.

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

- [README (v0.13.0)](https://github.com/nise-nabe/gradle-tapi-mcp-server/blob/v0.13.0/README.md)
- [gradle-tapi-mcp skill](https://github.com/nise-nabe/gradle-tapi-mcp-server/tree/v0.13.0/plugins/gradle-tapi-mcp/skills/gradle-tapi-mcp) (`SKILL.md` + `reference.md` tool reference)
