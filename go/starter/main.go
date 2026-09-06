package main

import (
	"context"
	"flag"
	"log"
	"time"

	"go.temporal.io/sdk/client"

	hello "temporal-workshop"
)

func main() {
	workflowName := flag.String("workflow", "hello", "workflow to start: hello, multiactivity, signal, nondet, versioned")
	id := flag.String("id", "demo-wf", "workflow ID")
	name := flag.String("name", "Temporal", "name argument passed to the workflow")
	a := flag.Int("a", 3, "first operand for the math demos (multiactivity, nondet, versioned)")
	b := flag.Int("b", 4, "second operand for the math demos (multiactivity, nondet, versioned)")
	wait := flag.Bool("wait", false, "wait for the workflow result")
	flag.Parse()

	var wf interface{}
	switch *workflowName {
	case "hello":
		wf = hello.GreetingWorkflow
	case "multiactivity":
		wf = hello.PipelineWorkflow
	case "signal":
		wf = hello.ApprovalWorkflow
	case "nondet":
		wf = hello.NonDeterminismWorkflow
	case "versioned":
		wf = hello.VersionedWorkflow
	default:
		log.Fatalf("unknown -workflow %q; valid values: hello, multiactivity, signal, nondet, versioned", *workflowName)
	}

	c, err := client.Dial(client.Options{Namespace: "default"})
	if err != nil {
		log.Fatalln("unable to create client:", err)
	}
	defer c.Close()

	options := client.StartWorkflowOptions{
		ID:                  *id,
		TaskQueue:           hello.TaskQueue,
		WorkflowTaskTimeout: 15 * time.Minute,
	}

	// The math demos take MathInput; the greeting/approval demos take GreetingInput.
	mathDemo := false
	switch *workflowName {
	case "multiactivity", "nondet", "versioned":
		mathDemo = true
	}

	var input interface{}
	if mathDemo {
		input = hello.MathInput{A: *a, B: *b}
	} else {
		input = hello.GreetingInput{Name: *name}
	}

	ctx := context.Background()
	we, err := c.ExecuteWorkflow(ctx, options, wf, input)
	if err != nil {
		log.Fatalln("unable to start workflow:", err)
	}
	log.Printf("Started %s WorkflowID=%s RunID=%s", *workflowName, we.GetID(), we.GetRunID())

	if *wait {
		if mathDemo {
			var r int
			if err := we.Get(ctx, &r); err != nil {
				log.Fatalln("unable to get workflow result:", err)
			}
			log.Printf("Result: %d", r)
		} else {
			var r string
			if err := we.Get(ctx, &r); err != nil {
				log.Fatalln("unable to get workflow result:", err)
			}
			log.Printf("Result: %s", r)
		}
	} else {
		log.Println("Signal-based demos need scripts/signal.sh to drive them (e.g. scripts/signal.sh proceed). Re-run with -wait to block on the result.")
	}
}
