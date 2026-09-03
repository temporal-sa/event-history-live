package workshop;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

/**
 * Demonstrates SAFE evolution of the SAME change using Workflow.getVersion.
 *
 * DEMO STEPS (same as NonDeterminismWorkflow, but it does NOT break):
 *   1. Start it:            scripts/start.sh java versioned
 *   2. It parks on "proceed".
 *   3. Kill the worker, UNCOMMENT the block below, restart the worker.
 *   4. Send the signal:     scripts/signal.sh proceed
 *   5. getVersion finds NO marker in the old history -> returns DEFAULT_VERSION ->
 *      farewell SKIPPED -> replay matches -> the workflow COMPLETES cleanly.
 *   6. Start a NEW versioned workflow: it records a version marker and runs the farewell.
 *      Old and new coexist. Compare with scripts/history.sh.
 */
public class VersionedWorkflowImpl implements VersionedWorkflow {

    private final GreetingActivities activities =
        Workflow.newActivityStub(
            GreetingActivities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofHours(1)).build());

    private boolean proceed;

    @Override
    public void proceed() {
        this.proceed = true;
    }

    @Override
    public String run(GreetingInput in) {
        String greeting = activities.composeGreeting(in);

        // ┌── STEP 2: UNCOMMENT this block, then kill + restart the worker ───────────────┐
        // int v = Workflow.getVersion("add-farewell", Workflow.DEFAULT_VERSION, 1);
        // if (v != Workflow.DEFAULT_VERSION) {
        //     String farewell = activities.composeFarewell(in);
        //     greeting = greeting + " " + farewell;
        // }
        // └────────────────────────────────────────────────────────────────────────────────┘

        Workflow.await(() -> proceed);
        return greeting;
    }
}
