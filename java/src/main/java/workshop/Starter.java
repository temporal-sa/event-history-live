package workshop;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.time.Duration;

public class Starter {

    public static void main(String[] args) {
        String type = args.length > 0 ? args[0] : "hello";

        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        WorkflowExecution execution;
        String id;

        if (type.equals("multiactivity")) {
            int a = args.length > 1 ? Integer.parseInt(args[1]) : 3;
            int b = args.length > 2 ? Integer.parseInt(args[2]) : 4;
            id = args.length > 3 ? args[3] : "demo-wf";

            WorkflowOptions options =
                WorkflowOptions.newBuilder()
                    .setWorkflowId(id)
                    .setTaskQueue(WorkerApp.TASK_QUEUE)
                    .setWorkflowTaskTimeout(Duration.ofMinutes(15))
                    .build();

            PipelineWorkflow stub = client.newWorkflowStub(PipelineWorkflow.class, options);
            execution = WorkflowClient.start(stub::run, new MathInput(a, b));
        } else {
            String name = args.length > 1 ? args[1] : "Temporal";
            id = args.length > 2 ? args[2] : "demo-wf";

            WorkflowOptions options =
                WorkflowOptions.newBuilder()
                    .setWorkflowId(id)
                    .setTaskQueue(WorkerApp.TASK_QUEUE)
                    // 15-min workflow-task timeout gives you headroom to sit on a
                    // breakpoint in WORKFLOW code. Requires the server-side cap
                    // raised to 15m (see workshop/README.md).
                    .setWorkflowTaskTimeout(Duration.ofMinutes(15))
                    .build();

            switch (type) {
                case "signal": {
                    ApprovalWorkflow stub =
                        client.newWorkflowStub(ApprovalWorkflow.class, options);
                    execution = WorkflowClient.start(stub::run, new GreetingInput(name));
                    break;
                }
                case "nondet": {
                    NonDeterminismWorkflow stub =
                        client.newWorkflowStub(NonDeterminismWorkflow.class, options);
                    execution = WorkflowClient.start(stub::run, new GreetingInput(name));
                    break;
                }
                case "versioned": {
                    VersionedWorkflow stub =
                        client.newWorkflowStub(VersionedWorkflow.class, options);
                    execution = WorkflowClient.start(stub::run, new GreetingInput(name));
                    break;
                }
                case "hello":
                default: {
                    GreetingWorkflow stub =
                        client.newWorkflowStub(GreetingWorkflow.class, options);
                    execution = WorkflowClient.start(stub::greet, new GreetingInput(name));
                    break;
                }
            }
        }

        System.out.println(
            "Started " + type + " WorkflowID=" + id + " RunID=" + execution.getRunId());
        System.exit(0);
    }
}
