package workshop;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class GreetingWorkflowImpl implements GreetingWorkflow {

    private final GreetingActivities activities =
        Workflow.newActivityStub(
            GreetingActivities.class,
            ActivityOptions.newBuilder()
                // Large timeout so a breakpoint held INSIDE the activity (Lab 3)
                // never trips the activity StartToClose timeout.
                .setStartToCloseTimeout(Duration.ofHours(1))
                .build());

    @Override
    public String greet(GreetingInput in) {
        // >>> BREAKPOINT (workflow code): pause here to inspect the DB mid-task. <<<
        // Requires TEMPORAL_DEBUG=true on the worker (see .vscode/launch.json) so
        // the deadlock detector doesn't fire.
        String greeting = activities.composeGreeting(in);
        // >>> BREAKPOINT (workflow): the activity result is inspectable here. <<<
        return greeting;
    }
}
