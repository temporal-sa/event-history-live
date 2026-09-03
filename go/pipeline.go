package hello

import (
	"time"

	"go.temporal.io/sdk/workflow"
)

// PipelineWorkflow computes 2*(a+b): it Adds a+b, then Doubles the result.
// Multi-activity demo — each activity is a durable checkpoint.
func PipelineWorkflow(ctx workflow.Context, in MathInput) (int, error) {
	ctx = workflow.WithActivityOptions(ctx, workflow.ActivityOptions{
		StartToCloseTimeout: time.Hour,
	})

	var sum int
	// >>> BREAKPOINT (workflow) <<<
	if err := workflow.ExecuteActivity(ctx, Add, in).Get(ctx, &sum); err != nil {
		return 0, err
	}

	var doubled int
	// >>> BREAKPOINT (workflow) <<<
	if err := workflow.ExecuteActivity(ctx, Double, DoubleInput{Value: sum}).Get(ctx, &doubled); err != nil {
		return 0, err
	}

	return doubled, nil
}
