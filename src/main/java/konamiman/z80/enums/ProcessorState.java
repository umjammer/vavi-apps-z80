package konamiman.z80.enums;


import konamiman.z80.Z80Processor;


/**
 * Represents the current processor state
 */
public enum ProcessorState {
    /**
     * Not running. Either the processor has never been running since it was instantiated,
     * or the {@link Z80Processor#start(Object)} or {@link Z80Processor#executeNextInstruction()}
     * methods were invoked and finished for any reason other than the invocation of {@link konamiman.z80.interfaces.ExecutionStopper#stop(boolean)}
     * with <c>isPause=true</c>.
     */
    Stopped,

    /**
     * Not running. The {@link Z80Processor#start(Object)} method or the {@link Z80Processor#executeNextInstruction()}
     * method was invoked and finished with an invocation of {@link konamiman.z80.interfaces.ExecutionStopper#stop(boolean)}
     * with <c>isPause=true</c>.
     */
    Paused,

    /**
     * Running. The {@link Z80Processor#start(Object)} method was invoked and has not returned yet.
     */
    Running,

    /**
     * Executing only one instruction. The {@link Z80Processor#executeNextInstruction()} method was invoked
     * and has not returned yet.
     */
    ExecutingOneInstruction
}
