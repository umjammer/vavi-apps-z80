package konamiman.z80.impls;


import dotnet4j.util.compat.StopWatch;


/**
 * Default implementation of {@link konamiman.z80.interfaces.ClockSynchronizer}.
 */
public class ClockSynchronizerImpl implements konamiman.z80.interfaces.ClockSynchronizer {

    private static final int MinMicrosecondsToWait = 10 * 1000;

    private double effectiveClockFrequencyInMHz = 2_000_000;

    public double getEffectiveClockFrequencyInMHz() {
        return effectiveClockFrequencyInMHz;
    }

    public void setEffectiveClockFrequencyInMHz(double value) {
        effectiveClockFrequencyInMHz = value;
    }

    private StopWatch stopWatch = new StopWatch();

    private long accummulatedMicroseconds;

    public void start() {
        stopWatch.reset();
        stopWatch.start();
    }

    public void stop() {
        stopWatch.stop();
    }

    public void tryWait(int periodLengthInCycles) {
        accummulatedMicroseconds += (periodLengthInCycles / effectiveClockFrequencyInMHz);

        var microsecondsPending = accummulatedMicroseconds - stopWatch.getElapsedMilliseconds();

        if (microsecondsPending >= MinMicrosecondsToWait) {
            try { Thread.sleep(microsecondsPending / 1000); } catch (InterruptedException ignored) {}
            accummulatedMicroseconds = 0;
            stopWatch.reset();
        }
    }
}
