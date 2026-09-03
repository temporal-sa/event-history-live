package workshop;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

/** Computes 2*(a+b): Add a+b, then Double the result. Each activity is a durable checkpoint. */
public class PipelineWorkflowImpl implements PipelineWorkflow {

    private final GreetingActivities activities =
        Workflow.newActivityStub(
            GreetingActivities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofHours(1)).build());

    @Override
    public int run(MathInput in) {
        // >>> BREAKPOINT (workflow) <<<
        int sum = activities.add(in);
        return activities.doubleValue(new DoubleInput(sum));
    }
}
