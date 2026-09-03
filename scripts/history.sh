#!/usr/bin/env bash
# Decode and print a workflow's history from the database via tdbg.
#   Usage: history.sh [workflow-id] [grep-filter]
#   Examples:
#     history.sh                       # full decoded history of demo-wf
#     history.sh demo-wf WorkflowTaskFailed   # just the non-determinism failures
source "$(dirname "$0")/lib.sh"
need_tdbg

WID="${1:-$WORKFLOW_ID_DEFAULT}"
FILTER="${2:-}"

if [ -n "$FILTER" ]; then
  "$TDBG" execution show --workflow-id "$WID" -n "$TEMPORAL_NAMESPACE" --decode | grep -iA4 "$FILTER"
else
  exec "$TDBG" execution show --workflow-id "$WID" -n "$TEMPORAL_NAMESPACE" --decode
fi
