package workshop;

import io.temporal.workflow.Workflow;
import java.util.ArrayList;
import java.util.List;

public class ApprovalWorkflowImpl implements ApprovalWorkflow {

    private final List<String> approvers = new ArrayList<>();
    private boolean done;

    @Override
    public void add(String approver) {
        // >>> BREAKPOINT (signal handler) <<<
        approvers.add(approver);
    }

    @Override
    public void done() {
        this.done = true;
    }

    @Override
    public String run(GreetingInput in) {
        Workflow.await(() -> done);
        return "Greeting for " + in.name + " approved by: " + String.join(", ", approvers);
    }
}
