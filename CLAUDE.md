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
  the server the demos run against. Pulled from `main` and built **as-is — no source patches**.

### Build the server in debug mode (in the temporal checkout, NOT here)
`make temporal-server-debug` compiles with the `TEMPORAL_DEBUG` build tag, which multiplies the
server's internal timeouts ×100 — enough slack for a debugging session to survive while paused
at a breakpoint. Note the `maxWorkflowTaskStartToCloseTimeout` cap (`120 * time.Second` on stock
`main`) is a plain constant the ×100 multiplier does *not* touch, so a **workflow-code**
breakpoint is bounded at ~2 min; **activity** breakpoints have no such cap (only their own
`StartToCloseTimeout`), so they're the path for long holds.

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
  `GreetingInput{name}`, `MathInput{a,b}`, `DoubleInput{value}`, `SquareInput{value}`.
- **Signals**: `proceed` (nondet/versioned), `add`(string approver) + `done` (approval demo).
- **Operations are deliberately primitive & visualizable**, one per category:
  arithmetic (math pipeline), string concat (greeting), array manipulation (approval).
- **Never `return` an activity result directly** — hoist it into a local first
  (`int squared = activities.square(...); return squared;`). A direct return gives the
  workshop audience nowhere to stand: you can't inspect the computed value at a breakpoint.
  Each such local is followed by a `>>> BREAKPOINT (workflow): inspect ... <<<` marker, and
  the markers line up across the three languages.
- **Activities are grouped by domain, not dumped in one bag.** Go `greeting_activities.go` /
  `math_activities.go`; Python `greeting_activities.py` / `math_activities.py`; Java
  `GreetingActivities(+Impl)` (`composeGreeting`/`composeFarewell`) and `MathActivities(+Impl)`
  (`add`/`doubleValue`/`square`). The Java worker registers **both** impls.

### The 5 demos
| Key | Workflow | Operation |
|-----|----------|-----------|
| `hello` | `GreetingWorkflow` | `ComposeGreeting` → `"Hello, {name}!"` |
| `multiactivity` | `PipelineWorkflow` | `Add(a,b)` → `Double(sum)` = **2*(a+b)** |
| `signal` | `ApprovalWorkflow` | `add`/`done` signals accumulate an approver array |
| `nondet` | `NonDeterminismWorkflow` | `Add(a,b)` → *[commented `Double`]* → signal park → `Square` |
| `versioned` | `VersionedWorkflow` | same, but the commented `Double` is gated by `GetVersion`/`patched` |

The nondet/versioned demo flow: start → parks on `proceed` → kill worker → **uncomment** the
marked block → restart → `scripts/signal-proceed.sh` → NDE (unversioned) or clean completion
(versioned). With `a=3 b=4`: unversioned baseline / versioned old run = `7^2` = **49**; a fresh
versioned run = `(2*7)^2` = **196**. Activities named per-language: Go `Add`/`Double`/`Square`,
Python `add`/`double`/`square`, Java `add`/`doubleValue`/`square` (`double` is a Java keyword).

**IMPORTANT — why the park uses a timer.** The park is `AwaitWithTimeout` / `Workflow.await(1h,…)`
/ `wait_condition(…, timeout=1h)`, **not** a bare signal wait. Non-determinism is only detected
when a replayed command *contradicts a command already recorded in history*. A bare await records
no command, so history ends with a command-less `WorkflowTaskCompleted` and a newly inserted
activity just appends past the end of recorded history — indistinguishable from normal progress,
and the workflow completes happily (this was a real bug in the first cut of these demos). The
timeout records a `TimerStarted` event, which is the recorded command the uncommented `Double`
collides with. Keep the commented block **between `Add` and the park**; moving it after the park
breaks the demo again.

## Debugging settings (already wired into the apps + launch configs)

- **Deadlock detector** off for workflow-code breakpoints: Go/Java env `TEMPORAL_DEBUG=true`
  (Go worker also sets `DeadlockDetectionTimeout: 15m`); Python `debug_mode=True`.
- **Python** also needs `UnsandboxedWorkflowRunner()` or breakpoints in `@workflow.run` never
  fire; launch config sets `justMyCode: false`.
- **Timeouts**: starters request a generous `WorkflowTaskTimeout` (the stock server caps it at
  ~2 min); activities set `StartToCloseTimeout: 1h` so activity-code breakpoints don't time out.
- Each language has `.vscode/launch.json` with `Worker (debug)` + `Start: <demo>` configs.
  Open the language sub-folder as the workspace root in Cursor/VSCode.

## Scripts (`scripts/`, language-agnostic where possible)

`run-worker.sh <lang>` · `start.sh <lang> <demo> [args]` · `signal.sh` (+ `signal-proceed/add/done.sh`)
· `history.sh` (tdbg decode) · `describe.sh` · `sql.sh q1|q2|q3|all` · `reset.sh`.
`lib.sh` holds shared config and resolves the temporal checkout (for `tdbg`) and the Gradle wrapper.
`start.sh <lang> <multiactivity|nondet|versioned> [a] [b] [id]`; `hello`/`signal` take `[name] [id]`.

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
end-to-end against a live server. The server is built from `temporalio/temporal` `main` in
debug mode (`make temporal-server-debug`) — no source patches.
