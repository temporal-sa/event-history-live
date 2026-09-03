package workshop;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface NonDeterminismWorkflow {
    @WorkflowMethod
    String run(GreetingInput in);

    @SignalMethod
    void proceed();
}
