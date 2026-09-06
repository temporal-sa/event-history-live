package hello

import "context"

// String-composition activities (the greeting demos).

// ComposeGreeting is the Hello World activity. Breakpoint here for the clean Lab 3
// pause — activities have no deadlock detector and no workflow-task timeout.
func ComposeGreeting(ctx context.Context, in GreetingInput) (string, error) {
	// >>> BREAKPOINT (activity code): recommended pause point. <<<
	return "Hello, " + in.Name + "!", nil
}

// ComposeFarewell returns a farewell string.
func ComposeFarewell(ctx context.Context, in GreetingInput) (string, error) {
	// >>> BREAKPOINT (activity) <<<
	return "Goodbye, " + in.Name + "!", nil
}
