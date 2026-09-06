"""String-composition activities (the greeting demos)."""

from temporalio import activity

from shared import GreetingInput


@activity.defn
async def compose_greeting(inp: GreetingInput) -> str:
    # >>> BREAKPOINT (activity) <<<
    return f"Hello, {inp.name}!"


@activity.defn
async def compose_farewell(inp: GreetingInput) -> str:
    # >>> BREAKPOINT (activity) <<<
    return f"Goodbye, {inp.name}!"
