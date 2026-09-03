package hello

import "context"

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

// ComposeFarewell returns a farewell string.
func ComposeFarewell(ctx context.Context, in GreetingInput) (string, error) {
	// >>> BREAKPOINT (activity) <<<
	return "Goodbye, " + in.Name + "!", nil
}
