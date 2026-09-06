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
}
