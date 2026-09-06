package workshop;

import java.time.Duration;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;

/**
 * Demonstrates a NON-DETERMINISM error (no versioning).
 *
 * Arithmetic shape:  add(a,b) -> [ doubleValue ] -> park on "proceed" -> square
 * With the block below commented out, a=3 b=4 yields 7*7 = 49.
 *
 * DEMO STEPS:
 *   1. Start it:            scripts/start.sh java nondet 3 4
 *   2. It runs add(), then PARKS waiting for the "proceed" signal (history stays open).
 *   3. Kill the worker.
 *   4. UNCOMMENT the block below. It inserts a NEW activity command between add() and
 *      the park.
 *   5. Restart the worker.
 *   6. Send the signal:     scripts/signal.sh proceed
 *   7. Replay reaches the point where history recorded TimerStarted (the park's timeout)
 *      but the new code issues ScheduleActivityTask(doubleValue) instead -> the workflow
 *      task FAILS with a non-determinism error and the workflow wedges (Running, never
 *      completes). Inspect: scripts/history.sh
 *
 * WHY THE PARK HAS A TIMEOUT: non-determinism is only detected when a replayed command
 * CONTRADICTS a command already recorded in history. A bare Workflow.await() records no
 * command at all, so a new activity added just before it would append past the end of
 * recorded history — indistinguishable from normal progress — and the workflow would
 * happily complete. Awaiting WITH a timeout records a TimerStarted event, which is the
 * recorded command the uncommented block collides with. (Bonus: the pending timer shows
 * up in scripts/sql.sh q3.)
 */
public class NonDeterminismWorkflowImpl implements NonDeterminismWorkflow {

    private final MathActivities activities =
        Workflow.newActivityStub(
            MathActivities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofHours(1)).build());

    private boolean proceed;

    @Override
    public void proceed() {
        this.proceed = true;
    }

    @Override
    public int run(MathInput in) {
        int sum = activities.add(in);

        // ┌── STEP 2: UNCOMMENT this block, then kill + restart the worker ───────────────┐
        // sum = activities.doubleValue(new DoubleInput(sum));
        // └────────────────────────────────────────────────────────────────────────────────┘

        // Park until "proceed" so history stays open across the worker restart. The
        // timeout records a command (TimerStarted) *after* the block above — that is what
        // makes the mismatch detectable.
        Workflow.await(Duration.ofHours(1), () -> proceed);

        int squared = activities.square(new SquareInput(sum));
        // >>> BREAKPOINT (workflow): inspect sum and squared here. <<<
        return squared;
    }
}
