package workshop;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface ApprovalWorkflow {
    @WorkflowMethod
    String run(GreetingInput in);

    @SignalMethod
    void add(String approver);

    @SignalMethod
    void done();
}
