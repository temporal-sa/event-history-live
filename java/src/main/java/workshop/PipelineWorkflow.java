package workshop;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface PipelineWorkflow {
    @WorkflowMethod
    int run(MathInput in);
}
