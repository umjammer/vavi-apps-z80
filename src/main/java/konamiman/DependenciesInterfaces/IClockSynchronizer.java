package konamiman.DependenciesInterfaces;


/**
 * Represents a class that helps on synchronizing with a simulated clock
 * by introducing delays that last for a specified amount of clock cycles.
 */
public interface IClockSynchronizer {
    /**
     * Gets or sets the clock speed of the simulated system, in MHz.
     */
    float getEffectiveClockFrequencyInMHz();
    void setEffectiveClockFrequencyInMHz(float value);

    /**
     * Starts the internal clock used to keep track of time.
     */
    void Start();

    /**
     * Stops the internal clock used to keep track of time.
     *
     * <remarks>
     * The class should still work even if this method is never invoked (that is, consecutive calls
     * to {@link Start} should be allowed). However invoking this method is convenient
     * in order to clean up resources.
     */
    void Stop();

    /**
     * Signals that a certain number of clock cycles have elapsed in the simulated system.
     *
     * @param periodLengthInCycles Amount of period cycles to simulate
     * <remarks> This method will do its best to accurately reproduce the simulated system's clock speed
     * by pausing the current thread for the specified amount of time. However, depending on the host system's
     * clock accuracy the method may need to accummulate several clock cycles across different method invocations
     * before actually pausing the thread.
     */
    void TryWait(int periodLengthInCycles);
}
