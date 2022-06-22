package konamiman.DependenciesInterfaces;


/**
 * Represents a class that can pause the current thread for a specific amount of time,
 * based on a clock frequency and a number of cycles elapsed.
 */
public interface IExecutionStopper {
    /**
     * Stops the processor execution, causing the {@link IZ80Processor.Start} method to return.
     *
     * @param isPause If <b>true</b>, the {@link IZ80Processor.StopReason} property of the
     * processor classs will return {@link Konamiman.Z80dotNet.StopReason.PauseInvoked} after the method returns.
     * If <b>false</b>, it will return {@link Konamiman.Z80dotNet.StopReason.StopInvoked}.
     */
    void Stop(boolean isPause/*= false*/);
}
