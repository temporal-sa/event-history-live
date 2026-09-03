#!/usr/bin/env bash
# Convenience: unpark a nondet/versioned demo. Usage: signal-proceed.sh [workflow-id]
DIR="$(dirname "$0")"
exec "$DIR/signal.sh" proceed "" "${1:-demo-wf}"
