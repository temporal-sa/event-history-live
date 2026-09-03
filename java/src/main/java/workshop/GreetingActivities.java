package workshop;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface GreetingActivities {
    @ActivityMethod
    String composeGreeting(GreetingInput in);

    @ActivityMethod
    String composeFarewell(GreetingInput in);

    @ActivityMethod
    int add(MathInput in);

    @ActivityMethod
    int doubleValue(DoubleInput in);
}
