---
name: plugin-release
description: >-
  Cut a release of the TypeSpec IntelliJ plugin: bump the version in
  plugin/build.gradle.kts, update CHANGELOG.md (Keep a Changelog format, rendered
  into changeNotes by the changelog Gradle plugin), run
  ./gradlew --non-interactive build, and produce the distribution ZIP via
  :plugin:buildPlugin. Use when the user asks to
  release, tag a version, or build the installable plugin ZIP.
---

# Plugin release

This repo has **no automated release pipeline** — no release workflow in
`.github/workflows`, no `publishPlugin`/Marketplace config, no signing, and no
git tags. Releases are manual: bump the version, update the changelog, verify
the build, and produce a distribution ZIP for manual install.

## Steps

1. **Bump the version** in `plugin/build.gradle.kts` (`version = "x.y.z"`).

2. **Update `CHANGELOG.md`** (Keep a Changelog format):
   - Rename `## [Unreleased]` to `## [x.y.z] - YYYY-MM-DD` and add a fresh empty
     `## [Unreleased]` section above it.
   - The `org.jetbrains.changelog` plugin renders the section matching
     `project.version` into `changeNotes` (falls back to `[Unreleased]`), so the
     section header must exactly match the new version string.

3. **Verify** with the standard CI gate:

   ```bash
   ./gradlew --non-interactive build
   ```

4. **Build the distribution ZIP**:

   ```bash
   ./gradlew --non-interactive :plugin:buildPlugin
   ```

   Output: `plugin/build/distributions/TypeSpecPlugin-<version>.zip`

5. **Distribute manually**: install the ZIP in the IDE via
   Settings | Plugins | gear icon | "Install Plugin from Disk".

## Not configured (do not assume)

- JetBrains Marketplace publishing (`publishPlugin`) — not set up.
- Plugin signing (`signPlugin`) — not set up.
- Git tags / GitHub Releases — the repo has no tags; past releases were not
  tagged. Ask the user before creating tags.
- CI release job — only `.github/workflows/main.yml` (build) exists.
