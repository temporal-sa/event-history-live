#!/usr/bin/env bash
# Show a workflow's decoded mutable state (tdbg) + high-level status (temporal CLI).
#   Usage: describe.sh [workflow-id]
source "$(dirname "$0")/lib.sh"

WID="${1:-$WORKFLOW_ID_DEFAULT}"

if [ -x "$TDBG" ]; then
  echo "== tdbg execution describe (decoded mutable state) =="
  "$TDBG" execution describe --workflow-id "$WID" -n "$TEMPORAL_NAMESPACE" || true
  echo
fi

if command -v temporal >/dev/null 2>&1; then
  echo "== temporal workflow describe (status) =="
  temporal workflow describe --workflow-id "$WID" || true
fi
