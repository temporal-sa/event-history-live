# Workshop Demos — Multi-Activity, Signals & Non-Determinism

Five runnable workflows, identical across **Go / Python / Java**, plus a `scripts/`
toolkit to drive them. Builds on the setup in [`README.md`](README.md) (MySQL server + a
worker per language).

| Demo | Workflow | Shows |
|------|----------|-------|
| Hello | `GreetingWorkflow` | one activity — the basics |
| Multi-activity | `PipelineWorkflow` | math pipeline `2*(a+b)` — chaining activities, stepping through each |
| Signal | `ApprovalWorkflow` | signal handlers, breakpointing a handler, signal events in history |
| Non-determinism | `NonDeterminismWorkflow` | how an incompatible code change **wedges** a running workflow |
| Versioned | `VersionedWorkflow` | the **same** change made safe with `GetVersion`/`patched` |

**How it's wired:** each language runs **one worker** on its own task queue
(`hello-go` / `hello-python` / `hello-java`) that registers all five workflows. You pick
which to start. All demos use workflow id `demo-wf` by default.

---

## Running demos: two ways

**From the IDE (best for breakpoints).** Open the language sub-folder as the workspace root,
run **"Worker (debug)"**, then run the matching **"Start: <demo>"** launch config.

**From the terminal (best for the non-determinism demo).**
```bash
scripts/run-worker.sh <go|python|java>          # start the worker (Ctrl-C to kill)
scripts/start.sh <lang> <demo> [name] [id]      # start a workflow
```
> Java from the terminal needs `gradle` on PATH (`brew install gradle`); otherwise use the
> IDE launch configs. Go and Python work out of the box.

---

## The `scripts/` toolkit

All scripts live in `scripts/` and default to workflow id `demo-wf`.

| Script | What it does |
|--------|--------------|
| `run-worker.sh <lang>` | Run a language's worker in the terminal |
| `start.sh <lang> <demo> [name] [id]` | Start a workflow (`hello`/`multiactivity`/`signal`/`nondet`/`versioned`) |
| `signal.sh <name> [payload] [id]` | Send any signal |
| `signal-proceed.sh [id]` | Unpark a nondet/versioned demo (`proceed`) |
| `signal-add.sh <name> [id]` | Add an approver to the signal demo (`add`) |
| `signal-done.sh [id]` | Finish the signal demo (`done`) |
| `history.sh [id] [filter]` | Decoded history via tdbg (optionally grep a filter) |
| `describe.sh [id]` | Decoded mutable state (tdbg) + status (temporal CLI) |
| `sql.sh [q1\|q2\|q3\|all] [id]` | Run the workshop MySQL queries |
| `reset.sh [id]` | Terminate the workflow to re-run cleanly |

---

## Demo 1 — Multi-activity (a math pipeline)

`PipelineWorkflow` takes a `MathInput{a, b}` and computes **`2*(a+b)`** with two activities:
`Add(a, b)` → then `Double(sum)`. With the defaults `a=3, b=4` the result is `2*(3+4) = 14`.

```bash
scripts/start.sh go multiactivity            # a=3 b=4  -> Result: 14
scripts/start.sh go multiactivity 10 5       # a=10 b=5 -> Result: 30
```

1. Set breakpoints in `Add` and `Double` (and/or on the workflow line).
2. Run **"Worker (debug)"**, then **"Start: multiactivity"** (or the command above).
3. Step through each activity. After it completes, watch the history:
   ```bash
   scripts/history.sh                 # two ActivityTaskScheduled/Completed pairs (Add, Double)
   scripts/sql.sh q2                  # the same, as raw appending rows
   ```

**Teaching point:** each activity is a durable checkpoint — `Add`'s result (`a+b`) is written to
history before `Double` runs. Kill the worker between the two activities and restart: it resumes
from the recorded sum and never re-runs `Add`. That's how Temporal turns a plain function chain
into a crash-proof one.

---

## Demo 2 — Signals (a human-in-the-loop gate)

`ApprovalWorkflow` collects approver names via `add` signals and finishes on `done`.

```bash
scripts/start.sh go signal                 # parks, waiting for signals
scripts/signal-add.sh Alice
scripts/signal-add.sh Bob
scripts/history.sh                         # two WorkflowExecutionSignaled events appended
scripts/signal-done.sh                     # workflow completes
scripts/describe.sh                        # status Completed
```

**Teaching points:**
- Breakpoint the `add` handler — it fires each time a signal arrives.
- Between signals the workflow is idle and its history is frozen; each signal appends exactly
  one `WorkflowExecutionSignaled` event. Signals are how the outside world durably pokes a
  running workflow.

---

## Demo 3 — Non-determinism (watch a workflow wedge)

`NonDeterminismWorkflow` runs one activity, then parks on `proceed`. A commented-out second
activity sits **before** the park. Uncommenting it changes the recorded command sequence.

```bash
# 1. Start it — runs ComposeGreeting, then parks.
scripts/start.sh go nondet
scripts/history.sh                         # history stops after the first activity

# 2. Kill the worker (Ctrl-C in its terminal).

# 3. In nondeterminism.go, UNCOMMENT the marked block (the second activity).

# 4. Restart the worker:
scripts/run-worker.sh go

# 5. Unpark it → triggers replay against the new code:
scripts/signal-proceed.sh

# 6. Observe the failure:
scripts/history.sh demo-wf WorkflowTaskFailed   # non-determinism error, retrying
scripts/describe.sh                             # still Running — WEDGED, not corrupted
```

**Teaching point:** the workflow doesn't crash or silently diverge — replay refuses to proceed
and parks it safely until you ship code that replays cleanly. That's Demo 4.

---

## Demo 4 — Versioned (the safe fix)

`VersionedWorkflow` is identical, but its commented block wraps the second activity in
`GetVersion` (Go/Java) / `patched` (Python). Run Demo 3's steps against `versioned`:

```bash
scripts/start.sh go versioned              # parks on proceed
# kill worker → UNCOMMENT the block in versioned.go → restart worker
scripts/run-worker.sh go
scripts/signal-proceed.sh                  # replays cleanly, COMPLETES
scripts/describe.sh                        # status Completed
```

The old in-flight workflow finds **no version marker** in its history → takes the
`DefaultVersion` path → skips the new activity → replay matches → it completes. Then start a
**fresh** versioned workflow and compare:

```bash
scripts/start.sh go versioned Temporal demo-wf-2
scripts/history.sh demo-wf-2 MarkerRecorded   # the NEW run records a version marker + runs the activity
```

**Teaching point:** one codebase, two behaviors keyed on a marker in history — old executions
and new executions both replay correctly. This is how you evolve long-running workflows in
production without draining them.

> For a fuller instructor script of Demos 3–4 (SAY/DO/EXPECT beats, MySQL observation,
> troubleshooting), see [`DETERMINISM-PLAYBOOK.md`](DETERMINISM-PLAYBOOK.md).

---

## Reset between runs

`demo-wf` can't be reused while it's still Running (the nondet/signal demos park). Reset with:

```bash
scripts/reset.sh                    # terminate demo-wf
# or start with a different id:  scripts/start.sh go nondet Temporal demo-wf-3
# or wipe everything:            make install-schema-mysql
```
