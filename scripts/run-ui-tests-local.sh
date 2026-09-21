#!/usr/bin/env bash
# Local-only UI test runner (no Xvfb; expects a real display, e.g. Windows/macOS).
#
# Prerequisite (one-time): the sandbox IDE needs an activated subscription.
# On 2025.3+ unified IDEA an unlicensed sandbox boots in Community-equivalent
# mode: com.intellij.modules.ultimate stays disabled, NodeJS/JavaScript fail to
# load, and this plugin (which depends on them) never loads either.
#   1. ./gradlew :plugin:runIdeForUiTests
#   2. In the sandbox IDE: Help | Manage Licenses -> Activate
#   (idea.key is persisted under .intellijPlatform/sandbox/plugin/<ver>/config_runIdeForUiTests/)
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

ROBOT_URL="${ROBOT_URL:-http://127.0.0.1:8082}"
WAIT_SECONDS="${WAIT_SECONDS:-300}"
POLL_INTERVAL_SECONDS=5
RUN_IDE_LOG="${ROOT_DIR}/build/run-ide-for-ui-tests-local.log"

robot_server_up() {
  local code
  code="$(curl -s -o /dev/null -w '%{http_code}' "${ROBOT_URL}/" 2>/dev/null || echo "000")"
  [[ "${code}" != "000" && "${code}" -ge 200 && "${code}" -lt 500 ]]
}

if robot_server_up; then
  echo "Robot server already up at ${ROBOT_URL}; reusing running IDE"
else
  echo "Starting :plugin:runIdeForUiTests (log: ${RUN_IDE_LOG})"
  "${ROOT_DIR}/scripts/prepare-jetbrains-consent.sh"
  mkdir -p "${ROOT_DIR}/build"
  nohup ./gradlew --non-interactive :plugin:runIdeForUiTests >"${RUN_IDE_LOG}" 2>&1 &
fi

elapsed=0
while [[ "${elapsed}" -lt "${WAIT_SECONDS}" ]]; do
  if robot_server_up; then
    echo "Robot server is up at ${ROBOT_URL}"
    break
  fi
  sleep "${POLL_INTERVAL_SECONDS}"
  elapsed=$((elapsed + POLL_INTERVAL_SECONDS))
done

if ! robot_server_up; then
  echo "Robot server did not become reachable at ${ROBOT_URL} within ${WAIT_SECONDS}s" >&2
  tail -n 80 "${RUN_IDE_LOG}" >&2 || true
  exit 1
fi

./gradlew --non-interactive :ui-test:test "-Drobot.server.url=${ROBOT_URL}"
