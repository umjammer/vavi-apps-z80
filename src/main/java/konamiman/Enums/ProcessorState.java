package konamiman.Enums;

/**
 * Represents the current processor state
 */
public enum ProcessorState {
    /**
     * Not running. Either the processor has never been running since it was instantiated,
     * or the {@link IZ80Processor.Start} or {@link IZ80Processor.ExecuteNextInstruction}
     * methods were invoked and finished for any reason other than the invocation of {@link IExecutionStopper.Stop}
     * with <c>isPause=true</c>.
     */
    Stopped,

    /**
     * Not running. The {@link IZ80Processor.Start} method or the {@link IZ80Processor.ExecuteNextInstruction}
     * method was invoked and finished with an invocation of {@link IExecutionStopper.Stop}
     * with <c>isPause=true</c>.
     */
    Paused,

    /**
     * Running. The {@link IZ80Processor.Start} method was invoked and has not returned yet.
     */
    Running,

    /**
     * Executing only one instruction. The {@link IZ80Processor.ExecuteNextInstruction} methodwas invoked
     * and has not returned yet.
     */
    ExecutingOneInstruction
}
