#!/usr/bin/env bash
# Convenience: add an approver to the signal demo. Usage: signal-add.sh <name> [workflow-id]
DIR="$(dirname "$0")"
APPROVER="${1:?usage: signal-add.sh <approver-name> [workflow-id]}"
exec "$DIR/signal.sh" add "$APPROVER" "${2:-demo-wf}"
