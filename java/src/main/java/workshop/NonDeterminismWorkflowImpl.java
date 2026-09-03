package workshop;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

/**
 * Demonstrates a NON-DETERMINISM error (no versioning).
 *
 * DEMO STEPS:
 *   1. Start it:            scripts/start.sh java nondet
 *   2. It runs one activity, then PARKS on the "proceed" signal (history stays open).
 *   3. Kill the worker.
 *   4. UNCOMMENT the block below (adds a NEW activity command BEFORE the park).
 *   5. Restart the worker.
 *   6. Send the signal:     scripts/signal.sh proceed
 *   7. Replay generates a command that was never recorded -> the workflow task FAILS with
 *      a non-determinism error and the workflow wedges. Inspect: scripts/history.sh
 */
public class NonDeterminismWorkflowImpl implements NonDeterminismWorkflow {

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
        // String farewell = activities.composeFarewell(in);
        // greeting = greeting + " " + farewell;
        // └────────────────────────────────────────────────────────────────────────────────┘

        Workflow.await(() -> proceed); // park so history stays open across the restart
        return greeting;
    }
}
