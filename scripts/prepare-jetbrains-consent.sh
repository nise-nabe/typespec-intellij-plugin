#!/usr/bin/env bash
# Pre-accept JetBrains statistics consent so IDE startup is not blocked by a
# consent prompt (relevant for headless CI and first sandbox starts).
set -euo pipefail

case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*)
    consent_dir="${APPDATA}/JetBrains/consentOptions" ;;
  Darwin)
    consent_dir="${HOME}/Library/Application Support/JetBrains/consentOptions" ;;
  *)
    consent_dir="${HOME}/.local/share/JetBrains/consentOptions" ;;
esac

mkdir -p "${consent_dir}"
printf 'rsch.send.usage.stat:1.1:0:%s000' "$(date +%s)" > "${consent_dir}/accepted"
