import asyncio

from temporalio.client import Client
from temporalio.worker import UnsandboxedWorkflowRunner, Worker

from activities import compose_greeting, compose_farewell, add, double
from workflows import (
    GreetingWorkflow,
    PipelineWorkflow,
    ApprovalWorkflow,
    NonDeterminismWorkflow,
    VersionedWorkflow,
)

# Unique per language so workers don't cross-poll.
TASK_QUEUE = "hello-python"


async def main() -> None:
    client = await Client.connect("localhost:7233", namespace="default")

    worker = Worker(
        client,
        task_queue=TASK_QUEUE,
        workflows=[
            GreetingWorkflow,
            PipelineWorkflow,
            ApprovalWorkflow,
            NonDeterminismWorkflow,
            VersionedWorkflow,
        ],
        activities=[compose_greeting, compose_farewell, add, double],
        # debug_mode routes workflow activations onto the main asyncio thread and
        # disables the deadlock detector, so breakpoints in WORKFLOW code fire.
        # (Setting env var TEMPORAL_DEBUG=1 does the same — see .vscode/launch.json.)
        debug_mode=True,
        # The default sandbox re-imports workflow modules and prevents breakpoints
        # in workflow code from being hit. Skip it for the workshop.
        workflow_runner=UnsandboxedWorkflowRunner(),
    )

    print(f"Worker started on task queue: {TASK_QUEUE}")
    await worker.run()


if __name__ == "__main__":
    asyncio.run(main())
