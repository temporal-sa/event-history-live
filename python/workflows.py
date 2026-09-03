from datetime import timedelta

from temporalio import workflow

with workflow.unsafe.imports_passed_through():
    from activities import compose_greeting, compose_farewell, add, double
    from shared import GreetingInput, MathInput, DoubleInput


@workflow.defn
class GreetingWorkflow:
    @workflow.run
    async def run(self, inp: GreetingInput) -> str:
        workflow.logger.info("GreetingWorkflow started")
        # >>> BREAKPOINT (workflow code): pause here to inspect the DB mid-task. <<<
        # Requires the worker's debug_mode=True (see worker.py) so this line is
        # reached on the main thread and the breakpoint actually fires.
        return await workflow.execute_activity(
            compose_greeting,
            inp,
            # Large timeout so a breakpoint inside the activity never trips it.
            start_to_close_timeout=timedelta(hours=1),
        )


@workflow.defn
class PipelineWorkflow:
    """Computes 2*(a+b): Add a+b, then Double the result. Each activity is a durable checkpoint."""

    @workflow.run
    async def run(self, inp: MathInput) -> int:
        opts = dict(start_to_close_timeout=timedelta(hours=1))
        # >>> BREAKPOINT (workflow) <<<
        total = await workflow.execute_activity(add, inp, **opts)
        return await workflow.execute_activity(double, DoubleInput(value=total), **opts)


@workflow.defn
class ApprovalWorkflow:
    def __init__(self) -> None:
        self._approvers: list[str] = []
        self._done = False

    @workflow.signal
    def add(self, approver: str) -> None:
        # >>> BREAKPOINT (signal handler) <<<
        self._approvers.append(approver)

    @workflow.signal
    def done(self) -> None:
        self._done = True

    @workflow.run
    async def run(self, inp: GreetingInput) -> str:
        await workflow.wait_condition(lambda: self._done)
        return f"Greeting for {inp.name} approved by: {', '.join(self._approvers)}"


@workflow.defn
class NonDeterminismWorkflow:
    """Demonstrates a NON-DETERMINISM error (no versioning).

    DEMO STEPS:
      1. Start it:            scripts/start.sh python nondet
      2. It runs one activity, then PARKS on the "proceed" signal (history stays open).
      3. Kill the worker.
      4. UNCOMMENT the block below (adds a NEW activity command BEFORE the park).
      5. Restart the worker.
      6. Send the signal:     scripts/signal.sh proceed
      7. Replay generates a command that was never recorded -> the workflow task FAILS
         with a non-determinism error and the workflow wedges. Inspect: scripts/history.sh
    """

    def __init__(self) -> None:
        self._proceed = False

    @workflow.signal
    def proceed(self) -> None:
        self._proceed = True

    @workflow.run
    async def run(self, inp: GreetingInput) -> str:
        greeting = await workflow.execute_activity(
            compose_greeting, inp, start_to_close_timeout=timedelta(hours=1)
        )
        # ┌── STEP 2: UNCOMMENT this block, then kill + restart the worker ──────────────┐
        # farewell = await workflow.execute_activity(
        #     compose_farewell, inp, start_to_close_timeout=timedelta(hours=1)
        # )
        # greeting = f"{greeting} {farewell}"
        # └────────────────────────────────────────────────────────────────────────────────┘
        await workflow.wait_condition(lambda: self._proceed)
        return greeting


@workflow.defn
class VersionedWorkflow:
    """Demonstrates SAFE evolution of the SAME change using workflow.patched().

    DEMO STEPS (same as NonDeterminismWorkflow, but it does NOT break):
      1. Start it:            scripts/start.sh python versioned
      2. It parks on "proceed".
      3. Kill the worker, UNCOMMENT the block below, restart the worker.
      4. Send the signal:     scripts/signal.sh proceed
      5. patched("add-farewell") returns False for the OLD history (predates the patch)
         -> farewell SKIPPED -> replay matches -> the workflow COMPLETES cleanly.
      6. Start a NEW versioned workflow: patched() returns True, records a marker, and
         runs the farewell. Old and new coexist. Compare with scripts/history.sh.
    """

    def __init__(self) -> None:
        self._proceed = False

    @workflow.signal
    def proceed(self) -> None:
        self._proceed = True

    @workflow.run
    async def run(self, inp: GreetingInput) -> str:
        greeting = await workflow.execute_activity(
            compose_greeting, inp, start_to_close_timeout=timedelta(hours=1)
        )
        # ┌── STEP 2: UNCOMMENT this block, then kill + restart the worker ──────────────┐
        # if workflow.patched("add-farewell"):
        #     farewell = await workflow.execute_activity(
        #         compose_farewell, inp, start_to_close_timeout=timedelta(hours=1)
        #     )
        #     greeting = f"{greeting} {farewell}"
        # └────────────────────────────────────────────────────────────────────────────────┘
        await workflow.wait_condition(lambda: self._proceed)
        return greeting
