# Hello World — Python

Open **this folder** (`python`) as the workspace root in Cursor/VSCode.

## Setup

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

Requires the [Python extension](https://marketplace.visualstudio.com/items?itemName=ms-python.python).
Select the `.venv` interpreter (Command Palette → "Python: Select Interpreter").

## Run with a breakpoint

1. Make sure the workshop server + MySQL are running (see `../README.md`).
2. Set a breakpoint:
   - **Recommended (Lab 3):** inside `compose_greeting` in `activities.py`.
   - **Workflow code:** on the `execute_activity` line in `workflows.py`.
3. Run **"Worker (debug)"** from the Run and Debug panel (▶). It uses
   `debug_mode=True` + `UnsandboxedWorkflowRunner()` so breakpoints in workflow
   code actually fire (`justMyCode` is off so activity breakpoints work too).
4. Run **"Start demo-wf"** to kick off the workflow.
5. The breakpoint hits. Switch to your `mysql` window and run the queries in `../README.md`.

- Task queue: `hello-python` · Workflow ID: `demo-wf`

> **Note:** breakpoints in `@workflow.run` only fire because of `debug_mode` +
> the unsandboxed runner. Both are debug-only conveniences — never ship them.
