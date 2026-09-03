#!/usr/bin/env bash
# Terminate the demo workflow so you can re-run from a clean slate.
#   Usage: reset.sh [workflow-id]
# To wipe ALL data (every workflow), instead run:  make install-schema-mysql
source "$(dirname "$0")/lib.sh"
need temporal

WID="${1:-$WORKFLOW_ID_DEFAULT}"

temporal workflow terminate --workflow-id "$WID" --reason "workshop reset" 2>/dev/null \
  && echo "terminated $WID" \
  || echo "nothing to terminate for $WID (not running)"

echo "For a full data wipe: (cd $TEMPORAL_SRC && make install-schema-mysql)"
