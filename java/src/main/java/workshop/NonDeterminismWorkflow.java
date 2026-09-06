package workshop;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface NonDeterminismWorkflow {
    @WorkflowMethod
    int run(MathInput in);

    @SignalMethod
    void proceed();
}
