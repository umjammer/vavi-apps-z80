package konamiman.DependenciesImplementations;

import konamiman.DependenciesInterfaces.IClockSynchronizer;


/**
 * Default implementation of {@link IClockSynchronizer}.
 */
public class ClockSynchronizer implements IClockSynchronizer {

    private static final int MinMicrosecondsToWait = 10 * 1000;

    private float EffectiveClockFrequencyInMHz;

    public float getEffectiveClockFrequencyInMHz() {
        return EffectiveClockFrequencyInMHz;
    }

    public void setEffectiveClockFrequencyInMHz(float value) {
        EffectiveClockFrequencyInMHz = value;
    }

    private long stopWatch = System.currentTimeMillis();

    private long accummulatedMicroseconds;

    public void Start() {
    }

    public void Stop() {
    }

    public void TryWait(int periodLengthInCycles) {
        accummulatedMicroseconds += (periodLengthInCycles / EffectiveClockFrequencyInMHz);

        var microsecondsPending = (accummulatedMicroseconds - stopWatch);

        if (microsecondsPending >= MinMicrosecondsToWait) {
            try { Thread.sleep(microsecondsPending / 1000); } catch (InterruptedException ignored) {}
            accummulatedMicroseconds = 0;
            stopWatch = System.currentTimeMillis();
        }
    }
}
