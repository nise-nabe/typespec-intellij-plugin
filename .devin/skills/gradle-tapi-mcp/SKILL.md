---
name: gradle-tapi-mcp
description: >-
  Use the gradle MCP server for token-efficient build verification in this repo.
  Prefer lightweight Tooling API queries before running tasks.
---

# Gradle Tooling API MCP

The canonical skill content lives at `.cursor/skills/gradle-tapi-mcp/SKILL.md`
(kept in the `.cursor/` directory so all agent tools share one source). Read
that file and follow its workflow: `gradle_connection_status` first, then
`gradle_get_build_environment`, `gradle_get_project_overview`, and
`gradle_run_tasks` / `gradle_get_build_status` for builds. Prefer shell
`./gradlew --non-interactive` for `build` and `:plugin:test`.
