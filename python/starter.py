import argparse
import asyncio
from datetime import timedelta

from temporalio.client import Client

from workflows import (
    GreetingWorkflow,
    PipelineWorkflow,
    ApprovalWorkflow,
    NonDeterminismWorkflow,
    VersionedWorkflow,
)
from worker import TASK_QUEUE
from shared import GreetingInput, MathInput

# Map a workflow key to the class run method to start.
WORKFLOWS = {
    "hello": GreetingWorkflow.run,
    "multiactivity": PipelineWorkflow.run,
    "signal": ApprovalWorkflow.run,
    "nondet": NonDeterminismWorkflow.run,
    "versioned": VersionedWorkflow.run,
}


async def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--workflow", default="hello", choices=list(WORKFLOWS))
    parser.add_argument("--id", default="demo-wf")
    parser.add_argument("--name", default="Temporal")
    parser.add_argument("--a", type=int, default=3)
    parser.add_argument("--b", type=int, default=4)
    parser.add_argument("--wait", action="store_true")
    args = parser.parse_args()

    run = WORKFLOWS[args.workflow]

    client = await Client.connect("localhost:7233", namespace="default")

    arg = MathInput(a=args.a, b=args.b) if args.workflow == "multiactivity" else GreetingInput(name=args.name)

    handle = await client.start_workflow(
        run,
        arg,
        id=args.id,
        task_queue=TASK_QUEUE,
        # 15-min workflow-task timeout gives you headroom to sit on a breakpoint
        # in WORKFLOW code. Requires the server-side cap raised to 15m.
        task_timeout=timedelta(minutes=15),
    )

    run_id = handle.result_run_id or handle.first_execution_run_id
    print(f"Started {args.workflow} WorkflowID={args.id} RunID={run_id}")

    if args.wait:
        print("Result:", await handle.result())
    else:
        print("Signal-based demos need a signal to finish — see scripts/signal.sh")


if __name__ == "__main__":
    asyncio.run(main())
