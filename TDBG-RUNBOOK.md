# Runbook — `tdbg`, the Data-Layer Decoder Ring

Your MySQL queries (`README.md`) prove that history **grows** and state is **versioned** —
but the `executions.state` and `history_node.data` columns are **opaque proto blobs**.
`tdbg` is the tool that decodes them, so students can read *what's actually inside* the rows
they're watching change.

- **Build it:** `make tdbg` (produces `./tdbg` in the repo root).
- **How it connects:** to the frontend's **AdminService** at `127.0.0.1:7233` (override with
  `--address` / `TEMPORAL_CLI_ADDRESS`). The AdminService exposes low-level reads into
  persistence — deeper than the normal `temporal` CLI, which only sees the WorkflowService.
- **Namespace:** `-n default` (default).
- **Command aliases:** `execution` = `e` = `w` = `workflow`. So `tdbg workflow show` and
  `tdbg execution show` are the same command.

> **The pitch to the room:** *"The `temporal` CLI and the Web UI show you the polished view.
> `tdbg` shows you what the server itself reads out of the database — the same bytes sitting in
> those MySQL columns, decoded."*

---

## The two star commands

### 1 · `execution show --decode` — decode the history ledger

This decodes `history_node.data` into readable events. It is the human-readable form of **Q2**.

```bash
./tdbg execution show --workflow-id demo-wf -n default --decode
```

- `--decode` renders payloads as JSON (without it, tdbg nudges you to add it).
- Slice a range to focus (e.g. around a failure):
  `--min-event-id 1 --max-event-id 20`
- Write to a file: `--output-filename demo-wf.json`

**SAY:** *"Q2 showed rows appending in `history_node`. This is those exact rows, decoded —
`WorkflowExecutionStarted`, `ActivityTaskScheduled`, `TimerStarted`, and so on. The blob* is *
the event history."*

### 2 · `execution describe` — decode the mutable state

This decodes the single mutable-state blob (`executions.state`/`data`) — the human-readable
form of **Q1** plus everything in the info-map tables.

```bash
./tdbg execution describe --workflow-id demo-wf -n default
```

Prints the decoded **database mutable state**: `ExecutionInfo` (type, status, `next_event_id`),
`VersionHistories` (the history-branch pointers), pending activities/timers, the current branch
token, and the **Shard ID** for this workflow. (`--workflow-id`/`--wid` is an alias for
`--business-id`; add `--run-id` to pin a run.)

**SAY:** *"Q1 showed `next_event_id` and `db_record_version` as numbers. `describe` shows the
whole state object those columns live in — including the pending activities and timers that Q3
counted as rows."*

---

## Mapping tdbg to the SQL tables

| MySQL query | Table / column (opaque) | tdbg equivalent (decoded) |
|-------------|-------------------------|---------------------------|
| Q2 — ledger | `history_node.data` | `execution show --decode` |
| Q1 — state counters | `executions.state` / `.data` | `execution describe` |
| Q3 — in-flight | `activity_info_maps`, `timer_info_maps` | pending activities/timers in `execution describe` |
| (raw blob) | any proto column | `decode proto` (see below) |

---

## Using tdbg in the determinism demo (Lab 4)

`tdbg` makes the non-determinism failure and the versioning fix **visible in the terminal** —
pair it with [`DETERMINISM-PLAYBOOK.md`](DETERMINISM-PLAYBOOK.md).

**When the workflow is wedged (Act I):**
```bash
./tdbg execution show --workflow-id demo-wf -n default --decode | grep -iA4 "WorkflowTaskFailed"
```
Shows the `WorkflowTaskFailed` events piling up **with the decoded failure reason** — the
nondeterminism message the SQL blob hides. Also:
```bash
./tdbg execution describe --workflow-id demo-wf -n default
```
Status is still `Running`, and you can see the failing workflow task in the mutable state —
concrete proof the execution is *wedged, not corrupted*.

**After the versioning fix (Act II):**
```bash
./tdbg execution show --workflow-id demo-wf -n default --decode | grep -i "MarkerRecorded"
```
A **fresh** (post-fix) workflow shows a `MarkerRecorded` event — the version marker written by
`GetVersion`/`patched`. The old, rescued workflow has **no** marker (that's why it replayed the
`DefaultVersion` path). Showing both side by side is the whole versioning lesson in two events.

---

## Bonus: decode a raw blob straight from MySQL (`decode proto`)

The killer "it's really just protobuf" moment. Pull a blob out of MySQL, hand it to tdbg,
watch it become a struct.

```bash
# 1) In mysql — dump one history batch as hex:
#    SELECT HEX(data) FROM history_node
#    WHERE tree_id = (SELECT run_id FROM executions WHERE workflow_id='demo-wf' LIMIT 1)
#    ORDER BY node_id LIMIT 1;

# 2) Decode that hex with tdbg (confirm exact input flag with --help first):
./tdbg decode proto --help
./tdbg decode proto --type temporal.server.api.history.v1.History --hex-data <HEX_FROM_STEP_1>
```

**SAY:** *"This is the byte-for-byte content of one row in `history_node`, decoded outside
Temporal entirely. The database is just storing protobuf — no magic."*

> Flag names on `decode proto` (e.g. `--hex-data` vs `--binary-file`, and the exact `--type`)
> vary by build — run `./tdbg decode proto --help` and confirm live. The `show`/`describe`
> commands are the reliable stars; treat `decode proto` as an optional flourish.

---

## Finding the shard (for the "where does this workflow live" tangent)

```bash
./tdbg execution describe --workflow-id demo-wf -n default   # prints Shard ID
./tdbg shard describe --shard-id <N>                         # shard's queue ack levels, etc.
```

With 4 shards (dev config), this shows how a workflow is deterministically mapped to a shard —
useful if someone asks "how does Temporal scale this."

---

## Quick reference

| Goal | Command |
|------|---------|
| Decode full history | `tdbg execution show -w demo-wf -n default --decode` |
| Decode a history slice | `... --min-event-id 1 --max-event-id 20 --decode` |
| Decode mutable state + pending work | `tdbg execution describe -w demo-wf -n default` |
| Find the failure reason | `tdbg execution show -w demo-wf -n default --decode \| grep -iA4 WorkflowTaskFailed` |
| See version markers | `tdbg execution show -w demo-wf -n default --decode \| grep -i MarkerRecorded` |
| Decode a raw SQL blob | `tdbg decode proto --type <proto> --hex-data <hex>` |
| Find/inspect the shard | `tdbg execution describe ...` → `tdbg shard describe --shard-id <N>` |

## Gotchas

| Symptom | Fix |
|---------|-----|
| `connection refused` | The workshop server isn't running, or wrong address — pass `--address 127.0.0.1:7233`. |
| History prints as opaque payloads | Add `--decode`. |
| `describe` says workflow not found | Wrong namespace or id — must be `-n default -w demo-wf`. |
| `decode proto` flag/type errors | Run `tdbg decode proto --help`; confirm the `--type` message name and input flag for your build. |
