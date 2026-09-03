#!/usr/bin/env bash
# Run a language's worker from the terminal (handy for the non-determinism demo,
# where you repeatedly kill + restart the worker). For breakpoint debugging, use the
# IDE "Worker (debug)" launch config instead.
#   Usage: run-worker.sh <go|python|java>
source "$(dirname "$0")/lib.sh"

LANG="${1:?usage: run-worker.sh <go|python|java>}"

case "$LANG" in
  go)
    need go
    cd "$ROOT_DIR/go"
    echo "Go worker on task queue hello-go (Ctrl-C to stop)"
    TEMPORAL_DEBUG=true exec go run ./worker
    ;;
  python)
    cd "$ROOT_DIR/python"
    PY="python3"; [ -x .venv/bin/python ] && PY=".venv/bin/python"
    echo "Python worker on task queue hello-python (Ctrl-C to stop)"
    TEMPORAL_DEBUG=1 exec "$PY" worker.py
    ;;
  java)
    cd "$ROOT_DIR/java"
    GRADLE="$(gradle_cmd)" || {
      echo "No gradle wrapper or system gradle found. Run the Java worker from the IDE" >&2
      echo "launch config 'Worker (debug)'." >&2; exit 1; }
    echo "Java worker on task queue hello-java (Ctrl-C to stop)"
    TEMPORAL_DEBUG=true exec "$GRADLE" -q run
    ;;
  *)
    echo "unknown language: $LANG (want go|python|java)" >&2
    exit 1
    ;;
esac
