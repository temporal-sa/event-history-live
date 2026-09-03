package main

import (
	"log"
	"time"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/worker"

	hello "temporal-workshop"
)

func main() {
	c, err := client.Dial(client.Options{
		HostPort:  client.DefaultHostPort, // localhost:7233
		Namespace: "default",
	})
	if err != nil {
		log.Fatalln("unable to create client:", err)
	}
	defer c.Close()

	w := worker.New(c, hello.TaskQueue, worker.Options{
		// Disable the SDK deadlock detector so a breakpoint in WORKFLOW code
		// doesn't panic with "Potential deadlock detected". Setting the env var
		// TEMPORAL_DEBUG=true (see .vscode/launch.json) does the same thing.
		DeadlockDetectionTimeout: 15 * time.Minute,
	})
	w.RegisterWorkflow(hello.GreetingWorkflow)
	w.RegisterWorkflow(hello.PipelineWorkflow)
	w.RegisterWorkflow(hello.ApprovalWorkflow)
	w.RegisterWorkflow(hello.NonDeterminismWorkflow)
	w.RegisterWorkflow(hello.VersionedWorkflow)
	w.RegisterActivity(hello.ComposeGreeting)
	w.RegisterActivity(hello.ComposeFarewell)
	w.RegisterActivity(hello.Add)
	w.RegisterActivity(hello.Double)

	log.Println("Worker started on task queue:", hello.TaskQueue)
	if err := w.Run(worker.InterruptCh()); err != nil {
		log.Fatalln("unable to start worker:", err)
	}
}
