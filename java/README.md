# Hello World — Java

The Java version of the workshop demos. If you're new to Java, this README has everything
you need — you do **not** need to install Gradle or hunt for the right JDK by hand.

Open **this folder** (`java`) as the workspace root in Cursor/VSCode.

---

## Prerequisites (just one thing)

- **A JDK, version 17 or newer.** Check with `java -version`. If you don't have one, install
  a recent Temurin/OpenJDK (e.g. `brew install openjdk@21` on macOS).

That's it. Everything else is automatic:
- **Gradle** — you don't install it. The repo ships a *Gradle wrapper* (`./gradlew`) that
  downloads the correct Gradle version (9.1) on first use.
- **Bytecode target** — the project compiles to Java 17 bytecode using your installed JDK, so
  it builds on any JDK 17+ and runs on Java 17 and newer.

> **What is the Gradle wrapper?** `gradlew` is a small script checked into the repo. Running
> `./gradlew <task>` uses the exact Gradle version pinned in
> `gradle/wrapper/gradle-wrapper.properties` — so everyone gets identical builds with nothing
> to install. Always use `./gradlew`, never a system `gradle`.

---

## How this project is laid out

Java Temporal code uses an **interface + implementation** pair for each workflow. The interface
declares the entry point (and any signals); the `*Impl` class is the actual logic. The worker
registers the `*Impl` classes; clients talk to the interface.

```
src/main/java/workshop/
  GreetingActivities.java       # @ActivityInterface — string composition ┐ activity
  GreetingActivitiesImpl.java   #   composeGreeting / composeFarewell     ┘ signatures
  MathActivities.java           # @ActivityInterface — arithmetic         ┐ + their
  MathActivitiesImpl.java       #   add / doubleValue / square            ┘ implementations
  GreetingWorkflow.java         # @WorkflowInterface  ┐  Hello demo
  GreetingWorkflowImpl.java     #   implementation    ┘
  PipelineWorkflow(+Impl).java  # math pipeline: 2*(a+b)
  ApprovalWorkflow(+Impl).java  # signal demo: collect approvers
  NonDeterminismWorkflow(+Impl) # non-determinism demo
  VersionedWorkflow(+Impl)      # versioned (safe) demo
  GreetingInput / MathInput / DoubleInput / SquareInput .java   # payload structs
  WorkerApp.java                # registers everything, starts the worker
  Starter.java                  # starts a chosen workflow
```

---

## Build it (first run downloads Gradle + deps — be patient)

```bash
./gradlew build
```

If that succeeds, you're ready. (First run can take a few minutes; later runs are fast.)

---

## Run the demos

You need **two** things running: the **worker** (executes workflow/activity code) and a
**starter** (kicks off one workflow). Use the IDE for breakpoints, or the terminal.

### From the IDE (best for breakpoints)
1. Make sure the workshop server + MySQL are running (see [`../README.md`](../README.md)).
2. Set a breakpoint (e.g. in `GreetingActivitiesImpl.composeGreeting`, or
   `MathActivitiesImpl.add` for the math demos).
3. Run **"Worker (debug)"** from the Run and Debug panel (▶). It sets `TEMPORAL_DEBUG=true`
   so breakpoints in workflow code don't trip the deadlock detector.
4. Run a **"Start: …"** config (e.g. **"Start: hello"**).
5. The breakpoint hits → inspect the DB (see [`../DEMOS.md`](../DEMOS.md)).

### From the terminal
```bash
# Terminal 1 — the worker (Ctrl-C to stop):
./gradlew run
#   ...or from the repo's scripts:  scripts/run-worker.sh java

# Terminal 2 — start a workflow:
./gradlew runStarter --args="hello Temporal"          # hello
./gradlew runStarter --args="multiactivity 3 4"       # math pipeline -> 2*(3+4)=14
./gradlew runStarter --args="signal Temporal"         # approval (drive it with scripts/signal-add.sh)
./gradlew runStarter --args="nondet 3 4"              # non-determinism demo
#   ...or from the repo's scripts:  scripts/start.sh java multiactivity 3 4
```

`runStarter --args` takes: `<demo> <name> [id]`, except **multiactivity** which takes
`multiactivity <a> <b> [id]`.

The five demos and what they teach are catalogued in [`../DEMOS.md`](../DEMOS.md).

---

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| `./gradlew: Permission denied` | `chmod +x gradlew` |
| `Unsupported class file major version` / Gradle won't start | Your `java -version` is too old — use JDK 17+. |
| Build stalls on "Toolchain" download | Gradle is fetching JDK 17; give it a minute (needs internet). |
| `Connection refused` when starting | The workshop Temporal server isn't running — see `../README.md`. |
| VSCode shows red squiggles but `./gradlew build` works | Reload the Java project: Command Palette → "Java: Clean Language Server Workspace". |

- Task queue: `hello-java` · Workflow ID: `demo-wf`
