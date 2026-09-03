from temporalio import activity

from shared import GreetingInput, MathInput, DoubleInput


@activity.defn
async def compose_greeting(inp: GreetingInput) -> str:
    # >>> BREAKPOINT (activity) <<<
    return f"Hello, {inp.name}!"


@activity.defn
async def compose_farewell(inp: GreetingInput) -> str:
    # >>> BREAKPOINT (activity) <<<
    return f"Goodbye, {inp.name}!"


@activity.defn
async def add(inp: MathInput) -> int:
    # >>> BREAKPOINT (activity) <<<
    return inp.a + inp.b


@activity.defn
async def double(inp: DoubleInput) -> int:
    # >>> BREAKPOINT (activity) <<<
    return inp.value * 2
