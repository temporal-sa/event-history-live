package workshop;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class WorkerApp {

    // Unique per language so workers don't cross-poll.
    static final String TASK_QUEUE = "hello-java";

    public static void main(String[] args) {
        // Connects to localhost:7233, namespace "default".
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);
        WorkerFactory factory = WorkerFactory.newInstance(client);

        Worker worker = factory.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(
            GreetingWorkflowImpl.class,
            PipelineWorkflowImpl.class,
            ApprovalWorkflowImpl.class,
            NonDeterminismWorkflowImpl.class,
            VersionedWorkflowImpl.class);
        worker.registerActivitiesImplementations(new GreetingActivitiesImpl());

        System.out.println("Worker started on task queue: " + TASK_QUEUE);
        factory.start();
    }
}
