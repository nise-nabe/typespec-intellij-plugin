#!/usr/bin/env bash
# CI/local: start IDE with robot-server under xvfb, wait until reachable, run ui-test.
# Keeps runIdeForUiTests in the same shell session (nohup + setsid) so GitHub Actions
# does not terminate the IDE when an earlier workflow step would have exited.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

ROBOT_URL="${ROBOT_URL:-http://127.0.0.1:8082}"
DISPLAY_NUM="${DISPLAY_NUM:-99}"
export DISPLAY=":${DISPLAY_NUM}"
WAIT_SECONDS="${WAIT_SECONDS:-600}"
POLL_INTERVAL_SECONDS=5
RUN_IDE_LOG="${ROOT_DIR}/build/run-ide-for-ui-tests.log"
RUN_IDE_PID_FILE="${ROOT_DIR}/build/run-ide-for-ui-tests.pid"

robot_server_up() {
  local code
  code="$(curl -s -o /dev/null -w '%{http_code}' "${ROBOT_URL}/" 2>/dev/null || echo "000")"
  [[ "${code}" != "000" && "${code}" -ge 200 && "${code}" -lt 500 ]]
}

if ! command -v Xvfb >/dev/null 2>&1; then
  echo "Xvfb is required. Install with: sudo apt-get install -y xvfb" >&2
  exit 1
fi

"${ROOT_DIR}/scripts/prepare-jetbrains-consent.sh"
mkdir -p "${ROOT_DIR}/build"

if ! pgrep -f "Xvfb :${DISPLAY_NUM}" >/dev/null 2>&1; then
  Xvfb ":${DISPLAY_NUM}" -screen 0 1920x1080x24 &
  sleep 2
fi

if [[ -f "${RUN_IDE_PID_FILE}" ]] && kill -0 "$(cat "${RUN_IDE_PID_FILE}")" 2>/dev/null; then
  echo "runIdeForUiTests already running (pid $(cat "${RUN_IDE_PID_FILE}"))"
else
  setsid nohup ./gradlew :plugin:runIdeForUiTests >"${RUN_IDE_LOG}" 2>&1 &
  echo $! >"${RUN_IDE_PID_FILE}"
  echo "Started runIdeForUiTests (pid $(cat "${RUN_IDE_PID_FILE}"))"
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

./gradlew :ui-test:test "-Drobot.server.url=${ROBOT_URL}"
