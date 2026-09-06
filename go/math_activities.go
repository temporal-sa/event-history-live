package hello

import "context"

// Arithmetic activities (the pipeline, non-determinism and versioned demos).

// Add returns a + b.
func Add(ctx context.Context, in MathInput) (int, error) {
	// >>> BREAKPOINT (activity) <<<
	return in.A + in.B, nil
}

// Double returns value * 2.
func Double(ctx context.Context, in DoubleInput) (int, error) {
	// >>> BREAKPOINT (activity) <<<
	return in.Value * 2, nil
}

// Square returns value * value.
func Square(ctx context.Context, in SquareInput) (int, error) {
	// >>> BREAKPOINT (activity) <<<
	return in.Value * in.Value, nil
}
