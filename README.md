# Temporal 101 — Watching the Data Layer While a Workflow is Paused

A hands-on workshop for new Temporal developers. We pause a running workflow three
different ways, then query the SQL underneath to see two truths at once:

- **History only grows** — events are appended to an immutable log.
- **State is a versioned, replayable derivative** of that history.

The demo runs against **MySQL** — a relational backend where you can *see* the
event-sourcing model with a plain `SELECT` (unlike the file-locked SQLite default or
Cassandra's opaque wide rows).

---

## Two repos: this one + a patched Temporal server

This repository holds the **workshop apps** (Go / Python / Java workflows), the `scripts/`
toolkit, and the docs. The **Temporal server** they run against lives in a *separate*
checkout of [temporalio/temporal](https://github.com/temporalio/temporal), because the
demos need one small source patch so a breakpoint held in *workflow code* doesn't trip the
server-side workflow-task timeout:

| File (in the temporal checkout) | Change | Why |
|------|--------|-----|
| `service/history/api/create_workflow_util.go` | `maxWorkflowTaskStartToCloseTimeout`: `120s` → `15m` | Lets a workflow task be granted up to a 15-minute start-to-close timeout, so you can hold a breakpoint in workflow code. |

The scripts locate that checkout via **`$TEMPORAL_SRC`** (default `~/temporal-oss/temporal`).
Anything below that uses `make`, `tdbg`, the MySQL config, or the schema runs **in that
checkout**; the worker, starters, and `scripts/` run **in this repo**.

## Prerequisites

- The **patched Temporal server checkout** (above), plus **make** + **Docker**.
- **Temporal CLI** — `brew install temporal` (create the namespace, send signals).
- A toolchain for the demo you run: **Go 1.22+**, **Python 3.10+**, or **JDK 17+**
  (Java uses the bundled Gradle wrapper — nothing else to install).

---

## Quick start

Open four terminals. Leave A, B, and D running for the whole session.

### Terminal A — dependencies (MySQL, UI, Grafana)

```bash
make start-dependencies
```

Starts Docker containers: MySQL on `:3306`, Temporal UI on `:8080`,
Prometheus/Grafana. (This repo's docker-compose is **dependencies-only** — the
Temporal server runs on your host, next.)

### Terminal B — schema + the debug server

```bash
make install-schema-mysql               # creates `temporal` + `temporal_visibility` DBs
make temporal-server-debug              # builds with TEMPORAL_DEBUG tag → relaxed internal timeouts
                                        # (also includes the 15m workflow-task cap change)
./temporal-server-debug \
  --config-file config/development-mysql8.yaml \
  --allow-no-auth start
```

**Why `temporal-server-debug` and not `make start-mysql`?** The debug binary is
built with the `TEMPORAL_DEBUG` build tag, which multiplies internal server timeouts
×100 — handy across a debugging session. It's compiled from the same source, so it
also carries the 15-minute workflow-task cap. (The `120s` workflow-task max is the one
thing the ×100 multiplier does *not* touch — that's why we changed the constant
directly.)

- gRPC: `7233` · HTTP: `7243` · UI: <http://localhost:8080>

### Terminal C — create the namespace

```bash
temporal operator namespace create --namespace default
```

### Terminal D — your live SQL window (project this one)

```bash
docker exec -it temporal-dev-mysql mysql -u temporal -ptemporal temporal
# (no space after -p; password is `temporal`)
```

### Then: start a worker

Point any SDK worker at `localhost:7233`, namespace `default`, and start a workflow
with id `demo-wf`. See [Holding a breakpoint](#holding-a-breakpoint-the-timeout-recipe)
for the worker-side settings Lab 3 needs.

---

## Sample apps (Java / Python / Go)

Ready-to-debug Hello World workflows live in this folder, one per SDK. Each has its own
`.vscode/launch.json` with **"Worker (debug)"** and **"Start demo-wf"** configs, and is
pre-wired with the breakpoint-friendly settings (15-min workflow-task timeout, deadlock
detector off, 1-hour activity timeout).

| Language | Folder | Setup |
|----------|--------|-------|
| Go | [`go/`](go/README.md) | `go mod tidy` |
| Python | [`python/`](python/README.md) | `pip install -r requirements.txt` |
| Java | [`java/`](java/README.md) | Gradle project — auto-imported by the VSCode Java pack |

**Open the sub-project folder as the workspace root** in Cursor/VSCode (e.g. open
`go`, not the repo root) so the language tooling and launch configs resolve.
Then: set a breakpoint → run **"Worker (debug)"** → run **"Start: hello"** → the
breakpoint hits → switch to the `mysql` client and run the three queries below. Each app uses a
distinct task queue (`hello-go` / `hello-python` / `hello-java`) and workflow id `demo-wf`.

Each app ships **five** workflows — Hello, a multi-activity pipeline, a signal/approval
gate, and the non-determinism + versioned pair — plus a `scripts/` toolkit to drive them
from the terminal. See **[`DEMOS.md`](DEMOS.md)** for the full catalog and step-by-step runs.

---

## The three queries you'll rerun all day

Run these **before** pausing, **during** the pause, and **after** resuming — the whole
demo is the diff between runs. (`run_id`, `namespace_id`, `tree_id` are raw 16-byte
`BINARY(16)`; wrap in `HEX(col)` to read them. `tree_id` equals the first run's
`run_id`, which is how Q2 joins history to a workflow.)

**Q1 — state snapshot & version counters**
```sql
SELECT workflow_id,
       next_event_id,           -- how many events history will have
       db_record_version,       -- bumps on every state write (optimistic lock)
       last_write_version,
       HEX(run_id) AS run_id
FROM executions
WHERE workflow_id = 'demo-wf';
```

**Q2 — the append-only ledger for this workflow**
```sql
SELECT node_id, txn_id, prev_txn_id, LENGTH(data) AS bytes
FROM history_node
WHERE tree_id = (SELECT run_id FROM executions
                 WHERE workflow_id = 'demo-wf' LIMIT 1)
ORDER BY node_id;
-- rerun after each step: rows are ADDED; old rows are byte-for-byte identical
```

**Q3 — what's in-flight right now**
```sql
SELECT 'timer'    AS kind, count(*) FROM timer_info_maps
UNION ALL
SELECT 'activity', count(*) FROM activity_info_maps
UNION ALL
SELECT 'signal',   count(*) FROM signal_info_maps;
```

**Decoder ring:** to see what's *inside* a history blob, `make tdbg` then
`./tdbg execution show --workflow-id demo-wf -n default --decode` — it prints the decoded
events, proving the bytes in `history_node.data` are the event history. Full guide (decoding
mutable state, mapping to the SQL tables, the determinism demo): [`TDBG-RUNBOOK.md`](TDBG-RUNBOOK.md).

---

## The three labs

| Lab | Pause method | Difficulty | The point |
|-----|--------------|------------|-----------|
| 1 | Durable timer (`sleep`) | easy — start here | The pause is a **database row**, not a blocked thread. |
| 2 | Block on a signal | easy | History advances **only on real events** — the cleanest before/after. |
| 3 | Debugger inside an activity | advanced — the "aha" | Server's durable record vs. worker's live execution. |

**Lab 1** — start a workflow that does an activity → `sleep(60s)` → another activity.
During the sleep, Q3 shows a `timer` row while no worker thread is blocked. After it
fires, Q2 shows `TimerFired` + the next events appended.

**Lab 2** — a workflow that `await`s a signal. While blocked, rerun Q2 three times:
byte-identical (the determinism moment). Then
`temporal workflow signal --workflow-id demo-wf --name proceed` and watch a
`WorkflowExecutionSignaled` row append with `db_record_version` bumping by one.

**Lab 4 — determinism & versioning** — build on the signal gate to break replay with an
incompatible code change (watch the workflow *wedge* with a non-determinism error), then
rescue the stuck execution with `GetVersion`. Full instructor script:
[`DETERMINISM-PLAYBOOK.md`](DETERMINISM-PLAYBOOK.md).

**Lab 3** — attach a debugger to the **worker** and breakpoint inside an activity.
Q3 shows an `activity` row (server recorded `ActivityTaskStarted`, waiting). Q2 shows
`ActivityTaskCompleted` is *absent* — history can't record a result that hasn't
happened. Resume → it appends. **Optional:** kill the worker while paused, restart it,
and watch Temporal *replay* history to rebuild state — every history row identical.

---

## Holding a breakpoint (the timeout recipe)

Where the breakpoint sits decides which clock can bite you.

### Breakpoint in an **activity** (Lab 3 — recommended)

Activities are **not** subject to the deadlock detector or the workflow-task timeout.
The only clock is the activity's own `StartToCloseTimeout`. Set it large on the client;
no server change needed:

```go
workflow.ActivityOptions{
    StartToCloseTimeout: time.Hour,   // hold the breakpoint as long as you like
    // no HeartbeatTimeout — a paused activity can't heartbeat
}
```

### Breakpoint in **workflow code** (needs all three)

1. **Server cap** — already compiled in (`maxWorkflowTaskStartToCloseTimeout = 15m`).
2. **Request it at start** (per-workflow, so you don't slow crashed-worker detection
   for Labs 1 & 2):
   ```go
   client.StartWorkflowOptions{
       WorkflowTaskTimeout: 15 * time.Minute,
   }
   ```
3. **Disable the worker's deadlock detector** (SDK-side, else it panics
   "Potential deadlock detected" in ~1s):
   ```bash
   TEMPORAL_DEBUG=true ./your-worker
   # or, in code:  worker.Options{ DeadlockDetectionTimeout: 15 * time.Minute }
   ```

> **Naming collision:** the *server* `TEMPORAL_DEBUG` is a **build tag** (×100 internal
> timeouts). The *Go SDK* `TEMPORAL_DEBUG` is a **runtime env var** on the worker
> (disables the deadlock detector). Same name, different mechanisms.

---

## Epilogue — the same model in Cassandra (2 min)

If you ran `make start-dependencies`, Cassandra is on `:9042`:

```bash
cqlsh 127.0.0.1 9042 -k temporal
SELECT node_id, txn_id FROM history_node LIMIT 10;   -- still append-only
```

Point out: `history_node` is the same append-only log; mutable state is one **wide row**
in `executions` (`execution_state`, `next_event_id`, `db_record_version`, plus the
activity/timer/signal maps) — the same fields, folded into columns.
*Append-only log + versioned state snapshot — every backend, only the storage shape changes.*

---

## Teardown

```bash
# Ctrl-C the server (Terminal B) and the worker
make stop-dependencies            # drops the MySQL/UI/Grafana containers
```

To reset the data between dry-runs without touching containers:

```bash
make install-schema-mysql    # re-creates a clean schema
```

---

## Reference

- Server config: `config/development-mysql8.yaml`
- SQL schema: `schema/mysql/v8/temporal/`
- Tools: `make temporal-sql-tool`, `make tdbg`
- The one code change: `service/history/api/create_workflow_util.go` (`maxWorkflowTaskStartToCloseTimeout`)
