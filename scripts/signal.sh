#!/usr/bin/env bash
# Send a signal to a workflow (language-agnostic — goes through the frontend).
#   Usage: signal.sh <signal-name> [payload] [workflow-id]
#   Examples:
#     signal.sh proceed              # nondet / versioned demos
#     signal.sh add Alice            # approval (signal) demo — payload is the approver
#     signal.sh done                 # approval demo — finish
source "$(dirname "$0")/lib.sh"
need temporal

NAME="${1:?usage: signal.sh <signal-name> [payload] [workflow-id]}"
PAYLOAD="${2:-}"
WID="${3:-$WORKFLOW_ID_DEFAULT}"

args=(workflow signal --workflow-id "$WID" --name "$NAME")
if [ -n "$PAYLOAD" ]; then
  # temporal --input takes JSON; wrap the payload as a JSON string.
  args+=(--input "\"$PAYLOAD\"")
fi

echo "signal '$NAME'${PAYLOAD:+ ($PAYLOAD)} -> $WID"
exec temporal "${args[@]}"
