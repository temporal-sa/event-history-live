package workshop;

import java.time.Duration;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;

/**
 * Demonstrates SAFE evolution of the SAME change using Workflow.getVersion.
 *
 * Arithmetic shape:  add(a,b) -> [ doubleValue, gated by getVersion ] -> park -> square
 *
 * DEMO STEPS (same as NonDeterminismWorkflow, but it does NOT break):
 *   1. Start it:            scripts/start.sh java versioned 3 4
 *   2. It runs add(), then parks on "proceed".
 *   3. Kill the worker, UNCOMMENT the block below, restart the worker.
 *   4. Send the signal:     scripts/signal.sh proceed
 *   5. getVersion finds NO marker in the old history -> returns DEFAULT_VERSION -> the
 *      doubleValue is SKIPPED -> replay matches -> the workflow COMPLETES cleanly with 49.
 *   6. Start a NEW versioned workflow: it records a version marker and DOES run the
 *      doubleValue, returning (2*7)^2 = 196. Old and new coexist from one codebase.
 *      Compare with scripts/history.sh.
 */
public class VersionedWorkflowImpl implements VersionedWorkflow {

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
        int v = Workflow.getVersion("double-the-sum", Workflow.DEFAULT_VERSION, 1);
        if (v != Workflow.DEFAULT_VERSION) {
            sum = activities.doubleValue(new DoubleInput(sum));
        }
        // └────────────────────────────────────────────────────────────────────────────────┘

        // Same timeout-backed park as NonDeterminismWorkflowImpl — see the note there for
        // why the park records a command.
        Workflow.await(Duration.ofHours(1), () -> proceed);

        int squared = activities.square(new SquareInput(sum));
        // >>> BREAKPOINT (workflow): inspect sum and squared here. <<<
        return squared;
    }
}
