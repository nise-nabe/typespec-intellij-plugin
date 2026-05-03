# Junie Guidelines

## Basic

* github 上のコードを取得する際は docker-mcp-proxy 上の `get_file_contents` を利用してください

## Build
- ビルドが必要な場面では、まず `build()` を実行してプロジェクト全体の成否を確認すること。
- テスト実行が目的なら `build` の代用にせず、対応する `run_test` を使うこと。
