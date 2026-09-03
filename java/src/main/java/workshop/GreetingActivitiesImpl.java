package workshop;

public class GreetingActivitiesImpl implements GreetingActivities {

    @Override
    public String composeGreeting(GreetingInput in) {
        // >>> BREAKPOINT (activity) <<<
        return "Hello, " + in.name + "!";
    }

    @Override
    public String composeFarewell(GreetingInput in) {
        // >>> BREAKPOINT (activity) <<<
        return "Goodbye, " + in.name + "!";
    }

    @Override
    public int add(MathInput in) {
        // >>> BREAKPOINT (activity) <<<
        return in.a + in.b;
    }

    @Override
    public int doubleValue(DoubleInput in) {
        // >>> BREAKPOINT (activity) <<<
        return in.value * 2;
    }
}
