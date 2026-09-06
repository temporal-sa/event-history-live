package workshop;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

/** Computes 2*(a+b): Add a+b, then Double the result. Each activity is a durable checkpoint. */
public class PipelineWorkflowImpl implements PipelineWorkflow {

    private final MathActivities activities =
        Workflow.newActivityStub(
            MathActivities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofHours(1)).build());

    @Override
    public int run(MathInput in) {
        // >>> BREAKPOINT (workflow) <<<
        int sum = activities.add(in);
        int doubled = activities.doubleValue(new DoubleInput(sum));
        // >>> BREAKPOINT (workflow): inspect sum and doubled here. <<<
        return doubled;
    }
}
