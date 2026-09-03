package hello

import (
	"time"

	"go.temporal.io/sdk/workflow"
)

// VersionedWorkflow demonstrates SAFE evolution of the SAME change using workflow.GetVersion.
//
// DEMO STEPS (same as NonDeterminismWorkflow, but it does NOT break):
//   1. Start it:            scripts/start.sh go versioned
//   2. It parks on "proceed".
//   3. Kill the worker, UNCOMMENT the block below, restart the worker.
//   4. Send the signal:     scripts/signal.sh proceed
//   5. GetVersion finds NO marker in the old history -> returns DefaultVersion ->
//      the farewell is SKIPPED -> replay matches -> the workflow COMPLETES cleanly.
//   6. Start a NEW versioned workflow: it records a version marker and DOES run the
//      farewell. Old and new coexist from one codebase. Compare with scripts/history.sh.
func VersionedWorkflow(ctx workflow.Context, in GreetingInput) (string, error) {
	ctx = workflow.WithActivityOptions(ctx, workflow.ActivityOptions{
		StartToCloseTimeout: time.Hour,
	})

	var greeting string
	if err := workflow.ExecuteActivity(ctx, ComposeGreeting, in).Get(ctx, &greeting); err != nil {
		return "", err
	}

	// ┌── STEP 2: UNCOMMENT this block, then kill + restart the worker ───────────────┐
	// v := workflow.GetVersion(ctx, "add-farewell", workflow.DefaultVersion, 1)
	// if v != workflow.DefaultVersion {
	// 	var farewell string
	// 	if err := workflow.ExecuteActivity(ctx, ComposeFarewell, in).Get(ctx, &farewell); err != nil {
	// 		return "", err
	// 	}
	// 	greeting = greeting + " " + farewell
	// }
	// └────────────────────────────────────────────────────────────────────────────────┘

	var sig string
	workflow.GetSignalChannel(ctx, "proceed").Receive(ctx, &sig)

	return greeting, nil
}
