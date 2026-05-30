#!/usr/bin/env bash
# Smoke-test: start sandbox IDE with the plugin under xvfb and verify startup from idea.log.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

DISPLAY_NUM="${DISPLAY_NUM:-99}"
export DISPLAY=":${DISPLAY_NUM}"
LOG_FILE="${ROOT_DIR}/build/idea-sandbox/system/log/idea.log"
STARTUP_TIMEOUT_SECONDS="${STARTUP_TIMEOUT_SECONDS:-900}"
POLL_INTERVAL_SECONDS=5

if ! command -v Xvfb >/dev/null 2>&1; then
  echo "Xvfb is required. Install with: sudo apt-get install -y xvfb" >&2
  exit 1
fi

"${ROOT_DIR}/scripts/prepare-jetbrains-consent.sh"

if ! pgrep -f "Xvfb :${DISPLAY_NUM}" >/dev/null 2>&1; then
  Xvfb ":${DISPLAY_NUM}" -screen 0 1920x1080x24 &
  sleep 2
fi

rm -f "${LOG_FILE}"
./gradlew :plugin:runIde --no-daemon &
GRADLE_PID=$!

cleanup() {
  if kill -0 "${GRADLE_PID}" 2>/dev/null; then
    kill "${GRADLE_PID}" 2>/dev/null || true
    wait "${GRADLE_PID}" 2>/dev/null || true
  fi
}
trap cleanup EXIT

elapsed=0
while [[ "${elapsed}" -lt "${STARTUP_TIMEOUT_SECONDS}" ]]; do
  if [[ -f "${LOG_FILE}" ]] && grep -qE 'Startup completed|IDE started' "${LOG_FILE}"; then
    echo "IDE smoke OK: startup message found in idea.log"
    exit 0
  fi
  if ! kill -0 "${GRADLE_PID}" 2>/dev/null; then
    echo "IDE smoke FAILED: Gradle runIde exited before startup completed" >&2
    [[ -f "${LOG_FILE}" ]] && tail -n 80 "${LOG_FILE}" >&2 || true
    exit 1
  fi
  sleep "${POLL_INTERVAL_SECONDS}"
  elapsed=$((elapsed + POLL_INTERVAL_SECONDS))
done

echo "IDE smoke FAILED: timed out after ${STARTUP_TIMEOUT_SECONDS}s waiting for startup" >&2
[[ -f "${LOG_FILE}" ]] && tail -n 80 "${LOG_FILE}" >&2 || true
exit 1
