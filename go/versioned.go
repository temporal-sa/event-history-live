package hello

import (
	"time"

	"go.temporal.io/sdk/workflow"
)

// VersionedWorkflow demonstrates SAFE evolution of the SAME change using workflow.GetVersion.
//
// Arithmetic shape:  Add(a,b) -> [ Double, gated by GetVersion ] -> park on "proceed" -> Square
//
// DEMO STEPS (same as NonDeterminismWorkflow, but it does NOT break):
//  1. Start it:            scripts/start.sh go versioned 3 4
//  2. It runs Add, then parks on "proceed".
//  3. Kill the worker, UNCOMMENT the block below, restart the worker.
//  4. Send the signal:     scripts/signal.sh proceed
//  5. GetVersion finds NO marker in the old history -> returns DefaultVersion -> the
//     Double is SKIPPED -> replay matches -> the workflow COMPLETES cleanly with 49.
//  6. Start a NEW versioned workflow: it records a version marker and DOES run the
//     Double, returning (2*7)^2 = 196. Old and new coexist from one codebase.
//     Compare with scripts/history.sh.
func VersionedWorkflow(ctx workflow.Context, in MathInput) (int, error) {
	ctx = workflow.WithActivityOptions(ctx, workflow.ActivityOptions{
		StartToCloseTimeout: time.Hour,
	})

	var sum int
	if err := workflow.ExecuteActivity(ctx, Add, in).Get(ctx, &sum); err != nil {
		return 0, err
	}

	// ┌── STEP 2: UNCOMMENT this block, then kill + restart the worker ───────────────┐
	// if workflow.GetVersion(ctx, "double-the-sum", workflow.DefaultVersion, 1) != workflow.DefaultVersion {
	// 	var doubled int
	// 	if err := workflow.ExecuteActivity(ctx, Double, DoubleInput{Value: sum}).Get(ctx, &doubled); err != nil {
	// 		return 0, err
	// 	}
	// 	sum = doubled
	// }
	// └────────────────────────────────────────────────────────────────────────────────┘

	// Same timer-backed park as NonDeterminismWorkflow — see the note there for why the
	// park records a command.
	sel := workflow.NewSelector(ctx)
	sel.AddReceive(workflow.GetSignalChannel(ctx, "proceed"), func(c workflow.ReceiveChannel, _ bool) {
		c.Receive(ctx, nil)
	})
	sel.AddFuture(workflow.NewTimer(ctx, time.Hour), func(workflow.Future) {})
	sel.Select(ctx)

	var squared int
	if err := workflow.ExecuteActivity(ctx, Square, SquareInput{Value: sum}).Get(ctx, &squared); err != nil {
		return 0, err
	}
	// >>> BREAKPOINT (workflow): inspect sum and squared here. <<<
	return squared, nil
}
