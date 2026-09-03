#!/usr/bin/env bash
# Start a demo workflow in the chosen language.
#   Usage:
#     start.sh <lang> multiactivity [a] [b] [workflow-id]     # math pipeline: result = 2*(a+b)
#     start.sh <lang> <hello|signal|nondet|versioned> [name] [workflow-id]
#   <lang> = go | python | java
# The worker for that language must already be running (IDE "Worker (debug)" or run-worker.sh).
source "$(dirname "$0")/lib.sh"

LANG="${1:?usage: start.sh <go|python|java> <hello|multiactivity|signal|nondet|versioned> [args...]}"
DEMO="${2:?missing demo: hello|multiactivity|signal|nondet|versioned}"

if [ "$DEMO" = "multiactivity" ]; then
  A="${3:-3}"; B="${4:-4}"; WID="${5:-$WORKFLOW_ID_DEFAULT}"
else
  NAME="${3:-Temporal}"; WID="${4:-$WORKFLOW_ID_DEFAULT}"
fi

# hello and multiactivity complete on their own, so block for the result.
WAIT_GO=""; WAIT_PY=""
case "$DEMO" in hello|multiactivity) WAIT_GO="-wait"; WAIT_PY="--wait";; esac

case "$LANG" in
  go)
    need go; cd "$ROOT_DIR/go"
    if [ "$DEMO" = "multiactivity" ]; then
      exec go run ./starter -workflow multiactivity -id "$WID" -a "$A" -b "$B" -wait
    else
      exec go run ./starter -workflow "$DEMO" -id "$WID" -name "$NAME" $WAIT_GO
    fi
    ;;
  python)
    cd "$ROOT_DIR/python"
    PY="python3"; [ -x .venv/bin/python ] && PY=".venv/bin/python"
    if [ "$DEMO" = "multiactivity" ]; then
      exec "$PY" starter.py --workflow multiactivity --id "$WID" --a "$A" --b "$B" --wait
    else
      exec "$PY" starter.py --workflow "$DEMO" --id "$WID" --name "$NAME" $WAIT_PY
    fi
    ;;
  java)
    cd "$ROOT_DIR/java"
    GRADLE="$(gradle_cmd)" || {
      echo "No gradle wrapper or system gradle found. Start the Java demo from the IDE" >&2
      echo "launch config 'Start: $DEMO'." >&2; exit 1; }
    if [ "$DEMO" = "multiactivity" ]; then
      exec "$GRADLE" -q runStarter --args="multiactivity $A $B $WID"
    else
      exec "$GRADLE" -q runStarter --args="$DEMO $NAME $WID"
    fi
    ;;
  *)
    echo "unknown language: $LANG (want go|python|java)" >&2; exit 1
    ;;
esac
