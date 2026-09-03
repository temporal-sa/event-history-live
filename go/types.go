package hello

// GreetingInput is the single input payload for greeting-based workflows/activities.
type GreetingInput struct {
	Name string `json:"name"`
}

// MathInput is the single input payload for the math pipeline and the Add activity.
type MathInput struct {
	A int `json:"a"`
	B int `json:"b"`
}

// DoubleInput is the single input payload for the Double activity.
type DoubleInput struct {
	Value int `json:"value"`
}
