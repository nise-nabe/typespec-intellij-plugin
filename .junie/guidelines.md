# Junie Guidelines

## Basic

* 基本操作は JetBrains MCP Server を利用してください
* github 上のコードを取得する際は GitHub MCP Server を利用してください

## Build
- 通常のビルド確認では、原則として JetBrains MCP の `build` ツールを利用すること。
- ビルドが必要な場面では、まず `build()` を実行してプロジェクト全体の成否を確認すること。
- `mcp_JetBrains_build_project` は、ユーザーから明示的に指定された場合、`projectPath` や `filesToRebuild` を細かく制御する必要がある場合に限って使うこと。
- テスト実行が目的なら `build` の代用にせず、対応する `run_test` を使うこと。
