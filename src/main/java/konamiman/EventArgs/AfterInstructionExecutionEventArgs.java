package konamiman.EventArgs;

import konamiman.DependenciesInterfaces.IExecutionStopper;


/**
 * Event args for the event triggered by the {@link IZ80Processor} class after an instruction is executed.
 */
public class AfterInstructionExecutionEventArgs extends ProcessorEventArgs {
    /**
     * Initializes a new instance of the class.
     *
     * @param opcode The opcode bytes of the instruction that has been executed.
     * @param stopper An instance of {@link IExecutionStopper} that can be used
     * by the event listener to request stop of the execution loop.
     * @param localUserState The state object from the matching {@link IZ80Processor.BeforeInstructionExecution} event.
     * @param tStates Total count of T states used for the instruction execution, including extra wait states
     */
    public AfterInstructionExecutionEventArgs(byte[] opcode, IExecutionStopper stopper, Object localUserState, int tStates)
    {
        super(localUserState);
        this.Opcode = opcode;
        this.ExecutionStopper = stopper;
        this.setLocalUserState(localUserState);
        this.TotalTStates = tStates;
    }

    /**
     * Contains the full opcode bytes of the instruction that has been executed.
     */
    private byte[] Opcode; public byte[] getOpcode() { return Opcode; } public void setOpcode(byte[] value) { Opcode = value; }

    /**
     * Contains the instance of {@link IExecutionStopper} that allows the event consumer
     * to ask termination of the processor execution.
     */
    private IExecutionStopper ExecutionStopper; public IExecutionStopper getExecutionStopper() { return ExecutionStopper; }

    /**
     * Contains the total count of T states required for the instruction execution, including
     * any extra wait states used for memory and ports access.
     */
    private int TotalTStates; public int getTotalTStates() { return TotalTStates; }
}
