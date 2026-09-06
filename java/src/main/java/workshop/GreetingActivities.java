package workshop;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/** String-composition activities (the greeting demos). */
@ActivityInterface
public interface GreetingActivities {
    @ActivityMethod
    String composeGreeting(GreetingInput in);

    @ActivityMethod
    String composeFarewell(GreetingInput in);
}
