package konamiman.z80.enums;


/**
 * Represents a reason for the processor not being running.
 */
public enum StopReason {
    /**
     * The processor is currently running, so no stop reason applies.
     */
    NotApplicable,

    /**
     * The processor has never been running since it was instantiated.
     */
    NeverRan,

    /**
     * The processor was running and finished with an invocation of {@link konamiman.z80.interfaces.ExecutionStopper#stop(boolean)}
     * with <c>isPause=false</c>.
     */
    StopInvoked,

    /**
     * The processor was running and finished with an invocation of {@link konamiman.z80.interfaces.ExecutionStopper#stop(boolean)}
     * with <c>isPause=true</c>.
     */
    PauseInvoked,

    /**
     * The {@link konamiman.z80.Z80Processor#executeNextInstruction()} was invoked and finished normally
     * (the {@link konamiman.z80.interfaces.ExecutionStopper#stop(boolean)} method was not invoked).
     */
    ExecuteNextInstructionInvoked,

    /**
     * A HALT instruction was encountered when interrupts were disabled,
     * and {@link konamiman.z80.Z80Processor#getAutoStopOnDiPlusHalt()} property was set to true.
     */
    DiPlusHalt,

    /**
     * A RET instruction was encountered, the stack is empty, and the
     * {@link konamiman.z80.Z80Processor#getAutoStopOnRetWithStackEmpty()} property was set to true.
     */
    RetWithStackEmpty,

    /**
     * An exception was thrown and not handled during the execution of the
     * {@link konamiman.z80.Z80Processor#start(Object)} method, the {@link konamiman.z80.Z80Processor#continue_()} method
     * or the {@link konamiman.z80.Z80Processor#executeNextInstruction()} method.
     */
    ExceptionThrown
}
