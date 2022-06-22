package konamiman.Enums;

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
     * The processor was running and finished with an invocation of {@link IExecutionStopper.Stop}
     * with <c>isPause=false</c>.
     */
    StopInvoked,

    /**
     * The processor was running and finished with an invocation of {@link IExecutionStopper.Stop}
     * with <c>isPause=true</c>.
     */
    PauseInvoked,

    /**
     * The {@link IZ80Processor.ExecuteNextInstruction} was invoked and finished normally
     * (the {@link IExecutionStopper.Stop} method was not invoked).
     */
    ExecuteNextInstructionInvoked,

    /**
     * A HALT instruction was encountered when interrupts were disabled,
     * and {@link IZ80Processor.AutoStopOnDiPlusHalt} property was set to true.
     */
    DiPlusHalt,

    /**
     * A RET instruction was encountered, the stack is empty, and the
     * {@link IZ80Processor.AutoStopOnRetWithStackEmpty} property was set to true.
     */
    RetWithStackEmpty,

    /**
     * An exception was thrown and not handled during he execution of the
     * {@link IZ80Processor.Start} method, the {@link IZ80Processor.Continue} method
     * or the {@link IZ80Processor.ExecuteNextInstruction} method.
     */
    ExceptionThrown
}
