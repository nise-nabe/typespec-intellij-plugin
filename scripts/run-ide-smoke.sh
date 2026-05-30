#!/usr/bin/env bash
# Smoke-test: start sandbox IDE with the plugin under xvfb and verify startup from idea.log.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

DISPLAY_NUM="${DISPLAY_NUM:-99}"
export DISPLAY=":${DISPLAY_NUM}"
STARTUP_TIMEOUT_SECONDS="${STARTUP_TIMEOUT_SECONDS:-900}"
POLL_INTERVAL_SECONDS=5

# IntelliJ Platform Gradle Plugin 2.x: .intellijPlatform/sandbox/plugin/<IDE-variant>/log/idea.log
# Platform Gradle Plugin 1.x: plugin/build/idea-sandbox/**/system/log/idea.log
find_idea_log() {
  find \
    "${ROOT_DIR}/.intellijPlatform/sandbox/plugin" \
    "${ROOT_DIR}/plugin/build/idea-sandbox" \
    "${ROOT_DIR}/build/idea-sandbox" \
    \( -path '*/log/idea.log' -o -path '*/system/log/idea.log' \) \
    -print -quit 2>/dev/null
}

ide_startup_ok() {
  local log_file="$1"
  [[ -n "${log_file}" && -f "${log_file}" ]] || return 1
  grep -qiE 'Startup completed|IDE started' "${log_file}" && return 0
  grep -qF 'Loaded custom plugins: TypeSpec Support' "${log_file}" && return 0
  return 1
}

clear_stale_idea_logs() {
  while IFS= read -r log; do
    rm -f "${log}"
  done < <(find \
    "${ROOT_DIR}/.intellijPlatform/sandbox/plugin" \
    "${ROOT_DIR}/plugin/build/idea-sandbox" \
    "${ROOT_DIR}/build/idea-sandbox" \
    \( -path '*/log/idea.log' -o -path '*/system/log/idea.log' \) \
    -print 2>/dev/null)
}

# IPGP 2.x may fork the IDE and return from Gradle while the sandbox is still starting.
SANDBOX_PLUGIN_CMD_MARKER="${ROOT_DIR}/.intellijPlatform/sandbox/plugin"

sandbox_ide_running() {
  pgrep -af java 2>/dev/null | grep -Fq "${SANDBOX_PLUGIN_CMD_MARKER}" || return 1
}

stop_sandbox_ide() {
  local pid
  while read -r pid; do
    [[ -n "${pid}" ]] && kill "${pid}" 2>/dev/null || true
  done < <(pgrep -af java 2>/dev/null | grep -F "${SANDBOX_PLUGIN_CMD_MARKER}" | awk '{print $1}')
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

clear_stale_idea_logs

./gradlew :plugin:runIde &
GRADLE_PID=$!

cleanup() {
  if kill -0 "${GRADLE_PID}" 2>/dev/null; then
    kill "${GRADLE_PID}" 2>/dev/null || true
    wait "${GRADLE_PID}" 2>/dev/null || true
  fi
  if sandbox_ide_running; then
    stop_sandbox_ide
  fi
}
trap cleanup EXIT

elapsed=0
while [[ "${elapsed}" -lt "${STARTUP_TIMEOUT_SECONDS}" ]]; do
  log_file="$(find_idea_log || true)"
  if ide_startup_ok "${log_file}"; then
    echo "IDE smoke OK: startup message found in ${log_file}"
    exit 0
  fi
  if ! kill -0 "${GRADLE_PID}" 2>/dev/null; then
    if sandbox_ide_running; then
      : # Gradle detached; keep polling idea.log until startup or timeout.
    else
      echo "IDE smoke FAILED: Gradle runIde exited before startup completed" >&2
      log_file="$(find_idea_log || true)"
      [[ -n "${log_file}" && -f "${log_file}" ]] && tail -n 80 "${log_file}" >&2 || true
      exit 1
    fi
  fi
  sleep "${POLL_INTERVAL_SECONDS}"
  elapsed=$((elapsed + POLL_INTERVAL_SECONDS))
done

echo "IDE smoke FAILED: timed out after ${STARTUP_TIMEOUT_SECONDS}s waiting for startup" >&2
log_file="$(find_idea_log || true)"
[[ -n "${log_file}" && -f "${log_file}" ]] && tail -n 80 "${log_file}" >&2 || true
exit 1
