#!/usr/bin/env bash
# Pre-accept JetBrains statistics consent so headless IDE startup is not blocked.
set -euo pipefail

consent_dir="${HOME}/.local/share/JetBrains/consentOptions"
mkdir -p "${consent_dir}"
printf 'rsch.send.usage.stat:1.1:0:%s000' "$(date +%s)" > "${consent_dir}/accepted"
