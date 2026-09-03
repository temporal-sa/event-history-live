# CLAUDE.md — event-history-live

Context for continuing this project in a fresh session. This repo is a **Temporal
workshop** that teaches the event-history / data layer by *looking at the database live*
while workflows run, pause, and evolve.

## What this repo is

Hands-on demos (Go / Python / Java, identical behavior) + a `scripts/` toolkit + docs.
The teaching arc:
1. **History only grows** (append-only event log) and **state is a versioned, replayable
   derivative** — shown with plain `SELECT`s against MySQL.
2. **Debugging** workflows/activities with breakpoints (three pause styles: durable timer,
   signal gate, real debugger).
3. **Determinism & versioning** — an incompatible code change *wedges* a running workflow;
   `GetVersion`/`patched` makes the same change safe.

## Two-repo topology (IMPORTANT)

- **This repo** = the workshop apps, scripts, docs.
- **A separate checkout of [temporalio/temporal](https://github.com/temporalio/temporal)** =
  the server the demos run against. Scripts find it via **`$TEMPORAL_SRC`** (default
  `~/temporal-oss/temporal`).

### The required server patch (lives in the temporal checkout, NOT here)
`service/history/api/create_workflow_util.go`:
`maxWorkflowTaskStartToCloseTimeout` changed from **`120 * time.Second` → `15 * time.Minute`**.
This raises the hard cap on the workflow-task timeout so a breakpoint held in *workflow code*
can survive up to 15 min. It is a plain constant, not dynamic config — the only way to change
it is editing the source and rebuilding. As of the migration this patch was **uncommitted** in
the temporal checkout. Rebuild the server after patching: `make temporal-server` (or
`make temporal-server-debug` for the ×100 internal-timeout debug build).

## Datastore

**MySQL** (dev config `config/development-mysql8.yaml` in the temporal checkout). Container
`temporal-dev-mysql`, db `temporal`, user/pass `temporal`/`temporal`, port 3306. Chosen over
SQLite (file-locked) / Cassandra (opaque wide rows) because the model is legible via SQL.

Three teaching queries (see `scripts/sql.sh` and `README.md`):
- **Q1** `executions`: `next_event_id`, `db_record_version` (mutable state, overwritten).
- **Q2** `history_node`: append-only rows (immutable log). `tree_id = first run's run_id`.
- **Q3** `timer/activity/signal_info_maps`: in-flight work. IDs are `BINARY(16)` → use `HEX()`.

## Conventions (shared across all three languages)

- One **worker per language**, one task queue each: `hello-go` / `hello-python` / `hello-java`.
  Each worker registers all 5 workflows. Default workflow id: `demo-wf`.
- **Single-struct payloads** (Temporal best practice — evolve without breaking signatures):
  `GreetingInput{name}`, `MathInput{a,b}`, `DoubleInput{value}`.
- **Signals**: `proceed` (nondet/versioned), `add`(string approver) + `done` (approval demo).
- **Operations are deliberately primitive & visualizable**, one per category:
  arithmetic (math pipeline), string concat (greeting), array manipulation (approval).

### The 5 demos
| Key | Workflow | Operation |
|-----|----------|-----------|
| `hello` | `GreetingWorkflow` | `ComposeGreeting` → `"Hello, {name}!"` |
| `multiactivity` | `PipelineWorkflow` | `Add(a,b)` → `Double(sum)` = **2*(a+b)** |
| `signal` | `ApprovalWorkflow` | `add`/`done` signals accumulate an approver array |
| `nondet` | `NonDeterminismWorkflow` | 1 activity + a **commented** 2nd activity before a signal park |
| `versioned` | `VersionedWorkflow` | same, but the commented block is gated by `GetVersion`/`patched` |

The nondet/versioned demo flow: start → parks on `proceed` → kill worker → **uncomment** the
marked block → restart → `scripts/signal-proceed.sh` → NDE (unversioned) or clean completion
(versioned). Activities named per-language: Go `Add`/`Double`, Python `add`/`double`, Java
`add`/`doubleValue` (`double` is a Java keyword).

## Debugging settings (already wired into the apps + launch configs)

- **Deadlock detector** off for workflow-code breakpoints: Go/Java env `TEMPORAL_DEBUG=true`
  (Go worker also sets `DeadlockDetectionTimeout: 15m`); Python `debug_mode=True`.
- **Python** also needs `UnsandboxedWorkflowRunner()` or breakpoints in `@workflow.run` never
  fire; launch config sets `justMyCode: false`.
- **Timeouts**: starters set `WorkflowTaskTimeout: 15m` (needs the server patch); activities
  set `StartToCloseTimeout: 1h` so activity-code breakpoints don't time out.
- Each language has `.vscode/launch.json` with `Worker (debug)` + `Start: <demo>` configs.
  Open the language sub-folder as the workspace root in Cursor/VSCode.

## Scripts (`scripts/`, language-agnostic where possible)

`run-worker.sh <lang>` · `start.sh <lang> <demo> [args]` · `signal.sh` (+ `signal-proceed/add/done.sh`)
· `history.sh` (tdbg decode) · `describe.sh` · `sql.sh q1|q2|q3|all` · `reset.sh`.
`lib.sh` holds shared config and resolves `$TEMPORAL_SRC` (for `tdbg`) and the Gradle wrapper.
`start.sh <lang> multiactivity [a] [b] [id]`; other demos take `[name] [id]`.

## Docs

- `README.md` — setup + the SQL queries + the labs + timeout recipe.
- `DEMOS.md` — the 5 demos, the scripts table, step-by-step runs.
- `DETERMINISM-PLAYBOOK.md` — instructor SAY/DO/EXPECT script for the determinism demo.
- `TDBG-RUNBOOK.md` — using `tdbg` (in the temporal checkout) to decode the SQL blobs.
- `java/README.md` — Java/Gradle guide for newcomers.

## Build / verify

- **Go** (`go/`): `go build ./...` (standalone module `temporal-workshop`; no go.work here).
- **Python** (`python/`): `python -m venv .venv && pip install -r requirements.txt`.
- **Java** (`java/`): `./gradlew build`. Gradle **9.1** via the committed wrapper; compiles to
  Java 17 bytecode using whatever JDK 17+ you have (no toolchain auto-download — a Foojay
  resolver was tried but is incompatible with Gradle 9). Verified: `BUILD SUCCESSFUL`.

## Status at migration

All three languages verified (Go builds, Python compiles, Java builds). Not yet run
end-to-end against a live server. The server patch is uncommitted in the temporal checkout.
