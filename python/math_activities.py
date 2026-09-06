"""Arithmetic activities (the pipeline, non-determinism and versioned demos)."""

from temporalio import activity

from shared import MathInput, DoubleInput, SquareInput


@activity.defn
async def add(inp: MathInput) -> int:
    # >>> BREAKPOINT (activity) <<<
    return inp.a + inp.b


@activity.defn
async def double(inp: DoubleInput) -> int:
    # >>> BREAKPOINT (activity) <<<
    return inp.value * 2


@activity.defn
async def square(inp: SquareInput) -> int:
    # >>> BREAKPOINT (activity) <<<
    return inp.value * inp.value
