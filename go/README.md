# Hello World — Go

Open **this folder** (`go`) as the workspace root in Cursor/VSCode.

## Setup

```bash
go mod tidy   # downloads the Temporal Go SDK
```

Requires the [VSCode Go extension](https://marketplace.visualstudio.com/items?itemName=golang.Go)
(and Delve, which it installs on first debug).

## Run with a breakpoint

1. Make sure the workshop server + MySQL are running (see `../README.md`).
2. Set a breakpoint:
   - **Recommended (Lab 3):** inside `ComposeGreeting` in `greeting_activities.go`
     (arithmetic activities live in `math_activities.go`).
   - **Workflow code:** on the `ExecuteActivity` line in `GreetingWorkflow`.
3. Run **"Worker (debug)"** from the Run and Debug panel (▶). It sets
   `TEMPORAL_DEBUG=true` so a workflow-code breakpoint won't hit the deadlock detector.
4. Run **"Start demo-wf"** (no debugger needed) to kick off the workflow.
5. The breakpoint hits. Switch to your `mysql` window and run the queries in `../README.md`.

- Task queue: `hello-go` · Workflow ID: `demo-wf`
