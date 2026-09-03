package hello

import (
	"fmt"
	"strings"

	"go.temporal.io/sdk/workflow"
)

// ApprovalWorkflow collects approver names via the "add" signal until a "done"
// signal is received.
func ApprovalWorkflow(ctx workflow.Context, in GreetingInput) (string, error) {
	approvers := []string{}
	done := false

	for !done {
		selector := workflow.NewSelector(ctx)

		selector.AddReceive(workflow.GetSignalChannel(ctx, "add"), func(c workflow.ReceiveChannel, more bool) {
			var approver string
			// >>> BREAKPOINT (signal handler) <<<
			c.Receive(ctx, &approver)
			approvers = append(approvers, approver)
		})

		selector.AddReceive(workflow.GetSignalChannel(ctx, "done"), func(c workflow.ReceiveChannel, more bool) {
			var ignored string
			c.Receive(ctx, &ignored)
			done = true
		})

		selector.Select(ctx)
	}

	return fmt.Sprintf("Greeting for %s approved by: %s", in.Name, strings.Join(approvers, ", ")), nil
}
