#!/usr/bin/env bash
# Run the workshop MySQL queries against the dev database container.
#   Usage: sql.sh [q1|q2|q3|all] [workflow-id]
#     q1  = state snapshot + version counters
#     q2  = the append-only history ledger
#     q3  = in-flight work (pending timers/activities/signals)
source "$(dirname "$0")/lib.sh"

Q="${1:-all}"
WID="${2:-$WORKFLOW_ID_DEFAULT}"

command -v docker >/dev/null 2>&1 || { echo "error: docker not found" >&2; exit 1; }

q1="SELECT workflow_id, next_event_id, db_record_version, last_write_version, HEX(run_id) AS run_id
    FROM executions WHERE workflow_id = '$WID';"
q2="SELECT node_id, txn_id, prev_txn_id, LENGTH(data) AS bytes
    FROM history_node
    WHERE tree_id = (SELECT run_id FROM executions WHERE workflow_id = '$WID' LIMIT 1)
    ORDER BY node_id;"
q3="SELECT 'timer' AS kind, COUNT(*) AS n FROM timer_info_maps
    UNION ALL SELECT 'activity', COUNT(*) FROM activity_info_maps
    UNION ALL SELECT 'signal', COUNT(*) FROM signal_info_maps;"

run() { docker exec -i "$MYSQL_CONTAINER" mysql -u temporal -ptemporal temporal -t -e "$1" 2>/dev/null; }

case "$Q" in
  q1) run "$q1";;
  q2) run "$q2";;
  q3) run "$q3";;
  all)
    echo "== Q1 · state + version counters =="; run "$q1"
    echo "== Q2 · history ledger =="; run "$q2"
    echo "== Q3 · in-flight work =="; run "$q3"
    ;;
  *) echo "unknown query: $Q (want q1|q2|q3|all)" >&2; exit 1;;
esac
