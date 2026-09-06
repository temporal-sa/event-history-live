package hello

import (
	"time"

	"go.temporal.io/sdk/workflow"
)

// NonDeterminismWorkflow demonstrates a NON-DETERMINISM error (no versioning).
//
// Arithmetic shape:  Add(a,b) -> [ Double ] -> park on "proceed" -> Square
// With the block below commented out, a=3 b=4 yields 7*7 = 49.
//
// DEMO STEPS:
//  1. Start it:            scripts/start.sh go nondet 3 4
//  2. It runs Add, then PARKS waiting for the "proceed" signal (history stays open).
//  3. Kill the worker (Ctrl-C in its terminal / stop the debug session).
//  4. UNCOMMENT the block below. It inserts a NEW activity command between Add and
//     the park.
//  5. Restart the worker.
//  6. Send the signal:     scripts/signal.sh proceed
//  7. Replay reaches the point where history recorded TimerStarted (the park's timeout)
//     but the new code issues ScheduleActivityTask(Double) instead -> the workflow task
//     FAILS with a non-determinism error and the workflow wedges (Running, never
//     completes). Inspect: scripts/history.sh
//
// WHY THE PARK USES A TIMER: non-determinism is only detected when a replayed command
// CONTRADICTS a command already recorded in history. Blocking on a bare signal receive
// records no command at all, so a new activity added just before it would append past the
// end of recorded history — indistinguishable from normal progress — and the workflow
// would happily complete. The timer gives the block below a recorded command to collide
// with. (Bonus: the pending timer shows up in scripts/sql.sh q3.)
func NonDeterminismWorkflow(ctx workflow.Context, in MathInput) (int, error) {
	ctx = workflow.WithActivityOptions(ctx, workflow.ActivityOptions{
		StartToCloseTimeout: time.Hour,
	})

	var sum int
	if err := workflow.ExecuteActivity(ctx, Add, in).Get(ctx, &sum); err != nil {
		return 0, err
	}

	// ┌── STEP 2: UNCOMMENT this block, then kill + restart the worker ───────────────┐
	// var doubled int
	// if err := workflow.ExecuteActivity(ctx, Double, DoubleInput{Value: sum}).Get(ctx, &doubled); err != nil {
	// 	return 0, err
	// }
	// sum = doubled
	// └────────────────────────────────────────────────────────────────────────────────┘

	// Park until "proceed" so history stays open across the worker restart. The timer
	// records a command (TimerStarted) *after* the block above — that is what makes the
	// mismatch detectable.
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
