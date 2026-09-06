package workshop;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/** Arithmetic activities (the pipeline, non-determinism and versioned demos). */
@ActivityInterface
public interface MathActivities {
    @ActivityMethod
    int add(MathInput in);

    // Named doubleValue because `double` is a Java keyword; Go has Double, Python double.
    @ActivityMethod
    int doubleValue(DoubleInput in);

    @ActivityMethod
    int square(SquareInput in);
}
