/*
 * https://github.com/xamarin/benchmarker/blob/6db3e120929da7a1ecd514fe5a37e6b2c5a61ab4/tests/scimark/Stopwatch.cs
 */

package dotnet4j.util.compat;


public class StopWatch {

    private long startTicks;

    public StopWatch() {
        start();
    }

    public void start() {
        startTicks = ticksNow();
    }

    public void reset() {
        stop();
        start();
    }

    public void stop() {
    }

    public long getElapsedMilliseconds() {
        return ticksNow() - startTicks;
    }

    private static long ticksNow() {
        return System.currentTimeMillis();
    }
}
