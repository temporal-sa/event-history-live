#!/usr/bin/env bash
# Convenience: finish the signal demo. Usage: signal-done.sh [workflow-id]
DIR="$(dirname "$0")"
exec "$DIR/signal.sh" done "" "${1:-demo-wf}"
