package hello

import (
	"context"
	"time"

	"go.temporal.io/sdk/workflow"
)

// TaskQueue is unique per language so workers don't cross-poll.
const TaskQueue = "hello-go"

// GreetingWorkflow is a minimal Hello World workflow: it runs one activity and
// returns the greeting.
func GreetingWorkflow(ctx workflow.Context, in GreetingInput) (string, error) {
	logger := workflow.GetLogger(ctx)
	logger.Info("GreetingWorkflow started", "name", in.Name)

	ao := workflow.ActivityOptions{
		// Large timeout so a breakpoint held INSIDE the activity (Lab 3) never
		// trips the activity StartToClose timeout.
		StartToCloseTimeout: time.Hour,
	}
	ctx = workflow.WithActivityOptions(ctx, ao)

	var greeting string
	// >>> BREAKPOINT (workflow code): pause here to inspect the DB mid-task. <<<
	err := workflow.ExecuteActivity(ctx, ComposeGreeting, in).Get(ctx, &greeting)
	if err != nil {
		return "", err
	}

	logger.Info("GreetingWorkflow completed", "greeting", greeting)
	return greeting, nil
}

// ComposeGreeting is the activity. Breakpoint here for the clean Lab 3 pause —
// activities have no deadlock detector and no workflow-task timeout.
func ComposeGreeting(ctx context.Context, in GreetingInput) (string, error) {
	// >>> BREAKPOINT (activity code): recommended pause point. <<<
	return "Hello, " + in.Name + "!", nil
}
