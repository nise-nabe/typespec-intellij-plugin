#!/usr/bin/env bash
# Smoke-test: start sandbox IDE with the plugin under xvfb and verify startup from idea.log.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

DISPLAY_NUM="${DISPLAY_NUM:-99}"
export DISPLAY=":${DISPLAY_NUM}"
STARTUP_TIMEOUT_SECONDS="${STARTUP_TIMEOUT_SECONDS:-900}"
POLL_INTERVAL_SECONDS=5

find_idea_log() {
  find "${ROOT_DIR}/plugin/build/idea-sandbox" "${ROOT_DIR}/build/idea-sandbox" \
    -path '*/system/log/idea.log' -print -quit 2>/dev/null
}

if ! command -v Xvfb >/dev/null 2>&1; then
  echo "Xvfb is required. Install with: sudo apt-get install -y xvfb" >&2
  exit 1
fi

"${ROOT_DIR}/scripts/prepare-jetbrains-consent.sh"

if ! pgrep -f "Xvfb :${DISPLAY_NUM}" >/dev/null 2>&1; then
  Xvfb ":${DISPLAY_NUM}" -screen 0 1920x1080x24 &
  sleep 2
fi

./gradlew :plugin:runIde &
GRADLE_PID=$!

cleanup() {
  if kill -0 "${GRADLE_PID}" 2>/dev/null; then
    kill "${GRADLE_PID}" 2>/dev/null || true
    wait "${GRADLE_PID}" 2>/dev/null || true
  fi
}
trap cleanup EXIT

log_file=""
elapsed=0
while [[ "${elapsed}" -lt "${STARTUP_TIMEOUT_SECONDS}" ]]; do
  if [[ -z "${log_file}" ]]; then
    log_file="$(find_idea_log || true)"
  fi
  if [[ -n "${log_file}" && -f "${log_file}" ]] && grep -qE 'Startup completed|IDE started' "${log_file}"; then
    echo "IDE smoke OK: startup message found in ${log_file}"
    exit 0
  fi
  if ! kill -0 "${GRADLE_PID}" 2>/dev/null; then
    echo "IDE smoke FAILED: Gradle runIde exited before startup completed" >&2
  log_file="$(find_idea_log || true)"
    [[ -n "${log_file}" && -f "${log_file}" ]] && tail -n 80 "${log_file}" >&2 || true
    exit 1
  fi
  sleep "${POLL_INTERVAL_SECONDS}"
  elapsed=$((elapsed + POLL_INTERVAL_SECONDS))
done

echo "IDE smoke FAILED: timed out after ${STARTUP_TIMEOUT_SECONDS}s waiting for startup" >&2
log_file="$(find_idea_log || true)"
[[ -n "${log_file}" && -f "${log_file}" ]] && tail -n 80 "${log_file}" >&2 || true
exit 1
