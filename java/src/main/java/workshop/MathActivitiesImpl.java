package workshop;

public class MathActivitiesImpl implements MathActivities {

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

    @Override
    public int square(SquareInput in) {
        // >>> BREAKPOINT (activity) <<<
        return in.value * in.value;
    }
}
