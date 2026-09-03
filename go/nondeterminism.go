package hello

import (
	"time"

	"go.temporal.io/sdk/workflow"
)

// NonDeterminismWorkflow demonstrates a NON-DETERMINISM error (no versioning).
//
// DEMO STEPS:
//   1. Start it:            scripts/start.sh go nondet
//   2. It runs one activity, then PARKS on the "proceed" signal (history stays open).
//   3. Kill the worker (Ctrl-C in its terminal / stop the debug session).
//   4. UNCOMMENT the block below (adds a NEW activity command BEFORE the park).
//   5. Restart the worker.
//   6. Send the signal:     scripts/signal.sh proceed
//   7. The worker replays history against the new code, generates a command that was
//      never recorded, and FAILS the workflow task with a non-determinism error. The
//      workflow wedges (Running, never completes). Inspect: scripts/history.sh
func NonDeterminismWorkflow(ctx workflow.Context, in GreetingInput) (string, error) {
	ctx = workflow.WithActivityOptions(ctx, workflow.ActivityOptions{
		StartToCloseTimeout: time.Hour,
	})

	var greeting string
	if err := workflow.ExecuteActivity(ctx, ComposeGreeting, in).Get(ctx, &greeting); err != nil {
		return "", err
	}

	// ┌── STEP 2: UNCOMMENT this block, then kill + restart the worker ───────────────┐
	// var farewell string
	// if err := workflow.ExecuteActivity(ctx, ComposeFarewell, in).Get(ctx, &farewell); err != nil {
	// 	return "", err
	// }
	// greeting = greeting + " " + farewell
	// └────────────────────────────────────────────────────────────────────────────────┘

	// Park until "proceed" so history stays open across the worker restart.
	var sig string
	workflow.GetSignalChannel(ctx, "proceed").Receive(ctx, &sig)

	return greeting, nil
}
