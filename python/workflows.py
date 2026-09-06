from datetime import timedelta

from temporalio import workflow

with workflow.unsafe.imports_passed_through():
    from greeting_activities import compose_greeting, compose_farewell
    from math_activities import add, double, square
    from shared import GreetingInput, MathInput, DoubleInput, SquareInput


@workflow.defn
class GreetingWorkflow:
    @workflow.run
    async def run(self, inp: GreetingInput) -> str:
        workflow.logger.info("GreetingWorkflow started")
        # >>> BREAKPOINT (workflow code): pause here to inspect the DB mid-task. <<<
        # Requires the worker's debug_mode=True (see worker.py) so this line is
        # reached on the main thread and the breakpoint actually fires.
        greeting = await workflow.execute_activity(
            compose_greeting,
            inp,
            # Large timeout so a breakpoint inside the activity never trips it.
            start_to_close_timeout=timedelta(hours=1),
        )
        # >>> BREAKPOINT (workflow): the activity result is inspectable here. <<<
        return greeting


@workflow.defn
class PipelineWorkflow:
    """Computes 2*(a+b): Add a+b, then Double the result. Each activity is a durable checkpoint."""

    @workflow.run
    async def run(self, inp: MathInput) -> int:
        opts = dict(start_to_close_timeout=timedelta(hours=1))
        # >>> BREAKPOINT (workflow) <<<
        total = await workflow.execute_activity(add, inp, **opts)
        doubled = await workflow.execute_activity(double, DoubleInput(value=total), **opts)
        # >>> BREAKPOINT (workflow): inspect total and doubled here. <<<
        return doubled


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

    Arithmetic shape:  add(a,b) -> [ double ] -> park on "proceed" -> square
    With the block below commented out, a=3 b=4 yields 7*7 = 49.

    DEMO STEPS:
      1. Start it:            scripts/start.sh python nondet 3 4
      2. It runs add(), then PARKS waiting for the "proceed" signal (history stays open).
      3. Kill the worker.
      4. UNCOMMENT the block below. It inserts a NEW activity command between add() and
         the park.
      5. Restart the worker.
      6. Send the signal:     scripts/signal.sh proceed
      7. Replay reaches the point where history recorded TimerStarted (the park's timeout)
         but the new code issues ScheduleActivityTask(double) instead -> the workflow task
         FAILS with a non-determinism error and the workflow wedges. Inspect:
         scripts/history.sh

    WHY THE PARK HAS A TIMEOUT: non-determinism is only detected when a replayed command
    CONTRADICTS a command already recorded in history. A bare wait_condition() records no
    command at all, so a new activity added just before it would append past the end of
    recorded history — indistinguishable from normal progress — and the workflow would
    happily complete. Waiting WITH a timeout records a TimerStarted event, which is the
    recorded command the uncommented block collides with. (Bonus: the pending timer shows
    up in scripts/sql.sh q3.)
    """

    def __init__(self) -> None:
        self._proceed = False

    @workflow.signal
    def proceed(self) -> None:
        self._proceed = True

    @workflow.run
    async def run(self, inp: MathInput) -> int:
        opts = dict(start_to_close_timeout=timedelta(hours=1))
        total = await workflow.execute_activity(add, inp, **opts)

        # ┌── STEP 2: UNCOMMENT this block, then kill + restart the worker ──────────────┐
        # total = await workflow.execute_activity(double, DoubleInput(value=total), **opts)
        # └────────────────────────────────────────────────────────────────────────────────┘

        # Park until "proceed" so history stays open across the worker restart. The
        # timeout records a command (TimerStarted) *after* the block above — that is what
        # makes the mismatch detectable.
        await workflow.wait_condition(lambda: self._proceed, timeout=timedelta(hours=1))

        squared = await workflow.execute_activity(square, SquareInput(value=total), **opts)
        # >>> BREAKPOINT (workflow): inspect total and squared here. <<<
        return squared


@workflow.defn
class VersionedWorkflow:
    """Demonstrates SAFE evolution of the SAME change using workflow.patched().

    Arithmetic shape:  add(a,b) -> [ double, gated by patched() ] -> park -> square

    DEMO STEPS (same as NonDeterminismWorkflow, but it does NOT break):
      1. Start it:            scripts/start.sh python versioned 3 4
      2. It runs add(), then parks on "proceed".
      3. Kill the worker, UNCOMMENT the block below, restart the worker.
      4. Send the signal:     scripts/signal.sh proceed
      5. patched("double-the-sum") returns False for the OLD history (it predates the
         patch) -> the double is SKIPPED -> replay matches -> COMPLETES cleanly with 49.
      6. Start a NEW versioned workflow: patched() returns True, records a marker, and
         runs the double, returning (2*7)^2 = 196. Old and new coexist from one codebase.
         Compare with scripts/history.sh.
    """

    def __init__(self) -> None:
        self._proceed = False

    @workflow.signal
    def proceed(self) -> None:
        self._proceed = True

    @workflow.run
    async def run(self, inp: MathInput) -> int:
        opts = dict(start_to_close_timeout=timedelta(hours=1))
        total = await workflow.execute_activity(add, inp, **opts)

        # ┌── STEP 2: UNCOMMENT this block, then kill + restart the worker ──────────────┐
        # if workflow.patched("double-the-sum"):
        #     total = await workflow.execute_activity(double, DoubleInput(value=total), **opts)
        # └────────────────────────────────────────────────────────────────────────────────┘

        # Same timeout-backed park as NonDeterminismWorkflow — see the note there for why
        # the park records a command.
        await workflow.wait_condition(lambda: self._proceed, timeout=timedelta(hours=1))

        squared = await workflow.execute_activity(square, SquareInput(value=total), **opts)
        # >>> BREAKPOINT (workflow): inspect total and squared here. <<<
        return squared
