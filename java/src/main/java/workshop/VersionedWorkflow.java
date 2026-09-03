package workshop;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface VersionedWorkflow {
    @WorkflowMethod
    String run(GreetingInput in);

    @SignalMethod
    void proceed();
}
