#!/usr/bin/env bash
# Shared config + helpers for the workshop scripts. Source this; don't run it.
set -euo pipefail

# Connection — the temporal CLI honors these env vars.
export TEMPORAL_ADDRESS="${TEMPORAL_ADDRESS:-localhost:7233}"
export TEMPORAL_NAMESPACE="${TEMPORAL_NAMESPACE:-default}"

WORKFLOW_ID_DEFAULT="demo-wf"

# Paths — this file lives in <repo>/scripts/. ROOT_DIR is this repo (the workshop apps).
SCRIPTS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPTS_DIR/.." && pwd)"

# The PATCHED Temporal server + its tooling (tdbg, `make` targets, MySQL dev config, schema)
# live in a SEPARATE checkout of temporalio/temporal — see CLAUDE.md / README.md.
# Point TEMPORAL_SRC at that checkout if the default is wrong.
TEMPORAL_SRC="${TEMPORAL_SRC:-$HOME/temporal-oss/temporal}"
TDBG="$TEMPORAL_SRC/tdbg"
MYSQL_CONTAINER="temporal-dev-mysql"

need() {
  command -v "$1" >/dev/null 2>&1 || { echo "error: '$1' not found in PATH" >&2; exit 1; }
}

need_tdbg() {
  [ -x "$TDBG" ] || {
    echo "error: tdbg not found at $TDBG" >&2
    echo "  Build it in your Temporal server checkout:  (cd \"\$TEMPORAL_SRC\" && make tdbg)" >&2
    echo "  If your checkout is elsewhere:  export TEMPORAL_SRC=/path/to/temporal" >&2
    exit 1
  }
}

# Echo the gradle command to use: the project wrapper if present, else a system gradle.
gradle_cmd() {
  if [ -x "$ROOT_DIR/java/gradlew" ]; then
    echo "$ROOT_DIR/java/gradlew"
  elif command -v gradle >/dev/null 2>&1; then
    echo "gradle"
  else
    return 1
  fi
}
