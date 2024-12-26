package konamiman.z80.impls;


import dotnet4j.util.compat.StopWatch;


/**
 * Default implementation of {@link konamiman.z80.interfaces.ClockSynchronizer}.
 */
public class ClockSynchronizerImpl implements konamiman.z80.interfaces.ClockSynchronizer {

    private static final int MinMicrosecondsToWait = 10 * 1000;

    private double effectiveClockFrequencyInMHz = 2_000_000;

    @Override
    public double getEffectiveClockFrequencyInMHz() {
        return effectiveClockFrequencyInMHz;
    }

    @Override
    public void setEffectiveClockFrequencyInMHz(double value) {
        effectiveClockFrequencyInMHz = value;
    }

    private final StopWatch stopWatch = new StopWatch();

    private long accumulatedMicroseconds;

    @Override
    public void start() {
        stopWatch.reset();
        stopWatch.start();
    }

    @Override
    public void stop() {
        stopWatch.stop();
    }

    @Override
    public void tryWait(int periodLengthInCycles) {
        accumulatedMicroseconds += (long) (periodLengthInCycles / effectiveClockFrequencyInMHz);

        var microsecondsPending = accumulatedMicroseconds - stopWatch.getElapsedMilliseconds();

        if (microsecondsPending >= MinMicrosecondsToWait) {
            try { Thread.sleep(microsecondsPending / 1000); } catch (InterruptedException ignored) {}
            accumulatedMicroseconds = 0;
            stopWatch.reset();
        }
    }
}
