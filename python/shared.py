from dataclasses import dataclass


@dataclass
class GreetingInput:
    name: str


@dataclass
class MathInput:
    a: int
    b: int


@dataclass
class DoubleInput:
    value: int


@dataclass
class SquareInput:
    value: int
