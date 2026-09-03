# Playbook — Demonstrating Workflow Determinism & Versioning

An instructor script for the "why is Temporal deterministic, and what happens when you
break it" segment. Runs on the workshop setup in [`README.md`](README.md) (MySQL server +
the Go/Python/Java Hello World app).

**Duration:** ~15 min · **Format:** three acts — *establish → break → rescue*.

> **Packaged version:** the apps now ship ready-made `NonDeterminismWorkflow` and
> `VersionedWorkflow` (uncomment-a-block demos) plus `scripts/` to drive them — see
> [`DEMOS.md`](DEMOS.md) Demos 3–4. This playbook is the deeper instructor script (the SAY/DO/
> EXPECT beats, MySQL observation, troubleshooting); the mechanics are the same, and it uses a
> hand-edited `hello.go` variant so you can narrate every step.

**The one sentence they should leave with:**
> Temporal rebuilds a workflow's state by replaying its history through your code. If a
> code change makes replay produce a different sequence of commands, replay fails — the
> execution wedges (it does not corrupt or silently diverge). Versioning lets old and new
> code coexist so in-flight workflows survive the change.

---

## 0 · Pre-flight (do this before the audience is watching)

- [ ] Workshop server running on MySQL with the 15-min cap (`README.md` → Quick start).
- [ ] `mysql` client window open and projected (Terminal D).
- [ ] The demo app open in Cursor/VSCode (Go shown below; Python/Java equivalents in §6).
- [ ] **Replace the workflow body with the blocking baseline** in §1 so the workflow parks
      mid-flight. Without a pause there is no partial history to replay, and the demo can't work.
- [ ] Know your three edits (baseline → broken → versioned). Have them ready to paste.

> **Mental model to keep saying:** *history is the source of truth; the running workflow is
> a cache rebuilt from it.* Everything in this demo is about that rebuild succeeding or failing.

---

## 1 · Baseline: a workflow that parks with partial history

Set `go/hello.go` `GreetingWorkflow` to this. It runs one activity, then **blocks on
a signal** — leaving durable, half-finished history for us to replay against.

```go
func GreetingWorkflow(ctx workflow.Context, name string) (string, error) {
	ctx = workflow.WithActivityOptions(ctx, workflow.ActivityOptions{
		StartToCloseTimeout: time.Hour,
	})

	var greeting string
	// COMMAND #0 — this is the command replay will check first.
	_ = workflow.ExecuteActivity(ctx, ComposeGreeting, name).Get(ctx, &greeting)

	// Park here until a "proceed" signal. History is now durably recorded to this point.
	var sig string
	workflow.GetSignalChannel(ctx, "proceed").Receive(ctx, &sig)

	return greeting, nil
}
```

### Run it

| # | DO | EXPECT |
|---|----|--------|
| 1 | Run **"Worker (debug)"**, then **"Start demo-wf"**. | Starter logs a RunID, then blocks (it's waiting for the result). |
| 2 | In `mysql`, run **Q2** (history ledger, see §5). | Rows through `WorkflowExecutionStarted → …ActivityTaskCompleted → WorkflowTaskCompleted`, then nothing. |
| 3 | Rerun Q2 a couple times. | **Byte-identical.** |

**SAY:** *"The workflow is parked. Its history is finished up to the signal wait and written
to disk. Notice the log stops growing — history only advances on real events, and nothing has
happened yet. Remember command #0 is the activity."*

---

## 2 · Act I — break determinism

Now change the code so replay produces a **different command in a slot that history already
recorded**. The reliable way: **insert a new command *before* the recorded activity.**

### The breaking edit

Stop the worker. Add a timer as the new first line of the workflow:

```go
	_ = workflow.Sleep(ctx, time.Second)                                          // NEW command #0
	var greeting string
	_ = workflow.ExecuteActivity(ctx, ComposeGreeting, name).Get(ctx, &greeting)  // was #0, now #1
```

**SAY:** *"Innocent-looking — I added a one-second sleep at the top. But I've shifted the
command sequence: history says command #0 is 'schedule activity'; my new code says command #0
is 'start timer'."*

### Trigger the replay

| # | DO | EXPECT |
|---|----|--------|
| 1 | Restart **"Worker (debug)"**. | Worker starts, quiet — no replay yet (nothing has scheduled a task). |
| 2 | `temporal workflow signal --workflow-id demo-wf --name proceed` | The signal schedules a workflow task → worker replays history against the new code. |
| 3 | Watch the **worker log**. | A **non-determinism error** — e.g. *"nondeterministic workflow… history event is ActivityTaskScheduled but code generated StartTimer"*. |
| 4 | In the UI (<http://localhost:8080>) open `demo-wf`, or run **Q2**. | New `WorkflowTaskFailed` events, cause `NON_DETERMINISM_ERROR`. `next_event_id` climbs with each retry. |
| 5 | Rerun Q1 after ~30s. | Workflow is **still Running** — not Failed, not Completed. It's **wedged**, retrying forever. |

**SAY (the punchline):** *"A bad deploy didn't corrupt anything and didn't silently do the
wrong thing — the two worst outcomes in distributed systems. Replay refused to proceed and
parked the workflow safely. It will sit here until we ship code that replays cleanly."*

---

## 3 · Act II — rescue with versioning

Leave the stuck workflow exactly as it is. Now ship the *same* logical change, gated by
`GetVersion`, and watch the wedged execution recover.

### The versioned edit

Stop the worker. Replace the bare `Sleep` with:

```go
	// GetVersion writes a marker for NEW executions and returns DefaultVersion for
	// histories recorded before this line existed.
	v := workflow.GetVersion(ctx, "add-initial-sleep", workflow.DefaultVersion, 1)
	if v != workflow.DefaultVersion {
		_ = workflow.Sleep(ctx, time.Second)   // only NEW workflows take this path
	}
	var greeting string
	_ = workflow.ExecuteActivity(ctx, ComposeGreeting, name).Get(ctx, &greeting)
```

### Watch it recover

| # | DO | EXPECT |
|---|----|--------|
| 1 | Restart **"Worker (debug)"**. | On the next retry, the wedged workflow replays with the new code. |
| 2 | Watch the worker log / UI / Q1. | `GetVersion` finds **no marker** in the old history → returns `DefaultVersion` → skips the sleep → commands match → **the NDE clears**. The signal is processed and the workflow **Completes**. |
| 3 | Run **"Start demo-wf"** again (a fresh workflow), then signal it. | Q2 for the new run shows a `MarkerRecorded` (version) event — the new execution took the sleep path. |

**SAY:** *"Same source change, but now the old in-flight workflow and every new workflow both
replay cleanly — from a single codebase. That marker is how Temporal remembers which branch a
given execution was born into. This is how you evolve long-running workflows in production
without draining them first."*

---

## 4 · Optional 60-second variations

- **Non-determinism from data, not structure:** put `if workflow.Now(ctx).Nanosecond()%2 == 0`
  around the activity, restart, signal. Same NDE — proves it's not just "adding lines," it's
  *any* replay-time divergence. (Then delete it.)
- **A *safe* change needs no version:** edit `ComposeGreeting`'s returned string and rerun a
  fresh workflow — no NDE, because **activities run fresh and never replay**. Great contrast.

---

## 5 · SQL you'll run live (MySQL)

```sql
-- Q1 · is it Running / wedged / done, and how many events?
SELECT workflow_id, next_event_id, db_record_version, HEX(run_id) AS run_id
FROM executions WHERE workflow_id = 'demo-wf';

-- Q2 · the ledger — watch WorkflowTaskFailed pile up, then stop after the fix
SELECT node_id, txn_id, prev_txn_id, LENGTH(data) AS bytes
FROM history_node
WHERE tree_id = (SELECT run_id FROM executions WHERE workflow_id = 'demo-wf' LIMIT 1)
ORDER BY node_id;
```

To see the **decoded** failure reason (raw blobs won't show it), use tdbg:

```bash
make tdbg
./tdbg workflow show --workflow-id demo-wf | grep -i -A3 "WorkflowTaskFailed"
```

---

## 6 · Other SDKs — same demo, different one-liners

| Step | Go | Python | Java |
|------|----|--------|------|
| Park on signal | `workflow.GetSignalChannel(ctx,"proceed").Receive(ctx,&s)` | `await workflow.wait_condition(lambda: self._go)` + a `@workflow.signal` setter | `Workflow.await(() -> proceed)` + `@SignalMethod` |
| Breaking edit | add `workflow.Sleep(ctx, …)` before the activity | add `await asyncio.sleep(1)` → use `workflow.sleep(1)` before the activity | add `Workflow.sleep(Duration.ofSeconds(1))` before the activity |
| Versioned fix | `workflow.GetVersion(ctx,"add-initial-sleep",workflow.DefaultVersion,1)` | `if workflow.patched("add-initial-sleep"):` | `Workflow.getVersion("add-initial-sleep",Workflow.DEFAULT_VERSION,1)` |

> Python: use `workflow.sleep()`, never `asyncio.sleep()`, inside workflow code — the latter
> isn't durable. Java signal handlers set a field the `Workflow.await` predicate reads.

---

## 7 · What breaks replay vs. what's safe

**Breaks (needs versioning):**
- Adding / removing / **reordering** activities, timers, child workflows, or signals-sent in
  code that has **already executed** in existing histories.
- Renaming a workflow or activity type.
- Branching workflow logic on wall-clock time, randomness, `os`/env, or map-iteration order.

**Safe (no versioning):**
- Changing an **activity's implementation** — activities execute fresh, never replay.
- Code that only runs in **brand-new** workflows.
- Comments, logging, refactors that don't change the command sequence.

---

## 8 · Troubleshooting

| Symptom | Cause / fix |
|---------|-------------|
| No NDE after the breaking edit | The workflow already **Completed** (you signaled before editing), or your worker still has the old code cached. Start a **fresh** `demo-wf`, park it, *then* edit and restart the worker. |
| NDE won't clear after the versioned fix | Worker didn't actually restart with new code, or you gated the wrong branch. Confirm the `GetVersion` change ID string is stable and the old path is truly unchanged. |
| Breakpoint/deadlock panic instead of NDE | That's the deadlock detector, not determinism. Ensure `TEMPORAL_DEBUG=true` (worker launch config). |
| Signal command not found | Namespace or workflow id mismatch — must be `--workflow-id demo-wf` in namespace `default`. |

### Reset between dry-runs

```bash
temporal workflow terminate --workflow-id demo-wf --reason "reset"   # or let it complete
make install-schema-mysql                                            # nuke all data for a clean slate
```
