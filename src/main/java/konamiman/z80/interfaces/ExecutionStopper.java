package konamiman.z80.interfaces;


import konamiman.z80.Z80Processor;


/**
 * Represents a class that can pause the current thread for a specific amount of time,
 * based on a clock frequency and a number of cycles elapsed.
 */
public interface ExecutionStopper {
    /**
     * Stops the processor execution, causing the {@link Z80Processor#start(Object)} method to return.
     *
     * @param isPause If <b>true</b>, the {@link konamiman.z80.enums.StopReason} property of the
     * processor classs will return {@link konamiman.z80.enums.StopReason#PauseInvoked} after the method returns.
     * If <b>false</b>, it will return {@link konamiman.z80.enums.StopReason#StopInvoked}.
     */
    void stop(boolean isPause /* = false */);
}
